package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.PiranPort;
import com.piranport.client.model.DeepOceanDestroyerModel;
import com.piranport.npc.deepocean.DeepOceanDestroyerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DeepOceanDestroyerRenderer extends MobRenderer<DeepOceanDestroyerEntity, DeepOceanDestroyerModel> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
            "textures/entity/deep_ocean/destroyer_model.png");

    public DeepOceanDestroyerRenderer(EntityRendererProvider.Context context) {
        super(context, new DeepOceanDestroyerModel(context.bakeLayer(DeepOceanDestroyerModel.LAYER_LOCATION)), 0.50F);
    }

    @Override
    public ResourceLocation getTextureLocation(DeepOceanDestroyerEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(DeepOceanDestroyerEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(0.92F, 0.92F, 0.92F);
    }
}
