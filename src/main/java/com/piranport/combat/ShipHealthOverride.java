package com.piranport.combat;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** 用一个可撤销的修正值覆盖舰装血量，保留原有药水和装备修正供解除变身后恢复。 */
public final class ShipHealthOverride {
    private ShipHealthOverride() {}

    public static void apply(AttributeInstance attribute, double maximum) {
        if (attribute == null || Math.abs(attribute.getValue() - maximum) < 1.0E-6) return;
        attribute.removeModifier(TransformationManager.HEALTH_MODIFIER_ID);
        double additive = 0.0;
        double baseMultiplier = 1.0;
        double totalMultiplier = 1.0;
        for (AttributeModifier modifier : attribute.getModifiers()) {
            switch (modifier.operation()) {
                case ADD_VALUE -> additive += modifier.amount();
                case ADD_MULTIPLIED_BASE -> baseMultiplier += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> totalMultiplier *= 1.0 + modifier.amount();
            }
        }
        double multiplier = baseMultiplier * totalMultiplier;
        // 原版生命提升及常规属性强化均为正倍率；无效倍率不写入非有限属性值。
        if (!(multiplier > 0.0) || !Double.isFinite(multiplier)) return;
        double correction = maximum / multiplier - attribute.getBaseValue() - additive;
        attribute.addTransientModifier(new AttributeModifier(TransformationManager.HEALTH_MODIFIER_ID,
                correction, AttributeModifier.Operation.ADD_VALUE));
    }
}
