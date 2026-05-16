package com.piranport.artillery;

import com.piranport.registry.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 炮弹实体基类。
 * Phase 1: 直线飞行，碰撞后消失并造成基础伤害。
 * Phase 2: 抛物线弹道，支持阻力衰减 + 重力 + 尾迹粒子 + 散布。
 */
public class ShellEntity extends ThrowableItemProjectile {
    private float damage = 6f;
    private float explosionRadius = 1.0f;

    // Phase 2: 物理参数
    private float initialSpeed = 3.0f;
    private float dragCoeff = 0.01f;       // 每 tick 速度衰减比例
    private float customGravity = 9.8f;     // 映射到 MC 重力单位

    // 实体类型注册用
    public ShellEntity(EntityType<? extends ShellEntity> type, Level level) {
        super(type, level);
    }

    // 发射用构造器
    public ShellEntity(Level level, LivingEntity shooter, ItemStack shellItem, float damage,
                        float initialSpeed, float dragCoeff, float customGravity) {
        super(ModEntityTypes.SHELL_PROJECTILE.get(), shooter, level);
        setItem(shellItem);
        this.damage = damage;
        this.initialSpeed = initialSpeed;
        this.dragCoeff = dragCoeff;
        this.customGravity = customGravity;
    }

    public void setExplosionRadius(float radius) { this.explosionRadius = radius; }

    // ===== Phase 2: 物理弹道 =====

    @Override
    protected double getDefaultGravity() {
        return customGravity / 196.0; // 9.8 → MC 0.05
    }

    @Override
    public void tick() {
        // Phase 2: 在 MC 原生重力生效前应用阻力
        Vec3 vel = getDeltaMovement();
        double speed = vel.length();
        if (speed > 0.01 && dragCoeff > 0) {
            vel = vel.scale(Math.max(0.1, 1.0 - dragCoeff));
            setDeltaMovement(vel);
        }

        super.tick();

        // Phase 2: 客户端尾迹粒子
        if (level().isClientSide && tickCount % 2 == 0 && !isRemoved()) {
            Vec3 pos = position();
            Vec3 v = getDeltaMovement();
            if (v.length() > 0.1) {
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        pos.x, pos.y, pos.z,
                        -v.x * 0.05 + random.nextGaussian() * 0.01,
                        -v.y * 0.05 + random.nextGaussian() * 0.01,
                        -v.z * 0.05 + random.nextGaussian() * 0.01);
            }
        }
    }

    // ===== 碰撞处理 =====

    @Override
    protected Item getDefaultItem() {
        return net.minecraft.world.item.Items.SNOWBALL;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (com.piranport.combat.FriendlyFireHelper.shouldBlockHit(target, getOwner())) return false;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) {
            result.getEntity().hurt(damageSources().thrown(this, getOwner()), damage);
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide) {
            Level.ExplosionInteraction interaction = Level.ExplosionInteraction.NONE;
            level().explode(this, getX(), getY(), getZ(), explosionRadius, interaction);
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().broadcastEntityEvent(this, (byte) 3);
            discard();
        }
    }

    // ===== NBT =====

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("ExplosionRadius", explosionRadius);
        tag.putFloat("InitialSpeed", initialSpeed);
        tag.putFloat("DragCoeff", dragCoeff);
        tag.putFloat("CustomGravity", customGravity);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        if (damage <= 0) damage = 6f;
        explosionRadius = tag.getFloat("ExplosionRadius");
        if (tag.contains("InitialSpeed")) initialSpeed = tag.getFloat("InitialSpeed");
        if (tag.contains("DragCoeff")) dragCoeff = tag.getFloat("DragCoeff");
        if (tag.contains("CustomGravity")) customGravity = tag.getFloat("CustomGravity");
    }
}
