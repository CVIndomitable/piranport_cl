package com.piranport.dungeon.event;

import com.piranport.PiranPort;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.instance.NodeBattleField;
import com.piranport.dungeon.network.DungeonStatePayload;
import com.piranport.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

/**
 * Routes node-entry decisions out of {@link DungeonEventHandler} so the event handler
 * can focus on lifecycle (death / logout / data reload).
 *
 * <p>Each handler is responsible for atomically claiming the node (via
 * {@link DungeonInstanceManager#advanceNode}) before dispensing rewards or deducting
 * costs, so a re-entrant SelectNodePayload cannot double-process.</p>
 *
 * <p>副本/00：玩家可分别进入同一副本，历史参与记录不构成强制传送队伍。</p>
 */
public final class DungeonNodeRouter {
    private DungeonNodeRouter() {}

    /** Result returned by the battle setup helper. */
    public record BattleSetup(ServerLevel dungeonLevel, List<ServerPlayer> players, List<UUID> playerUuids) {}

    public static void enterNode(ServerLevel level, DungeonInstance instance,
                                  NodeData node, StageData stage,
                                  ServerPlayer player, ItemStack keyStack) {
        if (instance.hasEnteredNode(node.nodeId())) {
            teleportToNode(player, instance, node.nodeId(), instance.getNodeSpawnPos(node.nodeId()), player.getYRot());
            return;
        }
        switch (node.type()) {
            case RESOURCE -> handleResourceNode(instance, node, player, keyStack);
            case COST -> handleCostNode(instance, node, player, keyStack);
            case BATTLE, BOSS -> {
                if (node.script() != null && !node.script().isEmpty()) {
                    handleScriptedBattleNode(level, instance, node, stage, player, keyStack);
                } else {
                    handleBattleNode(level, instance, node, stage, player, keyStack);
                }
            }
        }
    }

