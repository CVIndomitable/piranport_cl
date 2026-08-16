package com.piranport.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 进水 (Flooding) Buff — 策划 §7.7 表。
 *
 * <p>1 级（策划未定义更高等级），固定每秒 1 点魔法伤害。
 * 颜色：深蓝 0x3366AA。
 */
public class FloodingEffect extends MobEffect {
    public FloodingEffect() {
        super(MobEffectCategory.HARMFUL, 0x3366AA);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            // 策划 §7.7：进水 1 级，每秒 1 点魔法伤害
            entity.hurt(entity.damageSources().magic(), 1.0f);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // 每秒 1 tick (20 tick = 1s)
        return duration > 0 && duration % 20 == 0;
    }
}
