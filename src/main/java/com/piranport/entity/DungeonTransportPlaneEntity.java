package com.piranport.entity;

import com.piranport.dungeon.DungeonConstants;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Transport plane for the artillery-intro dungeon script.
 * Flies straight from spawn to a target drop point, signals when overhead,
 * then continues flying + climbs 30° until out of range and despawns.
 *
 * This entity has no collision, no gravity, and is always glowing.
 */
public class DungeonTransportPlaneEntity extends Entity {

    private static final EntityDataAccessor<Boolean> HAS_DROPPED =
            SynchedEntityData.defineId(DungeonTransportPlaneEntity.class, EntityDataSerializers.BOOLEAN);

    /** Horizontal flight speed in blocks/tick. */
    private static final double FLIGHT_SPEED = 0.8;
    /** Climb rate after drop (tan 30° ≈ 0.577). */
    private static final double CLIMB_RATE = 0.46;
    /** Max lifetime in ticks (safety net: 30 seconds). */
    private static final int MAX_LIFETIME = 600;
    /** How close (horizontal blocks) to the drop point to trigger drop. */
    private static final double DROP_THRESHOLD = 2.0;

    // Target drop position (set on spawn, server-only)
    private double targetX, targetZ;
    // Flight direction (normalized horizontal)
    private double dirX, dirZ;
    private boolean dropped = false;
    private boolean climbing = false;

    /** Callback set by the script — fired once when the plane reaches the drop point. */
    private Runnable onDrop;

    public DungeonTransportPlaneEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /**
     * Creates and configures a transport plane.
     *
     * @param level     the dungeon level
     * @param startX    spawn X (above sea)
     * @param startZ    spawn Z
     * @param targetX   drop target X
     * @param targetZ   drop target Z
     * @param onDrop    callback when plane is over drop point
     */
    public static DungeonTransportPlaneEntity create(ServerLevel level,
                                                      double startX, double startZ,
                                                      double targetX, double targetZ,
                                                      Runnable onDrop) {
        DungeonTransportPlaneEntity plane = new DungeonTransportPlaneEntity(
                ModEntityTypes.DUNGEON_TRANSPORT_PLANE.get(), level);
        double startY = DungeonConstants.SPAWN_Y + 15;
        plane.setPos(startX, startY, startZ);
        plane.targetX = targetX;
        plane.targetZ = targetZ;
        plane.onDrop = onDrop;

        // Compute normalized flight direction
        double dx = targetX - startX;
        double dz = targetZ - startZ;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01) len = 1.0;
        plane.dirX = dx / len;
        plane.dirZ = dz / len;

        // Face flight direction
        float yaw = (float) (Math.atan2(-plane.dirX, plane.dirZ) * (180.0 / Math.PI));
        plane.setYRot(yaw);

        return plane;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HAS_DROPPED, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (tickCount > MAX_LIFETIME) {
            discard();
            return;
        }

        if (!level().isClientSide()) {
            // Server: fly forward
            double vx = dirX * FLIGHT_SPEED;
            double vz = dirZ * FLIGHT_SPEED;
            double vy = climbing ? FLIGHT_SPEED * CLIMB_RATE : 0.0;
            setDeltaMovement(vx, vy, vz);
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());

            // Check if over drop point
            if (!dropped) {
                double hDist = Math.sqrt(
                        Math.pow(getX() - targetX, 2) + Math.pow(getZ() - targetZ, 2));
                if (hDist < DROP_THRESHOLD) {
                    dropped = true;
                    entityData.set(HAS_DROPPED, true);
                    if (onDrop != null) {
                        onDrop.run();
                    }
                }
            }

            // Start climbing 60 ticks after drop (3 seconds of level flight past drop point)
            if (dropped && !climbing && tickCount > 60) {
                climbing = true;
            }
        } else {
            // Client: apply movement from server sync
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());

            // Engine exhaust particles
            if (tickCount % 3 == 0) {
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        getX() - dirX * 1.2, getY() + 0.2, getZ() - dirZ * 1.2,
                        0, 0.02, 0);
            }
        }
    }

    public boolean hasDropped() {
        return entityData.get(HAS_DROPPED);
    }

    @Override
    public boolean isCurrentlyGlowing() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("TargetX", targetX);
        tag.putDouble("TargetZ", targetZ);
        tag.putDouble("DirX", dirX);
        tag.putDouble("DirZ", dirZ);
        tag.putBoolean("Dropped", dropped);
        tag.putBoolean("Climbing", climbing);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        targetX = tag.getDouble("TargetX");
        targetZ = tag.getDouble("TargetZ");
        dirX = tag.getDouble("DirX");
        dirZ = tag.getDouble("DirZ");
        dropped = tag.getBoolean("Dropped");
        climbing = tag.getBoolean("Climbing");
        entityData.set(HAS_DROPPED, dropped);
    }
}
