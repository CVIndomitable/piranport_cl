package com.piranport.entity;

import com.piranport.config.ModCommonConfig;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class AerialBombEntity extends ThrowableItemProjectile {

    private float damage = 10f;
    private float explosionPower = 2.5f;

    /** Required by entity type registration. */
    public AerialBombEntity(EntityType<? extends AerialBombEntity> type, Level level) {
        super(type, level);
    }

    /** Spawned by AircraftEntity; position and velocity set externally after construction. */
    public AerialBombEntity(Level level, float damage, float explosionPower) {
        super(ModEntityTypes.AERIAL_BOMB.get(), level);
        this.damage = damage;
        this.explosionPower = explosionPower;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.AERIAL_BOMB.get();
    }

    @Override
    public boolean isCurrentlyGlowing() {
        if (level().isClientSide() && com.piranport.ClientTickHandler.isHighlightEnabled()) {
            return true;
        }
        return super.isCurrentlyGlowing();
    }

    private static final int MAX_LIFETIME = 600; // 30 seconds

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > MAX_LIFETIME) discard();
    }

    /** Stronger gravity than default (0.03) to simulate free-fall bomb. */
    @Override
    protected double getDefaultGravity() {
        return 0.06;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (com.piranport.combat.FriendlyFireHelper.shouldBlockHit(target, getOwner())) return false;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide()) {
            Entity target = result.getEntity();
            target.hurt(damageSources().thrown(this, getOwner()), damage);
            Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                    ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
            level().explode(this, getX(), getY(), getZ(), explosionPower, interaction);
            notifyOwner(target);
            discard();
        }
    }

    private void notifyOwner(Entity target) {
        Entity owner = getOwner();
        if (!(owner instanceof Player player)) return;
        Component weaponName = getDefaultItem().getDescription();
        String key = target.isAlive() ? "message.piranport.weapon_hit" : "message.piranport.weapon_kill";
        player.sendSystemMessage(Component.translatable(key, weaponName, target.getDisplayName()));
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!level().isClientSide()) {
            Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                    ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
            level().explode(this, getX(), getY(), getZ(), explosionPower, interaction);
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("BombDamage", damage);
        tag.putFloat("ExplosionPower", explosionPower);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BombDamage")) damage = tag.getFloat("BombDamage");
        if (tag.contains("ExplosionPower")) explosionPower = tag.getFloat("ExplosionPower");
    }
}
