package com.piranport.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Phase 26: 装填加速 (Reload Boost)
 *
 * BENEFICIAL effect — cyan 0x00C8C8.
 * The actual reload acceleration is applied in TransformationManager.boostedCooldown():
 *   Level I  (amplifier 0) → 2× reload speed (cooldown ÷ 2)
 *   Level II (amplifier 1) → 3× reload speed (cooldown ÷ 3)
 * Only takes effect while the player is transformed.
 */
public class ReloadBoostEffect extends MobEffect {

    public ReloadBoostEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00C8C8);
    }

    // No tick logic — the boost is applied at fire time via TransformationManager.boostedCooldown().
}
