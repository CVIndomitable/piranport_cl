package com.piranport.dungeon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.client.SimpleEntityRenderShapes;
import com.piranport.dungeon.entity.DungeonPortalEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Simple renderer for the dungeon portal entity.
 * Visual effects are primarily handled via particles in DungeonPortalEntity.tick().
 */
public class DungeonPortalRenderer extends EntityRenderer<DungeonPortalEntity> {

    public DungeonPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.45f;
    }

    @Override
    public void render(DungeonPortalEntity entity, float entityYaw, float partialTick,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.75, 0.0);
        float spin = (entity.tickCount + partialTick) * 2.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));

        VertexConsumer consumer = SimpleEntityRenderShapes.translucentConsumer(buffer);
        Matrix4f pose = poseStack.last().pose();
        int glowLight = 0x00F000F0;

        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.72f, -0.82f, -0.08f,
                -0.54f, 0.82f, 0.08f, 0.18f, 0.05f, 0.28f, 0.86f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, 0.54f, -0.82f, -0.08f,
                0.72f, 0.82f, 0.08f, 0.18f, 0.05f, 0.28f, 0.86f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.72f, 0.64f, -0.08f,
                0.72f, 0.82f, 0.08f, 0.24f, 0.06f, 0.34f, 0.88f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.72f, -0.82f, -0.08f,
                0.72f, -0.64f, 0.08f, 0.12f, 0.04f, 0.22f, 0.82f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.50f, -0.58f, -0.025f,
                0.50f, 0.58f, 0.025f, 0.34f, 0.12f, 0.92f, 0.46f, glowLight);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DungeonPortalEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");
    }
}
