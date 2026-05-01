package com.piranport.effect;

import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;

/** 进水 debuff：每秒 1 点魔法伤害。 */
public class FloodingEffect extends MobEffect {
    public FloodingEffect() {
        super(MobEffectCategory.HARMFUL, 0x2060A0);
    }

    @Override
    public void applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amplifier) {
        DamageSources sources = entity.level().damageSources();
        // Find the source of the flooding effect — use generic magic damage
        entity.hurt(sources.magic(), 1.0f);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0; // tick every second
    }
}
