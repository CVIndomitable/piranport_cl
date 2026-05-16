package com.piranport.artillery;

import com.piranport.PiranPort;
import com.piranport.combat.FriendlyFireHelper;
import com.piranport.config.ModCommonConfig;
import com.piranport.config.ModProjectilesConfig;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 炮弹实体基类。
 * Phase 1: 直线飞行，碰撞后消失并造成基础伤害。
 * Phase 2: 抛物线弹道，支持阻力衰减 + 重力 + 尾迹粒子 + 散布。
 * Phase 3: HE 爆炸逻辑、范围伤害（距离衰减 + 护甲穿透）、水中弹药销毁。
 */
public class ShellEntity extends ThrowableItemProjectile {
    private float damage = 6f;
    private float explosionRadius = 1.0f;

    // Phase 2: 物理参数
    private float initialSpeed = 3.0f;
    private float dragCoeff = 0.01f;
    private float customGravity = 9.8f;

    // Phase 3: HE 弹属性
    private String shellType = "HE";
    private int underwaterTicks = 0;
    private boolean underwaterHandled = false;
    private float armorPenetration = 0.3f;
    private boolean damageFalloff = true;

    // 实体类型注册用
    public ShellEntity(EntityType<? extends ShellEntity> type, Level level) {
        super(type, level);
    }

    // 发射用构造器（Phase 2 版，默认弹种 HE）
    public ShellEntity(Level level, LivingEntity shooter, ItemStack shellItem, float damage,
                        float initialSpeed, float dragCoeff, float customGravity) {
        super(ModEntityTypes.SHELL_PROJECTILE.get(), shooter, level);
        setItem(shellItem);
        this.damage = damage;
        this.initialSpeed = initialSpeed;
        this.dragCoeff = dragCoeff;
        this.customGravity = customGravity;
    }

    // Phase 3: 发射用构造器（完整版）
    public ShellEntity(Level level, LivingEntity shooter, ItemStack shellItem, float damage,
                        float initialSpeed, float dragCoeff, float customGravity, String shellType) {
        super(ModEntityTypes.SHELL_PROJECTILE.get(), shooter, level);
        setItem(shellItem);
        this.damage = damage;
        this.initialSpeed = initialSpeed;
        this.dragCoeff = dragCoeff;
        this.customGravity = customGravity;
        this.shellType = shellType;
    }

    public void setExplosionRadius(float radius) { this.explosionRadius = radius; }

    public void setShellType(String type) { this.shellType = type; }

    // ===== Phase 2: 物理弹道 =====

    @Override
    protected double getDefaultGravity() {
        return customGravity / 196.0;
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

        // Phase 3: 水中弹药销毁
        if (!level().isClientSide && !underwaterHandled && isInWater()) {
            underwaterTicks++;
            int maxTicks = ModProjectilesConfig.UNDERWATER_DESTROY_TICKS.get();
            if (underwaterTicks > maxTicks) {
                underwaterHandled = true;
                if (ModProjectilesConfig.UNDERWATER_EXPLODE.get()) {
                    doExplosion(true);
                }
                discard();
            }
        }
    }

    // ===== Phase 3: 爆炸与伤害 =====

    /** 执行爆炸：按 underwater 决定实际半径，爆炸后对范围内实体施加自定义伤害。 */
    private void doExplosion(boolean underwater) {
        float radius = underwater
                ? explosionRadius * ModProjectilesConfig.UNDERWATER_EXPLOSION_MULTIPLIER.get().floatValue()
                : explosionRadius;
        if (radius <= 0) radius = 0.1f;

        Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
        level().explode(this, getX(), getY(), getZ(), radius, interaction);
        doBlastDamage(radius);
    }

    /** 对爆炸范围内的实体施加带距离衰减和护甲穿透的爆炸伤害。 */
    private void doBlastDamage(float radius) {
        AABB box = getBoundingBox().inflate(radius);
        List<Entity> targets = level().getEntities(this, box,
                e -> e instanceof LivingEntity && e.isAlive() && e != getOwner()
                        && !FriendlyFireHelper.shouldBlockHit(e, getOwner()));

        for (Entity target : targets) {
            double dist = target.distanceTo(this);
            if (dist > radius) continue;

            // 距离衰减
            float finalDmg = damage;
            if (ModProjectilesConfig.HE_DAMAGE_FALLOFF.get()) {
                finalDmg *= (float) (1.0 - dist / radius);
            }
            if (finalDmg <= 0) continue;

            // 护甲穿透：用临时 modifier 降低目标护甲
            if (target instanceof LivingEntity living && armorPenetration > 0) {
                AttributeInstance armorAttr = living.getAttribute(Attributes.ARMOR);
                if (armorAttr != null) {
                    ResourceLocation penId = ResourceLocation.fromNamespaceAndPath(
                            PiranPort.MOD_ID, "he_penetration/" + getUUID() + "_" + tickCount);
                    double reduction = armorAttr.getValue() * armorPenetration;
                    armorAttr.addTransientModifier(new AttributeModifier(
                            penId, -reduction, AttributeModifier.Operation.ADD_VALUE));
                    try {
                        living.hurt(damageSources().explosion(this, getOwner()), finalDmg);
                    } finally {
                        armorAttr.removeModifier(penId);
                    }
                    continue;
                }
            }
            target.hurt(damageSources().explosion(this, getOwner()), finalDmg);
        }
    }

    // ===== 碰撞处理 =====

    @Override
    protected Item getDefaultItem() {
        return net.minecraft.world.item.Items.SNOWBALL;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (FriendlyFireHelper.shouldBlockHit(target, getOwner())) return false;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) {
            // Phase 3: 直击伤害 + 爆炸 + 范围伤害
            result.getEntity().hurt(damageSources().thrown(this, getOwner()), damage);
            doExplosion(false);
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide) {
            // Phase 3: 爆炸（方块破坏取决于 ModCommonConfig.EXPLOSION_BLOCK_DAMAGE）+ 范围伤害
            doExplosion(false);
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
        tag.putString("ShellType", shellType);
        tag.putInt("UnderwaterTicks", underwaterTicks);
        tag.putBoolean("UnderwaterHandled", underwaterHandled);
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
        if (tag.contains("ShellType")) shellType = tag.getString("ShellType");
        if (tag.contains("UnderwaterTicks")) underwaterTicks = tag.getInt("UnderwaterTicks");
        if (tag.contains("UnderwaterHandled")) underwaterHandled = tag.getBoolean("UnderwaterHandled");
    }
}
