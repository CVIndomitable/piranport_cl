package com.piranport.artillery;

import com.piranport.component.EquipmentTier;

/**
 * 火炮数值公式（依据：策划决策/数值/06-火炮伤害装填与稀有度公式.md）。
 *
 * <p>口径一律以英寸计；一次开火打出联装数发。稀有度只改装填，不改面板与负重；
 * 初速只改负重，不改面板。纯函数，不触碰注册表，可直接单测。
 */
public final class CannonStatFormula {
    private CannonStatFormula() {}

    /** 浮点向上取整的容差：避免 84.00000000000001 这类误差把整数结果抬一档。 */
    private static final double CEIL_EPS = 1.0e-9;

    /** 初速档（国籍特色，相对该口径标准初速）。 */
    public enum VelocityClass {
        /** 初速 +50%：负重 ×1.30。 */
        HIGH(1.30),
        /** 标准初速：负重不变。 */
        STANDARD(1.0),
        /** 初速 −25%：负重 ×0.85。 */
        LOW(0.85);

        private final double weightMultiplier;

        VelocityClass(double weightMultiplier) { this.weightMultiplier = weightMultiplier; }

        public double weightMultiplier() { return weightMultiplier; }

        /** JSON/终端字符串 → 档位；空或未知返回 STANDARD。 */
        public static VelocityClass parse(String id) {
            if (id == null || id.isBlank()) return STANDARD;
            for (VelocityClass v : values()) if (v.name().equalsIgnoreCase(id)) return v;
            return STANDARD;
        }
    }

    static int ceil(double value) {
        return (int) Math.ceil(value - CEIL_EPS);
    }

    /** 高爆弹单发面板：ceil(0.07c² + 1.2c − 3)。 */
    public static int panelDamage(double caliberInches) {
        return ceil(0.07 * caliberInches * caliberInches + 1.2 * caliberInches - 3);
    }

    /** 齐射伤害 = 单发面板 × 联装数。 */
    public static int salvoDamage(double caliberInches, int barrels) {
        return panelDamage(caliberInches) * barrels;
    }

    /** 基础装填（tick）：(0.5c − 1) × (0.7 + 0.3n) × 20。口径 &lt;2 英寸时为负，不适用。 */
    public static double baseReloadTicks(double caliberInches, int barrels) {
        return (0.5 * caliberInches - 1) * (0.7 + 0.3 * barrels) * 20;
    }

    /** 实际装填（tick）= 基础装填 × 稀有度乘数；保留小数。 */
    public static double reloadTicks(double caliberInches, int barrels, EquipmentTier tier) {
        return baseReloadTicks(caliberInches, barrels) * tier.reloadMultiplier();
    }

    /** 基础负重：ceil(0.04 × (c + 15) × c × (n + 1))。 */
    public static int baseWeight(double caliberInches, int barrels) {
        return ceil(0.04 * (caliberInches + 15) * caliberInches * (barrels + 1));
    }

    /** 负重：先按基础公式向上取整，再乘初速系数，然后再向上取整。 */
    public static int weight(double caliberInches, int barrels, VelocityClass velocity) {
        int base = baseWeight(caliberInches, barrels);
        if (velocity == null || velocity == VelocityClass.STANDARD) return base;
        return ceil(base * velocity.weightMultiplier());
    }

    /** DPS = 齐射伤害 ÷（装填 tick / 20）。 */
    public static double dps(double caliberInches, int barrels, EquipmentTier tier) {
        return salvoDamage(caliberInches, barrels) / (reloadTicks(caliberInches, barrels, tier) / 20.0);
    }

    /**
     * 小数装填 → 本次实际 tick 数。按小数部分概率进位，长期平均等于 {@code ticks}
     * （8.45 tick：45% 取 9、55% 取 8）。{@code roll} 为 [0,1) 均匀随机数。
     */
    public static int resolveTicks(double ticks, double roll) {
        if (!Double.isFinite(ticks) || ticks <= 0) return 0;
        int whole = (int) Math.floor(ticks);
        double frac = ticks - whole;
        return whole + (roll < frac ? 1 : 0);
    }
}
