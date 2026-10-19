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
        String branchDefault,     // 都不满足时的兜底节点；null = 不分歧
        String displayName,       // 节点显示名；未填时见 label()
        EscortSpec escort,        // 《副本/12》护航：非 null 时本节点以运输船到达目标点过关
        DetourSpec detour         // 《副本/12》迂回：非 null 时本节点以玩家到达目标点过关
) {
    public NodeData {
        branches = branches == null ? List.of() : List.copyOf(branches);
        waves = Math.max(1, waves);
        if (escort != null && detour != null) {
            throw new IllegalArgumentException("节点不能同时配置 escort 与 detour: " + nodeId);
        }
    }

    /** 护航节点：刷出的运输船与其目标点（相对节点中心的偏移 [dx, dy, dz]）。 */
    public record EscortSpec(String entityId, int[] targetOffset) {}

    /** 迂回节点：玩家抵达的目标点（相对节点中心的偏移 [dx, dy, dz]），设在航道尽头。 */
    public record DetourSpec(int[] targetOffset) {}

    /** 胜利靠到达目标点（护航运输船或迂回玩家）而非清场：清场不开传送门，由到达判定开门。 */
    public boolean completesByArrival() {
        return escort != null || detour != null;
    }

    /** HUD 与结算展示用的名字：策划填了显示名就用它，否则退回节点 ID。 */
    public String label() {
        return displayName == null || displayName.isBlank() ? nodeId : displayName;
    }

    /** 是否配置了分歧带路。 */
    public boolean hasBranches() {
        return !branches.isEmpty() || branchDefault != null;
    }

    public enum NodeType {
        BATTLE, BOSS, RESOURCE, COST, DETOUR;

        public static NodeType fromString(String s) {
            return switch (s.toLowerCase()) {
                case "battle" -> BATTLE;
                case "boss" -> BOSS;
                case "resource" -> RESOURCE;
                case "cost" -> COST;
                case "detour" -> DETOUR;
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
