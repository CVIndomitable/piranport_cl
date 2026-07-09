package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.PiranPort;
import com.piranport.client.model.DeepOceanSupplyModel;
import com.piranport.npc.deepocean.DeepOceanSupplyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DeepOceanSupplyRenderer extends MobRenderer<DeepOceanSupplyEntity, DeepOceanSupplyModel> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
            "textures/entity/deep_ocean/supply_model.png");

    public DeepOceanSupplyRenderer(EntityRendererProvider.Context context) {
        super(context, new DeepOceanSupplyModel(context.bakeLayer(DeepOceanSupplyModel.LAYER_LOCATION)), 0.56F);
    }

    @Override
    public ResourceLocation getTextureLocation(DeepOceanSupplyEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(DeepOceanSupplyEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(0.90F, 0.90F, 0.90F);
    }
}
