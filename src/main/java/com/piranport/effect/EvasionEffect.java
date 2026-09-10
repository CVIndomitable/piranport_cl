package com.piranport.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Phase 26: 高速规避 (Evasion)
 *
 * BENEFICIAL effect — light blue 0x80D0FF.
 * The actual dodge logic is in EvasionHandler (LivingHurtEvent):
 *   Level I  (amplifier 0) → 10% dodge chance
 *   Level II (amplifier 1) → 20% dodge chance
 *   Level III(amplifier 2) → 30% dodge chance
 * 公式: chance = (amplifier + 1) * 0.10（详见 EvasionHandler.java:25）
 * Only triggers while the player is transformed (変身状態).
 *
 * 依据：策划决策/战斗/02-Buff系统核心设计.md（10/20/30% 回归）与 06-高速规避数值偏移.md。
 */
public class EvasionEffect extends MobEffect {

    public EvasionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x80D0FF);
    }

    // No tick logic — the dodge is handled in EvasionHandler.onLivingHurt().
}
