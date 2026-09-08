package com.piranport.dungeon.data;

/**
 * 副本场景枚举（整合版 §2.4 + 副本/12 关卡公式 7×5×5）。
 *
 * <p>场景控制环境氛围（背景音乐、粒子效果、装饰结构）；当前枚举仅供数据建模。</p>
 */
public enum SceneData {
    FOREST,    // 森林
    DESERT,    // 沙漠
    TUNDRA,    // 苔原
    VOLCANIC,  // 火山
    VOID;      // 虚空

    public static SceneData fromString(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase()) {
            case "forest" -> FOREST;
            case "desert" -> DESERT;
            case "tundra" -> TUNDRA;
            case "volcanic" -> VOLCANIC;
            case "void" -> VOID;
            default -> null;
        };
    }
}