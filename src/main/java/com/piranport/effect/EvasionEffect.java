package com.piranport.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** 高速规避：I/II/III 为 10%/20%/30%，由 EvasionHandler 执行变身玩家的概率免伤。 */
public class EvasionEffect extends MobEffect {

    public EvasionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x80D0FF);
    }

    // 不做周期处理，免伤在受伤事件中结算。
}
