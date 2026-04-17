package com.piranport.dungeon.script;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.entity.DungeonPortalEntity;
import com.piranport.dungeon.entity.LootShipEntity;
import com.piranport.dungeon.event.DungeonEventHandler;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.entity.DungeonTransportPlaneEntity;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
 *
 * <p>Persisted via {@link DungeonScriptManager}. The transient entity references
 * (transport plane and crate) are stored as UUIDs and re-resolved each tick so the
 * script can survive a server restart mid-phase.</p>
 */
public class ArtilleryIntroScript implements DungeonScript {

    public static final String TYPE_ID = "artillery_intro";

    private enum Phase { AIRDROP, LOOTING, BATTLE, COMPLETED }

    private final UUID instanceId;
    private final String nodeId;
    private final String stageDisplayName;
    private final List<UUID> playerUuids;
    private final BlockPos spawnPos;
    private final BlockPos crateDropPos;

    private Phase phase = Phase.AIRDROP;
    private int tickCounter = 0;

    // Phase 1 state — entities are looked up from UUIDs each tick after reload
    private UUID transportPlaneUuid;
    private UUID airdropCrateUuid;
    private boolean crateLanded = false;
    private boolean titleSent = false;
    private boolean crateSpawned = false;

    // Phase 2 state
    private int lootingTicks = 0;

    // Phase 3 state
    private int totalDestroyers = 0;
    private int destroyersKilled = 0;
    private BlockPos lastDestroyerDeathPos;
    private boolean portalSpawned = false;
    private int battleStartTick = -1;

    private boolean finished = false;

    public ArtilleryIntroScript(DungeonInstance instance, String nodeId,
                                 String stageDisplayName, List<UUID> playerUuids) {
        this.instanceId = instance.getInstanceId();
        this.nodeId = nodeId;
        this.stageDisplayName = stageDisplayName;
        this.playerUuids = List.copyOf(playerUuids);
        this.spawnPos = instance.getNodeSpawnPos(nodeId);

        // Pick a random drop point 30-50 blocks from spawn. Use a per-instance Random
        // so dungeon generation stays deterministic-ish without touching level RNG here.
        java.util.Random rng = new java.util.Random(instanceId.getLeastSignificantBits()
                ^ instanceId.getMostSignificantBits() ^ nodeId.hashCode());
        double angle = rng.nextDouble() * Math.PI * 2;
        double distance = 30 + rng.nextDouble() * 20;
        this.crateDropPos = spawnPos.offset(
                (int) (Math.cos(angle) * distance), 0,
                (int) (Math.sin(angle) * distance));
    }

    /** NBT-load constructor used by {@link DungeonScriptRegistry}. */
    private ArtilleryIntroScript(CompoundTag tag) {
        this.instanceId = NbtUtils.loadUUID(tag.get("InstanceId"));
        this.nodeId = tag.getString("NodeId");
        this.stageDisplayName = tag.getString("StageDisplayName");

        List<UUID> players = new ArrayList<>();
        ListTag plist = tag.getList("PlayerUuids", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < plist.size(); i++) {
            players.add(NbtUtils.loadUUID(plist.get(i)));
        }
        this.playerUuids = List.copyOf(players);

        this.spawnPos = NbtUtils.readBlockPos(tag, "SpawnPos").orElse(BlockPos.ZERO);
        this.crateDropPos = NbtUtils.readBlockPos(tag, "CrateDropPos").orElse(spawnPos);

        try {
            this.phase = Phase.valueOf(tag.getString("Phase"));
        } catch (IllegalArgumentException e) {
            this.phase = Phase.AIRDROP;
        }
        this.tickCounter = tag.getInt("TickCounter");
        if (tag.hasUUID("TransportPlaneUuid")) {
            this.transportPlaneUuid = tag.getUUID("TransportPlaneUuid");
        }
        if (tag.hasUUID("AirdropCrateUuid")) {
            this.airdropCrateUuid = tag.getUUID("AirdropCrateUuid");
        }
        this.crateLanded = tag.getBoolean("CrateLanded");
        this.titleSent = tag.getBoolean("TitleSent");
        this.crateSpawned = tag.getBoolean("CrateSpawned");
        this.lootingTicks = tag.getInt("LootingTicks");
        this.totalDestroyers = tag.getInt("TotalDestroyers");
        this.destroyersKilled = tag.getInt("DestroyersKilled");
        if (tag.contains("LastDestroyerDeathPos")) {
            NbtUtils.readBlockPos(tag, "LastDestroyerDeathPos").ifPresent(p -> this.lastDestroyerDeathPos = p);
        }
        this.portalSpawned = tag.getBoolean("PortalSpawned");
        this.battleStartTick = tag.contains("BattleStartTick") ? tag.getInt("BattleStartTick") : -1;
        this.finished = tag.getBoolean("Finished");
    }

