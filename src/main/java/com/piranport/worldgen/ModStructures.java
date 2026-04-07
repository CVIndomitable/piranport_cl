package com.piranport.worldgen;

/**
 * Registry helper for structure-related worldgen.
 * <p>
 * Structure definitions are data-driven (JSON files under worldgen/structure/).
 * NBT templates (portal_ruin_1.nbt, supply_depot_1.nbt, outpost_1.nbt, abyssal_base_1.nbt)
 * must be created manually using structure blocks in-game, then placed in
 * {@code data/piranport/structure/}.
 * <p>
 * Until proper NBT templates are created, the structures will fail to generate
 * silently (template pool references non-existent .nbt files).
 * Use the debug command {@code /ppd spawn_ruin <type>} to test with programmatic generation.
 */
public final class ModStructures {

    // Structure registry location constants (match the JSON file names)
    public static final String PORTAL_RUIN = "piranport:portal_ruin";
    public static final String SUPPLY_DEPOT = "piranport:supply_depot";
    public static final String OUTPOST = "piranport:outpost";
    public static final String ABYSSAL_BASE = "piranport:abyssal_base";

    private ModStructures() {}
}
