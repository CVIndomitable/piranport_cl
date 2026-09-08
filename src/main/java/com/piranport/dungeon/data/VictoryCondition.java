package com.piranport.dungeon.data;

/**
 * 副本胜利方式枚举（整合版 §2.4 + 副本/12 关卡公式 7×5×5）。
 *
 * <p>StageData 可声明多个胜利条件同时满足；具体判定逻辑由关卡设计落地。</p>
 */
public enum VictoryCondition {
    KILL_ALL,        // 歼灭战：击溃所有敌人
    KILL_BOSS,       // 击破首领：击杀 BOSS 节点旗舰
    SURVIVE,         // 存活：坚持 N 秒
    DEFEND_POINT,    // 守点：守住某点不丢失
    COLLECT_ITEMS,   // 收集：收集指定物品
    ESCORT,          // 护送：护送目标抵达
    PUZZLE;          // 解谜：完成谜题结构

    public static VictoryCondition fromString(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase()) {
            case "kill_all" -> KILL_ALL;
            case "kill_boss" -> KILL_BOSS;
            case "survive" -> SURVIVE;
            case "defend_point" -> DEFEND_POINT;
            case "collect_items" -> COLLECT_ITEMS;
            case "escort" -> ESCORT;
            case "puzzle" -> PUZZLE;
            default -> null;
        };
    }
}