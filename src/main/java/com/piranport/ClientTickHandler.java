package com.piranport;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side tick handler for managing highlight mode and other client state.
 * Placeholder implementation for 1.20.1 migration.
 */
@OnlyIn(Dist.CLIENT)
public class ClientTickHandler {
    private static boolean highlightEnabled = false;

    public static boolean isHighlightEnabled() {
        return highlightEnabled;
    }

    public static void setHighlightEnabled(boolean enabled) {
        highlightEnabled = enabled;
    }

    public static void toggleHighlight() {
        highlightEnabled = !highlightEnabled;
    }
}
