package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
import com.piranport.block.entity.B25DebugBlockEntity;
import com.piranport.client.model.B25Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Static B25 model renderer for the /ppd model_debug debug block.
 *
 * <p>Applies the same pose transform as {@link AircraftRenderer} at yaw=0/pitch=0
 * so we can calibrate what "yaw=0" actually points at in world space by reading
 * the six surrounding direction signs.
 */
public class B25DebugBlockEntityRenderer implements BlockEntityRenderer<B25DebugBlockEntity> {

    private static final ResourceLocation B25_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/b25.png");

    private final B25Model<Entity> model;

    public B25DebugBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new B25Model<>(ctx.getModelSet().bakeLayer(B25Model.LAYER_LOCATION));
    }

    @Override
    public void render(B25DebugBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // Center of the block
        poseStack.translate(0.5, 0.5, 0.5);

        // Mirror AircraftRenderer transform with yaw=0 / pitch=0:
        //   YP(180 - 0) = 180° around Y, then scale(-1,-1,1), then translate(0,-1.501,0).
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0f));
        poseStack.scale(-1.5f, -1.5f, 1.5f);
        poseStack.translate(0.0, -1.501, 0.0);

        VertexConsumer vc = bufferSource.getBuffer(model.renderType(B25_TEXTURE));
        model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();
    }
}
