package com.piranport.entity;

import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class TorpedoEntity extends ThrowableItemProjectile {
    private int caliber = 533;
    private float damage = 18f;
    private float torpedoSpeed = 1.2f;
    private int lifetime = 1200; // 1 minute hard cap
    private float explosionRadius = 2.0f;

    // Required constructor for entity type registration
    public TorpedoEntity(EntityType<? extends TorpedoEntity> type, Level level) {
        super(type, level);
    }

    // Constructor for launching
    public TorpedoEntity(Level level, LivingEntity shooter, int caliber) {
        super(ModEntityTypes.TORPEDO_ENTITY.get(), shooter, level);
        this.caliber = caliber;
        if (caliber == 610) {
            this.damage = 28f;
            this.torpedoSpeed = 1.0f;
            this.lifetime = 1200;
            this.explosionRadius = 2.5f;
        } else {
            this.damage = 18f;
            this.torpedoSpeed = 1.2f;
            this.lifetime = 1200;
            this.explosionRadius = 2.0f;
        }
    }

    public float getTorpedoSpeed() {
        return torpedoSpeed;
    }

    @Override
    protected Item getDefaultItem() {
        return caliber == 610 ? ModItems.TORPEDO_610MM.get() : ModItems.TORPEDO_533MM.get();
    }

    @Override
    public void tick() {
        // super.tick() handles movement, collision detection, drag (0.99), and gravity (0 via getDefaultGravity)
        super.tick();

        if (isRemoved()) return;

        // 1. 剩余航程检查 — 超时自爆
        if (--lifetime <= 0) {
            if (!level().isClientSide()) {
                level().explode(this, getX(), getY(), getZ(), explosionRadius, Level.ExplosionInteraction.TNT);
            }
            discard();
            return;
        }

        Vec3 motion = getDeltaMovement();

        // 2. 恢复水平速度（抵消 super.tick() 施加的 0.99 空气阻力）
        double currentH = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (currentH > 0.001) {
            double scale = torpedoSpeed / currentH;
            motion = new Vec3(motion.x * scale, motion.y, motion.z * scale);
        }

        // 3. 水面贴合 AI
        BlockPos pos = blockPosition();
        boolean inAir = level().getBlockState(pos).isAir();
        boolean waterBelow = level().getBlockState(pos.below()).getFluidState().is(Fluids.WATER);
        boolean inWater = level().getBlockState(pos).getFluidState().is(Fluids.WATER);

        if (inAir && waterBelow) {
            // 水面航行：清除垂直速度
            setDeltaMovement(motion.x, 0, motion.z);
        } else if (inWater) {
            // 在水中：向上浮
            setDeltaMovement(motion.x, motion.y + 0.04, motion.z);
        } else {
            // 空中：向下坠落
            setDeltaMovement(motion.x, motion.y - 0.06, motion.z);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!level().isClientSide()) {
            Entity target = result.getEntity();
            target.hurt(damageSources().thrown(this, getOwner()), damage);
            // 附加进水 debuff（3秒，每秒 1 点魔法伤害）
            if (target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(ModMobEffects.FLOODING, 60, 0));
            }
            notifyOwner(target);
            discard();
        }
    }

    private void notifyOwner(Entity target) {
        Entity owner = getOwner();
        if (!(owner instanceof Player player)) return;
        Component weaponName = new ItemStack(getDefaultItem()).getHoverName();
        String key = target.isAlive() ? "message.piranport.weapon_hit" : "message.piranport.weapon_kill";
        player.sendSystemMessage(Component.translatable(key, weaponName, target.getDisplayName()));
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!level().isClientSide()) {
            // 水中命中方块：静默消失，不爆炸
            if (level().getBlockState(result.getBlockPos()).getFluidState().is(Fluids.WATER)) {
                discard();
                return;
            }
            // 非水中命中方块：小范围爆炸
            level().explode(this, getX(), getY(), getZ(), explosionRadius, Level.ExplosionInteraction.TNT);
            discard();
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0; // 重力由 tick() 中的自定义逻辑控制
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Caliber", caliber);
        tag.putFloat("Damage", damage);
        tag.putFloat("TorpedoSpeed", torpedoSpeed);
        tag.putInt("Lifetime", lifetime);
        tag.putFloat("ExplosionRadius", explosionRadius);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        caliber = tag.getInt("Caliber");
        damage = tag.getFloat("Damage");
        torpedoSpeed = tag.getFloat("TorpedoSpeed");
        lifetime = tag.getInt("Lifetime");
        explosionRadius = tag.getFloat("ExplosionRadius");
    }
}
