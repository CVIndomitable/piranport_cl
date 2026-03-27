package com.piranport.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FloatingTargetEntity extends LivingEntity {

    private final NonNullList<ItemStack> armorSlots = NonNullList.withSize(4, ItemStack.EMPTY);
    private final NonNullList<ItemStack> handSlots  = NonNullList.withSize(2, ItemStack.EMPTY);

    @SuppressWarnings("unchecked")
    public FloatingTargetEntity(EntityType<FloatingTargetEntity> type, Level level) {
        super((EntityType<? extends LivingEntity>) type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    // ===== Equipment slots =====

    @Override
    public Iterable<ItemStack> getArmorSlots() { return armorSlots; }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            return armorSlots.get(slot.getIndex());
        }
        if (slot.getType() == EquipmentSlot.Type.HAND) {
            return handSlots.get(slot.getIndex());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        verifyEquippedItem(stack);
        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            armorSlots.set(slot.getIndex(), stack);
        } else if (slot.getType() == EquipmentSlot.Type.HAND) {
            handSlots.set(slot.getIndex(), stack);
        }
    }

    @Override
    public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }

    // ===== Right-click to equip armor =====

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) return InteractionResult.PASS;

        EquipmentSlot slot = this.getEquipmentSlotForItem(held);
        if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) return InteractionResult.PASS;

        if (!level().isClientSide()) {
            ItemStack current = getItemBySlot(slot);
            setItemSlot(slot, held.copyWithCount(1));
            if (!player.getAbilities().instabuild) held.shrink(1);
            if (!current.isEmpty()) {
                if (!player.getInventory().add(current)) player.drop(current, false);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    // ===== Water buoyancy =====

    @Override
    public void tick() {
        super.tick();
        if (!isAlive()) return;

        if (level().isClientSide()) {
            if (tickCount % 15 == 0) {
                level().addParticle(ParticleTypes.SPLASH,
                        getX() + (random.nextDouble() - 0.5),
                        getY() + 0.1,
                        getZ() + (random.nextDouble() - 0.5),
                        0, 0.05, 0);
            }
            return;
        }

        if (isInWater()) {
            Vec3 vel = getDeltaMovement();
            if (vel.y < 0.05) {
                setDeltaMovement(vel.x * 0.85, vel.y + 0.1, vel.z * 0.85);
            }
        }
    }

    @Override
    protected void serverAiStep() {
        // No AI
    }

    @Override
    public boolean isPickable() { return isAlive(); }

    @Override
    protected void dropAllDeathLoot(net.minecraft.server.level.ServerLevel level,
                                    net.minecraft.world.damagesource.DamageSource damageSource) {
        for (ItemStack stack : armorSlots) {
            if (!stack.isEmpty()) spawnAtLocation(stack);
        }
    }

    // Armor does not persist between sessions — it's a test target
}
