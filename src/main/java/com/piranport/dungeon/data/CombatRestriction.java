package com.piranport.dungeon.data;

/**
 * 副本战斗限制枚举（整合版 §2.4 + 副本/12 关卡公式 + 副本/14 反潜战）。
 *
 * <p>由决策 12（4 类）+ 决策 14（新增"反潜战"维度）合计 5 类。
 * 判定逻辑由 PlayerTickEvent + 各种 *Event 监听实现；当前枚举仅供数据建模。</p>
 *
 * <p>注意：旧版（NO_MELEE/NO_RANGED/NO_HEALING/NO_DODGE/TIME_LIMIT）是占位枚举，
 * 已于 2026-09-07 决策替换为决策 12/14 定义的"弹药/航空/火炮"轴向限制。</p>
 */
public enum CombatRestriction {
    NO_TORPEDO,    // 鱼雷限制：禁用鱼雷
    AIR_ONLY,      // 仅限航空：只允许舰载机/导弹攻击
    LIMITED_AIR,   // 限制航空：放飞数量/批次受限
    GUNS_ONLY,     // 仅限火炮：禁用鱼雷+航空
    ANTI_SUB;      // 反潜战：限定反潜装备有效（决策 14）

    public static CombatRestriction fromString(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase()) {
            case "no_torpedo" -> NO_TORPEDO;
            case "air_only" -> AIR_ONLY;
            case "limited_air" -> LIMITED_AIR;
            case "guns_only" -> GUNS_ONLY;
            case "anti_sub" -> ANTI_SUB;
            default -> null;
        };
    }
}