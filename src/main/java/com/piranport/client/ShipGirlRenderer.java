package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.npc.shipgirl.ShipGirlEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for ShipGirlEntity (艾拉) using the Villager model.
 */
public class ShipGirlRenderer extends MobRenderer<ShipGirlEntity, VillagerModel<ShipGirlEntity>> {

    private static final ResourceLocation VILLAGER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");

    public ShipGirlRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
    }

    @Override
    public void render(ShipGirlEntity entity, float entityYaw, float partialTick,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ShipGirlEntity entity) {
        return VILLAGER_TEXTURE;
    }
}
