package com.piranport.dungeon.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Set;

/**
 * Parsed node configuration within a stage.
 */
public record NodeData(
        String nodeId,
        NodeType type,
        String enemies,           // enemy_set ID (battle/boss only)
        List<RewardEntry> rewards, // resource node rewards
        List<CostEntry> cost,     // cost node costs
        String costMessage,       // cost node message
        int displayX,
        int displayY,
        String script,            // optional script ID (e.g. "artillery_intro") for scripted battle nodes
        TerrainType terrainType,  // 技术指南 05：副本地形类型
        Set<CombatRestriction> restrictions, // 整合版 §2.4 关卡公式 5 战斗限制
        SceneData scene,          // 整合版 §2.4 关卡公式 5 场景
        double difficultyScale,   // 《副本/00》节点级 difficulty_scale；<=0 表示继承关卡级
        int waves,                // 基础波数（节点敌人列表 = 一波），多人按 DungeonScaling.waveCount 放大
        List<BranchRule> branches, // 《副本/22》分歧带路规则（有序）
        String branchDefault      // 都不满足时的兜底节点；null = 不分歧
) {
    public NodeData {
        branches = branches == null ? List.of() : List.copyOf(branches);
        waves = Math.max(1, waves);
    }

    /** 兼容旧调用方（测试 / 客户端同步）：不带缩放与分歧字段。 */
    public NodeData(String nodeId, NodeType type, String enemies, List<RewardEntry> rewards,
                    List<CostEntry> cost, String costMessage, int displayX, int displayY,
                    String script, TerrainType terrainType, Set<CombatRestriction> restrictions,
                    SceneData scene) {
        this(nodeId, type, enemies, rewards, cost, costMessage, displayX, displayY, script,
                terrainType, restrictions, scene, 0.0, 1, List.of(), null);
    }

    /** 是否配置了分歧带路。 */
    public boolean hasBranches() {
        return !branches.isEmpty() || branchDefault != null;
    }

    public enum NodeType {
        BATTLE, BOSS, RESOURCE, COST;

        public static NodeType fromString(String s) {
            return switch (s.toLowerCase()) {
                case "battle" -> BATTLE;
                case "boss" -> BOSS;
                case "resource" -> RESOURCE;
                case "cost" -> COST;
                default -> throw new IllegalArgumentException("Unknown node type: " + s);
            };
        }
    }

    public record RewardEntry(String item, int count, float chance) {
        public RewardEntry(String item, int count) {
            this(item, count, 1.0f);
        }

        /** Resolves the registered Item once at access (registries already populated by data load). */
        public Item resolvedItem() {
            ResourceLocation id = ResourceLocation.tryParse(item);
            if (id == null) return null;
            return BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        }
    }

    public record CostEntry(String item, int count) {
        public Item resolvedItem() {
            ResourceLocation id = ResourceLocation.tryParse(item);
            if (id == null) return null;
            return BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        }
    }
}
