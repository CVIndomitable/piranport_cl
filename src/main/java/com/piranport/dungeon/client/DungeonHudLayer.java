package com.piranport.dungeon.client;

import com.piranport.PiranPort;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * HUD overlay shown while the player is inside a dungeon.
 * Displays current node, timer, and instance info.
 *
 * <p>整合版 P3 修复：本类原本实现 {@link LayeredDraw.Layer} 但没有任何地方注册到
 * {@link RegisterGuiLayersEvent}，HUD 实际从不显示。已在 {@link #onRegisterGuiLayers}
 * 中显式注册到 HOTBAR 层上方。</p>
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
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

    /**
     * 整合版 P3 修复：仅当服务端明确通知（DungeonStatePayload / PlayerDiedInDungeonPayload /
     * DungeonResultPayload 等）时才清；不在 render 里调用，避免每帧清除的隐性 bug。
     */
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

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ID,
                new DungeonHudLayer());
    }

    @Override
    public void render(GuiGraphics gfx, DeltaTracker deltaTracker) {
        // 整合版 P3 修复：仅在 inDungeon=true 时绘制；不主动调用 clearDungeonState
        // （离开副本维度由 DungeonStatePayload 处理，不在 render 中清除）
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
