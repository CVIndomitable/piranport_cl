package com.piranport.dungeon.event;

import com.piranport.PiranPort;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.entity.DungeonPortalEntity;
import com.piranport.dungeon.entity.LootShipEntity;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.instance.NodeBattleField;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import com.piranport.dungeon.key.FlagshipManager;
import com.piranport.dungeon.lobby.DungeonLobbyManager;
import com.piranport.dungeon.network.DungeonResultPayload;
import com.piranport.dungeon.network.DungeonStatePayload;
import com.piranport.dungeon.network.PlayerDiedInDungeonPayload;
import com.piranport.dungeon.saved.DungeonLeaderboard;
import com.piranport.dungeon.saved.DungeonSavedData;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Central event handler for the dungeon system.
 * Handles death, logout, data reload, and node transition logic.
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class DungeonEventHandler {

    public static final ResourceKey<Level> DUNGEON_DIMENSION =
            ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "dungeon"));

    /**
     * Gets the dungeon dimension level, or null if not available.
     */
    public static ServerLevel getDungeonLevel(MinecraftServer server) {
        return server.getLevel(DUNGEON_DIMENSION);
    }

    /**
     * Checks if a player is currently in the dungeon dimension.
     */
    public static boolean isInDungeon(ServerPlayer player) {
        return player.level().dimension().equals(DUNGEON_DIMENSION);
    }

    // ===== Event Listeners =====

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new com.piranport.dungeon.data.DungeonDataLoader());
        event.addListener(new com.piranport.npc.data.DeepOceanDataLoader());
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isInDungeon(player)) return;

        // Cancel death, keep inventory, add invulnerability frames to prevent re-damage before teleport
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        player.invulnerableTime = 40; // 2 seconds of damage immunity frames
        // Clear residual DOT effects (fire, poison, wither) to prevent re-death after teleport
        player.clearFire();
        player.removeAllEffects();

        // Find the player's instance and teleport to lectern
        DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) player.level());
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof DungeonKeyItem) {
                UUID instanceId = DungeonKeyItem.getInstanceId(stack);
                if (instanceId == null) continue;
                DungeonInstance instance = mgr.getInstance(instanceId);
                if (instance != null) {
                    teleportToLectern(player, instance);
                    break;
                }
            }
        }

        // Notify client to open revive screen
        PacketDistributor.sendToPlayer(player, new PlayerDiedInDungeonPayload());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isInDungeon(player)) return;

        // Check active instance and handle flagship transfer
        DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) player.level());
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof DungeonKeyItem) {
                UUID instanceId = DungeonKeyItem.getInstanceId(stack);
                if (instanceId == null) continue;
                DungeonInstance instance = mgr.getInstance(instanceId);
                if (instance != null) {
                    checkAndSuspendIfEmpty(player.server, instance);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        DungeonLobbyManager.INSTANCE.clearAll();
    }

    // ===== Node Entry Logic =====

    /**
     * Called when the flagship selects a node to enter.
     */
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
        // Give rewards directly
        for (NodeData.RewardEntry reward : node.rewards()) {
            if (reward.chance() < 1.0f && player.getRandom().nextFloat() > reward.chance()) continue;

            ResourceLocation itemId = ResourceLocation.tryParse(reward.item());
            if (itemId == null) continue;
            var itemType = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(itemId);
            if (itemType.isEmpty()) continue;

            ItemStack rewardStack = new ItemStack(itemType.get(), reward.count());
            if (!player.getInventory().add(rewardStack)) {
                player.drop(rewardStack, false);
            }
        }

        player.sendSystemMessage(Component.translatable("dungeon.piranport.resource_collected"));

        // Mark node cleared
        DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) player.level());
        mgr.advanceNode(instance.getInstanceId(), node.nodeId(), keyStack);
    }

    private static void handleCostNode(DungeonInstance instance, NodeData node,
                                        ServerPlayer player, ItemStack keyStack) {
        // Check costs
        for (NodeData.CostEntry cost : node.cost()) {
            ResourceLocation itemId = ResourceLocation.tryParse(cost.item());
            if (itemId == null) continue;
            var itemType = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(itemId);
            if (itemType.isEmpty()) continue;

            int count = countItem(player, itemType.get());
            if (count < cost.count()) {
                player.sendSystemMessage(Component.translatable("dungeon.piranport.insufficient_cost",
                        cost.count(), itemType.get().getDescription()));
                return;
            }
        }

        // Deduct costs
        for (NodeData.CostEntry cost : node.cost()) {
            ResourceLocation itemId = ResourceLocation.tryParse(cost.item());
            if (itemId == null) continue;
            var itemType = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(itemId);
            if (itemType.isEmpty()) continue;
            removeItems(player, itemType.get(), cost.count());
        }

        if (!node.costMessage().isEmpty()) {
            player.sendSystemMessage(Component.literal(node.costMessage()));
        }

        // Mark node cleared
        DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) player.level());
        mgr.advanceNode(instance.getInstanceId(), node.nodeId(), keyStack);
    }

    /**
     * Shared setup for battle nodes: validate dimension, advance progress, generate terrain,
     * gather and teleport players, close lobby. Returns the teleported players (and their UUIDs),
     * or null if the dungeon dimension is not available.
     */
    private record BattleSetupResult(ServerLevel dungeonLevel, List<ServerPlayer> players, List<UUID> playerUuids) {}

    private static BattleSetupResult prepareBattleNode(ServerLevel level, DungeonInstance instance,
                                                        NodeData node, ServerPlayer flagship,
                                                        ItemStack keyStack, DungeonLobbyManager.Lobby lobby) {
        ServerLevel dungeonLevel = getDungeonLevel(level.getServer());
        if (dungeonLevel == null) {
            PiranPort.LOGGER.error("Dungeon dimension not found!");
            flagship.sendSystemMessage(Component.literal("Error: Dungeon dimension not available"));
            return null;
        }

        // Update instance progress
        DungeonInstanceManager mgr = DungeonInstanceManager.get(level);
        mgr.advanceNode(instance.getInstanceId(), node.nodeId(), keyStack);

        // Generate battlefield
        NodeBattleField.generateTerrain(dungeonLevel, instance, node.nodeId());

        // Gather all lobby members
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

        // Teleport and sync
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

        // Close lobby after entering dungeon
        if (lobby != null) {
            DungeonLobbyManager.INSTANCE.removeLobby(lobby.getLecternPos());
        }

        return new BattleSetupResult(dungeonLevel, toTeleport, playerUuids);
    }

    private static void handleBattleNode(ServerLevel level, DungeonInstance instance,
                                          NodeData node, StageData stage,
                                          ServerPlayer flagship, ItemStack keyStack,
                                          DungeonLobbyManager.Lobby lobby) {
        BattleSetupResult setup = prepareBattleNode(level, instance, node, flagship, keyStack, lobby);
        if (setup == null) return;

        // Spawn enemies for standard battle
        NodeBattleField.spawnEnemies(setup.dungeonLevel(), instance, node);
    }

    /**
     * Handles battle nodes with a script field (e.g. "artillery_intro").
     * Teleports players to the battlefield first, then starts the script.
     */
    private static void handleScriptedBattleNode(ServerLevel level, DungeonInstance instance,
                                                   NodeData node, StageData stage,
                                                   ServerPlayer flagship, ItemStack keyStack,
                                                   DungeonLobbyManager.Lobby lobby) {
        BattleSetupResult setup = prepareBattleNode(level, instance, node, flagship, keyStack, lobby);
        if (setup == null) return;

        // Start the script
        StageData stageData = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        String displayName = stageData != null ? stageData.displayName() : instance.getStageId();

        if ("artillery_intro".equals(node.script())) {
            var script = new com.piranport.dungeon.script.ArtilleryIntroScript(
                    instance, node.nodeId(), displayName, setup.playerUuids());
            com.piranport.dungeon.script.DungeonScriptManager.get(level.getServer())
                    .start(instance.getInstanceId(), script);
        } else {
            PiranPort.LOGGER.warn("Unknown script: {}", node.script());
            // Fallback: spawn enemies normally
            NodeBattleField.spawnEnemies(setup.dungeonLevel(), instance, node);
        }
    }

    // ===== Portal Completion =====

    /**
     * Called when all players have entered the portal after clearing a node.
     */
    public static void onPortalComplete(ServerLevel dungeonLevel, DungeonInstance instance,
                                         String nodeId) {
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        if (stage == null) return;

        NodeData node = stage.nodes().get(nodeId);
        if (node == null) return;

        // Spawn loot ships for killed enemies (simplified: spawn one at portal location)
        // In a full implementation, this would track each killed enemy

        // Check if this was a boss node and all boss nodes are cleared
        if (node.type() == NodeData.NodeType.BOSS) {
            boolean allBossesCleared = stage.bossNodes().stream()
                    .allMatch(bossNode -> instance.getClearedNodes().contains(bossNode));

            if (allBossesCleared) {
                // Dungeon complete!
                completeDungeon(dungeonLevel, instance, stage);
                return;
            }
        }

        // Return players to the lectern to select next node via book
        teleportAllPlayersToLectern(dungeonLevel.getServer(), instance);
    }

    private static void completeDungeon(ServerLevel dungeonLevel, DungeonInstance instance,
                                         StageData stage) {
        MinecraftServer server = dungeonLevel.getServer();
        DungeonInstanceManager mgr = DungeonInstanceManager.get(dungeonLevel);
        DungeonSavedData savedData = DungeonSavedData.get(dungeonLevel);
        DungeonLeaderboard leaderboard = DungeonLeaderboard.get(dungeonLevel);

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - instance.getStartTimeMillis();
        mgr.completeInstance(instance.getInstanceId());

        // Process each player
        for (UUID playerUuid : instance.getPlayerUuids()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player == null) continue;

            boolean isFirstClear = !savedData.hasFirstCleared(stage.stageId(), playerUuid);

            List<String> rewardNames = new ArrayList<>();
            if (isFirstClear) {
                savedData.markFirstCleared(stage.stageId(), playerUuid);
                // Give first clear rewards
                for (NodeData.RewardEntry reward : stage.firstClearRewards()) {
                    ResourceLocation itemId = ResourceLocation.tryParse(reward.item());
                    if (itemId == null) continue;
                    var itemType = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(itemId);
                    if (itemType.isEmpty()) continue;

                    ItemStack rewardStack = new ItemStack(itemType.get(), reward.count());
                    rewardNames.add(rewardStack.getHoverName().getString() + " x" + reward.count());
                    if (!player.getInventory().add(rewardStack)) {
                        player.drop(rewardStack, false);
                    }
                }
            }

            // Submit to leaderboard
            leaderboard.submit(stage.stageId(), playerUuid,
                    player.getGameProfile().getName(), elapsed);

            // Teleport back
            teleportToLectern(player, instance);

            // Send result screen
            PacketDistributor.sendToPlayer(player,
                    new DungeonResultPayload(stage.displayName(), elapsed,
                            isFirstClear, rewardNames));
        }

        // Cleanup instance
        NodeBattleField.cleanupRegion(dungeonLevel, instance);
        mgr.cleanupInstance(instance.getInstanceId());
    }

    // ===== Utility Methods =====

    public static void teleportToLectern(ServerPlayer player, DungeonInstance instance) {
        BlockPos lecternPos = instance.getLecternPos();
        if (lecternPos == null) {
            // Fallback to world spawn
            lecternPos = player.server.overworld().getSharedSpawnPos();
        }

        String dimKey = instance.getLecternDimension();
        ServerLevel targetLevel;
        if (dimKey != null) {
            ResourceKey<Level> targetDim = ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.tryParse(dimKey));
            targetLevel = player.server.getLevel(targetDim);
        } else {
            targetLevel = player.server.overworld();
        }

        if (targetLevel == null) {
            targetLevel = player.server.overworld();
        }

        player.teleportTo(targetLevel,
                lecternPos.getX() + 0.5, lecternPos.getY() + 1, lecternPos.getZ() + 0.5,
                player.getYRot(), player.getXRot());
    }

    public static void teleportAllPlayersToLectern(MinecraftServer server,
                                                     DungeonInstance instance) {
        for (UUID playerUuid : instance.getPlayerUuids()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player != null && isInDungeon(player)) {
                teleportToLectern(player, instance);
            }
        }
    }

    public static void checkAndSuspendIfEmpty(MinecraftServer server,
                                                DungeonInstance instance) {
        ServerLevel dungeonLevel = getDungeonLevel(server);
        if (dungeonLevel == null) return;

        boolean anyOnline = false;
        for (UUID playerUuid : instance.getPlayerUuids()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player != null && isInDungeon(player)) {
                anyOnline = true;
                break;
            }
        }

        if (!anyOnline) {
            DungeonInstanceManager mgr = DungeonInstanceManager.get(dungeonLevel);
            mgr.suspendInstance(instance.getInstanceId());
        }
    }

    private static int countItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeItems(ServerPlayer player, net.minecraft.world.item.Item item,
                                     int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }
}
