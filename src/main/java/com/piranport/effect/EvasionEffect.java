package com.piranport.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Phase 26: 高速规避 (Evasion)
 *
 * BENEFICIAL effect — light blue 0x80D0FF.
 * The actual dodge logic is in EvasionHandler (LivingHurtEvent):
 *   Level I  (amplifier 0) → 15% dodge chance
 *   Level II (amplifier 1) → 25% dodge chance
 *   Level III(amplifier 2) → 35% dodge chance
 * Only triggers while the player is transformed (変身状態).
 */
public class EvasionEffect extends MobEffect {

    public EvasionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x80D0FF);
    }

    // No tick logic — the dodge is handled in EvasionHandler.onLivingHurt().
}
