package com.piranport.dungeon.data;

import java.util.List;
import java.util.Set;

/** 测试用节点工厂：只填战斗与展示字段，其余取规范构造器的默认值（不缩放、单波、无分歧、无护航/迂回）。 */
public final class TestNodes {
    private TestNodes() {}

    public static NodeData plain(String nodeId, NodeData.NodeType type, String enemies,
                                 List<NodeData.RewardEntry> rewards, List<NodeData.CostEntry> cost,
                                 String costMessage, int displayX, int displayY, String script,
                                 TerrainType terrainType, Set<CombatRestriction> restrictions,
                                 SceneData scene) {
        return new NodeData(nodeId, type, enemies, rewards, cost, costMessage, displayX, displayY, script,
                terrainType, restrictions, scene, 0.0, 1, List.of(), null, null, null, null);
    }
}
