package com.piranport.config;

/**
 * Defines the position mode for HUD panels.
 */
public enum HudPosition {
    /** Automatically detect the least occupied corner. */
    AUTO,
    /** Fixed position: top-left corner. */
    LEFT_TOP,
    /** Fixed position: top-right corner. */
    RIGHT_TOP,
    /** Fixed position: bottom-left corner. */
    LEFT_BOTTOM,
    /** Fixed position: bottom-right corner. */
    RIGHT_BOTTOM;

    /**
     * 按序号取位置，越界回退 {@link #AUTO}。
     *
     * <p>终端参数只支持整数，序号必须与上面的声明顺序绑定——{@code global.fire_control_visual.hud_corner}。</p>
     */
    public static HudPosition byIndex(int index) {
        HudPosition[] values = values();
        return index >= 0 && index < values.length ? values[index] : AUTO;
    }
}