    private static void handleResourceNode(DungeonInstance instance, NodeData node,
                                            ServerPlayer player, ItemStack keyStack) {
        DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) player.level());
        if (!mgr.advanceNode(instance.getInstanceId(), node.nodeId(), keyStack)) {
            return;
        }
        // 奖励统一在传送门完成节点时发放，避免玩家刚进入资源节点退出后重复领取。
        player.sendSystemMessage(Component.translatable("dungeon.piranport.resource_collected"));
        prepareNonBattleTerrain(instance, node, player);
    }

    private static void handleCostNode(DungeonInstance instance, NodeData node,
                                        ServerPlayer player, ItemStack keyStack) {
        for (NodeData.CostEntry cost : node.cost()) {
            Item item = cost.resolvedItem();
            if (item == null) continue;
            int count = countItem(player, item);
            if (count < cost.count()) {
                player.sendSystemMessage(Component.translatable("dungeon.piranport.insufficient_cost",
                        cost.count(), item.getDescription()));
                return;
            }
        }
        DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) player.level());
        if (!mgr.advanceNode(instance.getInstanceId(), node.nodeId(), keyStack)) {
            return;
        }
        for (NodeData.CostEntry cost : node.cost()) {
            Item item = cost.resolvedItem();
            if (item == null) continue;
            removeItems(player, item, cost.count());
        }
        prepareNonBattleTerrain(instance, node, player);
        if (!node.costMessage().isEmpty()) {
            player.sendSystemMessage(Component.literal(node.costMessage()));
        }
    }

    /** 首次进入时生成战场；重返只传送当前玩家，不重复登记或生成。 */
    public static BattleSetup prepareBattleNode(ServerLevel level, DungeonInstance instance,
                                                  NodeData node, ServerPlayer player,
                                                  ItemStack keyStack) {
        ServerLevel dungeonLevel = DungeonEventHandler.getDungeonLevel(level.getServer());
        if (dungeonLevel == null) {
            PiranPort.LOGGER.error("Dungeon dimension not found!");
            player.sendSystemMessage(Component.literal("Error: Dungeon dimension not available"));
            return null;
        }

        if (instance.hasEnteredNode(node.nodeId())) {
            teleportToNode(player, instance, node.nodeId(), instance.getNodeSpawnPos(node.nodeId()), player.getYRot());
            return null;
        }
        DungeonInstanceManager mgr = DungeonInstanceManager.get(level);
        if (!mgr.beginBattleNode(instance.getInstanceId(), node.nodeId(), keyStack)) return null;

        NodeBattleField.generateTerrain(dungeonLevel, instance, node);

        // 玩家自行进入，不将历史参与者从主世界或其他副本强制传送过来。
        List<ServerPlayer> toTeleport = List.of(player);
        List<UUID> playerUuids = List.of(player.getUUID());
        teleportToNode(player, instance, node.nodeId(), instance.getNodeSpawnPos(node.nodeId()), player.getYRot());

        return new BattleSetup(dungeonLevel, toTeleport, playerUuids);
    }

    private static void prepareNonBattleTerrain(DungeonInstance instance, NodeData node, ServerPlayer player) {
        ServerLevel dungeonLevel = DungeonEventHandler.getDungeonLevel(player.server);
        if (dungeonLevel == null) return;
        NodeBattleField.generateTerrain(dungeonLevel, instance, node);
        teleportToNode(player, instance, node.nodeId(), instance.getNodeSpawnPos(node.nodeId()), player.getYRot());
    }

    /** 所有重返路径共用副本维度与 HUD 同步；不改变正在运行脚本的当前节点。 */
    public static void teleportToNode(ServerPlayer player, DungeonInstance instance,
                                     String nodeId, BlockPos spawn, float yaw) {
        ServerLevel dungeonLevel = DungeonEventHandler.getDungeonLevel(player.server);
        if (dungeonLevel == null) return;
        boolean entering = DungeonInstanceManager.get(player.serverLevel()).getInstanceForPlayer(player) != instance;
        if (entering) {
            ItemStack scroll = new ItemStack(ModItems.TOWN_SCROLL.get());
            if (!player.getInventory().add(scroll)) player.drop(scroll, false);
        }
        player.teleportTo(dungeonLevel, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                yaw, player.getXRot());
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        PacketDistributor.sendToPlayer(player, new DungeonStatePayload(
                stage == null ? instance.getStageId() : stage.displayName(), nodeId, instance.getStartTimeMillis()));
        DungeonInstanceManager.get(dungeonLevel).refreshPlayerPresence(player.server);
    }

    private static void handleBattleNode(ServerLevel level, DungeonInstance instance,
                                          NodeData node, StageData stage,
                                          ServerPlayer player, ItemStack keyStack) {
        BattleSetup setup = prepareBattleNode(level, instance, node, player, keyStack);
        if (setup == null) return; // 已通关节点，setup 返回 null
        NodeBattleField.spawnEnemies(setup.dungeonLevel(), instance, node);
    }

    private static void handleScriptedBattleNode(ServerLevel level, DungeonInstance instance,
                                                   NodeData node, StageData stage,
                                                   ServerPlayer player, ItemStack keyStack) {
        BattleSetup setup = prepareBattleNode(level, instance, node, player, keyStack);
        if (setup == null) return;

        StageData stageData = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        String displayName = stageData != null ? stageData.displayName() : instance.getStageId();

        if ("artillery_intro".equals(node.script())) {
                var script = new com.piranport.dungeon.script.ArtilleryIntroScript(
                        instance, node.nodeId(), displayName, setup.playerUuids());
                com.piranport.dungeon.script.DungeonScriptManager.get(level.getServer())
                        .start(instance.getInstanceId(), script);
            } else if ("goldencatcat_activity".equals(node.script())) {
                var script = new com.piranport.dungeon.script.GoldencatcatScript(
                        instance, node.nodeId(), setup.playerUuids());
                script.onStart();
                script.spawnEntities(setup.dungeonLevel());
                com.piranport.dungeon.script.DungeonScriptManager.get(level.getServer())
                        .start(instance.getInstanceId(), script);
            } else if ("boss_intro".equals(node.script())) {
                var script = new com.piranport.dungeon.script.BossIntroScript(
                        instance, node.nodeId(), displayName, setup.playerUuids(), node.enemies());
                com.piranport.dungeon.script.DungeonScriptManager.get(level.getServer())
                        .start(instance.getInstanceId(), script);
            } else {
                PiranPort.LOGGER.warn("Unknown script: {}", node.script());
                NodeBattleField.spawnEnemies(setup.dungeonLevel(), instance, node);
            }
    }

    private static int countItem(ServerPlayer player, Item item) {
        Inventory inv = player.getInventory();
        int count = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeItems(ServerPlayer player, Item item, int amount) {
        Inventory inv = player.getInventory();
        int remaining = amount;
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == item) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }
}
