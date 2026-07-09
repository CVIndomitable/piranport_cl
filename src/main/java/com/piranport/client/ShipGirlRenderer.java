package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.PiranPort;
import com.piranport.client.model.UnicornModel;
import com.piranport.npc.shipgirl.ShipGirlEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for ShipGirlEntity using the existing shipgirl skin texture set.
 */
public class ShipGirlRenderer extends MobRenderer<ShipGirlEntity, PlayerModel<ShipGirlEntity>> {
    private final PlayerModel<ShipGirlEntity> defaultModel;
    private final UnicornModel unicornModel;

    public ShipGirlRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.defaultModel = this.model;
        this.unicornModel = new UnicornModel(context.bakeLayer(UnicornModel.LAYER_LOCATION));
        addLayer(new ShipGirlRiggingLayer(this));
    }

    @Override
    public void render(ShipGirlEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        PlayerModel<ShipGirlEntity> previousModel = this.model;
        this.model = entity.getSkinVariant() == UnicornModel.SKIN_ID ? this.unicornModel : this.defaultModel;
        try {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            this.model = previousModel;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ShipGirlEntity entity) {
        if (entity.getSkinVariant() == UnicornModel.SKIN_ID) {
            return UnicornModel.TEXTURE_LOCATION;
        }
        int skin = entity.getSkinVariant();
        if (skin <= 0) {
            skin = 4;
        }
        return ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/skin/skin_" + skin + ".png");
    }
}
