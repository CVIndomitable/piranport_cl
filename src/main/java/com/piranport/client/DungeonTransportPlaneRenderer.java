package com.piranport.client;

import com.piranport.entity.DungeonTransportPlaneEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Placeholder renderer for DungeonTransportPlaneEntity.
 * The entity uses isCurrentlyGlowing()=true for visibility.
 */
public class DungeonTransportPlaneRenderer extends EntityRenderer<DungeonTransportPlaneEntity> {

    public DungeonTransportPlaneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(DungeonTransportPlaneEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/boat/oak.png");
    }
}
