package com.piranport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModClientConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /**
     * When true, the legacy HUD reload bar (displayed above the hotbar) is shown.
     * Default false — replaced by the item decorator on the ship core icon.
     */
    public static final ModConfigSpec.BooleanValue SHOW_LEGACY_RELOAD_HUD =
            BUILDER
                    .comment(
                            "Show the legacy reload progress HUD bar above the hotbar.",
                            "Default: false (replaced by item decorator on the ship core icon).",
                            "Set to true to restore the old HUD bar. (旧版装填HUD条，默认关闭)")
                    .define("showLegacyReloadHud", false);

    /**
     * When true, the "编组" (Flight Group) button is shown in the ship core GUI,
     * allowing players to configure aircraft flight groups.
     * Default false — disable until the feature is ready for use.
     */
    public static final ModConfigSpec.BooleanValue FLIGHT_GROUP_ENABLED =
            BUILDER
                    .comment(
                            "Enable the Flight Group (编组) button in the ship core GUI.",
                            "Default: false (hidden by default).",
                            "Set to true to show the flight group configuration screen. (飞机编组功能，默认关闭)")
                    .define("flightGroupEnabled", false);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
