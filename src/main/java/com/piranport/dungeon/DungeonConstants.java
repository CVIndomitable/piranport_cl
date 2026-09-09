package com.piranport.dungeon;

public final class DungeonConstants {
    private DungeonConstants() {}

    /** Each dungeon instance occupies a 1024x1024 block region in the dungeon dimension. */
    public static final int REGION_SIZE = 1024;

    /**
     * Number of instance cells along one axis of the 2D grid used to allocate
     * instance regions in the shared dungeon dimension.
     * Total concurrent capacity = GRID_SIZE * GRID_SIZE = 64 instances, well above the
     * 1-2 expected concurrent instances per server (副本/01 §2.1).
     */
    public static final int GRID_SIZE = 8;

    /**
     * Within each 1024x1024 instance region, the actual usable map area is 512x512
     * (副本/01 §2.1：地图实际使用 512×512 居中). The remaining 256-block border on each
     * side acts as a buffer / 边界保护 padding so players cannot stray into neighboring
     * instances even at high speed.
     */
    public static final int MAP_USABLE_SIZE = 512;

    /** Padding from region origin to usable map edge (centered layout). */
    public static final int MAP_BORDER_PADDING = (REGION_SIZE - MAP_USABLE_SIZE) / 2;

    /** Each node battlefield is 128x128 blocks within the instance's usable area. */
    public static final int NODE_AREA_SIZE = 128;

    /** Maximum nodes per usable side (MAP_USABLE_SIZE / NODE_AREA_SIZE = 4). */
    public static final int MAX_NODES_PER_USABLE_SIDE = MAP_USABLE_SIZE / NODE_AREA_SIZE;

    /** Water level (Y coordinate) in the dungeon battlefield. */
    public static final int SEA_LEVEL = 63;

    /** Spawn Y above water. */
    public static final int SPAWN_Y = SEA_LEVEL + 1;

    /** Loot ship auto-despawn time in ticks (300 seconds). */
    public static final int LOOT_SHIP_DESPAWN_TICKS = 300 * 20;

    /** Town scroll cooldown in ticks (3 seconds). */
    public static final int TOWN_SCROLL_COOLDOWN_TICKS = 3 * 20;

    /** Window in milliseconds during which a TownScrollUsePayload is accepted after right-click. */
    public static final long TOWN_SCROLL_INTENT_WINDOW_MS = 10_000L;

    /** Maximum nodes per stage. */
    public static final int MAX_NODES_PER_STAGE = 26;

    /** Maximum leaderboard entries per stage. */
    public static final int MAX_LEADERBOARD_ENTRIES = 100;

    // ===== Artillery Intro Script =====

    /** Looting phase timeout in ticks (60 seconds). */
    public static final int ARTILLERY_INTRO_LOOTING_TIMEOUT = 60 * 20;

    /** Distance (blocks) at which looting phase ends. */
    public static final double ARTILLERY_INTRO_LEAVE_DISTANCE = 20.0;

    /** Transport plane altitude above sea level. */
    public static final int TRANSPORT_PLANE_ALTITUDE = 15;
}
