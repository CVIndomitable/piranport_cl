package com.piranport.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Phase 26: 装填加速 (Reload Boost)
 *
 * BENEFICIAL effect — cyan 0x00C8C8.
 * The actual reload acceleration is applied in TransformationManager.boostedCooldown():
 *   Level I/II/III (amplifier 0/1/2) → 0.9/0.8/0.7x original time
 */
public class ReloadBoostEffect extends MobEffect {

    public ReloadBoostEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00C8C8);
    }

    // No tick logic — cooldown/draw-time hooks read the effect when needed.
}
