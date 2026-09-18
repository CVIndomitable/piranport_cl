package com.piranport.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** 经验提升：击杀掉落经验按 I/II/III 级乘 1.2/1.4/1.6，由 ServerGameEvents 结算。 */
public class ExperienceBoostEffect extends MobEffect {

    public ExperienceBoostEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700);
    }
}
