package com.piranport.entity;

import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class BulletEntity extends ThrowableItemProjectile {

    private float damage = 2f;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
    }

    public BulletEntity(Level level, float damage) {
        super(ModEntityTypes.BULLET.get(), level);
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FIGHTER_AMMO.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.005;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!level().isClientSide()) {
            Entity target = result.getEntity();
            target.hurt(damageSources().thrown(this, getOwner()), damage);
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("BulletDamage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BulletDamage")) damage = tag.getFloat("BulletDamage");
    }
}
