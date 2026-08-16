package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.PiranPort;
import com.piranport.client.model.KitchenGoddessModel;
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
    private final PlayerModel<ShipGirlEntity> slimModel;
    private final UnicornModel unicornModel;
    private final KitchenGoddessModel kitchenGoddessModel;

    public ShipGirlRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.defaultModel = this.model;
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.unicornModel = new UnicornModel(context.bakeLayer(UnicornModel.LAYER_LOCATION));
        this.kitchenGoddessModel = new KitchenGoddessModel(context.bakeLayer(KitchenGoddessModel.LAYER_LOCATION));
        addLayer(new ShipGirlRiggingLayer(this));
        addLayer(new FubukiShipGirlRiggingLayer(this));
    }

    @Override
    public void render(ShipGirlEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        PlayerModel<ShipGirlEntity> previousModel = this.model;
        int skinId = entity.getSkinVariant();
        if (skinId == UnicornModel.SKIN_ID) {
            this.model = this.unicornModel;
        } else if (skinId == KitchenGoddessModel.SKIN_ID) {
            this.model = this.kitchenGoddessModel;
        } else if (FubukiShipGirlRiggingLayer.supports(skinId)) {
            this.model = this.slimModel;
        } else {
            this.model = this.defaultModel;
        }
        try {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            this.model = previousModel;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ShipGirlEntity entity) {
        int skin = entity.getSkinVariant();
        if (skin == UnicornModel.SKIN_ID) {
            return UnicornModel.TEXTURE_LOCATION;
        }
        if (skin == KitchenGoddessModel.SKIN_ID) {
            return KitchenGoddessModel.TEXTURE_LOCATION;
        }
        if (skin <= 0) {
            skin = 4;
        }
        return ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/skin/skin_" + skin + ".png");
    }
}
