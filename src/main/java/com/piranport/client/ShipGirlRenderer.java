package com.piranport.client;

import com.piranport.npc.shipgirl.ShipGirlEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for ShipGirlEntity (艾拉) using the vanilla PlayerModel with the default Steve texture.
 */
public class ShipGirlRenderer extends MobRenderer<ShipGirlEntity, PlayerModel<ShipGirlEntity>> {

    private static final ResourceLocation STEVE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    public ShipGirlRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(ShipGirlEntity entity) {
        return STEVE_TEXTURE;
    }
}
