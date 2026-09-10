package com.piranport.dungeon.data;

/**
 * 副本胜利方式枚举（整合版 §2.4 + 副本/12 关卡公式 8×5×5）。
 *
 * <p>StageData 可声明多个胜利条件同时满足；具体判定逻辑由关卡设计落地。</p>
 *
 * <p>2026-09-09 依据《副本/12-关卡多样化扩展枚举》新增：</p>
 * <ul>
 *   <li>{@link #REACH_POINT} 运输任务：玩家到达指定位置</li>
 *   <li>{@link #CAPTURE_FLAG} 夺旗任务：存活计时 或 全灭敌人 双路径（OR 判定）</li>
 * </ul>
 */
public enum VictoryCondition {
    KILL_ALL,        // 歼灭战：击溃所有敌人
    KILL_BOSS,       // 击破首领：击杀 BOSS 节点旗舰
    SURVIVE,         // 存活：坚持 N 秒
    DEFEND_POINT,    // 守点：守住某点不丢失
    COLLECT_ITEMS,   // 收集：收集指定物品
    ESCORT,          // 护送：护送目标抵达
    PUZZLE,          // 解谜：完成谜题结构
    REACH_POINT,     // 运输（2026-09-09 新增）：玩家到达指定位置
    CAPTURE_FLAG;    // 夺旗（2026-09-09 新增）：存活计时 或 全灭（双路径 OR）

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
            case "reach_point" -> REACH_POINT;
            case "capture_flag" -> CAPTURE_FLAG;
            default -> null;
        };
    }
}