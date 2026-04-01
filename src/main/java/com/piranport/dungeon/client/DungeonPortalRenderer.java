package com.piranport.dungeon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.dungeon.entity.DungeonPortalEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Simple renderer for the dungeon portal entity.
 * Visual effects are primarily handled via particles in DungeonPortalEntity.tick().
 * A proper model/texture can be added later.
 */
public class DungeonPortalRenderer extends EntityRenderer<DungeonPortalEntity> {

    public DungeonPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DungeonPortalEntity entity, float entityYaw, float partialTick,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Placeholder: no custom model yet. Particles handle visuals.
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DungeonPortalEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");
    }
}
