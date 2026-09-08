package com.piranport.dungeon.data;

/**
 * 副本战斗限制枚举（整合版 §2.4 + 副本/12 关卡公式 7×5×5）。
 *
 * <p>判定逻辑由 PlayerTickEvent + 各种 *Event 监听实现；当前枚举仅供数据建模。</p>
 */
public enum CombatRestriction {
    NO_MELEE,    // 禁用近战
    NO_RANGED,   // 禁用远程炮击
    NO_HEALING,  // 禁用治疗
    NO_DODGE,    // 禁用规避
    TIME_LIMIT;  // 时间限制

    public static CombatRestriction fromString(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase()) {
            case "no_melee" -> NO_MELEE;
            case "no_ranged" -> NO_RANGED;
            case "no_healing" -> NO_HEALING;
            case "no_dodge" -> NO_DODGE;
            case "time_limit" -> TIME_LIMIT;
            default -> null;
        };
    }
}