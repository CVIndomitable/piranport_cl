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
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class DungeonHudLayer implements LayeredDraw.Layer {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "dungeon_hud");

    // Client-side state updated by S2C payloads
    private static String currentStageName = "";
    private static String currentNodeName = "";
    /** 服务端同步下来的关卡总用时。客户端不自己算（原来用墙钟算，暂停后仍在跑）。 */
    private static long timerElapsedMillis = 0;
    private static boolean inDungeon = false;
    private static String bossName = "";
    private static String bossShipType = "";
    private static String bossChapter = "";
    private static int bossSegment = 0;
    private static float bossHealth;
    private static float bossMaxHealth;
    private static boolean bossVisible;
    private static long quietUntilMillis;
    private static final long CAPTION_DURATION_MILLIS = 6000L;
    private static String captionObjectiveKey = "";
    private static String captionSceneKey = "";
    private static long captionUntilMillis;

    public static void setDungeonState(String stageName, String nodeName, long elapsedMillis,
                                       String objectiveKey, String sceneKey) {
        if ((stageName == null || stageName.isEmpty())
                && (nodeName == null || nodeName.isEmpty()) && elapsedMillis <= 0L) {
            clearDungeonState();
            return;
        }
        currentStageName = stageName;
        currentNodeName = nodeName;
        timerElapsedMillis = Math.max(0L, elapsedMillis);
        inDungeon = true;
        if (objectiveKey != null && !objectiveKey.isEmpty()) {
            captionObjectiveKey = objectiveKey;
            captionSceneKey = sceneKey == null ? "" : sceneKey;
            captionUntilMillis = System.currentTimeMillis() + CAPTION_DURATION_MILLIS;
        }
    }

    /**
     * 整合版 P3 修复：仅当服务端明确通知（DungeonStatePayload / DungeonResultPayload 等）时才清；不在 render 里调用，避免每帧清除的隐性 bug。
     */
    public static void clearDungeonState() {
        inDungeon = false;
        currentStageName = "";
        currentNodeName = "";
        timerElapsedMillis = 0;
        captionUntilMillis = 0;
        clearBossOverlay();
    }

    /** Update just the node ID for immediate feedback when entering a new node. */
    public static void updateNode(String nodeName) {
        currentNodeName = nodeName;
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

    private static void renderEntryCaption(GuiGraphics gfx, net.minecraft.client.gui.Font font,
                                           int screenWidth, int screenHeight) {
        int center = screenWidth / 2;
        int y = screenHeight / 3;
        gfx.drawCenteredString(font, Component.literal(currentStageName + " · " + currentNodeName),
                center, y, 0xFFFFD700);
        gfx.drawCenteredString(font, Component.translatable(captionObjectiveKey),
                center, y + 14, 0xFFFFFFFF);
        if (!captionSceneKey.isEmpty()) {
            gfx.drawCenteredString(font, Component.translatable(captionSceneKey),
                    center, y + 28, 0xFFFFB0B0);
        }
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
        if (!currentNodeName.isEmpty()) {
            gfx.drawCenteredString(font,
                    Component.translatable("hud.piranport.dungeon.node", currentNodeName),
                    screenWidth / 2, 17, 0xFFAAFFAA);
        }

        if (System.currentTimeMillis() < captionUntilMillis) {
            renderEntryCaption(gfx, font, screenWidth, gfx.guiHeight());
        }

        // Timer (top right)：直接画服务端同步值，暂停时没有新包自然停表。
        // 只显示到秒——服务端 5 tick 同步一次，毫秒位是没有意义的噪声。
        long totalSec = timerElapsedMillis / 1000;
        String timeStr = String.format("%02d:%02d", totalSec / 60, totalSec % 60);
        int tw = font.width(timeStr);
        gfx.drawString(font, timeStr, screenWidth - tw - 5, 5, 0xFFAAFFAA, false);

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
