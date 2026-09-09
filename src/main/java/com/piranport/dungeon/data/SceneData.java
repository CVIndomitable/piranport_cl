package com.piranport.dungeon.data;

/**
 * 副本场景枚举（整合版 §2.4 + 副本/12 关卡公式 7×5×5）。
 *
 * <p>决策 12：场景 5 类 = 白天/夜战/雷雨/大雾/烈日（天气/能见度轴），与旧版占位
 * FOREST/DESERT/TUNDRA/VOLCANIC/VOID（生物群系轴）完全不一致，已于 2026-09-07 替换。
 * 实际效果在阶段 3 通过场景 Buff（夜战鱼雷强化/雷雨禁放飞/大雾视距低/烈日发光+响应距离大）实现。</p>
 *
 * <p>当前枚举仅供数据建模；MVP 阶段统一按用户口径走"全部海面"地形，场景枚举
 * 暂不生成不同地形变体，仅保留轴向分类用于后续场景 Buff。</p>
 */
public enum SceneData {
    DAY,        // 白天
    NIGHT,      // 夜战：鱼雷强化、舰载机无法降落
    STORM,      // 雷雨：禁放飞、被雷劈
    FOG,        // 大雾：视距低、怪物响应距离降低
    HOT_SUN;    // 烈日：敌人发光、响应距离增大

    public static SceneData fromString(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase()) {
            case "day" -> DAY;
            case "night" -> NIGHT;
            case "storm" -> STORM;
            case "fog" -> FOG;
            case "hot_sun" -> HOT_SUN;
            default -> null;
        };
    }
}