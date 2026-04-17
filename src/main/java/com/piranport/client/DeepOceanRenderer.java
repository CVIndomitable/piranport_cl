package com.piranport.client;

import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for all deep ocean entities using the Stray (小白) model.
 * Fleet state particles are spawned server-side in AbstractDeepOceanEntity.tick().
 */
public class DeepOceanRenderer extends HumanoidMobRenderer<AbstractDeepOceanEntity, HumanoidModel<AbstractDeepOceanEntity>> {

    private static final ResourceLocation STRAY_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/skeleton/stray.png");

    public DeepOceanRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractDeepOceanEntity entity) {
        return STRAY_TEXTURE;
    }
}
