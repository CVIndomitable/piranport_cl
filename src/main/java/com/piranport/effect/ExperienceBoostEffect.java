package com.piranport.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 经验提升 (Experience Boost)
 *
 * BENEFICIAL effect — gold 0xFFD700.
 * Marker effect: actual XP boost is applied in GameEvents.onXpDrop().
 * Level I (amplifier 0) → +50% XP from mob kills
 */
public class ExperienceBoostEffect extends MobEffect {

    public ExperienceBoostEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700);
    }
}
