package com.piranport.dungeon;

import com.piranport.dungeon.data.VictoryCondition;
import com.piranport.dungeon.instance.DungeonInstance;

/**
 * 关卡胜利条件评估器（框架）。
 * 依据：策划决策/副本/12-关卡多样化扩展枚举.md
 *
 * <p>本类提供关卡胜利条件通用判定入口 — 关卡设计侧可在具体脚本中按需扩展。
 * 关卡胜利条件的具体业务判定（运输/夺旗双路径/歼灭/护送/守点/收集/解谜）
 * 通过 {@link #check} 派发。</p>
 *
 * <p>由于关卡数据结构未承载"目标点/计时器/收集物数量/护送目标"等具体业务字段，
 * 当前实现为通用框架：枚举已扩展（REACH_POINT / CAPTURE_FLAG），
 * 但具体业务判定由关卡脚本侧调用 {@link DungeonInstance} 的现有钩子完成。</p>
 *
 * <p>策划已定稿的判定细则（2026-09-09）：</p>
 * <ul>
 *   <li>运输（REACH_POINT）：玩家到达指定位置后可以过关</li>
 *   <li>歼灭（KILL_ALL）：玩家全灭副本敌人后可以过关</li>
 *   <li>护航（ESCORT）：护航对象到达指定位置后可以过关</li>
 *   <li>夺旗（CAPTURE_FLAG）：玩家在指定范围内保持存活指定时间 <b>或</b> 全灭副本敌人后可以过关（双路径 OR）</li>
 * </ul>
 *
 * <p>关卡脚本集成提示：</p>
 * <pre>
 *   if (VictoryEvaluator.checkSingle(instance, VictoryCondition.REACH_POINT)) {
 *       // 触发关卡完成逻辑（具体方法视 DungeonInstance API 而定）
 *   }
 * </pre>
 *
 * <p>当前实现仅作为枚举派发入口 — 各条件判定逻辑留待关卡脚本侧填充。</p>
 */
public final class VictoryEvaluator {

    private VictoryEvaluator() {}

    /**
     * 判定当前关卡是否满足胜利条件（任一）。
     * @return true = 任一 victoryConditions 已达成
     */
    public static boolean check(DungeonInstance instance,
                                 Iterable<VictoryCondition> victoryConditions) {
        if (instance == null || victoryConditions == null) return false;
        for (VictoryCondition cond : victoryConditions) {
            if (checkSingle(instance, cond)) return true;
        }
        return false;
    }

    /**
     * 单条件判定。
     * <p>当前实现为通用框架：具体业务判定（关卡侧 enemy 清空 / 玩家抵达目标 / 计时到达 等）
     * 由关卡脚本在自身的 tick / 事件处理器中填充；本方法仅作为条件枚举的派发入口。</p>
     */
    public static boolean checkSingle(DungeonInstance instance, VictoryCondition cond) {
        if (instance == null || cond == null) return false;
        // 各条件具体的业务判定由关卡脚本侧自行实现；
        // 默认返回 false 表示"需关卡设计填充"。
        return false;
    }
}