package com.piranport.dungeon.client;

import com.piranport.PiranPort;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
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
    private static String bossName = "";
    private static String bossShipType = "";
    private static String bossChapter = "";
    private static int bossSegment = 0;
    private static float bossHealth;
    private static float bossMaxHealth;
    private static boolean bossVisible;
    private static long quietUntilMillis;

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
        clearBossOverlay();
    }

    /** Update just the node ID for immediate feedback when entering a new node. */
    public static void updateNode(String nodeId) {
        currentNodeId = nodeId;
    }

    public static boolean isInDungeon() {
        return inDungeon;
    }

    /** Boss 铭牌/分段血条状态由服务端同步，客户端只负责展示。 */
    public static void updateBossOverlay(String name, String shipType, String chapter,
                                         int segment, float health, float maxHealth,
                                         boolean visible, boolean quietBattlefield) {
        bossName = name == null ? "" : name;
        bossShipType = shipType == null ? "" : shipType;
        bossChapter = chapter == null ? "" : chapter;
        bossSegment = Math.max(0, Math.min(4, segment));
        bossHealth = Math.max(0, health);
        bossMaxHealth = Math.max(1, maxHealth);
        bossVisible = visible;
        if (quietBattlefield) quietUntilMillis = System.currentTimeMillis() + 500L;
        applyBattlefieldVolume();
    }

    private static void clearBossOverlay() {
        bossVisible = false;
        bossName = "";
        bossShipType = "";
        bossChapter = "";
        bossSegment = 0;
        quietUntilMillis = 0;
        applyBattlefieldVolume();
    }

    private static void applyBattlefieldVolume() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() == null) return;
        float volume = System.currentTimeMillis() < quietUntilMillis
                ? 0.35f : mc.options.getSoundSourceVolume(SoundSource.HOSTILE);
        mc.getSoundManager().updateSourceVolume(SoundSource.HOSTILE, volume);
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
        applyBattlefieldVolume();

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

        if (bossVisible && !bossName.isEmpty()) {
            int center = screenWidth / 2;
            int panelWidth = Math.min(420, screenWidth - 24);
            int left = center - panelWidth / 2;
            int top = 32;
            gfx.fill(left, top, left + panelWidth, top + 39, 0xB0101018);
            gfx.renderOutline(left, top, panelWidth, 39, 0xFF9B2020);
            String caption = bossName + (bossShipType.isEmpty() ? "" : " · " + bossShipType)
                    + (bossChapter.isEmpty() ? "" : " · " + bossChapter);
            gfx.drawCenteredString(font, Component.literal(caption), center, top + 4, 0xFFFFD0D0);
            int barLeft = left + 12;
            int barTop = top + 18;
            int barWidth = panelWidth - 24;
            gfx.fill(barLeft, barTop, barLeft + barWidth, barTop + 8, 0xFF351010);
            int filled = (int) (barWidth * Math.min(1f, bossHealth / bossMaxHealth));
            if (filled > 0) gfx.fill(barLeft, barTop, barLeft + filled, barTop + 8, 0xFFCB3030);
            for (int i = 1; i < 4; i++) {
                int mark = barLeft + barWidth * i / 4;
                gfx.fill(mark, barTop, mark + 1, barTop + 8, 0xFFD88A8A);
            }
            gfx.drawCenteredString(font, Component.literal("▰".repeat(bossSegment) + " "
                    + Math.max(0, Math.round(bossHealth)) + "/" + Math.max(1, Math.round(bossMaxHealth))),
                    center, top + 28, 0xFFFFB0B0);
        }
    }
}
