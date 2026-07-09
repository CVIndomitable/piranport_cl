package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.PiranPort;
import com.piranport.client.model.DeepOceanLightCarrierModel;
import com.piranport.npc.deepocean.DeepOceanLightCarrierEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DeepOceanLightCarrierRenderer
        extends MobRenderer<DeepOceanLightCarrierEntity, DeepOceanLightCarrierModel> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
            "textures/entity/deep_ocean/light_carrier_model.png");

    public DeepOceanLightCarrierRenderer(EntityRendererProvider.Context context) {
        super(context, new DeepOceanLightCarrierModel(context.bakeLayer(DeepOceanLightCarrierModel.LAYER_LOCATION)), 0.68F);
    }

    @Override
    public ResourceLocation getTextureLocation(DeepOceanLightCarrierEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(DeepOceanLightCarrierEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(1.02F, 1.02F, 1.02F);
    }
}
