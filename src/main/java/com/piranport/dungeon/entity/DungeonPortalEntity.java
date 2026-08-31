package com.piranport.dungeon.entity;

import com.piranport.PiranPort;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Portal entity spawned when a dungeon node's flagship is defeated.
 * All dungeon players must enter the portal to advance.
 */
public class DungeonPortalEntity extends Entity {
    private static final EntityDataAccessor<Integer> SPIN_TICK =
            SynchedEntityData.defineId(DungeonPortalEntity.class, EntityDataSerializers.INT);

    /** Auto-discard a portal that nobody touches in 10 minutes — prevents orphan portals
     *  if NBT was corrupted (instanceId == null) or instance was cleaned up early. */
    private static final int ORPHAN_DESPAWN_TICKS = 10 * 60 * 20;
    /** Defensive cap for persisted/observed portal entries. */
    private static final int MAX_ENTERED_PLAYERS = 64;
    private static final int MAX_NODE_ID_LENGTH = 128;

    private UUID instanceId;
    private String nodeId;
    private final Set<UUID> enteredPlayers = new HashSet<>();
    private int orphanTicks = 0;

    public DungeonPortalEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static DungeonPortalEntity create(ServerLevel level, UUID instanceId, String nodeId,
                                              double x, double y, double z) {
        if (instanceId == null || nodeId == null || nodeId.isBlank()
                || nodeId.length() > MAX_NODE_ID_LENGTH) {
            PiranPort.LOGGER.warn("Rejected invalid dungeon portal creation (instance={}, nodeLength={})",
                    instanceId, nodeId == null ? -1 : nodeId.length());
            return null;
        }
        DungeonPortalEntity portal = new DungeonPortalEntity(
                ModEntityTypes.DUNGEON_PORTAL.get(), level);
        portal.instanceId = instanceId;
        portal.nodeId = nodeId;
        portal.setPos(x, y, z);
        return portal;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SPIN_TICK, 0);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            // Client-side: spinning visual + particles (use tickCount directly, no SynchedEntityData write)
            if (tickCount % 4 == 0) {
                double angle = tickCount * 0.1;
                level().addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL,
                        getX() + Math.cos(angle) * 1.5,
                        getY() + 1.0 + Math.sin(tickCount * 0.05) * 0.3,
                        getZ() + Math.sin(angle) * 1.5,
                        0, 0.05, 0);
            }
            return;
        }

        // Despawn orphan portals (broken NBT / cleaned-up instance) to avoid
        // leaving permanently-stuck entities in the dungeon dimension.
        if (instanceId == null || nodeId == null || nodeId.isBlank()) {
            PiranPort.LOGGER.warn("Discarding invalid dungeon portal {} (instance={}, nodeLength={})",
                    getId(), instanceId, nodeId == null ? -1 : nodeId.length());
            discard();
            return;
        }
        DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) level());
        if (mgr.getInstance(instanceId) == null) {
            discard();
            return;
        }

        // Server-side: check for nearby players every 10 ticks instead of every tick
        if (tickCount % 10 != 0) return;

        AABB area = getBoundingBox().inflate(2.0, 1.0, 2.0);
        List<ServerPlayer> nearbyPlayers = ((ServerLevel) level())
                .getEntitiesOfClass(ServerPlayer.class, area);

        Set<UUID> members = mgr.getInstance(instanceId).getPlayerUuids();
        for (ServerPlayer player : nearbyPlayers) {
            UUID playerId = player.getUUID();
            if (!members.contains(playerId)) continue;
            if (enteredPlayers.size() >= MAX_ENTERED_PLAYERS && !enteredPlayers.contains(playerId)) {
                continue;
            }
            enteredPlayers.add(playerId);
        }

        boolean memberNearby = false;
        for (UUID memberId : members) {
            if (enteredPlayers.contains(memberId)) {
                memberNearby = true;
                break;
            }
        }
        if (!memberNearby) {
            orphanTicks += 10;
            if (orphanTicks > ORPHAN_DESPAWN_TICKS) {
                discard();
                return;
            }
        } else {
            orphanTicks = 0;
        }

        // Check if all dungeon players have entered
        if (!enteredPlayers.isEmpty()) {
            DungeonInstance instance = mgr.getInstance(instanceId);
            if (instance != null) {
                Set<UUID> allPlayers = instance.getPlayerUuids();
                // Check: every member who is currently online in this dimension has entered.
                boolean allEntered = true;
                for (UUID playerUuid : allPlayers) {
                    ServerPlayer onlinePlayer = ((ServerLevel) level()).getServer()
                            .getPlayerList().getPlayer(playerUuid);
                    if (onlinePlayer != null
                            && onlinePlayer.level().dimension().equals(level().dimension())
                            && !enteredPlayers.contains(playerUuid)) {
                        allEntered = false;
                        break;
                    }
                }

                if (allEntered) {
                    // Advance to next phase — handled by DungeonEventHandler
                    com.piranport.dungeon.event.DungeonEventHandler.onPortalComplete(
                            (ServerLevel) level(), instance, nodeId);
                    discard();
                    return;
                }
            }
        }
    }

    public UUID getInstanceId() { return instanceId; }
    public String getNodeId() { return nodeId; }
    public int getSpinTick() { return tickCount; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (instanceId != null) tag.putUUID("InstanceId", instanceId);
        if (nodeId != null) tag.putString("NodeId", nodeId);
        if (!enteredPlayers.isEmpty()) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (UUID uuid : enteredPlayers) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("UUID", uuid);
                list.add(entry);
            }
            tag.put("EnteredPlayers", list);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("InstanceId")) instanceId = tag.getUUID("InstanceId");

        if (tag.contains("NodeId", net.minecraft.nbt.Tag.TAG_STRING)) {
            String loadedNodeId = tag.getString("NodeId");
            nodeId = (loadedNodeId.isBlank() || loadedNodeId.length() > MAX_NODE_ID_LENGTH)
                    ? null : loadedNodeId;
        }

        if (tag.contains("EnteredPlayers")) {
            net.minecraft.nbt.ListTag list = tag.getList("EnteredPlayers", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && enteredPlayers.size() < MAX_ENTERED_PLAYERS; i++) {
                CompoundTag entry = list.getCompound(i);
                if (entry.hasUUID("UUID")) {
                    enteredPlayers.add(entry.getUUID("UUID"));
                }
            }
        }
    }
}
