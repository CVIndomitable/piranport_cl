package com.piranport.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class FlammableEffect extends MobEffect {

    public FlammableEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF6600);
    }

    /**
     * Every 3 ticks: deal 0.5f fire damage.
     * Every ~39 ticks: 15% chance of small explosion.
     * Uses entity.tickCount instead of instance variable to avoid singleton shared state.
     */
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            entity.hurt(entity.damageSources().onFire(), 0.5f);
            // tickCount % 39 == 0 fires once every ~39 ticks (~2 seconds)
            if (entity.tickCount % 39 == 0) {
                if (entity.getRandom().nextFloat() < 0.15f) {
                    // Source is the entity itself for proper death messages
                    entity.level().explode(entity,
                            entity.getX(), entity.getY(), entity.getZ(),
                            0.8f, Level.ExplosionInteraction.NONE);
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 3 == 0;
    }
}
