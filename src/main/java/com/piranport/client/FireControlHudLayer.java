package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.aviation.ClientFireControlData;
import com.piranport.combat.TransformationManager;
import com.piranport.item.ShipCoreItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Renders a fire-control panel in the top-right corner of the screen.
 * Shows each locked target's name and health bar.
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class FireControlHudLayer {

    private static final int PANEL_WIDTH = 110;
    private static final int LINE_HEIGHT  = 18;

    // Cache UUID→Entity mappings, rebuild every 20 game ticks (not per-call)
    private static final Map<UUID, Entity> entityCache = new HashMap<>();
    private static long lastRebuildTick = -1;
    private static final int CACHE_REBUILD_INTERVAL = 20;

    public static void clearCache() { entityCache.clear(); lastRebuildTick = -1; }

    @Nullable
    private static Entity findEntityByUUID(Minecraft mc, UUID uuid) {
        if (mc.level == null) return null;
        // Check cache first before rebuilding
        Entity cached = entityCache.get(uuid);
        if (cached != null && cached.isAlive()) return cached;

        // Rebuild cache once per 20 game ticks only if target not found
        long currentTick = mc.level.getGameTime();
        if (currentTick - lastRebuildTick >= CACHE_REBUILD_INTERVAL) {
            lastRebuildTick = currentTick;
            entityCache.clear();
            for (Entity e : mc.level.entitiesForRendering()) {
                entityCache.put(e.getUUID(), e);
            }
            cached = entityCache.get(uuid);
            if (cached != null) return cached;
        }
        // Fast path for players (always accessible)
        return mc.level.getPlayerByUUID(uuid);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (!TransformationManager.isPlayerTransformed(player)) return;

        List<UUID> targets = ClientFireControlData.getTargets();
        if (targets.isEmpty()) return;

        GuiGraphics gui = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int rows = targets.size();
        int panelHeight = rows * LINE_HEIGHT + 4;

        // Calculate panel position using the new layout system
        HudPosition mode = com.piranport.config.ModClientConfig.FIRE_CONTROL_POSITION.get();
        int offsetX = com.piranport.config.ModClientConfig.FIRE_CONTROL_OFFSET_X.get();
        int offsetY = com.piranport.config.ModClientConfig.FIRE_CONTROL_OFFSET_Y.get();

        PanelPosition pos = FireControlLayoutCalculator.calculatePosition(
                sw, sh, PANEL_WIDTH, panelHeight, mode, offsetX, offsetY
        );

        int panelX = pos.x();
        int panelY = pos.y();
        boolean isRightAligned = pos.alignment() == PanelPosition.Alignment.RIGHT_TOP
                || pos.alignment() == PanelPosition.Alignment.RIGHT_BOTTOM;

        // Background
        gui.fill(panelX - 3, panelY - 2,
                panelX + PANEL_WIDTH + 3, panelY + panelHeight - 2,
                0x99000000);

        for (int i = 0; i < rows; i++) {
            UUID uuid = targets.get(i);
            Entity entity = mc.level != null ? findEntityByUUID(mc, uuid) : null;
            int y = panelY + i * LINE_HEIGHT;

            if (entity instanceof LivingEntity living && living.isAlive()) {
                // Target marker + name
                String label = "◆ " + living.getDisplayName().getString();
                if (label.length() > 14) label = label.substring(0, 13) + "…";

                if (isRightAligned) {
                    // Right-aligned: draw text from right edge
                    int textW = mc.font.width(label);
                    gui.drawString(mc.font, label, panelX + PANEL_WIDTH - textW, y, 0xFF55AAFF, false);
                } else {
                    // Left-aligned: draw text from left edge
                    gui.drawString(mc.font, label, panelX, y, 0xFF55AAFF, false);
                }

                // HP bar
                float hpRatio = Math.max(0, living.getHealth() / living.getMaxHealth());
                int barY = y + mc.font.lineHeight + 1;
                int barW = PANEL_WIDTH;
                gui.fill(panelX, barY, panelX + barW, barY + 3, 0xFF444444);
                int fillW = Math.round(barW * hpRatio);
                if (fillW > 0) {
                    int color = hpRatio > 0.5f ? 0xFF44CC44
                            : hpRatio > 0.25f ? 0xFFFFAA00
                            : 0xFFFF3333;
                    gui.fill(panelX, barY, panelX + fillW, barY + 3, color);
                }

                // HP text (always right-aligned within the bar)
                String hpText = (int)living.getHealth() + "/" + (int)living.getMaxHealth();
                int textW = mc.font.width(hpText);
                gui.drawString(mc.font, hpText, panelX + barW - textW, y, 0xFFAAAAAA, false);
            } else {
                // Target lost / dead
                String lostLabel = "◇ [---]";
                if (isRightAligned) {
                    int textW = mc.font.width(lostLabel);
                    gui.drawString(mc.font, lostLabel, panelX + PANEL_WIDTH - textW, y, 0xFF777777, false);
                } else {
                    gui.drawString(mc.font, lostLabel, panelX, y, 0xFF777777, false);
                }
            }
        }
    }
}
