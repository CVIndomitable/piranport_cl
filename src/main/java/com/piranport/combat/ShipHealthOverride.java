package com.piranport.combat;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** 用一个可撤销的修正值覆盖舰装血量，保留原有药水和装备修正供解除变身后恢复。 */
public final class ShipHealthOverride {
    private ShipHealthOverride() {}

    /**
     * 纯函数：计算将属性修正到目标最大值所需的 additive 修正值。
     * 不依赖 Minecraft 运行时，可直接单元测试。
     *
     * @param baseValue  属性基础值
     * @param modifiers  当前已有的修正列表
     * @param maximum    目标最大值
     * @return 需要的修正值；若倍率无效则返回 NaN
     */
    public static double computeCorrection(double baseValue, Iterable<AttributeModifier> modifiers, double maximum) {
        double additive = 0.0;
        double baseMultiplier = 1.0;
        double totalMultiplier = 1.0;
        for (AttributeModifier modifier : modifiers) {
            switch (modifier.operation()) {
                case ADD_VALUE -> additive += modifier.amount();
                case ADD_MULTIPLIED_BASE -> baseMultiplier += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> totalMultiplier *= 1.0 + modifier.amount();
            }
        }
        double multiplier = baseMultiplier * totalMultiplier;
        // 原版生命提升及常规属性强化均为正倍率；无效倍率不写入非有限属性值。
        if (!(multiplier > 0.0) || !Double.isFinite(multiplier)) return Double.NaN;
        return maximum / multiplier - baseValue - additive;
    }

    public static void apply(AttributeInstance attribute, double maximum) {
        if (attribute == null || Math.abs(attribute.getValue() - maximum) < 1.0E-6) return;
        attribute.removeModifier(TransformationManager.HEALTH_MODIFIER_ID);
        double correction = computeCorrection(attribute.getBaseValue(), attribute.getModifiers(), maximum);
        if (Double.isNaN(correction)) return;
        attribute.addTransientModifier(new AttributeModifier(TransformationManager.HEALTH_MODIFIER_ID,
                correction, AttributeModifier.Operation.ADD_VALUE));
    }
}
