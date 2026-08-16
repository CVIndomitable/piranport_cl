package com.piranport.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 策划 §7.7：着火 Buff
 *
 * <p>每 3/level 秒（按 Minecraft "秒"=20 tick 折算）对生物造成 1 点魔法伤害。
 * 等级 1-4 (amplifier 0-3) → 间隔 60/40/30/20 tick，策划要求伤害恒为 1 点/次。
 */
public class BurningEffect extends MobEffect {

    public BurningEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF6600);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            entity.hurt(entity.damageSources().magic(), 1.0f);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // amplifier=0 → 60t=3s, amplifier=1 → 40t=2s, amplifier=2 → 30t=1.5s, amplifier=3 → 20t=1s
        int interval = switch (Math.min(amplifier, 3)) {
            case 0 -> 60;
            case 1 -> 40;
            case 2 -> 30;
            default -> 20;
        };
        return duration > 0 && duration % interval == 0;
    }
}