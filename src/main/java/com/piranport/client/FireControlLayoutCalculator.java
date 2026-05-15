package com.piranport.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Calculates the position of the fire control HUD panel based on configuration and screen size.
 */
@OnlyIn(Dist.CLIENT)
public class FireControlLayoutCalculator {

    private static final int MARGIN = 5; // Margin from screen edges

    /**
     * Calculates the panel position based on the configured mode, screen dimensions, and offsets.
     *
     * @param screenWidth  The GUI-scaled screen width
     * @param screenHeight The GUI-scaled screen height
     * @param panelWidth   The panel width in pixels
     * @param panelHeight  The panel height in pixels
     * @param configMode   The configured position mode (AUTO or fixed corner)
     * @param offsetX      User-configured X offset
     * @param offsetY      User-configured Y offset
     * @return The calculated panel position with alignment
     */
    public static PanelPosition calculatePosition(
            int screenWidth,
            int screenHeight,
            int panelWidth,
            int panelHeight,
            HudPosition configMode,
            int offsetX,
            int offsetY
    ) {
        // Step 1: Determine the target corner
        HudPosition targetCorner = configMode;
        if (configMode == HudPosition.AUTO) {
            targetCorner = HudLayoutDetector.detectBestPosition();
        }

        // Step 2: Calculate base position for the target corner
        int baseX, baseY;
        PanelPosition.Alignment alignment;

        switch (targetCorner) {
            case LEFT_TOP -> {
                baseX = MARGIN;
                baseY = MARGIN;
                alignment = PanelPosition.Alignment.LEFT_TOP;
            }
            case RIGHT_TOP -> {
                baseX = screenWidth - panelWidth - MARGIN;
                baseY = MARGIN;
                alignment = PanelPosition.Alignment.RIGHT_TOP;
            }
            case LEFT_BOTTOM -> {
                baseX = MARGIN;
                baseY = screenHeight - panelHeight - MARGIN;
                alignment = PanelPosition.Alignment.LEFT_BOTTOM;
            }
            case RIGHT_BOTTOM -> {
                baseX = screenWidth - panelWidth - MARGIN;
                baseY = screenHeight - panelHeight - MARGIN;
                alignment = PanelPosition.Alignment.RIGHT_BOTTOM;
            }
            default -> {
                // Fallback to right-top (original behavior)
                baseX = screenWidth - panelWidth - MARGIN;
                baseY = MARGIN;
                alignment = PanelPosition.Alignment.RIGHT_TOP;
            }
        }

        // Step 3: Apply user offsets
        int finalX = baseX + offsetX;
        int finalY = baseY + offsetY;

        // Step 4: Boundary check — ensure panel stays within screen bounds
        if (panelWidth > screenWidth || panelHeight > screenHeight) {
            // Panel too large: clamp to screen, preserve alignment preference
            finalX = Math.max(0, Math.min(finalX, screenWidth - Math.min(panelWidth, screenWidth)));
            finalY = Math.max(0, Math.min(finalY, screenHeight - Math.min(panelHeight, screenHeight)));
        } else {
            finalX = Math.max(0, Math.min(finalX, screenWidth - panelWidth));
            finalY = Math.max(0, Math.min(finalY, screenHeight - panelHeight));
        }

        return new PanelPosition(finalX, finalY, alignment);
    }
}
