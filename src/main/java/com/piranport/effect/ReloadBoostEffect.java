package com.piranport.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 装填加速。阶段 3 仅注册占位以让食物 effect 引用，实际加速逻辑等
 * 阶段 5（实体 + 火控）/ 阶段 8（按键 + 客户端 tick）回填。
 */
public class ReloadBoostEffect extends MobEffect {
    public ReloadBoostEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00C8C8);
    }
}
