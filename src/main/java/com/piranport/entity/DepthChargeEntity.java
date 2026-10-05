package com.piranport.entity;

import com.piranport.config.ModCommonConfig;
import com.piranport.config.ModProjectilesConfig;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

/**
 * 深水炸弹 — 从飞机投下或由深弹投射器发射，重力较大，入水后继续下沉。
 * 检测到附近实体时引爆，爆炸后对该高度水平范围内的实体造成无视护甲的魔法伤害。
 */
public class DepthChargeEntity extends ThrowableItemProjectile {

    // 兜底伤害/威力走终端 global.depth_charge.*：默认 = 下沉前写死值。发射策略/兼容层
    // 传入的值同样取自这些参数（再叠加玩家强化加成），保证唯一来源。
    private float damage = (float) (double) ModProjectilesConfig.DEPTH_CHARGE_DAMAGE.get();
    private float explosionPower = (float) (double) ModProjectilesConfig.DEPTH_CHARGE_EXPLOSION_POWER.get();
    private boolean detonated = false;

    /** Required by entity type registration. */
    public DepthChargeEntity(EntityType<? extends DepthChargeEntity> type, Level level) {
        super(type, level);
    }

    /** Spawned by AircraftEntity; position and velocity set externally after construction. */
    public DepthChargeEntity(Level level, float damage, float explosionPower) {
        super(ModEntityTypes.DEPTH_CHARGE.get(), level);
        this.damage = damage;
        this.explosionPower = explosionPower;
    }

    /** Spawned by DepthChargeLauncherItem via ShipCoreItem firing. */
    public DepthChargeEntity(Level level, LivingEntity shooter, float damage, float explosionPower) {
        super(ModEntityTypes.DEPTH_CHARGE.get(), shooter, level);
        this.damage = damage;
        this.explosionPower = explosionPower;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.DEPTH_CHARGE.get();
    }

    /** Heavy — sinks faster than aerial bombs. 重力走终端（默认 0.08）。 */
    @Override
    protected double getDefaultGravity() {
        return ModProjectilesConfig.DEPTH_CHARGE_GRAVITY.get();
    }

    private boolean inWater = false;
    private boolean waterEntryEffectPlayed = false;

    @Override
    public void tick() {
        super.tick();
        if (isRemoved() || detonated) return;

        if (!level().isClientSide()) {
            // 超时消失
            if (tickCount > ModProjectilesConfig.DEPTH_CHARGE_LIFETIME_TICKS.get()) {
                discard();
                return;
            }

            // 水中检测：遇到水面时不下沉则销毁（防止漂浮）
            boolean nowInWater = isInWater();
            if (nowInWater != inWater) {
                inWater = nowInWater;
                if (inWater) {
                    waterEntryEffectPlayed = false;
                    // 入水时播放水花音效
                    level().playSound(null, getX(), getY(), getZ(),
                            net.minecraft.sounds.SoundEvents.GENERIC_SPLASH, net.minecraft.sounds.SoundSource.NEUTRAL,
                            0.5f, 0.8f + level().getRandom().nextFloat() * 0.4f);
                }
            }

            // 在水中：阻力增大，加重力加速下沉（阻力/附加下沉量走终端）
            if (inWater) {
                var movement = getDeltaMovement();
                double drag = ModProjectilesConfig.DEPTH_CHARGE_WATER_DRAG.get();
                setDeltaMovement(movement.x * drag,
                        movement.y - ModProjectilesConfig.DEPTH_CHARGE_WATER_SINK_ACCEL.get(),
                        movement.z * drag);
            }

            // P2优化: 错峰执行近炸检测，避免齐投时同一tick内多次检测
            // 入水后短暂延迟再开始检测，模拟真实深弹入水后下潜启动引信
            if (tickCount > ModProjectilesConfig.DEPTH_CHARGE_ARM_TICKS.get()
                    + (inWater ? ModProjectilesConfig.DEPTH_CHARGE_WATER_SINK_DURATION.get() : 0)
                    && (tickCount + getId()) % ModProjectilesConfig.DEPTH_CHARGE_PROXIMITY_INTERVAL.get() == 0) {
                checkProximity();
            }
        }
    }

