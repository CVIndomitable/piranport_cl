package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
import com.piranport.skin.ClientSkinData;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Render layer that overlays a custom skin texture on the player model
 * when they have an active skin core equipped.
 * Skin textures are located at: assets/piranport/textures/skin/skin_{id}.png
 * These should be standard 64x64 player skin format PNG files.
 */
public class SkinOverlayLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public SkinOverlayLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        int skinId = ClientSkinData.getActiveSkin(player.getUUID());
        if (skinId <= 0) return;
        if (player.isInvisible()) return;

        ResourceLocation skinTexture = ResourceLocation.fromNamespaceAndPath(
                PiranPort.MOD_ID, "textures/skin/skin_" + skinId + ".png");

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(skinTexture));
        this.getParentModel().renderToBuffer(poseStack, vc, packedLight,
                OverlayTexture.NO_OVERLAY, -1);
    }
}
