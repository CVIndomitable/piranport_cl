package com.piranport.effect;

import com.piranport.config.ModEquipmentConfig;

/**
 * 战斗 Buff 数值规则。等级上限由策划定稿，命令施加超等级效果也不能越过数值边界。
 *
 * <p>WHY 从这里直接读终端参数：本类是规避/经验/装填/燃烧四类效果数值的唯一出口
 * （EvasionHandler、TransformationManager、ServerGameEvents、BurningEffect 均调用这里），
 * 参数化本类即全面生效，无需逐个消费者改。默认值 = 下沉前的写死值，行为等价。
 * 键：{@code global.combat_effect.*}。规避按档位拆成 3 个独立参数；经验/装填保留
 * 「基数 ± 步进 × 档位」原算法；燃烧按 4 档位各给一个整数间隔。
 */
public final class CombatEffectRules {
    private CombatEffectRules() {}

    public static float evasionChance(int amplifier) {
        int level = Math.max(0, Math.min(amplifier, 2));
        double chance = switch (level) {
            case 0 -> ModEquipmentConfig.COMBAT_EFFECT_EVASION_L1.get();
            case 1 -> ModEquipmentConfig.COMBAT_EFFECT_EVASION_L2.get();
            default -> ModEquipmentConfig.COMBAT_EFFECT_EVASION_L3.get();
        };
        return (float) chance;
    }

    public static double experienceMultiplier(int amplifier) {
        return ModEquipmentConfig.COMBAT_EFFECT_EXPERIENCE_BASE.get()
                + Math.max(0, Math.min(amplifier, 2)) * ModEquipmentConfig.COMBAT_EFFECT_EXPERIENCE_STEP.get();
    }

    public static double reloadMultiplier(int amplifier) {
        return ModEquipmentConfig.COMBAT_EFFECT_RELOAD_BASE.get()
                - Math.max(0, Math.min(amplifier, 2)) * ModEquipmentConfig.COMBAT_EFFECT_RELOAD_STEP.get();
    }

    public static int burningInterval(int amplifier) {
        int level = Math.max(0, Math.min(amplifier, 3));
        return switch (level) {
            case 0 -> ModEquipmentConfig.COMBAT_EFFECT_BURNING_INTERVAL_L1.get();
            case 1 -> ModEquipmentConfig.COMBAT_EFFECT_BURNING_INTERVAL_L2.get();
            case 2 -> ModEquipmentConfig.COMBAT_EFFECT_BURNING_INTERVAL_L3.get();
            default -> ModEquipmentConfig.COMBAT_EFFECT_BURNING_INTERVAL_L4.get();
        };
    }
}
