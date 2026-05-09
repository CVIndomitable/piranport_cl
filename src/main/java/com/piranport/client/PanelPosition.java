package com.piranport.client;

/**
 * Represents the calculated position and alignment of a HUD panel.
 *
 * @param x         The X coordinate (left edge for left-aligned, right edge for right-aligned)
 * @param y         The Y coordinate (top edge)
 * @param alignment The corner alignment, determines text/bar rendering direction
 */
public record PanelPosition(int x, int y, Alignment alignment) {

    public enum Alignment {
        LEFT_TOP,
        RIGHT_TOP,
        LEFT_BOTTOM,
        RIGHT_BOTTOM
    }
}
