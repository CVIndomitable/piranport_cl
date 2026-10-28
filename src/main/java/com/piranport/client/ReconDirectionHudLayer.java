package com.piranport.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.piranport.aviation.ClientReconData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Matrix4f;

/**
 * 侦察机方向指示：在准星周围画一个箭头，指向侦察机当前的机头/实际飞行方向。
 *
 * <p><b>为什么不用 {@code aircraft.getYRot()}</b>：侦察模式里客户端实体的 yRot 每 tick
 * 被 {@link com.piranport.client.input.ReconInputHandler} 镜像成玩家视线角度，
 * 用来让相机跟着鼠标转。它表示的是「玩家在看哪」，不是「飞机在往哪飞」。
 * 服务端的真实机头在 {@code AircraftFlight} 里由速度反算，客户端拿不到，
 * 因此这里改用客户端实体的逐 tick 位移方向——位置经 {@code lerpTo} 插值同步，
 * 方向与服务端用的速度方向同源，且不会因为玩家转头而漂移。
 *
 * <p>相对方位 = wrapDegrees(机头 yaw - 相机 yaw)。MC 中 yaw 增大是向右转，
 * 所以正值画在准星右侧，0 度画在准星正上方（即准星指向）。
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class ReconDirectionHudLayer {

    /** 箭头离准星中心的半径（像素）。十字准星臂长约 9px，留出间隙不遮挡准星本体。 */
    private static final float RING_RADIUS = 21.0f;
    /** 位移平方阈值：约 (0.001 格/tick)^2，低于此认为本 tick 没有水平移动。 */
    private static final double MIN_MOVE_SQ = 1.0E-6;

    private static final int ARROW_COLOR = 0xDC7FE9FF;   // 青色，半透明
    private static final int RING_COLOR = 0x2DFFFFFF;    // 极淡白圈，仅作方位参考

    /** 最近一次解析出的机头 yaw；位移为 0 的 tick 沿用它，避免箭头闪回正上方。 */
    private static float lastHeadingYaw = Float.NaN;
    /** 上一次观测的实体 id，换机（或重新进入侦察）时清空缓存。 */
    private static int lastEntityId = -1;

    private ReconDirectionHudLayer() {}

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.CROSSHAIR.equals(event.getName())) return;

        // 不在侦察模式时顺手清缓存，避免下次侦察沿用上一架的机头方向。
        if (!ClientReconData.isInReconMode()) {
            reset();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;

        int entityId = ClientReconData.getReconEntityId();
        if (entityId != lastEntityId) {
            lastHeadingYaw = Float.NaN;
            lastEntityId = entityId;
        }

        Entity craft = mc.level.getEntity(entityId);
        if (craft == null) return;   // 区块未加载/已回收：静默跳过，不刷日志

        float headingYaw;
        double dx = craft.getX() - craft.xo;
        double dz = craft.getZ() - craft.zo;
        if (dx * dx + dz * dz > MIN_MOVE_SQ) {
            // 与服务端 AircraftFlight 的换算式保持一致：yaw = atan2(-vx, vz)
            headingYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            lastHeadingYaw = headingYaw;
        } else if (!Float.isNaN(lastHeadingYaw)) {
            headingYaw = lastHeadingYaw;
        } else {
            return;   // 还没有位移样本，宁可不画也不给一个假的「正上方」读数
        }

        // 屏幕朝向由相机决定：侦察模式下相机就是侦察机实体，其 yaw 已被镜像成玩家视线。
        float viewYaw = mc.gameRenderer.getMainCamera().getYRot();
        float relative = Mth.wrapDegrees(headingYaw - viewYaw);

        drawBearingArrow(event.getGuiGraphics(), relative);
    }

    /** 在准星周围按相对方位角画方向箭头；relative 正值 = 屏幕右侧。 */
    private static void drawBearingArrow(GuiGraphics gfx, float relative) {
        int cx = gfx.guiWidth() / 2;
        int cy = gfx.guiHeight() / 2;

        PoseStack pose = gfx.pose();
        pose.pushPose();
        // GUI 的 y 轴朝下，Axis.ZP 的正向旋转在屏幕上表现为顺时针，
        // 正好对应 yaw 增大向右转，无需再取反。
        pose.translate(cx, cy, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(relative));
        Matrix4f mat = pose.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        Tesselator tesselator = Tesselator.getInstance();

        // ---- 方位参考圈 ----
        BufferBuilder ring = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        RenderSystem.lineWidth(1.0f);
        int segments = 40;
        int ringA = RING_COLOR >>> 24;
        int ringR = (RING_COLOR >>> 16) & 0xFF;
        int ringG = (RING_COLOR >>> 8) & 0xFF;
        int ringB = RING_COLOR & 0xFF;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            ring.addVertex(mat, (float) (Math.sin(a0) * RING_RADIUS), (float) (-Math.cos(a0) * RING_RADIUS), 0)
                    .setColor(ringR, ringG, ringB, ringA);
            ring.addVertex(mat, (float) (Math.sin(a1) * RING_RADIUS), (float) (-Math.cos(a1) * RING_RADIUS), 0)
                    .setColor(ringR, ringG, ringB, ringA);
        }
        BufferUploader.drawWithShader(ring.buildOrThrow());

        // ---- 箭头：三角尖端朝外，指向飞机前进方向 ----
        int arrowA = ARROW_COLOR >>> 24;
        int arrowR = (ARROW_COLOR >>> 16) & 0xFF;
        int arrowG = (ARROW_COLOR >>> 8) & 0xFF;
        int arrowB = ARROW_COLOR & 0xFF;
        float baseY = -(RING_RADIUS - 5.0f);
        float tipY = -(RING_RADIUS + 6.0f);
        float shaftEndY = -(RING_RADIUS - 8.0f);
        float halfWidth = 4.5f;

        BufferBuilder arrow = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        // GUI 渲染保留背面剔除，顶点顺序必须与原版 GuiGraphics.fill 同向，
        // 否则箭头会被整块剔掉（三角形绕序按屏幕坐标 y 向下计算）。
        arrow.addVertex(mat, -halfWidth, baseY, 0).setColor(arrowR, arrowG, arrowB, arrowA);
        arrow.addVertex(mat, halfWidth, baseY, 0).setColor(arrowR, arrowG, arrowB, arrowA);
        arrow.addVertex(mat, 0, tipY, 0).setColor(arrowR, arrowG, arrowB, arrowA);
        // 第二笔画出箭杆，让方向在小尺寸下也能看清
        arrow.addVertex(mat, -1.5f, baseY, 0).setColor(arrowR, arrowG, arrowB, arrowA);
        arrow.addVertex(mat, -1.5f, shaftEndY, 0).setColor(arrowR, arrowG, arrowB, arrowA);
        arrow.addVertex(mat, 1.5f, shaftEndY, 0).setColor(arrowR, arrowG, arrowB, arrowA);
        arrow.addVertex(mat, -1.5f, baseY, 0).setColor(arrowR, arrowG, arrowB, arrowA);
        arrow.addVertex(mat, 1.5f, shaftEndY, 0).setColor(arrowR, arrowG, arrowB, arrowA);
        arrow.addVertex(mat, 1.5f, baseY, 0).setColor(arrowR, arrowG, arrowB, arrowA);
        BufferUploader.drawWithShader(arrow.buildOrThrow());

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        pose.popPose();
    }

    private static void reset() {
        lastHeadingYaw = Float.NaN;
        lastEntityId = -1;
    }
}
