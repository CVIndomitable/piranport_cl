package com.piranport.entity;

import com.piranport.registry.ModBlocks;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Flare projectile — soft white light orb.
 * On hitting a block: places a flare light block on the adjacent face.
 * On hitting an entity: applies 5s Glowing + knockback, no damage.
 */
public class FlareProjectileEntity extends ThrowableItemProjectile {

    private static final int MAX_LIFETIME = 200; // 10 seconds

    public FlareProjectileEntity(EntityType<? extends FlareProjectileEntity> type, Level level) {
        super(type, level);
    }

    public FlareProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntityTypes.FLARE_PROJECTILE.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FLARE_LAUNCHER.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > MAX_LIFETIME) {
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide()) {
            // Place flare light on the hit face
            Direction face = result.getDirection();
            BlockPos placePos = result.getBlockPos().relative(face);
            // Check protection: only place if the owner is allowed to interact here
            Entity owner = getOwner();
            boolean mayPlace = true;
            if (owner instanceof net.minecraft.world.entity.player.Player p) {
                mayPlace = level().mayInteract(p, placePos);
            }
            if (mayPlace) {
                BlockState existing = level().getBlockState(placePos);
                if (existing.isAir() || existing.canBeReplaced()) {
                    level().setBlock(placePos, ModBlocks.FLARE_LIGHT.get().defaultBlockState(),
                            Block.UPDATE_ALL);
                }
            }
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide()) {
            Entity target = result.getEntity();
            // Glowing 5 seconds
            if (target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            }
            // Knockback (no damage)
            double knockStrength = 0.5;
            double dx = this.getX() - target.getX();
            double dz = this.getZ() - target.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.01) {
                target.push(-dx / dist * knockStrength, 0.2, -dz / dist * knockStrength);
                target.hurtMarked = true;
            }
            discard();
        }
    }
}
