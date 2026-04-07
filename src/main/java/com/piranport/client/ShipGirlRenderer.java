package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.npc.shipgirl.ShipGirlEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Placeholder renderer for ShipGirlEntity.
 * Uses isCurrentlyGlowing()=true for visibility. Replace with skin system later.
 */
public class ShipGirlRenderer extends EntityRenderer<ShipGirlEntity> {

    public ShipGirlRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ShipGirlEntity entity, float entityYaw, float partialTick,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ShipGirlEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");
    }
}
