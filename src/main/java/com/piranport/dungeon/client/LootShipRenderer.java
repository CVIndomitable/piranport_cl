package com.piranport.dungeon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.client.SimpleEntityRenderShapes;
import com.piranport.dungeon.entity.LootShipEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Simple renderer for the loot ship entity.
 */
public class LootShipRenderer extends EntityRenderer<LootShipEntity> {

    public LootShipRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.55f;
    }

    @Override
    public void render(LootShipEntity entity, float entityYaw, float partialTick,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));

        VertexConsumer consumer = SimpleEntityRenderShapes.translucentConsumer(buffer);
        Matrix4f pose = poseStack.last().pose();

        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.75f, -0.18f, -0.28f,
                0.75f, 0.02f, 0.28f, 0.46f, 0.25f, 0.12f, 0.95f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.62f, 0.00f, -0.22f,
                0.62f, 0.10f, 0.22f, 0.66f, 0.43f, 0.22f, 0.92f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.52f, 0.10f, -0.18f,
                -0.10f, 0.46f, 0.18f, 0.58f, 0.32f, 0.12f, 0.96f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.48f, 0.44f, -0.19f,
                -0.14f, 0.50f, 0.19f, 0.92f, 0.78f, 0.32f, 0.90f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, 0.22f, 0.08f, -0.03f,
                0.30f, 0.78f, 0.03f, 0.82f, 0.72f, 0.40f, 0.72f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, 0.18f, 0.72f, -0.12f,
                0.34f, 0.88f, 0.12f, 0.26f, 0.62f, 1.00f, 0.45f, 0x00F000F0);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LootShipEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/boat/oak.png");
    }
}
