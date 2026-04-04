package com.piranport.dungeon.script;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.entity.DungeonPortalEntity;
import com.piranport.dungeon.entity.LootShipEntity;
import com.piranport.dungeon.event.DungeonEventHandler;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.entity.DungeonTransportPlaneEntity;
import com.piranport.entity.LowTierDestroyerEntity;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Scripted 3-phase sequence for the artillery-introduction dungeon node.
 *
 * Phase 1 (AIRDROP): Transport plane flies in and drops a supply crate.
 *   - Title/subtitle shows stage name
 *   - Action bar shows narration text
 *   - Crate falls to water, then control prompt appears
 *
 * Phase 2 (LOOTING): Players pick up supplies. Advances when:
 *   - Any player is >20 blocks from crate, OR
 *   - All players have opened the crate, OR
 *   - 60 seconds elapsed
 *
 * Phase 3 (BATTLE): Destroyers spawn based on player ship type.
 *   - Large → 6, Medium → 4, Small/Submarine → 3 per player
 *   - When all destroyed → portal spawns at last kill location
 */
public class ArtilleryIntroScript implements DungeonScript {

    private enum Phase { AIRDROP, LOOTING, BATTLE, COMPLETED }

    private final UUID instanceId;
    private final String nodeId;
    private final String stageDisplayName;
    private final List<UUID> playerUuids;
    private final BlockPos spawnPos;

    private Phase phase = Phase.AIRDROP;
    private int tickCounter = 0;

    // Phase 1 state
    private DungeonTransportPlaneEntity transportPlane;
    private LootShipEntity airdropCrate;
    private BlockPos crateDropPos;
    private boolean crateLanded = false;
    private boolean titleSent = false;

    // Phase 2 state
    private int lootingTicks = 0;

    // Phase 3 state
    private int totalDestroyers = 0;
    private int destroyersKilled = 0;
    private BlockPos lastDestroyerDeathPos;
    private boolean portalSpawned = false;
    private int battleStartTick = -1; // tick when battle phase began

    private boolean finished = false;

    public ArtilleryIntroScript(DungeonInstance instance, String nodeId,
                                 String stageDisplayName, List<UUID> playerUuids) {
        this.instanceId = instance.getInstanceId();
        this.nodeId = nodeId;
        this.stageDisplayName = stageDisplayName;
        this.playerUuids = List.copyOf(playerUuids);
        this.spawnPos = instance.getNodeSpawnPos(nodeId);

        // Pick a random drop point 30-50 blocks from spawn
        Random rng = new Random();
        double angle = rng.nextDouble() * Math.PI * 2;
        double distance = 30 + rng.nextDouble() * 20;
        this.crateDropPos = spawnPos.offset(
                (int) (Math.cos(angle) * distance), 0,
                (int) (Math.sin(angle) * distance));
    }

    @Override
    public void tick(ServerLevel dungeonLevel) {
        tickCounter++;
        switch (phase) {
            case AIRDROP -> tickAirdrop(dungeonLevel);
            case LOOTING -> tickLooting(dungeonLevel);
            case BATTLE -> tickBattle(dungeonLevel);
            case COMPLETED -> {} // no-op
        }
    }

    // ========== Phase 1: Airdrop ==========

    private void tickAirdrop(ServerLevel level) {
        // Tick 1: send title animation + spawn transport plane
        if (tickCounter == 1) {
            sendTitleToAll(level);
            spawnTransportPlane(level);
            sendActionBar(level, Component.translatable("dungeon.piranport.artillery_intro.incoming"));
        }

        // Check if crate has landed
        if (crateLanded) {
            sendActionBar(level, Component.translatable("dungeon.piranport.artillery_intro.crate_landed"));
            phase = Phase.LOOTING;
            lootingTicks = 0;
            PiranPort.LOGGER.info("[ArtilleryIntro] Phase 1 → 2 (crate landed), instance={}", instanceId);
        }
    }