    /** 扫描检测范围内的存活实体，发现目标后引爆。 */
    private void checkProximity() {
        AABB searchBox = getBoundingBox().inflate(ModProjectilesConfig.DEPTH_CHARGE_DETECT_RANGE.get());
        List<Entity> nearby = level().getEntities(this, searchBox, e -> {
            if (e == getOwner()) return false;
            if (com.piranport.combat.FriendlyFireHelper.shouldBlockHit(e, getOwner())) return false;
            return e.isAlive() && e.isPickable() && e instanceof LivingEntity;
        });
        if (!nearby.isEmpty()) {
            detonate();
        }
    }

    /**
     * 引爆：对该高度水平范围内的实体造成无视护甲的魔法伤害 + 视觉爆炸效果。
     * 伤害范围为以爆炸点为中心、{@code global.depth_charge.blast_radius} 水平半径、
     * ±{@code blast_height} 垂直容差的扁平圆柱。
     */
    private void detonate() {
        if (detonated) return;
        detonated = true;

        double cx = getX(), cy = getY(), cz = getZ();
        double blastRadius = ModProjectilesConfig.DEPTH_CHARGE_BLAST_RADIUS.get();
        double blastHeight = ModProjectilesConfig.DEPTH_CHARGE_BLAST_HEIGHT.get();
        float edgeRatio = (float) (double) ModProjectilesConfig.DEPTH_CHARGE_EDGE_DAMAGE_RATIO.get();

        // 区域魔法伤害（无视护甲）
        AABB damageBox = new AABB(
                cx - blastRadius, cy - blastHeight, cz - blastRadius,
                cx + blastRadius, cy + blastHeight, cz + blastRadius);
        List<Entity> targets = level().getEntities(this, damageBox, e -> {
            if (e == getOwner()) return false;
            if (com.piranport.combat.FriendlyFireHelper.shouldBlockHit(e, getOwner())) return false;
            return e.isAlive() && e instanceof LivingEntity;
        });
        for (Entity target : targets) {
            // 距离衰减：中心全额伤害，边缘只保留 edgeRatio（同时作为伤害下限）
            double dist = Math.sqrt(
                    (target.getX() - cx) * (target.getX() - cx) +
                    (target.getZ() - cz) * (target.getZ() - cz));
            float ratio = 1.0f - (float) (dist / blastRadius) * edgeRatio;
            // 多枚深弹齐投：重置无敌帧保证每枚都造成伤害
            target.invulnerableTime = 0;
            target.hurt(damageSources().indirectMagic(this, getOwner()), damage * Math.max(ratio, edgeRatio));
            notifyOwner(target);
        }

        // 视觉爆炸（不破坏方块）
        level().explode(this, cx, cy, cz, explosionPower, Level.ExplosionInteraction.NONE);
        discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (com.piranport.combat.FriendlyFireHelper.shouldBlockHit(target, getOwner())) return false;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide() && !detonated) {
            detonate();
        }
    }

    private void notifyOwner(Entity target) {
        Entity owner = getOwner();
        if (!(owner instanceof Player player)) return;
        Component weaponName = getDefaultItem().getDescription();
        String key = target.isAlive() ? "message.piranport.weapon_hit" : "message.piranport.weapon_kill";
        com.piranport.combat.HitNotifier.send(player, Component.translatable(key, weaponName, target.getDisplayName()));
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!level().isClientSide() && !detonated) {
            // 遇到水面时不引爆，让深水炸弹继续下沉
            if (level().getBlockState(result.getBlockPos()).getFluidState().is(FluidTags.WATER)
                    || level().getBlockState(result.getBlockPos().above()).getFluidState().is(FluidTags.WATER)) {
                return;
            }
            BlockPos pos = result.getBlockPos();
            // 砰在固体表面（如舰体）上时引爆
            if (!level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()) {
                detonate();
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("ChargeDamage", damage);
        tag.putFloat("ExplosionPower", explosionPower);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ChargeDamage")) damage = tag.getFloat("ChargeDamage");
        if (tag.contains("ExplosionPower")) explosionPower = tag.getFloat("ExplosionPower");
    }
}
