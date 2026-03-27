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
     * Every 40 ticks: 15% chance of small contained explosion (power 0.8, no block damage).
     */
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            entity.hurt(entity.damageSources().onFire(), 0.5f);
            if (entity.level().getGameTime() % 40 == 0 && entity.getRandom().nextFloat() < 0.15f) {
                entity.level().explode(entity,
                        entity.getX(), entity.getY(), entity.getZ(),
                        0.8f, Level.ExplosionInteraction.NONE);
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 3 == 0;
    }
}
