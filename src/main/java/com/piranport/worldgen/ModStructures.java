package com.piranport.worldgen;

/**
 * Registry helper for structure-related worldgen.
 * <p>
 * Structure definitions are data-driven (JSON files under worldgen/structure/).
 * NBT templates are included under {@code data/piranport/structure/}.
 * They can still be replaced by final structure-block exports when art/design
 * supplies hand-authored versions.
 * Use the debug command {@code /ppd spawn_ruin <type>} to place the same NBT
 * templates used by world generation for direct validation.
 */
public final class ModStructures {

    // Structure registry location constants (match the JSON file names)
    public static final String PORTAL_RUIN = "piranport:portal_ruin";
    public static final String SUPPLY_DEPOT = "piranport:supply_depot";
    public static final String OUTPOST = "piranport:outpost";
    public static final String ABYSSAL_BASE = "piranport:abyssal_base";

    /**
     * 废弃传送门（决策/副本/09 + 17）：讲台 + 深渊传送门多方块结构，
     * 作为世界中的副本教学触点，讲台内预设教学关卡钥匙（{@code t-1}）。
     */
    public static final String ABANDONED_PORTAL = "piranport:abandoned_portal";

    private ModStructures() {}
}
