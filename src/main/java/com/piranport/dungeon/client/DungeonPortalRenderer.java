package com.piranport.dungeon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.client.SimpleEntityRenderShapes;
import com.piranport.dungeon.entity.DungeonPortalEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
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
        renderTargetLabel(entity, poseStack, buffer, packedLight);
    }

    /**
     * 多扇门时在门上方标出去向。旧存档的单门 target 为空，不显示。
     * 使用面向摄像机的 billboard 文本，远处也能分辨每扇门通向哪个节点。
     */
    private void renderTargetLabel(DungeonPortalEntity entity, PoseStack poseStack,
                                   MultiBufferSource buffer, int packedLight) {
        String target = entity.getTargetNodeId();
        if (target == null || target.isBlank()) return;
        Component text = Component.literal("→ " + target);
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        poseStack.translate(0.0, 1.25, 0.0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-0.025f, -0.025f, 0.025f);
        // 门内文字用满亮，夜间/水下也能看清每扇门的去向。
        font.drawInBatch(text, -font.width(text) / 2.0f, 0.0f, 0xFFFFFFFF, false,
                poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(DungeonPortalEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");
    }
}
