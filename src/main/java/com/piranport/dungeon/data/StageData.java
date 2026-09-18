package com.piranport.dungeon.data;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从 JSON 解析的关卡配置。
 *
 * <p>关卡公式 = 胜利方式 × 战斗限制 × 场景（决策 §副本/12）。
 * 顶层 {@link #sceneData} 与 {@link #combatRestriction} 字段用于关卡级默认值；
 * 节点级配置（{@link NodeData#restrictions()} / {@link NodeData#scene()}）优先级更高。
 * {@link #victoryObjectives} 携带业务字段：运输目标点、护航目标、夺旗半径/计时等。</p>
 */
public record StageData(
        String stageId,
        String chapter,
        String displayName,
        Map<String, NodeData> nodes,
        List<EdgeData> edges,
        String startNode,
        List<String> bossNodes,
        List<NodeData.RewardEntry> firstClearRewards,
        List<CheckpointData> checkpoints,
        Set<VictoryCondition> victoryConditions,
        SceneData sceneData,
        Set<CombatRestriction> combatRestriction,
        VictoryObjectives victoryObjectives
) {
    public StageData {
        // 配置发布后保持拓扑稳定，旧快照也不能被调用方持有的可变容器修改。
        nodes = Map.copyOf(nodes);
        edges = List.copyOf(edges);
        bossNodes = List.copyOf(bossNodes);
        firstClearRewards = List.copyOf(firstClearRewards);
        checkpoints = List.copyOf(checkpoints);
        victoryConditions = Set.copyOf(victoryConditions);
        combatRestriction = Set.copyOf(combatRestriction);
    }

    public record EdgeData(String from, String to) {}

    /**
     * 关卡业务判定字段集合 — 决策 §副本/12 各胜利方式的业务参数。
     * <p>可选字段均允许缺失：缺失时对应业务判定由关卡脚本侧补充。</p>
     */
    public record VictoryObjectives(
            /** KILL_ALL：若 true，检查 instance.clearedNodes 覆盖所有 battle/boss 节点 */
            boolean requireAllNodesCleared,
            /** SURVIVE：存活时长（秒），0 表示无计时 */
            int surviveSeconds,
            /** ESCORT：护送目标 TagKey 资源位置或实体 id（前缀 escorte_/escort_/...） */
            String escortEntityKey,
            /** REACH_POINT：目标点相对节点中心的偏移 [dx, dy, dz]（节点坐标 + 偏移 = 实际目标） */
            int[] reachPointOffset,
            /** CAPTURE_FLAG：夺旗判定半径（方块）+ 存活时间（秒） */
            int captureRadius,
            int captureHoldSeconds
    ) {
        public static final VictoryObjectives EMPTY =
                new VictoryObjectives(false, 0, null, null, 0, 0);
    }

    /**
     * 返回当前配置中可从指定节点直接抵达的节点。
     * 关卡节点规模较小，直接计算可避免按关卡 ID 缓存串用不同配置快照的拓扑。
     */
    public Set<String> getReachableFrom(String nodeId) {
        Set<String> reachable = new HashSet<>();
        for (EdgeData edge : edges) {
            if (edge.from().equals(nodeId)) {
                reachable.add(edge.to());
            }
        }
        return Set.copyOf(reachable);
    }

    /**
     * 按节点 ID 字典序计算稳定序号，避免非字母节点 ID 的战场坐标重叠。
     * 未知节点沿用序号 0，具体的节点合法性由入口校验。
     */
    public int nodeIndexOf(String nodeId) {
        if (!nodes.containsKey(nodeId)) {
            return 0;
        }
        int index = 0;
        for (String key : nodes.keySet()) {
            if (key.compareTo(nodeId) < 0) {
                index++;
            }
        }
        return index;
    }
}
