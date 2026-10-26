package com.piranport.entity;

import com.piranport.npc.ai.ballistic.ProximityFuse;
import com.piranport.npc.ai.ballistic.TrackingCalculator;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.config.ModCommonConfig;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.entity.AircraftEntity;
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

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Projectile fired by deep ocean enemies.
 * Supports four ballistic modes: PARABOLIC, PARABOLIC_TRACKING, DIRECT, and GUIDED.
 * Does not damage other deep ocean entities (friendly fire protection).
 */
public class DeepOceanProjectileEntity extends ThrowableItemProjectile {

    public enum BallisticType {
        PARABOLIC,           // Standard arc
        PARABOLIC_TRACKING,  // Arc then track after apex
        DIRECT,              // Straight line (no gravity)
        // 全程比例导引、无重力。防空导弹专用。
        // WHY 必须追加在末尾：NBT 存的是 ordinal，插在中间会让旧存档的弹道类型整体错位。
        GUIDED
    }

    // 兜底伤害/爆炸威力走调试终端（global.deep_ocean_projectile.*）；实际开火由发射方传入覆盖值，
    // 此处仅在实体注册构造与 NBT 缺省回填时生效。
    private float damage = (float) (double) ModEquipmentConfig.DEEP_OCEAN_PROJECTILE_DAMAGE.get();
    private float explosionPower = (float) (double) ModEquipmentConfig.DEEP_OCEAN_PROJECTILE_EXPLOSION_POWER.get();
    private BallisticType ballisticType = BallisticType.PARABOLIC;
    private UUID trackingTargetUuid;
    private boolean pastApex = false;
    private Vec3 prevVelocity = Vec3.ZERO;
    private boolean exploded = false;

