package com.piranport.dungeon.data;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * Returns the set of node IDs reachable from the given node.
     */
    public Set<String> getReachableFrom(String nodeId) {
        return edges.stream()
                .filter(e -> e.from().equals(nodeId))
                .map(EdgeData::to)
                .collect(Collectors.toSet());
    }
}