    private void sendTitleToAll(ServerLevel level) {
        if (titleSent) return;
        titleSent = true;

        for (ServerPlayer player : getOnlinePlayers(level)) {
            // Animation: fade in 20 ticks (1s), stay 40 ticks (2s), fade out 20 ticks (1s)
            player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 40, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal(stageDisplayName).withStyle(
                            net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.translatable("dungeon.piranport.artillery_intro.subtitle")
                            .withStyle(net.minecraft.ChatFormatting.YELLOW)));
        }
    }

    private void spawnTransportPlane(ServerLevel level) {
        // Plane spawns 60 blocks behind spawn point (opposite side of drop point)
        double dx = crateDropPos.getX() - spawnPos.getX();
        double dz = crateDropPos.getZ() - spawnPos.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1) len = 1;
        double startX = spawnPos.getX() - (dx / len) * 60;
        double startZ = spawnPos.getZ() - (dz / len) * 60;

        transportPlane = DungeonTransportPlaneEntity.create(level,
                startX, startZ,
                crateDropPos.getX() + 0.5, crateDropPos.getZ() + 0.5,
                () -> dropCrate(level));

        level.addFreshEntity(transportPlane);
    }

    private void dropCrate(ServerLevel level) {
        // Create supply crate at transport plane's current position (high up), set dropping
        airdropCrate = LootShipEntity.create(level,
                crateDropPos.getX() + 0.5,
                DungeonConstants.SPAWN_Y + 15,
                crateDropPos.getZ() + 0.5,
                0);
        airdropCrate.setDropping(true);
        airdropCrate.setOnLanded(() -> {
            crateLanded = true;
            // Fill crate with supplies for each player
            fillCrateSupplies(level);
        });

        level.addFreshEntity(airdropCrate);
        sendActionBar(level, Component.translatable("dungeon.piranport.artillery_intro.crate_dropping"));
    }

    private void fillCrateSupplies(ServerLevel level) {
        List<ItemStack> items = new ArrayList<>();
        for (UUID uuid : playerUuids) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player == null) continue;

            ShipCoreItem.ShipType shipType = getPlayerShipType(player);

            // Determine cannon and ammo based on ship type
            ItemStack cannon;
            ItemStack heAmmo;
            ItemStack apAmmo;
            switch (shipType) {
                case LARGE -> {
                    cannon = new ItemStack(ModItems.LARGE_GUN.get());
                    heAmmo = new ItemStack(ModItems.LARGE_HE_SHELL.get(), 32);
                    apAmmo = new ItemStack(ModItems.LARGE_AP_SHELL.get(), 32);
                }
                case MEDIUM -> {
                    cannon = new ItemStack(ModItems.MEDIUM_GUN.get());
                    heAmmo = new ItemStack(ModItems.MEDIUM_HE_SHELL.get(), 64);
                    apAmmo = new ItemStack(ModItems.MEDIUM_AP_SHELL.get(), 64);
                }
                default -> { // SMALL, SUBMARINE
                    cannon = new ItemStack(ModItems.SMALL_GUN.get());
                    heAmmo = new ItemStack(ModItems.SMALL_HE_SHELL.get(), 128);
                    apAmmo = new ItemStack(ModItems.SMALL_AP_SHELL.get(), 128);
                }
            }
            items.add(cannon);
            items.add(heAmmo);
            items.add(apAmmo);
        }

        if (airdropCrate != null) {
            airdropCrate.fillInventory(items);
        }
    }

    // ========== Phase 2: Looting ==========

    private void tickLooting(ServerLevel level) {
        lootingTicks++;

        // Condition 1: timeout
        if (lootingTicks >= DungeonConstants.ARTILLERY_INTRO_LOOTING_TIMEOUT) {
            startBattle(level, "timeout");
            return;
        }

        List<ServerPlayer> online = getOnlinePlayers(level);
        if (online.isEmpty()) return;

        // Condition 2: any player > 20 blocks from crate
        if (airdropCrate != null) {
            for (ServerPlayer player : online) {
                double dist = player.distanceToSqr(
                        airdropCrate.getX(), airdropCrate.getY(), airdropCrate.getZ());
                double threshold = DungeonConstants.ARTILLERY_INTRO_LEAVE_DISTANCE;
                if (dist > threshold * threshold) {
                    startBattle(level, "player_far");
                    return;
                }
            }

            // Condition 3: all players have opened the crate
            Set<UUID> opened = airdropCrate.getOpenedBy();
            boolean allOpened = true;
            for (UUID uuid : playerUuids) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
                if (player != null && DungeonEventHandler.isInDungeon(player)
                        && !opened.contains(uuid)) {
                    allOpened = false;
                    break;
                }
            }
            if (allOpened) {
                startBattle(level, "all_opened");
                return;
            }
        }

        // Periodic action bar reminder
        if (lootingTicks % 100 == 0) {
            int remaining = (DungeonConstants.ARTILLERY_INTRO_LOOTING_TIMEOUT - lootingTicks) / 20;
            sendActionBar(level, Component.translatable(
                    "dungeon.piranport.artillery_intro.looting", remaining));
        }
    }

    private void startBattle(ServerLevel level, String reason) {
        phase = Phase.BATTLE;
        battleStartTick = tickCounter;
        PiranPort.LOGGER.info("[ArtilleryIntro] Phase 2 → 3 ({}), instance={}", reason, instanceId);
        spawnDestroyers(level);
        sendActionBar(level, Component.translatable("dungeon.piranport.artillery_intro.battle_start"));
    }

    // ========== Phase 3: Battle ==========

    private void spawnDestroyers(ServerLevel level) {
        // Determine spawn count per player based on ship type
        List<ServerPlayer> online = getOnlinePlayers(level);
        totalDestroyers = 0;
        destroyersKilled = 0;

        // Enemy spawn point: opposite side of crate from player spawn
        double dx = crateDropPos.getX() - spawnPos.getX();
        double dz = crateDropPos.getZ() - spawnPos.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1) len = 1;
        // Spawn destroyers 40 blocks past the crate
        double enemyBaseX = crateDropPos.getX() + (dx / len) * 40;
        double enemyBaseZ = crateDropPos.getZ() + (dz / len) * 40;

        for (ServerPlayer player : online) {
            ShipCoreItem.ShipType shipType = getPlayerShipType(player);
            int count = switch (shipType) {
                case LARGE -> 6;
                case MEDIUM -> 4;
                default -> 3; // SMALL, SUBMARINE
            };
            totalDestroyers += count;

            for (int i = 0; i < count; i++) {
                var destroyer = new com.piranport.entity.LowTierDestroyerEntity(
                        ModEntityTypes.LOW_TIER_DESTROYER.get(), level);
                // Spread destroyers around enemy base
                double angle = Math.random() * Math.PI * 2;
                double spread = 5 + Math.random() * 15;
                double ex = enemyBaseX + Math.cos(angle) * spread;
                double ez = enemyBaseZ + Math.sin(angle) * spread;
                destroyer.setPos(ex, DungeonConstants.SPAWN_Y, ez);

                // Tag for tracking
                destroyer.addTag("dungeon_script");
                destroyer.addTag("dungeon_instance_" + instanceId);
                destroyer.addTag("dungeon_node_" + nodeId);

                level.addFreshEntity(destroyer);
            }
        }

        // If no destroyers were spawned (all players offline?), just complete
        if (totalDestroyers == 0) {
            spawnCompletionPortal(level);
        }

        PiranPort.LOGGER.info("[ArtilleryIntro] Spawned {} destroyers for {} players",
                totalDestroyers, online.size());
    }

    private void tickBattle(ServerLevel level) {
        // Battle progress is driven by onEntityDeath() calls.
        // Safety timeout: 10 minutes from battle start
        if (battleStartTick >= 0 && (tickCounter - battleStartTick) > 12000 && !portalSpawned) {
            PiranPort.LOGGER.warn("[ArtilleryIntro] Battle timeout, forcing portal spawn");
            spawnCompletionPortal(level);
        }
    }

    @Override
    public void onEntityDeath(Entity entity) {
        if (phase != Phase.BATTLE) return;
        if (!entity.getTags().contains("dungeon_script")) return;
        if (!entity.getTags().contains("dungeon_instance_" + instanceId)) return;

        destroyersKilled++;
        lastDestroyerDeathPos = entity.blockPosition();

        // Notify players of progress
        if (entity.level() instanceof ServerLevel sl) {
            int remaining = totalDestroyers - destroyersKilled;
            sendActionBar(sl, Component.translatable(
                    "dungeon.piranport.artillery_intro.kills",
                    destroyersKilled, totalDestroyers, remaining));

            if (remaining <= 0) {
                spawnCompletionPortal(sl);
            }
        }
    }

    private void spawnCompletionPortal(ServerLevel level) {
        if (portalSpawned) return;
        portalSpawned = true;

        BlockPos portalPos = lastDestroyerDeathPos != null
                ? lastDestroyerDeathPos
                : crateDropPos;

        DungeonPortalEntity portal = DungeonPortalEntity.create(level, instanceId, nodeId,
                portalPos.getX() + 0.5, DungeonConstants.SPAWN_Y, portalPos.getZ() + 0.5);
        level.addFreshEntity(portal);

        sendActionBar(level, Component.translatable("dungeon.piranport.artillery_intro.cleared"));
        phase = Phase.COMPLETED;
        finished = true;

        PiranPort.LOGGER.info("[ArtilleryIntro] All destroyers killed, portal spawned at {}",
                portalPos);
    }

    // ========== Utilities ==========

    @Override
    public boolean isFinished() {
        return finished;
    }

    private List<ServerPlayer> getOnlinePlayers(ServerLevel level) {
        List<ServerPlayer> result = new ArrayList<>();
        for (UUID uuid : playerUuids) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null && DungeonEventHandler.isInDungeon(player)) {
                result.add(player);
            }
        }
        return result;
    }

    private void sendActionBar(ServerLevel level, Component message) {
        for (ServerPlayer player : getOnlinePlayers(level)) {
            player.displayClientMessage(message, true);
        }
    }

    /**
     * Determine the player's current ship type. Falls back to SMALL if not transformed.
     */
    private ShipCoreItem.ShipType getPlayerShipType(ServerPlayer player) {
        ItemStack core = TransformationManager.findTransformedCore(player);
        if (!core.isEmpty() && core.getItem() instanceof ShipCoreItem sci) {
            return sci.getShipType();
        }
        // Not transformed — scan inventory for any ship core
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ShipCoreItem sci) {
                return sci.getShipType();
            }
        }
        return ShipCoreItem.ShipType.SMALL; // default
    }
}
