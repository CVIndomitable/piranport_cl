package com.piranport.dungeon;

public final class DungeonConstants {
    private DungeonConstants() {}

    /** Each dungeon instance occupies a 1024x1024 block region in the dungeon dimension. */
    public static final int REGION_SIZE = 1024;

    /** Each node battlefield is 128x128 blocks within the instance region. */
    public static final int NODE_AREA_SIZE = 128;

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
