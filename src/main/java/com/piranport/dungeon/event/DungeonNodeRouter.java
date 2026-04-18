package com.piranport.dungeon.event;

import com.piranport.PiranPort;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.instance.NodeBattleField;
import com.piranport.dungeon.lobby.DungeonLobbyManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Routes node-entry decisions out of {@link DungeonEventHandler} so the event handler
 * can focus on lifecycle (death / logout / data reload).
 *
 * <p>Each handler is responsible for atomically claiming the node (via
 * {@link DungeonInstanceManager#advanceNode}) before dispensing rewards or deducting
 * costs, so a re-entrant SelectNodePayload cannot double-process.</p>
 */
public final class DungeonNodeRouter {
    private DungeonNodeRouter() {}

    /** Result returned by the battle setup helper. */
    public record BattleSetup(ServerLevel dungeonLevel, List<ServerPlayer> players, List<UUID> playerUuids) {}

    public static void enterNode(ServerLevel level, DungeonInstance instance,
                                  NodeData node, StageData stage,
                                  ServerPlayer flagship, ItemStack keyStack,
                                  DungeonLobbyManager.Lobby lobby) {
        switch (node.type()) {
            case RESOURCE -> handleResourceNode(instance, node, flagship, keyStack);
            case COST -> handleCostNode(instance, node, flagship, keyStack);
            case BATTLE, BOSS -> {
                if (node.script() != null && !node.script().isEmpty()) {
                    handleScriptedBattleNode(level, instance, node, stage,
                            flagship, keyStack, lobby);
                } else {
                    handleBattleNode(level, instance, node, stage,
                            flagship, keyStack, lobby);
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
        for (NodeData.RewardEntry reward : node.rewards()) {
            RewardDispatcher.give(player, reward, null);
        }
        player.sendSystemMessage(Component.translatable("dungeon.piranport.resource_collected"));
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
        if (!node.costMessage().isEmpty()) {
            player.sendSystemMessage(Component.literal(node.costMessage()));
        }
    }

    /**
     * Shared setup for battle nodes: validate dimension, advance progress, generate terrain,
     * gather and teleport players, close lobby. Returns null if the dungeon dimension is unavailable.
     */
    public static BattleSetup prepareBattleNode(ServerLevel level, DungeonInstance instance,
                                                  NodeData node, ServerPlayer flagship,
                                                  ItemStack keyStack, DungeonLobbyManager.Lobby lobby) {
        ServerLevel dungeonLevel = DungeonEventHandler.getDungeonLevel(level.getServer());
        if (dungeonLevel == null) {
            PiranPort.LOGGER.error("Dungeon dimension not found!");
            flagship.sendSystemMessage(Component.literal("Error: Dungeon dimension not available"));
            return null;
        }

        DungeonInstanceManager mgr = DungeonInstanceManager.get(level);
        mgr.advanceNode(instance.getInstanceId(), node.nodeId(), keyStack);

        NodeBattleField.generateTerrain(dungeonLevel, instance, node.nodeId());

        BlockPos spawn = instance.getNodeSpawnPos(node.nodeId());
        List<ServerPlayer> toTeleport = new ArrayList<>();
        List<UUID> playerUuids = new ArrayList<>();
        toTeleport.add(flagship);
        playerUuids.add(flagship.getUUID());

        if (lobby != null) {
            for (UUID memberUuid : lobby.getMemberUuids()) {
                if (memberUuid.equals(flagship.getUUID())) continue;
                ServerPlayer member = level.getServer().getPlayerList().getPlayer(memberUuid);
                if (member != null) {
                    toTeleport.add(member);
                    playerUuids.add(memberUuid);
                    instance.addPlayer(memberUuid);
                }
            }
        }

        for (ServerPlayer player : toTeleport) {
            ItemStack scroll = new ItemStack(ModItems.TOWN_SCROLL.get());
            if (!player.getInventory().add(scroll)) {
                player.drop(scroll, false);
            }
            player.teleportTo(dungeonLevel,
                    spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                    player.getYRot(), player.getXRot());

            StageData stageData = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
            String stageName = stageData != null ? stageData.displayName() : instance.getStageId();
            PacketDistributor.sendToPlayer(player,
                    new DungeonStatePayload(stageName, node.nodeId(),
                            instance.getStartTimeMillis()));
        }

        if (lobby != null) {
            DungeonLobbyManager.INSTANCE.removeLobby(lobby.getLecternPos());
        }

        return new BattleSetup(dungeonLevel, toTeleport, playerUuids);
    }

    private static void handleBattleNode(ServerLevel level, DungeonInstance instance,
                                          NodeData node, StageData stage,
                                          ServerPlayer flagship, ItemStack keyStack,
                                          DungeonLobbyManager.Lobby lobby) {
        BattleSetup setup = prepareBattleNode(level, instance, node, flagship, keyStack, lobby);
        if (setup == null) return;
        NodeBattleField.spawnEnemies(setup.dungeonLevel(), instance, node);
    }

    private static void handleScriptedBattleNode(ServerLevel level, DungeonInstance instance,
                                                   NodeData node, StageData stage,
                                                   ServerPlayer flagship, ItemStack keyStack,
                                                   DungeonLobbyManager.Lobby lobby) {
        BattleSetup setup = prepareBattleNode(level, instance, node, flagship, keyStack, lobby);
        if (setup == null) return;

        StageData stageData = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        String displayName = stageData != null ? stageData.displayName() : instance.getStageId();

        if ("artillery_intro".equals(node.script())) {
            var script = new com.piranport.dungeon.script.ArtilleryIntroScript(
                    instance, node.nodeId(), displayName, setup.playerUuids());
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
