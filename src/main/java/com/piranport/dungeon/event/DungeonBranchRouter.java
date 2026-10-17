package com.piranport.dungeon.event;

import com.piranport.combat.TransformationManager;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.instance.NodeBattleField;
import com.piranport.item.ShipCoreItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 《副本/22》分歧带路的服务端接线：取玩家上下文 → {@link BranchEvaluator} 判定 →
 * 快照到 {@link DungeonInstance#setBranchChoice} → 聊天/标题反馈 → 个人传送。
 *
 * <p>随从系统未开发：escorts 按 0、role 按“无”判定；捆绑传送随从不做。</p>
 */
public final class DungeonBranchRouter {
    private DungeonBranchRouter() {}

    /** 分歧带路的每玩家重触发冷却（见 routeFromPortal）。键为玩家 UUID，值为服务端 tick。 */
    private static final Map<UUID, Long> LAST_ROUTE_TICK = new ConcurrentHashMap<>();

    /** 构造玩家当前的分歧上下文（舰体取变身中的舰装核心）。 */
    static BranchEvaluator.Context contextFor(ServerPlayer player) {
        String hull = null;
        ItemStack core = TransformationManager.findTransformedCore(player);
        if (!core.isEmpty() && core.getItem() instanceof ShipCoreItem shipCore) {
            hull = shipCore.getShipType().name().toLowerCase(Locale.ROOT);
        }
        return new BranchEvaluator.Context(hull, 0, Set.of(),
                itemId -> carries(player, itemId), () -> player.getRandom().nextDouble());
    }

    private static boolean carries(ServerPlayer player, String itemId) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 取（必要时生成）玩家在分歧点 {@code fromNode} 的快照选择。已有快照直接返回、不重掷概率。
     * 非分歧节点返回 null。
     */
    public static String resolveChoice(ServerPlayer player, DungeonInstance instance, NodeData fromNode) {
        if (fromNode == null || !fromNode.hasBranches()) return null;
        String existing = instance.getBranchChoice(player.getUUID(), fromNode.nodeId());
        if (existing != null) return existing;
        BranchEvaluator.Decision decision = BranchEvaluator.evaluate(
                fromNode.branches(), fromNode.branchDefault(), contextFor(player));
        if (decision.target() == null) return null;
        instance.setBranchChoice(player.getUUID(), fromNode.nodeId(), decision.target());
        DungeonInstanceManager.get(player.serverLevel()).setDirty();
        announce(player, fromNode, decision);
        return decision.target();
    }

    /** UX：聊天逐条列出条件与满足状态，标题显示去向；被“沟”到默认路线额外提示 + 低音。 */
    private static void announce(ServerPlayer player, NodeData fromNode, BranchEvaluator.Decision decision) {
        player.sendSystemMessage(Component.translatable("dungeon.piranport.branch.header", fromNode.nodeId())
                .withStyle(ChatFormatting.GOLD));
        int idx = 1;
        for (BranchEvaluator.RuleResult rr : decision.results()) {
            MutableComponent line = Component.literal(" " + idx++ + ". → " + rr.rule().to() + "  ");
            for (BranchEvaluator.ConditionResult c : rr.conditions()) {
                line.append(Component.literal((c.met() ? "✔ " : "✘ ") + c.label() + "  ")
                        .withStyle(c.met() ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            if (rr.conditions().isEmpty()) {
                line.append(Component.translatable("dungeon.piranport.branch.no_condition")
                        .withStyle(ChatFormatting.GRAY));
            }
            player.sendSystemMessage(line);
        }
        Component title;
        Component subtitle;
        if (decision.diverted()) {
            player.sendSystemMessage(Component.translatable("dungeon.piranport.branch.diverted",
                    decision.target()).withStyle(ChatFormatting.YELLOW));
            title = Component.translatable("dungeon.piranport.branch.diverted_title").withStyle(ChatFormatting.YELLOW);
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0f, 0.6f);
        } else {
            player.sendSystemMessage(Component.translatable("dungeon.piranport.branch.matched",
                    decision.target()).withStyle(ChatFormatting.GREEN));
            title = Component.translatable("dungeon.piranport.branch.title").withStyle(ChatFormatting.AQUA);
            player.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0f, 1.2f);
        }
        subtitle = Component.translatable("dungeon.piranport.branch.subtitle", decision.target());
        player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }

    /**
     * 已清分歧节点的出口：快照判定后把该玩家个人送往目标节点。
     *
     * @return true 表示已接管（传送或排队），调用方不再回讲台
     */
    public static boolean routeFromPortal(ServerPlayer player, DungeonInstance instance, StageData stage,
                                          NodeData fromNode) {
        // 传送门实体每 10 tick 扫一次玩家；enterNode 未消费（地形未就绪/费用不足等未传送）
        // 时会反复触发带路广播与排队。给每个玩家加 40 tick 重触发冷却，避免每 10 tick 刷屏。
        long now = player.server.getTickCount();
        Long last = LAST_ROUTE_TICK.get(player.getUUID());
        if (last != null && now - last < 40) return true; // 视为已接管，阻止调用方回讲台连跳
        LAST_ROUTE_TICK.put(player.getUUID(), now);

        String target = resolveChoice(player, instance, fromNode);
        if (target == null) return false;
        NodeData targetNode = stage.nodes().get(target);
        ServerLevel dungeonLevel = DungeonEventHandler.getDungeonLevel(player.server);
        if (targetNode == null || dungeonLevel == null) return false;
        ItemStack key = DungeonEventHandler.readKeyFromLectern(instance, player.server);
        if (key.isEmpty()) return false; // 没有讲台钥匙就退回讲台，由书页 UI 正常进入（canEnter 会尊重快照）

        // 推进规则（如别人正在打的节点未清）不允许时退回讲台
        if (!DungeonEntryRules.canEnter(instance, stage, target)) return false;

        if (!instance.hasEnteredNode(target)
                && !NodeBattleField.isTerrainReady(dungeonLevel, instance, targetNode)) {
            // 地形未就绪：不退本，原地推进建造；传送门每 10 tick 会再次调用本方法。
            NodeBattleField.tickTerrain(dungeonLevel, instance, targetNode);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "dungeon.piranport.advance_building", target), true);
            return true;
        }
        DungeonNodeRouter.enterNode(player.serverLevel(), instance, targetNode, stage, player, key);
        DungeonEventHandler.markLecternChanged(instance, player.server);
        // enterNode 某些分支（过路费不足等）不传送：让玩家继续站在门里，40 tick 后由
        // routeFromPortal 顶部的冷却逻辑重试，而不是直接送回讲台。
        return true;
    }
}
