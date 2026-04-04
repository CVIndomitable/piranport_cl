package com.piranport.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared GUI drawing utilities to avoid code duplication across Screen classes.
 */
public final class GuiHelper {

    private GuiHelper() {}

    /** Draws a 3D-beveled 18x18 slot background at the given position. */
    public static void drawSlotBg(GuiGraphics gfx, int x, int y) {
        gfx.fill(x, y, x + 18, y + 1, 0xFF373737);
        gfx.fill(x, y, x + 1, y + 18, 0xFF373737);
        gfx.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        gfx.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        gfx.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
    }
}
