package com.piranport.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.piranport.combat.BallisticSolver;
import com.piranport.combat.BallisticSolverStats;
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
 * 在瞄准模式下绘制十字准星、距离信息、算法性能统计。
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

        // ---- 横向刻度线 ----
        int scaleY = cy;  // 刻度线 Y 坐标（与准星横线相同）
        int totalScales = 21;  // -10 到 +10
        int scaleRange = (int)(w * 0.7);  // 覆盖屏幕宽度的 70%
        int scaleSpacing = scaleRange / 20;  // 每个刻度间距
        int scaleStartX = cx - scaleRange / 2;  // 起始 X 坐标

        Tesselator scaleTess = Tesselator.getInstance();
        BufferBuilder scaleBuf = scaleTess.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < totalScales; i++) {
            int scaleValue = i - 10;  // -10 到 +10
            int scaleX = scaleStartX + i * scaleSpacing;
            boolean isMainScale = (scaleValue % 5 == 0);  // 主刻度判断

            int scaleLength = isMainScale ? 10 : 5;
            int alpha = isMainScale ? 230 : 180;

            // 绘制垂直刻度线（向下延伸）
            scaleBuf.addVertex(mat, scaleX, scaleY, 0).setColor(255, 255, 255, alpha);
            scaleBuf.addVertex(mat, scaleX, scaleY + scaleLength, 0).setColor(255, 255, 255, alpha);
        }

        RenderSystem.lineWidth(1.5f);
        BufferUploader.drawWithShader(scaleBuf.buildOrThrow());
        RenderSystem.disableBlend();

        pose.popPose();

        // ---- 刻度线数字标注 ----
        for (int i = 0; i < totalScales; i++) {
            int scaleValue = i - 10;
            if (scaleValue % 5 == 0) {  // 仅主刻度标注数字
                int scaleX = scaleStartX + i * scaleSpacing;
                String label = String.valueOf(scaleValue);
                int labelWidth = mc.font.width(label);
                graphics.drawString(mc.font, label,
                        scaleX - labelWidth / 2,  // 居中对齐
                        cy + 12,  // 刻度线下方 2 像素
                        0xFFFFFF, true);  // 白色带阴影
            }
        }

        // ---- 距离信息 ----
        double dist = ClientScopeHandler.getTargetDistance();
        double vert = ClientScopeHandler.getTargetVertical();
        if (dist > 0) {
            String distText = String.format("§f距离: §e%.1f§fm  §7(Δy: §b%+.1f§7)", dist, vert);
            graphics.drawString(mc.font, distText, cx - mc.font.width(distText) / 2, cy + 25, 0xFFFFFF, true);
        }

        // ---- 超出射程提示 ----
        if (ClientScopeHandler.hasSolved() && ClientScopeHandler.isLastOutOfRange()) {
            String warnText = "§c⚠ 超出射程";
            graphics.drawString(mc.font, warnText, cx - mc.font.width(warnText) / 2, cy + 37, 0xFF5555, true);
        }

        // ---- 算法性能统计 ----
        drawAlgorithmStats(graphics, mc, cx, cy);

        // ---- 调试信息 ----
        if (com.piranport.debug.PiranPortDebug.isClientEnabled()) {
            String debugText = String.format("§7[火控] 长按: %dt/%dt  zoom: %.1f",
                    ClientScopeHandler.getHoldTicks(),
                    ClientScopeHandler.getScopeThreshold(),
                    ClientScopeHandler.getZoomLevel());
            graphics.drawString(mc.font, debugText, cx - mc.font.width(debugText) / 2, cy + 40, 0xFFFFFF, true);
        }
    }

    /**
     * 绘制算法性能统计信息
     */
    private static void drawAlgorithmStats(GuiGraphics graphics, Minecraft mc, int cx, int cy) {
        BallisticSolverStats stats = BallisticSolverStats.getInstance();

        int startY = cy + 50; // 距离信息下方
        int lineHeight = 12;
        int textColor = 0xAAAAAA; // 灰色
        int highlightColor = 0x55FF55; // 绿色（被选中的算法）
        int accuracyColor = 0xFFFF55; // 黄色（精度）

        // 标题
        String title = "§6[弹道解算性能]";
        graphics.drawString(mc.font, title, cx - mc.font.width(title) / 2, startY, 0xFFFFFF, true);
        startY += lineHeight + 2;

        // 三分法统计
        boolean ternaryChosen = stats.getLastChosen() == BallisticSolverStats.Algorithm.TERNARY;
        int ternaryColor = ternaryChosen ? highlightColor : textColor;
        String ternaryStats = String.format("§7三分法: §f%3d/%3d/%3d§7µs  §e误差:§f%.3f§7格 %s",
                stats.getTernaryMinUs(), stats.getTernaryAvgUs(), stats.getTernaryMaxUs(),
                stats.getTernaryAccuracy(),
                ternaryChosen ? "§a✓" : "");
        graphics.drawString(mc.font, ternaryStats, cx - mc.font.width(ternaryStats) / 2, startY, ternaryColor, true);
        startY += lineHeight;

        // 牛顿迭代法统计
        boolean newtonChosen = stats.getLastChosen() == BallisticSolverStats.Algorithm.NEWTON;
        int newtonColor = newtonChosen ? highlightColor : textColor;
        String newtonStats = String.format("§7牛顿法: §f%3d/%3d/%3d§7µs  §e误差:§f%.3f§7格 %s",
                stats.getNewtonMinUs(), stats.getNewtonAvgUs(), stats.getNewtonMaxUs(),
                stats.getNewtonAccuracy(),
                newtonChosen ? "§a✓" : "");
        graphics.drawString(mc.font, newtonStats, cx - mc.font.width(newtonStats) / 2, startY, newtonColor, newtonColor != textColor);
        startY += lineHeight;

        // 组合结果
        String combinedInfo = String.format("§7最终精度: §f%.3f§7格  §7选择: §f%s",
                stats.getCombinedAccuracy(),
                stats.getLastChosen().getDisplayName());
        graphics.drawString(mc.font, combinedInfo, cx - mc.font.width(combinedInfo) / 2, startY, accuracyColor, true);
        startY += lineHeight;

        // 服务端迭代次数
        int sTern = ClientScopeHandler.getServerTernaryIters();
        int sNewt = ClientScopeHandler.getServerNewtonIters();
        String serverInfo = String.format("§7服务端迭代: §f三分法 %d§7次  §f牛顿法 %d§7次", sTern, sNewt);
        graphics.drawString(mc.font, serverInfo, cx - mc.font.width(serverInfo) / 2, startY, accuracyColor, true);
    }
}
