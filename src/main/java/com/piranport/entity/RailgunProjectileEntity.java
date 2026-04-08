package com.piranport.entity;

import com.piranport.combat.FriendlyFireHelper;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Railgun projectile (电磁炮弹) — high-speed, no gravity, 7 fixed damage (bypasses armor).
 */
public class RailgunProjectileEntity extends ThrowableItemProjectile {

    private float damage = 7f;
    private static final int MAX_LIFETIME = 200; // 10 seconds

    public RailgunProjectileEntity(EntityType<? extends RailgunProjectileEntity> type, Level level) {
        super(type, level);
    }

    public RailgunProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntityTypes.RAILGUN_PROJECTILE.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PROJECTILE_BULLET.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0; // no gravity
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > MAX_LIFETIME) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (FriendlyFireHelper.shouldBlockHit(target, getOwner())) return false;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide()) {
            Entity target = result.getEntity();
            // indirectMagic bypasses armor → fixed damage
            target.hurt(damageSources().indirectMagic(this, getOwner()), damage);
            notifyOwner(target);
            discard();
        }
    }

    private void notifyOwner(Entity target) {
        Entity owner = getOwner();
        if (!(owner instanceof Player player)) return;
        Component weaponName = Component.translatable("item.piranport.mysterious_weapon");
        String key = target.isAlive() ? "message.piranport.weapon_hit" : "message.piranport.weapon_kill";
        player.sendSystemMessage(Component.translatable(key, weaponName, target.getDisplayName()));
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide()) {
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("RailgunDamage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("RailgunDamage")) damage = tag.getFloat("RailgunDamage");
    }
}
