package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.npc.shipgirl.ShipGirlEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 吹雪舰娘 NPC 备用渲染器。
 * 使用 Alex/slim 玩家体型，吹雪特征通过皮肤和舰装层表现。
 */
public class FubukiShipGirlRenderer extends MobRenderer<ShipGirlEntity, PlayerModel<ShipGirlEntity>> {
    private static final ResourceLocation TEXTURE_LOCATION =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/skin/skin_8.png");

    public FubukiShipGirlRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
        addLayer(new FubukiShipGirlRiggingLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(ShipGirlEntity entity) {
        return TEXTURE_LOCATION;
    }
}
