package com.piranport.dungeon.data;

import java.util.List;

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
        int displayY
) {
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
    }

    public record CostEntry(String item, int count) {}
}
