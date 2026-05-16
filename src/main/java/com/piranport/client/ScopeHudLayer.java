package com.piranport.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Matrix4f;

/**
 * 瞄准镜准星 HUD 覆盖层。
 * 在瞄准模式下绘制十字准星、距离信息。
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class ScopeHudLayer {

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (event.getName() != VanillaGuiLayers.CROSSHAIR) return;
        if (!ClientScopeHandler.isFullyScoped()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int cx = w / 2;
        int cy = h / 2;
        int crossSize = 8;

        // ---- 十字准星（白色线条） ----
        PoseStack pose = graphics.pose();
        pose.pushPose();
        Matrix4f mat = pose.last().pose();

        RenderSystem.setShaderColor(1, 1, 1, 0.9f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.0f);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        // 水平线
        buf.addVertex(mat, cx - crossSize - 4, cy, 0).setColor(255, 255, 255, 230);
        buf.addVertex(mat, cx - 2, cy, 0).setColor(255, 255, 255, 230);
        buf.addVertex(mat, cx + 2, cy, 0).setColor(255, 255, 255, 230);
        buf.addVertex(mat, cx + crossSize + 4, cy, 0).setColor(255, 255, 255, 230);
        // 垂直线
        buf.addVertex(mat, cx, cy - crossSize - 4, 0).setColor(255, 255, 255, 230);
        buf.addVertex(mat, cx, cy - 2, 0).setColor(255, 255, 255, 230);
        buf.addVertex(mat, cx, cy + 2, 0).setColor(255, 255, 255, 230);
        buf.addVertex(mat, cx, cy + crossSize + 4, 0).setColor(255, 255, 255, 230);
        // 中心红点
        buf.addVertex(mat, cx - 1, cy - 1, 0).setColor(255, 50, 50, 200);
        buf.addVertex(mat, cx + 1, cy + 1, 0).setColor(255, 50, 50, 200);
        buf.addVertex(mat, cx + 1, cy - 1, 0).setColor(255, 50, 50, 200);
        buf.addVertex(mat, cx - 1, cy + 1, 0).setColor(255, 50, 50, 200);

        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.disableBlend();

        pose.popPose();

        // ---- 距离信息 ----
        double dist = ClientScopeHandler.getTargetDistance();
        double vert = ClientScopeHandler.getTargetVertical();
        if (dist > 0) {
            String distText = String.format("§f距离: §e%.1f§fm  §7(Δy: §b%+.1f§7)", dist, vert);
            graphics.drawString(mc.font, distText, cx - mc.font.width(distText) / 2, cy + 20, 0xFFFFFF, true);
        }

        // ---- 调试信息 ----
        if (com.piranport.debug.PiranPortDebug.isClientEnabled()) {
            String debugText = String.format("§7[火控] 长按: %dt/%dt  zoom: %.1f",
                    ClientScopeHandler.getHoldTicks(),
                    ClientScopeHandler.getScopeThreshold(),
                    ClientScopeHandler.getZoomLevel());
            graphics.drawString(mc.font, debugText, cx - mc.font.width(debugText) / 2, cy + 35, 0xFFFFFF, true);
        }
    }
}
