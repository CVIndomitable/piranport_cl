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
 */
public record StageData(
        String stageId,
        String chapter,
        String displayName,
        Map<String, NodeData> nodes,
        List<EdgeData> edges,
        String startNode,
        List<String> bossNodes,
        List<NodeData.RewardEntry> firstClearRewards
) {
    public record EdgeData(String from, String to) {}

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
