package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.npc.shipgirl.ShipGirlEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.joml.Matrix4f;

/**
 * 吹雪专用舰装层；人体保持原版 Alex/slim 玩家模型。
 */
public class FubukiShipGirlRiggingLayer extends RenderLayer<ShipGirlEntity, PlayerModel<ShipGirlEntity>> {
    private static final float METAL_R = 0.44F;
    private static final float METAL_G = 0.45F;
    private static final float METAL_B = 0.40F;
    private static final float DARK_R = 0.20F;
    private static final float DARK_G = 0.22F;
    private static final float DARK_B = 0.23F;
    private static final float CANVAS_R = 0.82F;
    private static final float CANVAS_G = 0.80F;
    private static final float CANVAS_B = 0.70F;
    private static final float MARK_R = 0.93F;
    private static final float MARK_G = 0.92F;
    private static final float MARK_B = 0.84F;

    public FubukiShipGirlRiggingLayer(RenderLayerParent<ShipGirlEntity, PlayerModel<ShipGirlEntity>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       ShipGirlEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }

        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0D, 0.25D + Math.sin(ageInTicks * 0.08F) * 0.01D, 0.36D);

        renderBackHarness(poseStack, bufferSource, packedLight);
        renderSideRigging(poseStack, bufferSource, packedLight, -1.0F);
        renderSideRigging(poseStack, bufferSource, packedLight, 1.0F);
        renderCenterMast(poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    private static void renderBackHarness(PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        VertexConsumer consumer = SimpleEntityRenderShapes.translucentConsumer(bufferSource);
        Matrix4f pose = poseStack.last().pose();

        cuboid(consumer, pose, light,
                -0.23F, -0.05F, -0.05F, 0.23F, 0.18F, 0.10F,
                DARK_R, DARK_G, DARK_B, 0.78F);
        cuboid(consumer, pose, light,
                -0.06F, -0.12F, -0.02F, 0.06F, 0.42F, 0.08F,
                METAL_R, METAL_G, METAL_B, 0.78F);
        cuboid(consumer, pose, light,
                -0.34F, 0.09F, 0.02F, 0.34F, 0.15F, 0.10F,
                CANVAS_R, CANVAS_G, CANVAS_B, 0.70F);
    }

    private static void renderSideRigging(PoseStack poseStack, MultiBufferSource bufferSource, int light, float side) {
        poseStack.pushPose();
        poseStack.translate(side * 0.48D, 0.02D, 0.06D);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -8.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * 2.0F));

        VertexConsumer consumer = SimpleEntityRenderShapes.translucentConsumer(bufferSource);
        Matrix4f pose = poseStack.last().pose();

        renderHull(consumer, pose, light, side);
        renderTurret(consumer, pose, light, side, -0.04F, 0.17F);
        renderTurret(consumer, pose, light, side, 0.12F, 0.11F);
        renderTripleTorpedoRack(consumer, pose, light, side);

        poseStack.popPose();
    }

    private static void renderHull(VertexConsumer consumer, Matrix4f pose, int light, float side) {
        cuboid(consumer, pose, light,
                side * 0.02F, -0.08F, -0.10F, side * 0.32F, 0.09F, 0.24F,
                METAL_R, METAL_G, METAL_B, 0.82F);
        cuboid(consumer, pose, light,
                side * 0.04F, 0.07F, -0.02F, side * 0.30F, 0.20F, 0.17F,
                DARK_R, DARK_G, DARK_B, 0.80F);
        cuboid(consumer, pose, light,
                side * 0.08F, -0.12F, 0.18F, side * 0.26F, -0.07F, 0.27F,
                CANVAS_R, CANVAS_G, CANVAS_B, 0.74F);

        if (side > 0.0F) {
            cuboid(consumer, pose, light,
                    0.095F, 0.095F, 0.171F, 0.125F, 0.18F, 0.205F,
                    MARK_R, MARK_G, MARK_B, 0.88F);
            cuboid(consumer, pose, light,
                    0.165F, 0.095F, 0.171F, 0.195F, 0.18F, 0.205F,
                    MARK_R, MARK_G, MARK_B, 0.88F);
        }
    }

    private static void renderTurret(VertexConsumer consumer, Matrix4f pose, int light,
                                     float side, float x, float y) {
        float baseX = side * x;
        cuboid(consumer, pose, light,
                baseX - 0.065F, y, -0.18F, baseX + 0.065F, y + 0.075F, -0.04F,
                CANVAS_R, CANVAS_G, CANVAS_B, 0.86F);
        cuboid(consumer, pose, light,
                baseX - 0.020F, y + 0.030F, -0.36F, baseX + 0.020F, y + 0.055F, -0.17F,
                MARK_R, MARK_G, MARK_B, 0.84F);
    }

    private static void renderTripleTorpedoRack(VertexConsumer consumer, Matrix4f pose, int light, float side) {
        float rackX = side * 0.18F;
        cuboid(consumer, pose, light,
                rackX - 0.085F, -0.015F, 0.22F, rackX + 0.085F, 0.135F, 0.27F,
                DARK_R, DARK_G, DARK_B, 0.78F);
        for (int i = 0; i < 3; i++) {
            float y = 0.005F + i * 0.045F;
            cuboid(consumer, pose, light,
                    rackX - 0.065F, y, 0.255F, rackX + 0.065F, y + 0.025F, 0.43F,
                    METAL_R * 0.92F, METAL_G * 0.92F, METAL_B * 0.92F, 0.84F);
            cuboid(consumer, pose, light,
                    rackX - 0.055F, y + 0.004F, 0.425F, rackX + 0.055F, y + 0.021F, 0.475F,
                    MARK_R, MARK_G, MARK_B, 0.82F);
        }
    }

    private static void renderCenterMast(PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        VertexConsumer consumer = SimpleEntityRenderShapes.translucentConsumer(bufferSource);
        Matrix4f pose = poseStack.last().pose();

        cuboid(consumer, pose, light,
                -0.08F, 0.16F, -0.02F, 0.08F, 0.31F, 0.08F,
                CANVAS_R, CANVAS_G, CANVAS_B, 0.78F);
        cuboid(consumer, pose, light,
                -0.018F, 0.30F, -0.005F, 0.018F, 0.63F, 0.035F,
                MARK_R, MARK_G, MARK_B, 0.80F);
        cuboid(consumer, pose, light,
                -0.18F, 0.49F, 0.002F, 0.18F, 0.525F, 0.032F,
                METAL_R, METAL_G, METAL_B, 0.78F);
        cuboid(consumer, pose, light,
                0.05F, 0.40F, 0.010F, 0.25F, 0.43F, 0.040F,
                MARK_R, MARK_G, MARK_B, 0.76F);
    }

    private static void cuboid(VertexConsumer consumer, Matrix4f pose, int light,
                               float minX, float minY, float minZ,
                               float maxX, float maxY, float maxZ,
                               float r, float g, float b, float alpha) {
        float x1 = Math.min(minX, maxX);
        float x2 = Math.max(minX, maxX);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose,
                x1, minY, minZ, x2, maxY, maxZ, r, g, b, alpha, light);
    }
}
