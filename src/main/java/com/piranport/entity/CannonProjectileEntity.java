package com.piranport.entity;

import com.piranport.PiranPort;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class CannonProjectileEntity extends ThrowableItemProjectile {
    private float damage = 6f;
    private boolean isHE = true;
    private float explosionPower = 1.5f;

    // Required constructor for entity type registration
    public CannonProjectileEntity(EntityType<? extends CannonProjectileEntity> type, Level level) {
        super(type, level);
    }

    // Constructor for firing
    public CannonProjectileEntity(Level level, LivingEntity shooter,
                                   ItemStack shellItem, float damage,
                                   boolean isHE, float explosionPower) {
        super(ModEntityTypes.CANNON_PROJECTILE.get(), shooter, level);
        setItem(shellItem);
        this.damage = damage;
        this.isHE = isHE;
        this.explosionPower = explosionPower;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SMALL_HE_SHELL.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) {
            Entity target = result.getEntity();
            if (isHE) {
                // HE: area explosion damage
                level().explode(this, getX(), getY(), getZ(), explosionPower,
                        Level.ExplosionInteraction.NONE);
            } else {
                // AP: 130% direct damage, ignores 50% of target armor
                float apDamage = damage * 1.3f;
                if (target instanceof LivingEntity living) {
                    AttributeInstance armorAttr = living.getAttribute(Attributes.ARMOR);
                    ResourceLocation apPenId = ResourceLocation.fromNamespaceAndPath(
                            PiranPort.MOD_ID, "ap_penetration");
                    if (armorAttr != null) {
                        double halfArmor = living.getAttributeValue(Attributes.ARMOR) * 0.5;
                        armorAttr.addTransientModifier(new AttributeModifier(
                                apPenId, -halfArmor, AttributeModifier.Operation.ADD_VALUE));
                    }
                    try {
                        living.hurt(damageSources().thrown(this, getOwner()), apDamage);
                    } finally {
                        if (armorAttr != null) {
                            armorAttr.removeModifier(apPenId);
                        }
                    }
                } else {
                    target.hurt(damageSources().thrown(this, getOwner()), apDamage);
                }
            }
            notifyOwner(target);
        }
    }

    private void notifyOwner(Entity target) {
        Entity owner = getOwner();
        if (!(owner instanceof Player player)) return;
        Component weaponName = getItem().getHoverName();
        String key = target.isAlive() ? "message.piranport.weapon_hit" : "message.piranport.weapon_kill";
        player.sendSystemMessage(Component.translatable(key, weaponName, target.getDisplayName()));
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide) {
            if (isHE) {
                level().explode(this, getX(), getY(), getZ(), explosionPower,
                        Level.ExplosionInteraction.NONE);
            }
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

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putBoolean("IsHE", isHE);
        tag.putFloat("ExplosionPower", explosionPower);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        isHE = tag.getBoolean("IsHE");
        explosionPower = tag.getFloat("ExplosionPower");
    }
}
