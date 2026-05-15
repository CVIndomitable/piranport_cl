package com.piranport.artillery;

import com.piranport.registry.ModEntityTypes;
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

/**
 * 炮弹实体基类。
 * Phase 1: 直线飞行，碰撞后消失并造成基础伤害。
 */
public class ShellEntity extends ThrowableItemProjectile {
    private float damage = 6f;
    private float explosionRadius = 1.0f;

    // 实体类型注册用
    public ShellEntity(EntityType<? extends ShellEntity> type, Level level) {
        super(type, level);
    }

    // 发射用
    public ShellEntity(Level level, LivingEntity shooter, ItemStack shellItem, float damage) {
        super(ModEntityTypes.SHELL_PROJECTILE.get(), shooter, level);
        setItem(shellItem);
        this.damage = damage;
    }

    public void setExplosionRadius(float radius) { this.explosionRadius = radius; }

    /** Phase 1: 直线飞行，无重力 */
    @Override
    protected double getDefaultGravity() { return 0; }

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
}
