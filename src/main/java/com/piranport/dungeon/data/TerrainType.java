package com.piranport.dungeon.data;

/**
 * 副本地形类型（决策 副本/15 + 技术指南 05）。
 *
 * <p>六种地形按编号 T1–T6 排列，掩体强度递增。
 * 关卡 JSON 的节点可声明 {@code terrainType} 字段，
 * 由 {@link NodeBattleField#generateTerrain} 读取并生成对应地形特征。</p>
 */
public enum TerrainType {
    /** 大海：一望无际水面，无水面特征（默认地形）。 */
    T1_OCEAN("大海", 0),

    /** 岛礁群：散布小岛礁，轻掩体。 */
    T2_ISLAND_REEFS("岛礁群", 1),

    /** 残骸带：沉船残骸结构横贯战场，中掩体。 */
    T3_WRECKAGE("残骸带", 2),

    /** 岛链湾：岛屿链围出弯曲航道，中-强掩体。 */
    T4_ISLAND_CHAIN("岛链湾", 3),

    /** 要塞环礁：中心环形礁盘+要塞，强掩体。 */
    T5_FORTRESS_REEF("要塞环礁", 4),

    /** 港口遗迹：岸式结构，中掩体。 */
    T6_PORT_RUINS("港口遗迹", 5);

    private final String displayName;
    private final int sortOrder;

    TerrainType(String displayName, int sortOrder) {
        this.displayName = displayName;
        this.sortOrder = sortOrder;
    }

    public String displayName() {
        return displayName;
    }

    public int sortOrder() {
        return sortOrder;
    }

    public static TerrainType fromString(String s) {
        if (s == null) return T1_OCEAN;
        return switch (s.toUpperCase()) {
            case "T1", "OCEAN", "大海" -> T1_OCEAN;
            case "T2", "ISLAND_REEFS", "岛礁群" -> T2_ISLAND_REEFS;
            case "T3", "WRECKAGE", "残骸带" -> T3_WRECKAGE;
            case "T4", "ISLAND_CHAIN", "岛链湾" -> T4_ISLAND_CHAIN;
            case "T5", "FORTRESS_REEF", "要塞环礁" -> T5_FORTRESS_REEF;
            case "T6", "PORT_RUINS", "港口遗迹" -> T6_PORT_RUINS;
            default -> T1_OCEAN;
        };
    }

    /**
     * 严格解析：无法识别时返回 null，由调用方拒绝加载。
     * 关卡 JSON 里的地形写错一个字（如 "T7"、"岛嶕群"）不该静默变成大海——
     * 那是玩家要打完一整关才会察觉的偏差，加载期就该拦下。
     */
    public static TerrainType parseStrict(String s) {
        TerrainType parsed = fromString(s);
        if (parsed == T1_OCEAN && !isExplicitOceanAlias(s)) return null;
        return parsed;
    }

    private static boolean isExplicitOceanAlias(String s) {
        if (s == null) return true;
        return switch (s.toUpperCase()) {
            case "T1", "OCEAN", "大海" -> true;
            default -> false;
        };
    }
}
