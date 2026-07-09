package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.entity.DungeonTransportPlaneEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Lightweight transport aircraft silhouette for dungeon insertion events. */
public class DungeonTransportPlaneRenderer extends EntityRenderer<DungeonTransportPlaneEntity> {

    public DungeonTransportPlaneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.55f;
    }

    @Override
    public void render(DungeonTransportPlaneEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() > 1.0e-5) {
            double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            float yaw = (float) (Mth.atan2(velocity.x, velocity.z) * Mth.RAD_TO_DEG);
            float pitch = (float) (Mth.atan2(velocity.y, horizontal) * Mth.RAD_TO_DEG);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));
        }

        VertexConsumer consumer = SimpleEntityRenderShapes.translucentConsumer(buffer);
        Matrix4f pose = poseStack.last().pose();
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.10f, -0.08f, -0.72f,
                0.10f, 0.10f, 0.70f, 0.62f, 0.70f, 0.76f, 0.96f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.95f, -0.03f, -0.12f,
                0.95f, 0.04f, 0.28f, 0.50f, 0.58f, 0.66f, 0.86f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.40f, 0.02f, 0.48f,
                0.40f, 0.08f, 0.68f, 0.52f, 0.60f, 0.70f, 0.82f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.04f, 0.08f, 0.46f,
                0.04f, 0.42f, 0.64f, 0.56f, 0.64f, 0.78f, 0.82f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.15f, -0.11f, -0.86f,
                0.15f, 0.13f, -0.68f, 0.24f, 0.36f, 0.48f, 0.92f, packedLight);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DungeonTransportPlaneEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/boat/oak.png");
    }
}
