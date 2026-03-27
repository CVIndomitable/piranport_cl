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

import java.util.List;
import java.util.UUID;

/**
 * Renders a fire-control panel in the top-right corner of the screen.
 * Shows each locked target's name and health bar.
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class FireControlHudLayer {

    private static final int PANEL_WIDTH = 110;
    private static final int LINE_HEIGHT  = 18;

    @Nullable
    private static Entity findEntityByUUID(Minecraft mc, UUID uuid) {
        if (mc.level == null) return null;
        // Check players first (fast path)
        Entity player = mc.level.getPlayerByUUID(uuid);
        if (player != null) return player;
        // Fall back to full entity scan (within render distance)
        for (Entity e : mc.level.entitiesForRendering()) {
            if (uuid.equals(e.getUUID())) return e;
        }
        return null;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Only show while transformed
        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof ShipCoreItem)) return;
        if (!TransformationManager.isTransformed(mainHand)) return;

        List<UUID> targets = ClientFireControlData.getTargets();
        if (targets.isEmpty()) return;

        GuiGraphics gui = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();

        int panelX = sw - PANEL_WIDTH - 5;
        int panelY = 8;
        int rows = targets.size();

        // Background
        gui.fill(panelX - 3, panelY - 2,
                sw - 2, panelY + rows * LINE_HEIGHT + 2,
                0x99000000);

        for (int i = 0; i < rows; i++) {
            UUID uuid = targets.get(i);
            Entity entity = mc.level != null ? findEntityByUUID(mc, uuid) : null;
            int y = panelY + i * LINE_HEIGHT;

            if (entity instanceof LivingEntity living && living.isAlive()) {
                // Target marker + name
                String label = "◆ " + living.getDisplayName().getString();
                if (label.length() > 14) label = label.substring(0, 13) + "…";
                gui.drawString(mc.font, label, panelX, y, 0xFF55AAFF, false);

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

                // HP text (right-aligned)
                String hpText = (int)living.getHealth() + "/" + (int)living.getMaxHealth();
                int textW = mc.font.width(hpText);
                gui.drawString(mc.font, hpText, panelX + barW - textW, y, 0xFFAAAAAA, false);
            } else {
                // Target lost / dead
                gui.drawString(mc.font, "◇ [---]", panelX, y, 0xFF777777, false);
            }
        }
    }
}
