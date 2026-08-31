package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.aviation.ClientFireControlData;
import com.piranport.combat.TransformationManager;
import com.piranport.config.HudPosition;
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

import java.lang.ref.WeakReference;
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

    // Cache UUID→Entity mappings. Weak references prevent retaining entities removed from the level.
    private static final Map<UUID, WeakReference<Entity>> entityCache = new HashMap<>();
    private static final int EVICT_INTERVAL = 100;
    private static int evictCounter = 0;

    public static void clearCache() { entityCache.clear(); }

    @Nullable
    private static Entity findEntityByUUID(Minecraft mc, UUID uuid) {
        if (mc.level == null) return null;

        WeakReference<Entity> cachedRef = entityCache.get(uuid);
        Entity cached = cachedRef != null ? cachedRef.get() : null;
        if (cached != null && cached.isAlive()) return cached;
        entityCache.remove(uuid);

        // 增量插入：只添加当前缺失的 UUID，不遍历全量
        Entity found = mc.level.getPlayerByUUID(uuid);
        if (found != null && found.isAlive()) {
            entityCache.put(uuid, new WeakReference<>(found));
            return found;
        }
        // Fallback: 从渲染实体列表查找（可能开销较大，但每个 UUID 最多一次）
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid) && entity.isAlive()) {
                entityCache.put(uuid, new WeakReference<>(entity));
                return entity;
            }
        }

        // 定期清理失效或已死亡实体（无需全量重建）
        evictCounter++;
        if (evictCounter >= EVICT_INTERVAL) {
            evictCounter = 0;
            entityCache.values().removeIf(reference -> {
                Entity entity = reference.get();
                return entity == null || !entity.isAlive();
            });
        }
        return null;
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
                float maxHp = living.getMaxHealth();
                float hpRatio = maxHp > 0 ? Math.max(0, Math.min(1, living.getHealth() / maxHp)) : 0;
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

                // HP text (below the bar, right-aligned)
                String hpText = (int)living.getHealth() + "/" + (int)maxHp;
                int hpTextW = mc.font.width(hpText);
                gui.drawString(mc.font, hpText, panelX + barW - hpTextW, barY + 4, 0xFFAAAAAA, false);
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
