package com.piranport.client;

/**
 * Defines the position mode for HUD panels (e.g., fire control display).
 */
public enum HudPosition {
    /** Automatically detect the least occupied corner */
    AUTO,
    /** Fixed position: top-left corner */
    LEFT_TOP,
    /** Fixed position: top-right corner */
    RIGHT_TOP,
    /** Fixed position: bottom-left corner */
    LEFT_BOTTOM,
    /** Fixed position: bottom-right corner */
    RIGHT_BOTTOM
}
