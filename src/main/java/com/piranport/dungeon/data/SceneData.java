package com.piranport.dungeon.data;

/**
 * 副本天气枚举，规则见《副本/27-天气与昼夜》。
 *
 * <p>战斗影响由 {@code com.piranport.dungeon.weather} 包在服务端实时结算；
 * 默认天气是白天，无任何加成或减益。</p>
 */
public enum SceneData {
    DAY,        // 白天：无额外影响
    NIGHT,      // 夜战：放飞的舰载机无法降落，无鱼雷强化
    STORM,      // 雷雨：无法放飞飞机，闪电伤害玩家
    FOG,        // 大雾：怪物响应距离降低（视觉部分未做）
    HOT_SUN,    // 烈日：敌人发光，怪物响应距离增加
    CLEAR;      // 晴天：潜艇常驻高亮，与声呐高亮相同

    public static SceneData fromString(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase()) {
            case "day" -> DAY;
            case "night" -> NIGHT;
            case "storm" -> STORM;
            case "fog" -> FOG;
            case "hot_sun" -> HOT_SUN;
            case "clear" -> CLEAR;
            default -> null;
        };
    }
}
