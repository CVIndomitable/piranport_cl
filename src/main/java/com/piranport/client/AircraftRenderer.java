package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.client.model.B25Model;
import com.piranport.client.model.F4FModel;
import com.piranport.entity.AircraftEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Aircraft renderer using 3D entity models. The model and texture are chosen
 * from the entity's immutable aircraft definition visual id.
 */
public class AircraftRenderer extends EntityRenderer<AircraftEntity> {

    private final B25Model<AircraftEntity> b25Model;
    private final F4FModel<AircraftEntity> f4fModel;

    public AircraftRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5f;
        this.b25Model = new B25Model<>(ctx.bakeLayer(B25Model.LAYER_LOCATION));
        this.f4fModel = new F4FModel<>(ctx.bakeLayer(F4FModel.LAYER_LOCATION));
    }

    @Override
    public void render(AircraftEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Canonical MC entity rotation: YP(180 - yaw) with scale(-1, -1, 1).
        // The model is authored facing -Z (nose at Z=-5); this rotates the
        // nose to +Z at yaw=0 (entity facing south) and flips the upside-down
        // Blockbench Y/X convention.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));
        float pitch = net.minecraft.util.Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        // Pitch flips sign because the model's forward (+Z after Y rotation) is
        // achieved via a 180° flip; a positive Minecraft xRot (nose-down) must
        // rotate the model's local nose downward.
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.scale(-1.5f, -1.5f, 1.5f);
        poseStack.translate(0.0, -1.501, 0.0);

        EntityModel<AircraftEntity> model;
        AircraftVisualRegistry.AircraftVisual visual = AircraftVisualRegistry.resolve(entity);
        ResourceLocation texture = visual.texture();
        if (visual.modelKind() == AircraftVisualRegistry.ModelKind.B25) {
            model = b25Model;
        } else {
            model = f4fModel;
        }

        VertexConsumer vertexConsumer = bufferSource.getBuffer(model.renderType(texture));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();

        // 鱼雷机平飞段：画一条机头→预测撞击点的预瞄线。
        // RenderType.lines() 的顶点在 entity 局部空间（原点在插值后的实体位置），
        // 所以世界坐标要减去插值实体位置；颜色用半透明白，不遮视野。
        Vec3 aim = entity.getTorpedoAimPoint();
        if (aim != null) {
            double ex = net.minecraft.util.Mth.lerp(partialTick, entity.xo, entity.getX());
            double ey = net.minecraft.util.Mth.lerp(partialTick, entity.yo, entity.getY());
            double ez = net.minecraft.util.Mth.lerp(partialTick, entity.zo, entity.getZ());
            poseStack.pushPose();
            VertexConsumer line = bufferSource.getBuffer(RenderType.lines());
            PoseStack.Pose last = poseStack.last();
            addLineVertex(line, last, 0.0f, 0.5f, 0.0f);
            addLineVertex(line, last,
                    (float) (aim.x - ex), (float) (aim.y - ey), (float) (aim.z - ez));
            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void addLineVertex(VertexConsumer c, PoseStack.Pose pose,
                                      float x, float y, float z) {
        // lines() 需要 normal；方向随便给一个非零向量就行，线段渲染不用光照。
        c.addVertex(pose.pose(), x, y, z)
                .setColor(255, 255, 255, 160)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(AircraftEntity entity) {
        return AircraftVisualRegistry.resolve(entity).texture();
    }
}
