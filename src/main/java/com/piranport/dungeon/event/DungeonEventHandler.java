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
        // Clear residual DOT effects only — preserve buffs from food / 装填加速 / 高速规避 etc.
        player.clearFire();
        java.util.List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> harmful =
                new java.util.ArrayList<>();
        for (var inst : player.getActiveEffects()) {
            if (inst.getEffect().value().getCategory()
                    == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                harmful.add(inst.getEffect());
            }
        }
        for (var h : harmful) player.removeEffect(h);

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

        // Always sweep the player's keys: a player may log out from the lectern
        // (overworld) after returning via town scroll, in which case the early
        // isInDungeon() guard previously left the instance permanently ACTIVE.
        DungeonInstanceManager mgr = DungeonInstanceManager.get(player.server.overworld());
        UUID leavingId = player.getUUID();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof DungeonKeyItem) {
                UUID instanceId = DungeonKeyItem.getInstanceId(stack);
                if (instanceId == null) continue;
                DungeonInstance instance = mgr.getInstance(instanceId);
                if (instance != null) {
                    // If the leaving player is the flagship, hand off to another
                    // online member so the dungeon can continue.
                    if (leavingId.equals(instance.getFlagshipUuid())) {
                        ServerPlayer next = pickNextFlagshipPlayer(player.server, instance, leavingId);
                        if (next != null) {
                            instance.setFlagshipUuid(next.getUUID());
                            mgr.setDirty();
                            // Stamp the instanceId onto the new flagship's key so they can
                            // continue selecting nodes (SelectNodePayload reads instanceId
                            // from the key stack).
                            int newKeySlot = FlagshipManager.findAnyKeySlot(next);
                            if (newKeySlot >= 0) {
                                ItemStack newKeyStack = next.getInventory().getItem(newKeySlot);
                                DungeonKeyItem.setInstanceId(newKeyStack, instanceId);
                                String stageId = DungeonKeyItem.getStageId(stack);
                                if (stageId != null && !stageId.isEmpty()) {
                                    newKeyStack.set(com.piranport.registry.ModDataComponents.DUNGEON_STAGE_ID.get(), stageId);
                                }
                            }
                            next.sendSystemMessage(Component.translatable(
                                    "dungeon.piranport.flagship_promoted"));
                        }
                    }
                    checkAndSuspendIfEmpty(player.server, instance);
                }
            }
        }
    }

    /** Find another online instance member to take over the flagship role. */
    private static ServerPlayer pickNextFlagshipPlayer(MinecraftServer server,
                                                         DungeonInstance instance, UUID leaving) {
        for (UUID uuid : instance.getPlayerUuids()) {
            if (uuid.equals(leaving)) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) return p;
        }
        return null;
    }

    @SubscribeEvent
    public static void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        DungeonLobbyManager.INSTANCE.clearAll();
    }

    // ===== Node Entry Logic — delegated to DungeonNodeRouter =====

    /** Called when the flagship selects a node to enter. */
    public static void enterNode(ServerLevel level, DungeonInstance instance,
                                   NodeData node, StageData stage,
                                   ServerPlayer flagship, ItemStack keyStack,
                                   DungeonLobbyManager.Lobby lobby) {
        DungeonNodeRouter.enterNode(level, instance, node, stage, flagship, keyStack, lobby);
    }

    // (Battle / Resource / Cost / Scripted handlers live in DungeonNodeRouter.)

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
                for (NodeData.RewardEntry reward : stage.firstClearRewards()) {
                    RewardDispatcher.give(player, reward, rewardNames);
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
        ServerLevel targetLevel = null;
        if (dimKey != null) {
            ResourceLocation parsed = ResourceLocation.tryParse(dimKey);
            if (parsed != null) {
                ResourceKey<Level> targetDim = ResourceKey.create(Registries.DIMENSION, parsed);
                targetLevel = player.server.getLevel(targetDim);
            }
            if (targetLevel == null) {
                PiranPort.LOGGER.warn(
                        "Could not resolve lectern dimension '{}' for instance {} — falling back to overworld",
                        dimKey, instance.getInstanceId());
            }
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

}
