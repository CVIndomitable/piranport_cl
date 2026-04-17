package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.PiranPort;
import com.piranport.block.entity.ModelDebugBlockEntity;
import com.piranport.client.model.B25Model;
import com.piranport.client.model.F4FModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Static entity-model renderer for /ppd model_debug.
 *
 * <p>Applies the same pose transform as {@link AircraftRenderer} at yaw=0/pitch=0
 * so we can confirm what "yaw=0" points at in world space via the six surrounding
 * direction signs placed by the debug command.
 */
public class ModelDebugBlockEntityRenderer implements BlockEntityRenderer<ModelDebugBlockEntity> {

    private static final ResourceLocation B25_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/b25.png");
    private static final ResourceLocation F4F_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/f4f.png");

    private final B25Model<Entity> b25;
    private final F4FModel<Entity> f4f;

    public ModelDebugBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.b25 = new B25Model<>(ctx.getModelSet().bakeLayer(B25Model.LAYER_LOCATION));
        this.f4f = new F4FModel<>(ctx.getModelSet().bakeLayer(F4FModel.LAYER_LOCATION));
    }

    @Override
    public void render(ModelDebugBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        EntityModel<Entity> model;
        ResourceLocation texture;
        switch (be.getModelType()) {
            case "f4f" -> { model = f4f; texture = F4F_TEXTURE; }
            case "b25" -> { model = b25; texture = B25_TEXTURE; }
            default    -> { model = b25; texture = B25_TEXTURE; }
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // Mirror AircraftRenderer transform with yaw=0 / pitch=0.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.scale(-1.5f, -1.5f, 1.5f);
        poseStack.translate(0.0, -1.501, 0.0);

        VertexConsumer vc = bufferSource.getBuffer(model.renderType(texture));
        model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();
    }
}
