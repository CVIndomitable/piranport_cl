package com.piranport.entity;

import com.piranport.npc.ai.ballistic.ProximityFuse;
import com.piranport.npc.ai.ballistic.TrackingCalculator;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.config.ModCommonConfig;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/**
 * Projectile fired by deep ocean enemies.
 * Supports three ballistic modes: PARABOLIC, PARABOLIC_TRACKING, and DIRECT.
 * Does not damage other deep ocean entities (friendly fire protection).
 */
public class DeepOceanProjectileEntity extends ThrowableItemProjectile {

    public enum BallisticType {
        PARABOLIC,           // Standard arc
        PARABOLIC_TRACKING,  // Arc then track after apex
        DIRECT               // Straight line (no gravity)
    }

    private float damage = 5.0f;
    private float explosionPower = 1.5f;
    private BallisticType ballisticType = BallisticType.PARABOLIC;
    private UUID trackingTargetUuid;
    private boolean pastApex = false;
    private Vec3 prevVelocity = Vec3.ZERO;
    private boolean exploded = false;

    /** Proximity fuze settings. */
    private boolean hasProximityFuse = false;
    private double proximityRange = 3.0;
    private static final int ARM_TICKS = 5;

    // Required constructor for entity type registration
    public DeepOceanProjectileEntity(EntityType<? extends DeepOceanProjectileEntity> type, Level level) {
        super(type, level);
    }

    // Firing constructor
    public DeepOceanProjectileEntity(Level level, LivingEntity shooter,
                                      float damage, float explosionPower,
                                      BallisticType ballisticType) {
        super(ModEntityTypes.DEEP_OCEAN_PROJECTILE.get(), shooter, level);
        this.damage = damage;
        this.explosionPower = explosionPower;
        this.ballisticType = ballisticType;
    }

    public void setTrackingTarget(Entity target) {
        this.trackingTargetUuid = target != null ? target.getUUID() : null;
    }

    public void setProximityFuse(boolean enabled, double range) {
        this.hasProximityFuse = enabled;
        this.proximityRange = range;
    }

    @Override
    public void tick() {
        prevVelocity = getDeltaMovement();
        super.tick();

        if (!level().isClientSide) {
            // Tracking after apex (for PARABOLIC_TRACKING type)
            if (ballisticType == BallisticType.PARABOLIC_TRACKING && trackingTargetUuid != null) {
                if (!pastApex) {
                    pastApex = TrackingCalculator.hasPassedApex(getDeltaMovement(), prevVelocity);
                }
                if (pastApex && level() instanceof ServerLevel sl) {
                    Entity target = sl.getEntity(trackingTargetUuid);
                    if (target != null && target.isAlive()) {
                        Vec3 newVel = TrackingCalculator.steer(position(), getDeltaMovement(), target);
                        setDeltaMovement(newVel);
                    } else {
                        // Target gone — degrade to plain parabolic
                        ballisticType = BallisticType.PARABOLIC;
                        trackingTargetUuid = null;
                    }
                }
            }

            // Proximity fuze check
            if (hasProximityFuse && !exploded) {
                if (ProximityFuse.shouldDetonate(this, proximityRange, ARM_TICKS)) {
                    detonate();
                }
            }

            // Discard after 200 ticks (10 seconds) to prevent leaks
            if (tickCount > 200) {
                discard();
            }
        }
    }

    @Override
    protected double getDefaultGravity() {
        return ballisticType == BallisticType.DIRECT ? 0.0 : 0.05;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PROJECTILE_BULLET.get();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        // Don't hit other deep ocean entities (friendly fire protection)
        if (target instanceof AbstractDeepOceanEntity) return false;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide && !exploded) {
            Entity target = result.getEntity();
            target.hurt(damageSources().explosion(this, getOwner()), damage);
            detonate();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide && !exploded) {
            detonate();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (exploded) return;
        super.onHit(result);
        if (!level().isClientSide && !exploded) {
            level().broadcastEntityEvent(this, (byte) 3);
            discard();
        }
    }

    private void detonate() {
        if (exploded) return;
        exploded = true;
        Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
        level().explode(this, getX(), getY(), getZ(), explosionPower, interaction);
        level().broadcastEntityEvent(this, (byte) 3);
        discard();
    }

    // --- Persistence ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("ExplosionPower", explosionPower);
        tag.putInt("BallisticType", ballisticType.ordinal());
        tag.putBoolean("HasProxFuse", hasProximityFuse);
        tag.putDouble("ProxRange", proximityRange);
        if (trackingTargetUuid != null) tag.putUUID("TrackingTarget", trackingTargetUuid);
        tag.putBoolean("PastApex", pastApex);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        if (damage <= 0) damage = 5.0f;
        explosionPower = tag.getFloat("ExplosionPower");
        int typeOrd = tag.getInt("BallisticType");
        BallisticType[] types = BallisticType.values();
        ballisticType = typeOrd >= 0 && typeOrd < types.length ? types[typeOrd] : BallisticType.PARABOLIC;
        hasProximityFuse = tag.getBoolean("HasProxFuse");
        proximityRange = tag.getDouble("ProxRange");
        trackingTargetUuid = tag.hasUUID("TrackingTarget") ? tag.getUUID("TrackingTarget") : null;
        pastApex = tag.getBoolean("PastApex");
    }
}
