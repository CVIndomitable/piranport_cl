package com.piranport.dungeon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.dungeon.entity.LootShipEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Simple renderer for the loot ship entity.
 * A proper boat/chest model can be added later.
 */
public class LootShipRenderer extends EntityRenderer<LootShipEntity> {

    public LootShipRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LootShipEntity entity, float entityYaw, float partialTick,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Placeholder: no custom model yet
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LootShipEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/boat/oak.png");
    }
}
