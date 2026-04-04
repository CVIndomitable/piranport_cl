package com.piranport.dungeon.client;

import com.piranport.PiranPort;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * HUD overlay shown while the player is inside a dungeon.
 * Displays current node, timer, and instance info.
 */
public class DungeonHudLayer implements LayeredDraw.Layer {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "dungeon_hud");

    // Client-side state updated by S2C payloads
    private static String currentStageName = "";
    private static String currentNodeId = "";
    private static long timerStartMillis = 0;
    private static boolean inDungeon = false;

    public static void setDungeonState(String stageName, String nodeId, long startMillis) {
        currentStageName = stageName;
        currentNodeId = nodeId;
        timerStartMillis = startMillis;
        inDungeon = true;
    }

    public static void clearDungeonState() {
        inDungeon = false;
        currentStageName = "";
        currentNodeId = "";
        timerStartMillis = 0;
    }

    /** Update just the node ID for immediate feedback when entering a new node. */
    public static void updateNode(String nodeId) {
        currentNodeId = nodeId;
    }

    public static boolean isInDungeon() {
        return inDungeon;
    }

    @Override
    public void render(GuiGraphics gfx, DeltaTracker deltaTracker) {
        if (!inDungeon) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int screenWidth = gfx.guiWidth();
        var font = mc.font;

        // Stage name (top center)
        if (!currentStageName.isEmpty()) {
            gfx.drawCenteredString(font, Component.literal(currentStageName),
                    screenWidth / 2, 5, 0xFFFFD700);
        }

        // Current node (top center, below stage name)
        if (!currentNodeId.isEmpty()) {
            gfx.drawCenteredString(font,
                    Component.translatable("hud.piranport.dungeon.node", currentNodeId),
                    screenWidth / 2, 17, 0xFFAAFFAA);
        }

        // Timer (top right)
        if (timerStartMillis > 0) {
            long elapsed = System.currentTimeMillis() - timerStartMillis;
            long totalSec = elapsed / 1000;
            String timeStr = String.format("%02d:%02d.%03d",
                    totalSec / 60, totalSec % 60, elapsed % 1000);
            int tw = font.width(timeStr);
            gfx.drawString(font, timeStr, screenWidth - tw - 5, 5, 0xFFAAFFAA, false);
        }
    }
}