    /** Proximity fuze settings. */
    private boolean hasProximityFuse = false;
    private double proximityRange = ModEquipmentConfig.DEEP_OCEAN_PROJECTILE_PROXIMITY_RANGE.get();
    /**
     * 近炸直接伤害。0 = 沿用旧行为（只靠 explosionPower 结算）。
     * <p>WHY 需要独立字段：防空弹的 explosionPower 必须为 0（不能炸方块、不能误伤水面），
     * 但原版爆炸在威力 0 时打不出伤害，近炸就会变成只播特效的空弹。</p>
     */
    private float proximityDamage = 0.0f;

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
        setProximityFuse(enabled, range, 0.0f);
    }

    /**
     * @param proximityDamage 近炸触发的直接伤害；&lt;= 0 时保持旧行为（只播特效/走爆炸威力）。
     */
    public void setProximityFuse(boolean enabled, double range, float proximityDamage) {
        this.hasProximityFuse = enabled;
        this.proximityRange = range;
        this.proximityDamage = Math.max(0.0f, proximityDamage);
    }

    @Override
    public void tick() {
        prevVelocity = getDeltaMovement();
        super.tick();

        if (!level().isClientSide) {
            // 制导修正放在 super.tick() 之后：此时位置已经推进、速度已衰减，
            // 改的方向直接作用于下一 tick 的位移。
            if (ballisticType == BallisticType.GUIDED && trackingTargetUuid != null) {
                steerGuided();
            } else if (ballisticType == BallisticType.PARABOLIC_TRACKING && trackingTargetUuid != null) {
                // Tracking after apex (for PARABOLIC_TRACKING type)
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

            // Proximity fuze check。prevVelocity 是本 tick 实际用于位移的速度，
            // 用「上一位置 → 当前位置」的线段判距，避免高速弹一步跨过近炸球。
            if (hasProximityFuse && !exploded) {
                if (ProximityFuse.shouldDetonate(this, position().subtract(prevVelocity), proximityRange,
                        ModEquipmentConfig.DEEP_OCEAN_PROJECTILE_ARM_TICKS.get())) {
                    detonate(true);
                }
            }

            // Discard after lifetime ticks (default 200 = 10 seconds) to prevent leaks
            if (tickCount > ModEquipmentConfig.DEEP_OCEAN_PROJECTILE_LIFETIME_TICKS.get()) {
                if (hasProximityFuse && !exploded) {
                    detonate(true);
                } else {
                    discard();
                }
            }
        }
    }

    /**
     * GUIDED 每 tick 的制导步数。
     * <p>WHY 不是 1：{@link TrackingCalculator} 的最大转角（默认 0.08 rad/tick）是给反舰
     * 追踪弹调的，防空导弹要拦横向高速掠过的飞机，一次转向不够，会绕到目标后方。
     * 连续走两步比例导引 = 局部把可用转角提到约 0.16 rad/tick，不改全局反舰手感。</p>
     */
    private static final int GUIDED_STEER_STEPS = 2;

    /** GUIDED：每 tick 比例导引朝目标修正；目标消失后改为无重力直飞，避免像抛物线一样栽下去。 */
    private void steerGuided() {
        if (!(level() instanceof ServerLevel sl)) return;
        Entity target = sl.getEntity(trackingTargetUuid);
        if (target == null || !target.isAlive() || target.isRemoved()) {
            ballisticType = BallisticType.DIRECT;
            trackingTargetUuid = null;
            return;
        }
        Vec3 velocity = getDeltaMovement();
        for (int i = 0; i < GUIDED_STEER_STEPS; i++) {
            velocity = TrackingCalculator.steer(position(), velocity, target);
        }
        setDeltaMovement(velocity);
    }

    @Override
    protected double getDefaultGravity() {
        // DIRECT 与 GUIDED 都不吃重力：防空弹打的是空中目标，抛物线重力会让射程和弹道无法解算。
        return ballisticType == BallisticType.DIRECT || ballisticType == BallisticType.GUIDED ? 0.0
                : ModEquipmentConfig.DEEP_OCEAN_PROJECTILE_GRAVITY.get();
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
        detonate(false);
    }

    /**
     * @param proximityTriggered 是否由近炸引信触发。只有近炸才结算 {@link #proximityDamage}：
     *                           直接命中已在 {@link #onHitEntity} 用过 damage，再结算一次会双倍伤害。
     */
    private void detonate(boolean proximityTriggered) {
        if (exploded) return;
        exploded = true;

        if (proximityTriggered && proximityDamage > 0.0f) {
            Entity victim = findProximityVictim();
            if (victim != null) {
                // 与直接命中同源，副本伤害缩放等监听器行为一致。
                victim.hurt(damageSources().explosion(this, getOwner()), proximityDamage);
            }
        }

        // explosionPower=0 的防空弹直接跳过 explode：0 威力既打不出伤害，也不该走一遍
        // 方块/实体的爆炸结算；只靠近炸直接伤害 + 消失粒子收尾。
        if (explosionPower > 0.0f) {
            Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                    ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
            level().explode(this, getX(), getY(), getZ(), explosionPower, interaction);
        }
        level().broadcastEntityEvent(this, (byte) 3);
        discard();
    }

    /**
     * 近炸伤害目标：优先锁定目标（若仍在近炸范围内），否则取范围内最近的玩家所属飞机。
     * <p>WHY 排除深海自主飞机：它们没有 ownerUUID，打自己人会让防空变成内耗；
     * 同时排除发射者与所有深海实体，保持与 {@link #canHitEntity} 一致的友伤保护。</p>
     */
    @Nullable
    private Entity findProximityVictim() {
        Entity owner = getOwner();
        if (trackingTargetUuid != null && level() instanceof ServerLevel sl) {
            Entity tracked = sl.getEntity(trackingTargetUuid);
            if (isProximityVictimInRange(tracked, owner)) return tracked;
        }
        Vec3 segmentStart = position().subtract(prevVelocity);
        // 搜索盒要覆盖本 tick 的整段位移，否则线段起点附近的飞机会被漏掉。
        List<AircraftEntity> nearby = level().getEntitiesOfClass(AircraftEntity.class,
                getBoundingBox().expandTowards(segmentStart.subtract(position())).inflate(proximityRange),
                e -> e.getOwnerUUID() != null && isProximityVictimInRange(e, owner));
        Entity nearest = null;
        double best = Double.MAX_VALUE;
        for (AircraftEntity aircraft : nearby) {
            Vec3 center = aircraft.position().add(0, aircraft.getBbHeight() * 0.5, 0);
            double dist = ProximityFuse.distanceToSegment(center, segmentStart, position());
            if (dist <= proximityRange && dist < best) {
                best = dist;
                nearest = aircraft;
            }
        }
        return nearest;
    }

    private boolean isProximityVictimInRange(@Nullable Entity candidate, @Nullable Entity owner) {
        if (candidate == null || candidate == owner) return false;
        if (!candidate.isAlive() || candidate.isRemoved()) return false;
        if (candidate instanceof AbstractDeepOceanEntity) return false;
        Vec3 center = candidate.position().add(0, candidate.getBbHeight() * 0.5, 0);
        // 与引信一致，用本 tick 的位移线段判距，否则「引信已触发但找不到受害者」。
        double distance = ProximityFuse.distanceToSegment(center, position().subtract(prevVelocity), position());
        return distance <= proximityRange;
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
        tag.putFloat("ProxDamage", proximityDamage);
        if (trackingTargetUuid != null) tag.putUUID("TrackingTarget", trackingTargetUuid);
        tag.putBoolean("PastApex", pastApex);
        tag.putDouble("PrevVelX", prevVelocity.x);
        tag.putDouble("PrevVelY", prevVelocity.y);
        tag.putDouble("PrevVelZ", prevVelocity.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        if (damage <= 0) damage = (float) (double) ModEquipmentConfig.DEEP_OCEAN_PROJECTILE_DAMAGE.get();
        // 允许 0：防空弹按设计不带爆炸威力，旧代码把 <0.1 抬到 0.1 会让防空弹读档后突然能炸方块。
        explosionPower = tag.contains("ExplosionPower")
                ? Math.max(0.0f, tag.getFloat("ExplosionPower"))
                : (float) (double) ModEquipmentConfig.DEEP_OCEAN_PROJECTILE_EXPLOSION_POWER.get();
        int typeOrd = tag.getInt("BallisticType");
        BallisticType[] types = BallisticType.values();
        ballisticType = typeOrd >= 0 && typeOrd < types.length ? types[typeOrd] : BallisticType.PARABOLIC;
        hasProximityFuse = tag.getBoolean("HasProxFuse");
        proximityRange = tag.getDouble("ProxRange");
        if (proximityRange < 0) proximityRange = 0;
        proximityDamage = Math.max(0.0f, tag.getFloat("ProxDamage"));
        trackingTargetUuid = tag.hasUUID("TrackingTarget") ? tag.getUUID("TrackingTarget") : null;
        pastApex = tag.getBoolean("PastApex");
        prevVelocity = new Vec3(
                tag.getDouble("PrevVelX"),
                tag.getDouble("PrevVelY"),
                tag.getDouble("PrevVelZ"));
    }
}
