package com.piranport.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class FloodingEffect extends MobEffect {
    public FloodingEffect() {
        super(MobEffectCategory.HARMFUL, 0x3366AA);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            // Higher amplifier = more damage: 1.0 base + 0.5 per level
            float damage = 1.0f + amplifier * 0.5f;
            entity.hurt(entity.damageSources().magic(), damage);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Higher amplifier = more frequent ticks: 20, 15, 10, 7, 5 (min 5)
        int interval = Math.max(5, 20 - amplifier * 5);
        return duration > 0 && duration % interval == 0;
    }
}
