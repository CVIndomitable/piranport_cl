package com.piranport.config;

import com.piranport.client.HudPosition;
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

    /**
     * Fire control panel position on screen.
     * AUTO: automatically detect the least occupied corner
     * LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM: fixed positions
     * Default: AUTO
     */
    public static final ModConfigSpec.EnumValue<HudPosition> FIRE_CONTROL_POSITION =
            BUILDER
                    .comment(
                            "Fire control panel position on screen.",
                            "AUTO: automatically detect free space (avoids minimap mods)",
                            "LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM: fixed positions",
                            "Default: AUTO (火控面板位置，AUTO=自动检测空闲位置)")
                    .defineEnum("fireControlPosition", HudPosition.AUTO);

    /**
     * X offset for fire control panel (pixels).
     * Positive = move right, negative = move left.
     */
    public static final ModConfigSpec.IntValue FIRE_CONTROL_OFFSET_X =
            BUILDER
                    .comment(
                            "X offset for fire control panel (pixels).",
                            "Positive = move right, negative = move left.",
                            "Range: -500 to 500, Default: 0 (火控面板X轴偏移)")
                    .defineInRange("fireControlOffsetX", 0, -500, 500);

    /**
     * Y offset for fire control panel (pixels).
     * Positive = move down, negative = move up.
     */
    public static final ModConfigSpec.IntValue FIRE_CONTROL_OFFSET_Y =
            BUILDER
                    .comment(
                            "Y offset for fire control panel (pixels).",
                            "Positive = move down, negative = move up.",
                            "Range: -500 to 500, Default: 0 (火控面板Y轴偏移)")
                    .defineInRange("fireControlOffsetY", 0, -500, 500);

    /**
     * Screen shake intensity multiplier.
     * 0 = disabled, 1.0 = default intensity.
     */
    public static final ModConfigSpec.DoubleValue SCREEN_SHAKE_MULTIPLIER =
            BUILDER
                    .comment(
                            "Screen shake intensity multiplier.",
                            "0 = disabled, 1.0 = default.",
                            "Range: 0.0 to 3.0, Default: 1.0 (屏幕震动强度倍率)")
                    .defineInRange("screenShakeMultiplier", 1.0, 0.0, 3.0);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
