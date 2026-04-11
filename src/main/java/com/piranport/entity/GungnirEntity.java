package com.piranport.entity;

import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Thrown Gungnir entity.
 * - Deals 6 damage on entity hit
 * - Destroys leaf blocks on contact
 * - Returns to owner on non-leaf block hit or when 10+ blocks away
 * - Passes through blocks while returning
 */
public class GungnirEntity extends ThrowableItemProjectile {

    private static final float THROW_DAMAGE = 6.0f;
    private static final double MAX_DISTANCE_SQ = 10.0 * 10.0; // 10 blocks squared
    private static final double RETURN_SPEED = 0.8;
    private static final int MAX_LIFETIME = 600; // 30 seconds safety

    private static final EntityDataAccessor<Boolean> DATA_RETURNING =
            SynchedEntityData.defineId(GungnirEntity.class, EntityDataSerializers.BOOLEAN);

    private ItemStack gungnirStack = ItemStack.EMPTY;

    public GungnirEntity(EntityType<? extends GungnirEntity> type, Level level) {
        super(type, level);
    }

    public GungnirEntity(Level level, LivingEntity shooter, ItemStack stack) {
        super(ModEntityTypes.GUNGNIR.get(), shooter, level);
        this.gungnirStack = stack.copy();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RETURNING, false);
    }

    public boolean isReturning() {
        return entityData.get(DATA_RETURNING);
    }

    private void setReturning(boolean returning) {
        entityData.set(DATA_RETURNING, returning);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GUNGNIR.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.01; // very slight gravity
    }

    @Override
    public void tick() {
        if (isReturning()) {
            // Bypass normal projectile tick — fly directly to owner
            baseTick();
            noPhysics = true; // pass through blocks while returning

            Entity owner = getOwner();
            if (owner == null || !owner.isAlive()) {
                if (!level().isClientSide()) {
                    spawnAtLocation(getReturnStack());
                    discard();
                }
                return;
            }

            Vec3 target = owner.getEyePosition();
            Vec3 toOwner = target.subtract(position());
            double dist = toOwner.length();

            if (dist < 1.5) {
                // Close enough — return item to owner
                if (!level().isClientSide()) {
                    returnToOwner(owner);
                    discard();
                }
                return;
            }

            Vec3 velocity = toOwner.normalize().scale(RETURN_SPEED);
            setDeltaMovement(velocity);
            setPos(position().add(velocity));
            return;
        }

        // Normal flight — reset noPhysics for proper collision
        noPhysics = false;
        super.tick();

        if (!level().isClientSide()) {
            // Distance check
            Entity owner = getOwner();
            if (owner != null && distanceToSqr(owner) > MAX_DISTANCE_SQ) {
                setReturning(true);
            }

            // Safety timeout
            if (tickCount > MAX_LIFETIME) {
                setReturning(true);
            }

            // Break leaves at current position
            breakLeavesAtPosition();
        }
    }

    private void breakLeavesAtPosition() {
        net.minecraft.core.BlockPos pos = blockPosition();
        BlockState state = level().getBlockState(pos);
        if (state.is(BlockTags.LEAVES)) {
            level().destroyBlock(pos, false);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!level().isClientSide()) {
            BlockState state = level().getBlockState(result.getBlockPos());
            if (state.is(BlockTags.LEAVES)) {
                // Destroy leaf and keep flying
                level().destroyBlock(result.getBlockPos(), false);
            } else {
                // Non-leaf block — start returning
                setReturning(true);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!level().isClientSide()) {
            Entity target = result.getEntity();
            Entity owner = getOwner();
            // Don't hurt the owner
            if (target == owner) return;
            target.hurt(damageSources().thrown(this, owner), THROW_DAMAGE);
        }
        // Continue flying after hitting an entity (doesn't trigger return)
    }

    private void returnToOwner(Entity owner) {
        ItemStack stack = getReturnStack();
        // Damage the item
        if (owner instanceof Player player) {
            // Use main hand as default since we can't reliably track the original hand
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(net.minecraft.world.InteractionHand.MAIN_HAND));
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        } else {
            spawnAtLocation(stack);
        }
    }

    private ItemStack getReturnStack() {
        return gungnirStack.isEmpty() ? new ItemStack(ModItems.GUNGNIR.get()) : gungnirStack.copy();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Returning", isReturning());
        if (!gungnirStack.isEmpty()) {
            tag.put("GungnirItem", gungnirStack.save(registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setReturning(tag.getBoolean("Returning"));
        if (tag.contains("GungnirItem")) {
            gungnirStack = ItemStack.parse(registryAccess(), tag.getCompound("GungnirItem"))
                    .orElse(new ItemStack(ModItems.GUNGNIR.get()));
        }
    }
}
