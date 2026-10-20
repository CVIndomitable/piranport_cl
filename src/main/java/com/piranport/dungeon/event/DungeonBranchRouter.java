package com.piranport.dungeon.event;

import com.piranport.combat.TransformationManager;
import com.piranport.dungeon.data.BranchRule;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.event.BranchDoors.Door;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.instance.NodeBattleField;
import com.piranport.item.ShipCoreItem;
import com.piranport.npc.follower.DungeonFollowerManager;
import com.piranport.npc.shipgirl.ShipGirlEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 《副本/22》分歧门的服务端接线：玩家抵达门群时定格本群亮灭快照，走入亮门才写入选择并个人传送。
 *
 * <p>随从按 {@link DungeonFollowerManager} 中触发者在场且存活的随从计 escorts 与职能位；捆绑传送随从不做。</p>
 */
public final class DungeonBranchRouter {
    private DungeonBranchRouter() {}

    /** 门的重触发冷却（见 routeFromPortal）。键为玩家 UUID，值为服务端 tick。 */
    private static final Map<UUID, Long> LAST_ROUTE_TICK = new ConcurrentHashMap<>();
    private static final int ROUTE_COOLDOWN_TICKS = 40;

    /** 构造玩家当前的分歧上下文（舰体取变身中的舰装核心，随从取本副本实例内的在场随从）。 */
    static BranchEvaluator.Context contextFor(ServerPlayer player, DungeonInstance instance) {
        String hull = null;
        ItemStack core = TransformationManager.findTransformedCore(player);
        if (!core.isEmpty() && core.getItem() instanceof ShipCoreItem shipCore) {
            hull = shipCore.getShipType().name().toLowerCase(Locale.ROOT);
        }
        List<ShipGirlEntity> followers =
                DungeonFollowerManager.getFollowers(instance.getInstanceId(), player.getUUID());
        Set<String> roles = new HashSet<>();
        for (ShipGirlEntity follower : followers) {
            roles.addAll(follower.getFollowerRoles());
        }
        return new BranchEvaluator.Context(hull, followers.size(), roles,
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
     * 玩家首次抵达分歧门群时定格本群亮灭；之后同群门只读这份快照，走近别的门不会重掷概率。
     */
    public static void reachGroup(ServerPlayer player, DungeonInstance instance, NodeData fromNode) {
        if (fromNode == null || !fromNode.hasBranches()) return;
        doorsFor(player, instance, fromNode);
    }

    private static List<Door> doorsFor(ServerPlayer player, DungeonInstance instance, NodeData fromNode) {
        List<Door> doors = instance.getBranchDoors(player.getUUID(), fromNode.nodeId());
        if (doors == null) {
            String chosen = instance.getBranchChoice(player.getUUID(), fromNode.nodeId());
            doors = BranchDoors.compute(fromNode, contextFor(player, instance),
                    instance.getClearedNodes()::contains, chosen);
            instance.setBranchDoors(player.getUUID(), fromNode.nodeId(), doors);
        }
        return doors;
    }

    /**
     * 玩家从已清分歧节点的门走入：按定格快照处理。亮门写入选择并送往目标节点；
     * 暗门与隐藏门原地提示、不传送；已走过的门（即已选的目标）允许回走。
     *
     * @param targetNodeId 玩家走入的那扇门通往的节点；空串（旧存档单门）按 branchDefault 处理
     * @return true 表示已接管（传送、排队或拒绝），调用方不再回讲台
     */
    public static boolean routeFromPortal(ServerPlayer player, DungeonInstance instance, StageData stage,
                                          NodeData fromNode, String targetNodeId) {
        // 传送门实体每 10 tick 扫一次玩家；enterNode 未消费（地形未就绪/费用不足等未传送）
        // 时会反复触发带路与提示。给每个玩家加冷却，避免每 10 tick 刷屏。
        long now = player.server.getTickCount();
        Long last = LAST_ROUTE_TICK.get(player.getUUID());
        if (last != null && now - last < ROUTE_COOLDOWN_TICKS) return true;
        LAST_ROUTE_TICK.put(player.getUUID(), now);

        String target = targetNodeId == null || targetNodeId.isBlank() ? fromNode.branchDefault() : targetNodeId;
        if (target == null) return false;
        Door door = findDoor(doorsFor(player, instance, fromNode), target);
        if (door == null) return false;

        switch (door.state()) {
            case HIDDEN -> {
                player.displayClientMessage(Component.translatable("dungeon.piranport.branch.closed"), true);
                return true;
            }
            case DARK -> {
                notifyDark(player, fromNode, door);
                return true;
            }
            case LIT -> choose(player, instance, fromNode, target);
            case PASSED -> { }
        }

        NodeData targetNode = stage.nodes().get(target);
        ServerLevel dungeonLevel = DungeonEventHandler.getDungeonLevel(player.server);
        if (targetNode == null || dungeonLevel == null) return false;
        ItemStack key = DungeonEventHandler.readKeyFromLectern(instance, player.server);
        if (key.isEmpty()) return false;

        // 推进规则（如别人正在打的节点未清）不允许时退回讲台
        if (!DungeonEntryRules.canEnter(instance, stage, target)) return false;

        if (!instance.hasEnteredNode(target)
                && !NodeBattleField.isTerrainReady(dungeonLevel, instance, targetNode)) {
            // 地形未就绪：不退本，原地推进建造；传送门每 10 tick 会再次调用本方法。
            NodeBattleField.tickTerrain(dungeonLevel, instance, targetNode);
            player.displayClientMessage(Component.translatable(
                    "dungeon.piranport.advance_building", target), true);
            return true;
        }
        DungeonNodeRouter.enterNode(player.serverLevel(), instance, targetNode, stage, player, key);
        DungeonEventHandler.markLecternChanged(instance, player.server);
        // enterNode 某些分支（过路费不足等）不传送：让玩家继续站在门里，冷却结束后再试。
        return true;
    }

    private static Door findDoor(List<Door> doors, String target) {
        for (Door door : doors) {
            if (door.target().equals(target)) return door;
        }
        return null;
    }

    private static void choose(ServerPlayer player, DungeonInstance instance, NodeData fromNode, String target) {
        instance.setBranchChoice(player.getUUID(), fromNode.nodeId(), target);
        instance.setBranchDoors(player.getUUID(), fromNode.nodeId(), BranchDoors.compute(
                fromNode, contextFor(player, instance), instance.getClearedNodes()::contains, target));
        DungeonInstanceManager.get(player.serverLevel()).setDirty();
        player.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    /** 暗门：已清目标只提示“已清”；条件未满足则标题列出缺的条件。 */
    private static void notifyDark(ServerPlayer player, NodeData fromNode, Door door) {
        if (door.unmetMask() == 0 || door.ruleIndex() < 0) {
            player.displayClientMessage(Component.translatable("dungeon.piranport.branch.target_cleared"), true);
            return;
        }
        BranchRule rule = fromNode.branches().get(door.ruleIndex());
        List<Component> missing = new ArrayList<>();
        for (BranchDoorText.Line line : BranchDoorText.conditions(rule)) {
            if ((door.unmetMask() & line.bit()) != 0) missing.add(line.text());
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 10));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("dungeon.piranport.branch.dark_title",
                        BranchDoorText.join(missing)).withStyle(ChatFormatting.RED)));
    }
}
