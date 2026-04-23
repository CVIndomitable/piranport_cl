package com.piranport.entity;

import com.piranport.combat.HitNotifier;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Individual pellet fired by a Type 3 (Sanshiki) shell.
 * 64 pellets are spawned in an 8x8 pyramidal spread pattern.
 * Each pellet deals 1/4 of the same-caliber HE shell's base damage.
 */
public class SanshikiPelletEntity extends ThrowableItemProjectile {

    private float damage = 1.5f;
    private ItemStack shellForRender = ItemStack.EMPTY;

    private static final int MAX_LIFETIME = 200; // 10 seconds

    public SanshikiPelletEntity(EntityType<? extends SanshikiPelletEntity> type, Level level) {
        super(type, level);
    }

    public SanshikiPelletEntity(Level level, LivingEntity shooter, float damage, ItemStack shellForRender) {
        super(ModEntityTypes.SANSHIKI_PELLET.get(), shooter, level);
        this.damage = damage;
        this.shellForRender = shellForRender.copyWithCount(1);
        setItem(this.shellForRender);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PROJECTILE_BULLET.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > MAX_LIFETIME) discard();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.06;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = getOwner();
        if (owner != null && target == owner) return false; // don't hit the firer
        if (com.piranport.combat.FriendlyFireHelper.shouldBlockHit(target, owner)) return false;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide()) {
            Entity target = result.getEntity();
            target.hurt(damageSources().thrown(this, getOwner()), damage);
            notifyOwner(target);
            discard();
        }
    }

    private void notifyOwner(Entity target) {
        Entity owner = getOwner();
        if (!(owner instanceof Player player)) return;
        Component weaponName = shellForRender.isEmpty()
                ? getDefaultItem().getDescription()
                : shellForRender.getHoverName();
        String key = target.isAlive() ? "message.piranport.weapon_hit" : "message.piranport.weapon_kill";
        HitNotifier.send(player, Component.translatable(key, weaponName, target.getDisplayName()));
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
        tag.putFloat("PelletDamage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("PelletDamage")) damage = tag.getFloat("PelletDamage");
    }
}