    public static ArtilleryIntroScript loadFromNbt(CompoundTag tag) {
        return new ArtilleryIntroScript(tag);
    }

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public void writeNbt(CompoundTag tag) {
        tag.put("InstanceId", NbtUtils.createUUID(instanceId));
        tag.putString("NodeId", nodeId);
        tag.putString("StageDisplayName", stageDisplayName);

        ListTag plist = new ListTag();
        for (UUID u : playerUuids) {
            plist.add(NbtUtils.createUUID(u));
        }
        tag.put("PlayerUuids", plist);

        tag.put("SpawnPos", NbtUtils.writeBlockPos(spawnPos));
        tag.put("CrateDropPos", NbtUtils.writeBlockPos(crateDropPos));
        tag.putString("Phase", phase.name());
        tag.putInt("TickCounter", tickCounter);
        if (transportPlaneUuid != null) tag.putUUID("TransportPlaneUuid", transportPlaneUuid);
        if (airdropCrateUuid != null) tag.putUUID("AirdropCrateUuid", airdropCrateUuid);
        tag.putBoolean("CrateLanded", crateLanded);
        tag.putBoolean("TitleSent", titleSent);
        tag.putBoolean("CrateSpawned", crateSpawned);
        tag.putInt("LootingTicks", lootingTicks);
        tag.putInt("TotalDestroyers", totalDestroyers);
        tag.putInt("DestroyersKilled", destroyersKilled);
        if (lastDestroyerDeathPos != null) {
            tag.put("LastDestroyerDeathPos", NbtUtils.writeBlockPos(lastDestroyerDeathPos));
        }
        tag.putBoolean("PortalSpawned", portalSpawned);
        tag.putInt("BattleStartTick", battleStartTick);
        tag.putBoolean("Finished", finished);
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
        // Initial setup (tick 1 OR first tick after reload if nothing was spawned yet)
        if (!titleSent) {
            sendTitleToAll(level);
            sendActionBar(level, Component.translatable("dungeon.piranport.artillery_intro.incoming"));
        }
        if (transportPlaneUuid == null && !crateSpawned) {
            spawnTransportPlane(level);
        }

        // Resolve current entity references (post-reload safe)
        Entity plane = transportPlaneUuid != null ? level.getEntity(transportPlaneUuid) : null;
        if (plane != null && !plane.isAlive()) plane = null;
        LootShipEntity crate = airdropCrateUuid != null
                && level.getEntity(airdropCrateUuid) instanceof LootShipEntity c ? c : null;
        if (crate != null && !crate.isAlive()) crate = null;

        // If the plane has reached its drop point but we never spawned a crate
        // (e.g. server restarted exactly between drop and crate spawn), do it now.
        if (!crateSpawned) {
            boolean planeDropped = plane instanceof DungeonTransportPlaneEntity dp && dp.hasDropped();
            boolean planeGone = plane == null && transportPlaneUuid != null;
            if (planeDropped || planeGone) {
                dropCrate(level);
                crate = airdropCrateUuid != null
                        && level.getEntity(airdropCrateUuid) instanceof LootShipEntity c ? c : null;
            }
        }

        // Detect landing without relying on the (non-persisted) callback.
        if (!crateLanded && crate != null && !crate.isDropping()) {
            crateLanded = true;
            // Ensure inventory is filled (in case reload skipped the callback)
            fillCrateSupplies(level, crate);
        }

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
                            ChatFormatting.GOLD, ChatFormatting.BOLD)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.translatable("dungeon.piranport.artillery_intro.subtitle")
                            .withStyle(ChatFormatting.YELLOW)));
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

        DungeonTransportPlaneEntity plane = DungeonTransportPlaneEntity.create(level,
                startX, startZ,
                crateDropPos.getX() + 0.5, crateDropPos.getZ() + 0.5,
                () -> dropCrate(level));

        level.addFreshEntity(plane);
        transportPlaneUuid = plane.getUUID();
    }

    private void dropCrate(ServerLevel level) {
        if (crateSpawned) return; // idempotent — survives reload + plane callback re-fire
        // Create supply crate at transport plane's current position (high up), set dropping
        LootShipEntity crate = LootShipEntity.create(level,
                crateDropPos.getX() + 0.5,
                DungeonConstants.SPAWN_Y + 15,
                crateDropPos.getZ() + 0.5,
                0);
        crate.setDropping(true);
        crate.setOnLanded(() -> {
            crateLanded = true;
            fillCrateSupplies(level, crate);
        });

        level.addFreshEntity(crate);
        airdropCrateUuid = crate.getUUID();
        crateSpawned = true;
        sendActionBar(level, Component.translatable("dungeon.piranport.artillery_intro.crate_dropping"));
    }

    private void fillCrateSupplies(ServerLevel level, LootShipEntity crate) {
        if (crate == null) return;
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
        crate.fillInventory(items);
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

        // Resolve crate (UUID lookup, post-reload safe)
        LootShipEntity crate = airdropCrateUuid != null
                && level.getEntity(airdropCrateUuid) instanceof LootShipEntity c ? c : null;

        // If crate disappeared (despawned/destroyed after reload), advance to battle
        if (crate == null) {
            startBattle(level, "crate_missing");
            return;
        }

        // Condition 2: any player > 20 blocks from crate
        for (ServerPlayer player : online) {
            double dist = player.distanceToSqr(crate.getX(), crate.getY(), crate.getZ());
            double threshold = DungeonConstants.ARTILLERY_INTRO_LEAVE_DISTANCE;
            if (dist > threshold * threshold) {
                startBattle(level, "player_far");
                return;
            }
        }

        // Condition 3: all players have opened the crate
        Set<UUID> opened = crate.getOpenedBy();
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
        List<ServerPlayer> online = getOnlinePlayers(level);
        totalDestroyers = 0;
        destroyersKilled = 0;

        // Enemy spawn point: opposite side of crate from player spawn
        double dx = crateDropPos.getX() - spawnPos.getX();
        double dz = crateDropPos.getZ() - spawnPos.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1) len = 1;
        double enemyBaseX = crateDropPos.getX() + (dx / len) * 40;
        double enemyBaseZ = crateDropPos.getZ() + (dz / len) * 40;

        var rng = level.getRandom();
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
                double angle = rng.nextDouble() * Math.PI * 2;
                double spread = 5 + rng.nextDouble() * 15;
                double ex = enemyBaseX + Math.cos(angle) * spread;
                double ez = enemyBaseZ + Math.sin(angle) * spread;
                destroyer.setPos(ex, DungeonConstants.SPAWN_Y, ez);

                destroyer.addTag("dungeon_script");
                destroyer.addTag("dungeon_instance_" + instanceId);
                destroyer.addTag("dungeon_node_" + nodeId);

                level.addFreshEntity(destroyer);
            }
        }

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
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ShipCoreItem sci) {
                return sci.getShipType();
            }
        }
        return ShipCoreItem.ShipType.SMALL;
    }
}
