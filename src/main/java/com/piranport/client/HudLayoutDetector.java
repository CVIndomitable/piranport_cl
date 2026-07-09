package com.piranport.client;

import com.piranport.config.HudPosition;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Detects which screen corners are least occupied by other mods' HUD elements.
 * Uses a heuristic approach since NeoForge doesn't provide a standard API for HUD occupancy detection.
 */
@OnlyIn(Dist.CLIENT)
public class HudLayoutDetector {

    private static HudPosition cachedBestPosition = HudPosition.LEFT_TOP;
    private static long lastDetectTick = -1;
    private static final int DETECT_INTERVAL = 60; // Re-detect every 60 ticks (3 seconds)

    /**
     * Returns the least occupied corner position.
     * Results are cached for performance — detection runs at most once per 60 ticks.
     */
    public static HudPosition detectBestPosition() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return cachedBestPosition;

        long currentTick = mc.level.getGameTime();
        if (currentTick - lastDetectTick < DETECT_INTERVAL) {
            return cachedBestPosition;
        }

        lastDetectTick = currentTick;

        // Heuristic detection: assume right-top is most likely occupied (minimap mods),
        // so prioritize: LEFT_TOP → LEFT_BOTTOM → RIGHT_BOTTOM → RIGHT_TOP
        // Future enhancement: implement pixel sampling or event-based detection
        cachedBestPosition = detectUsingHeuristic();

        return cachedBestPosition;
    }

    /**
     * Heuristic-based detection.
     * Assumes common mod HUD patterns:
     * - Right-top: minimap (JourneyMap, Xaero's, etc.)
     * - Left-top: usually free or less crowded
     * - Bottom corners: less common for persistent HUD elements
     */
    /** 启发式布局检测：首选左上角（避免常见的右上角小地图覆盖）。当前为静态实现。 */
    private static HudPosition detectUsingHeuristic() {
        return HudPosition.LEFT_TOP;
    }

    /**
     * Clears the cached detection result, forcing a re-detection on next call.
     */
    public static void clearCache() {
        lastDetectTick = -1;
    }
}
