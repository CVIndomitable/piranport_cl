package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.PiranPort;
import com.piranport.client.model.B25Model;
import com.piranport.client.model.F4FModel;
import com.piranport.component.AircraftInfo;
import com.piranport.entity.AircraftEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Aircraft renderer using 3D entity models.
 * B25 (LEVEL_BOMBER) uses B25Model, all others use F4FModel.
 */
public class AircraftRenderer extends EntityRenderer<AircraftEntity> {

    private static final ResourceLocation B25_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/b25.png");
    private static final ResourceLocation F4F_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/f4f.png");

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
        ResourceLocation texture;
        if (entity.getAircraftType() == AircraftInfo.AircraftType.LEVEL_BOMBER) {
            model = b25Model;
            texture = B25_TEXTURE;
        } else {
            model = f4fModel;
            texture = F4F_TEXTURE;
        }

        VertexConsumer vertexConsumer = bufferSource.getBuffer(model.renderType(texture));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AircraftEntity entity) {
        if (entity.getAircraftType() == AircraftInfo.AircraftType.LEVEL_BOMBER) {
            return B25_TEXTURE;
        }
        return F4F_TEXTURE;
    }
}
