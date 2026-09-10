package com.piranport.dungeon.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parsed stage (level) configuration from JSON.
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

    // ===== Lazily-built indexes (kept off the record so equality / Json stay clean) =====

    private static final ConcurrentHashMap<String, Map<String, Set<String>>> ADJ_CACHE =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Map<String, Integer>> INDEX_CACHE =
            new ConcurrentHashMap<>();

    /**
     * Returns the set of node IDs reachable from the given node. O(1) after first call.
     */
    public Set<String> getReachableFrom(String nodeId) {
        Map<String, Set<String>> adj = ADJ_CACHE.computeIfAbsent(stageId, k -> {
            Map<String, Set<String>> built = new HashMap<>();
            for (EdgeData e : edges) {
                built.computeIfAbsent(e.from(), kk -> new HashSet<>()).add(e.to());
            }
            return Map.copyOf(built);
        });
        return adj.getOrDefault(nodeId, Set.of());
    }

    /**
     * Returns a stable lexicographic index for a node ID. Used for laying out node
     * battlefields along the X axis without colliding when nodeIds are non-letter.
     */
    public int nodeIndexOf(String nodeId) {
        Map<String, Integer> idx = INDEX_CACHE.computeIfAbsent(stageId, k -> {
            TreeMap<String, NodeData> sorted = new TreeMap<>(nodes);
            Map<String, Integer> built = new HashMap<>();
            int i = 0;
            for (String key : sorted.keySet()) {
                built.put(key, i++);
            }
            return Map.copyOf(built);
        });
        return idx.getOrDefault(nodeId, 0);
    }

    /** Drop cached indexes for this stage — call when datapack reloads change topology. */
    public static void invalidateCaches() {
        ADJ_CACHE.clear();
        INDEX_CACHE.clear();
    }
}
