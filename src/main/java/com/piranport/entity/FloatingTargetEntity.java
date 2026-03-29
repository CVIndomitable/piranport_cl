package com.piranport.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A floating target dummy that sits on water and can be equipped with armor.
 * Extends ArmorStand so it renders with the armor stand model and supports
 * all vanilla armor slot behavior.
 */
public class FloatingTargetEntity extends ArmorStand {

    @SuppressWarnings("unchecked")
    public FloatingTargetEntity(EntityType<FloatingTargetEntity> type, Level level) {
        super((EntityType<? extends ArmorStand>) type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return ArmorStand.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!isAlive()) return;

        // Water buoyancy: push upward while submerged
        if (!level().isClientSide() && isInWater()) {
            Vec3 vel = getDeltaMovement();
            if (vel.y < 0.05) {
                setDeltaMovement(vel.x * 0.85, vel.y + 0.1, vel.z * 0.85);
            }
        }

        // Splash particles on client
        if (level().isClientSide() && tickCount % 15 == 0) {
            level().addParticle(ParticleTypes.SPLASH,
                    getX() + (random.nextDouble() - 0.5),
                    getY() + 0.1,
                    getZ() + (random.nextDouble() - 0.5),
                    0, 0.05, 0);
        }
    }

    @Override
    public boolean isPickable() { return isAlive(); }
}
