package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.entity.LowTierDestroyerEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Lightweight ship silhouette for the low-tier dungeon destroyer. */
public class LowTierDestroyerRenderer extends EntityRenderer<LowTierDestroyerEntity> {

    public LowTierDestroyerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.42f;
    }

    @Override
    public void render(LowTierDestroyerEntity entity, float entityYaw, float partialTick,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));

        VertexConsumer consumer = SimpleEntityRenderShapes.translucentConsumer(buffer);
        Matrix4f pose = poseStack.last().pose();

        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.62f, -0.08f, -0.18f,
                0.62f, 0.10f, 0.18f, 0.28f, 0.34f, 0.48f, 0.92f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.76f, -0.04f, -0.08f,
                -0.54f, 0.14f, 0.12f, 0.18f, 0.24f, 0.36f, 0.84f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, 0.54f, -0.04f, -0.08f,
                0.76f, 0.14f, 0.12f, 0.18f, 0.24f, 0.36f, 0.84f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.12f, 0.10f, -0.10f,
                0.12f, 0.38f, 0.14f, 0.42f, 0.48f, 0.58f, 0.92f, packedLight);
        renderTurret(consumer, pose, -0.34f, packedLight);
        renderTurret(consumer, pose, 0.34f, packedLight);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void renderTurret(VertexConsumer consumer, Matrix4f pose, float x, int light) {
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, x - 0.09f, 0.12f, -0.18f,
                x + 0.09f, 0.22f, -0.02f, 0.54f, 0.58f, 0.64f, 0.94f, light);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, x - 0.025f, 0.16f, -0.34f,
                x + 0.025f, 0.20f, -0.16f, 0.74f, 0.76f, 0.80f, 0.92f, light);
    }

    @Override
    public ResourceLocation getTextureLocation(LowTierDestroyerEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
    }
}
