package com.piranport.dungeon;

public final class DungeonConstants {
    private DungeonConstants() {}

    /** Each dungeon instance occupies a 4096x4096 block region in the dungeon dimension. */
    public static final int REGION_SIZE = 4096;

    /**
     * Number of instance cells along one axis of the 2D grid used to allocate
     * instance regions in the shared dungeon dimension.
     * Total concurrent capacity = GRID_SIZE * GRID_SIZE = 64 instances, well above the
     * 1-2 expected concurrent instances per server (副本/01 §2.1).
     */
    public static final int GRID_SIZE = 8;

    /**
     * Within each 4096x4096 instance region, the actual usable map area is 2048x2048.
     * A node is the basic battlefield unit and occupies 512x512 blocks. The remaining
     * 1024-block border on each side acts as a buffer / 边界保护 padding so players
     * cannot stray into neighboring instances even at high speed.
     */
    public static final int MAP_USABLE_SIZE = 2048;

    /** Padding from region origin to usable map edge (centered layout). */
    public static final int MAP_BORDER_PADDING = (REGION_SIZE - MAP_USABLE_SIZE) / 2;

    /** Each node battlefield is 512x512 blocks within the instance's usable area. */
    public static final int NODE_AREA_SIZE = 512;

    /** Maximum nodes per usable side (MAP_USABLE_SIZE / NODE_AREA_SIZE = 4). */
    public static final int MAX_NODES_PER_USABLE_SIDE = MAP_USABLE_SIZE / NODE_AREA_SIZE;

    /** Player/enemy spawn points stay this far from their node's nearest edge. */
    public static final int NODE_SPAWN_EDGE_MARGIN = 128;

    /** Water level (Y coordinate) in the dungeon battlefield. */
    public static final int SEA_LEVEL = 63;

    /** Spawn Y above water. */
    public static final int SPAWN_Y = SEA_LEVEL + 1;

    // 战利品箱消失时长、回城卷轴冷却改走调试终端 global.dungeon_timing.*（见 ModEquipmentConfig）。

    /** Window in milliseconds during which a TownScrollUsePayload is accepted after right-click. */
    public static final long TOWN_SCROLL_INTENT_WINDOW_MS = 10_000L;

    /** Maximum nodes per stage. */
    public static final int MAX_NODES_PER_STAGE = 26;

    /** Maximum leaderboard entries per stage. */
    public static final int MAX_LEADERBOARD_ENTRIES = 100;

    // ===== Boss Intro / Artillery Intro Script =====
    // Boss 演出超时（开场延时 / 战斗超时 / 击败延时）与火炮登场时序全部走调试终端
    // global.dungeon_timing.*（见 ModEquipmentConfig），不再在此保留常量。
}
