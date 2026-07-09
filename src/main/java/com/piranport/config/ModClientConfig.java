package com.piranport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModClientConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /**
     * Reload progress bar display style.
     * HOTBAR: above hotbar (default, like vanilla experience bar)
     * CENTER: center of screen (legacy position)
     * HIDDEN: completely hidden
     */
    public static final ModConfigSpec.EnumValue<ReloadHudStyle> RELOAD_HUD_STYLE =
            BUILDER
                    .comment(
                            "Reload progress bar display style.",
                            "HOTBAR: above hotbar (default, like vanilla experience bar)",
                            "CENTER: center of screen (legacy position)",
                            "HIDDEN: completely hidden",
                            "Default: HOTBAR (装填进度条显示样式)")
                    .defineEnum("reloadHudStyle", ReloadHudStyle.HOTBAR);

    /**
     * Enable fade-in/fade-out animation for reload progress bar.
     * When true, the bar fades out 2 seconds after reload completes.
     */
    public static final ModConfigSpec.BooleanValue RELOAD_HUD_FADE_ANIMATION =
            BUILDER
                    .comment(
                            "Enable fade-in/fade-out animation for reload progress bar.",
                            "When true, the bar fades out 2 seconds after reload completes.",
                            "Default: true (装填进度条淡入淡出动画)")
                    .define("reloadHudFadeAnimation", true);

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

    public enum ReloadHudStyle {
        HOTBAR,   // 物品栏上方（新默认）
        CENTER,   // 屏幕中央（旧版）
        HIDDEN    // 完全隐藏
    }
}
