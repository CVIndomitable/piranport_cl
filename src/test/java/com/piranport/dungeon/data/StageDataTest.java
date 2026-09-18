package com.piranport.dungeon.data;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StageDataTest {
    @Test
    void sameStageIdKeepsEachSnapshotsTopologyAndNodeOrder() {
        StageData previous = stage(nodes("A", "B", "C"),
                List.of(new StageData.EdgeData("A", "B")));
        StageData replacement = stage(nodes("A", "C"),
                List.of(new StageData.EdgeData("A", "C")));

        assertEquals(Set.of("B"), previous.getReachableFrom("A"));
        assertEquals(2, previous.nodeIndexOf("C"));
        assertEquals(Set.of("C"), replacement.getReachableFrom("A"));
        assertEquals(1, replacement.nodeIndexOf("C"));
        assertEquals(Set.of("B"), previous.getReachableFrom("A"));
        assertEquals(2, previous.nodeIndexOf("C"));
        assertEquals(0, replacement.nodeIndexOf("missing"));
    }

    @Test
    void reachableNodesCannotBeChangedByConsumers() {
        StageData stage = stage(nodes("A", "B"),
                List.of(new StageData.EdgeData("A", "B")));

        assertThrows(UnsupportedOperationException.class,
                () -> stage.getReachableFrom("A").add("C"));
        assertThrows(UnsupportedOperationException.class,
                () -> stage.getReachableFrom("B").add("A"));
        assertEquals(Set.of("B"), stage.getReachableFrom("A"));
        assertEquals(Set.of(), stage.getReachableFrom("B"));
    }

    @Test
    void constructionTakesAnImmutableTopologySnapshot() {
        Map<String, NodeData> nodes = nodes("A", "B");
        List<StageData.EdgeData> edges = new ArrayList<>(
                List.of(new StageData.EdgeData("A", "B")));
        StageData stage = stage(nodes, edges);

        nodes.clear();
        edges.clear();

        assertEquals(Set.of("A", "B"), stage.nodes().keySet());
        assertEquals(Set.of("B"), stage.getReachableFrom("A"));
        assertEquals(1, stage.nodeIndexOf("B"));
        assertThrows(UnsupportedOperationException.class, () -> stage.nodes().clear());
        assertThrows(UnsupportedOperationException.class, () -> stage.edges().clear());
    }

    private static StageData stage(Map<String, NodeData> nodes, List<StageData.EdgeData> edges) {
        return new StageData("same-stage", "chapter", "关卡", nodes, edges, "A",
                List.of(), List.of(), List.of(), Set.of(), SceneData.DAY, Set.of(),
                StageData.VictoryObjectives.EMPTY);
    }

    private static Map<String, NodeData> nodes(String... nodeIds) {
        Map<String, NodeData> nodes = new HashMap<>();
        for (String nodeId : nodeIds) {
            nodes.put(nodeId, new NodeData(nodeId, NodeData.NodeType.RESOURCE, null,
                    List.of(), List.of(), null, 0, 0, null, TerrainType.T1_OCEAN,
                    Set.of(), SceneData.DAY));
        }
        return nodes;
    }
}
