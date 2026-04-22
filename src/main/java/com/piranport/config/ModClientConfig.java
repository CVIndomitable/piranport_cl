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

    public static final ModConfigSpec SPEC = BUILDER.build();
}
