package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.entity.LowTierDestroyerEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Placeholder renderer for LowTierDestroyerEntity.
 * The entity uses isCurrentlyGlowing()=true so it shows a glowing outline.
 * Replace with a proper model/texture later.
 */
public class LowTierDestroyerRenderer extends EntityRenderer<LowTierDestroyerEntity> {

    public LowTierDestroyerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LowTierDestroyerEntity entity, float entityYaw, float partialTick,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LowTierDestroyerEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
    }
}
