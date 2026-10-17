package com.piranport.dungeon.event;

import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.SceneData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.data.TerrainType;
import com.piranport.dungeon.instance.DungeonInstance;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 出口传送门的下一节点选择策略。
 *
 * <p>只测纯拓扑选择，不启动游戏：节点完成后的传送、地形建造由
 * {@link DungeonProgressionRouter#advanceTo} 在服务端完成。</p>
 */
class DungeonProgressionRouterTest {

    @Test
    void choosesFirstOutgoingEdgeInDeclarationOrder() {
        DungeonInstance instance = instance();
        instance.addClearedNode("A");
        instance.setCurrentNode("A");

        assertEquals("B", DungeonProgressionRouter.chooseNext(instance, stage(), "A"));
    }

    @Test
    void skipsClearedTargetsBeforeUsingFallback() {
        DungeonInstance instance = instance();
        instance.addClearedNode("A");
        instance.addClearedNode("B");
        instance.setCurrentNode("A");

        // A 的直连后继里 C 还没清，优先走 C，而不是从 B 跳到 D。
        assertEquals("C", DungeonProgressionRouter.chooseNext(instance, stage(), "A"));

        instance.addClearedNode("C");
        // A 的直连后继都清了，从已清节点的出边找下一个未清节点 D。
        assertEquals("D", DungeonProgressionRouter.chooseNext(instance, stage(), "A"));
    }

    @Test
    void deadEndHasNoNextNode() {
        DungeonInstance instance = instance();
        instance.addClearedNode("D");
        instance.setCurrentNode("D");

        assertNull(DungeonProgressionRouter.chooseNext(instance, stage(), "D"));
    }

    private static DungeonInstance instance() {
        DungeonInstance instance = new DungeonInstance(UUID.randomUUID(), "test", 0);
        instance.setState(DungeonInstance.State.ACTIVE);
        return instance;
    }

    private static StageData stage() {
        Map<String, NodeData> nodes = new LinkedHashMap<>();
        for (String id : List.of("A", "B", "C", "D")) {
            nodes.put(id, new NodeData(id, NodeData.NodeType.BATTLE, null,
                    List.of(), List.of(), "", 0, 0, null,
                    TerrainType.T1_OCEAN, Set.of(), SceneData.DAY));
        }
        List<StageData.EdgeData> edges = List.of(
                new StageData.EdgeData("A", "B"),
                new StageData.EdgeData("A", "C"),
                new StageData.EdgeData("B", "D"),
                new StageData.EdgeData("C", "D"));
        return new StageData("test", "chapter_1", "Test", nodes, edges, "A", List.of("D"),
                List.of(), List.of(), Set.of(), SceneData.DAY, Set.of(),
                StageData.VictoryObjectives.EMPTY);
    }
}
