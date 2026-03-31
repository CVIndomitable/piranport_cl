package com.piranport.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class FlammableEffect extends MobEffect {

    public FlammableEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF6600);
    }

    /** Counts ticks between explosion checks (incremented every applyEffectTick call). */
    private int explosionTimer = 0;

    /**
     * Every 3 ticks: deal 0.5f fire damage.
     * Every ~40 ticks (13 calls × 3 tick interval ≈ 39 ticks): 15% chance of small explosion.
     */
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            entity.hurt(entity.damageSources().onFire(), 0.5f);
            explosionTimer++;
            if (explosionTimer >= 13) { // 13 × 3 ticks = 39 ticks ≈ 2 seconds
                explosionTimer = 0;
                if (entity.getRandom().nextFloat() < 0.15f) {
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
