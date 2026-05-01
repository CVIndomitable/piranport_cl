package com.piranport.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 高速规避。阶段 3 仅注册占位让食物 effect 引用；
 * 真正的闪避在阶段 5/7 的 LivingHurtEvent 处理器里实现。
 */
public class EvasionEffect extends MobEffect {
    public EvasionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x80D0FF);
    }
}
