package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.PiranPort;
import com.piranport.client.model.DeepOceanSubmarineModel;
import com.piranport.npc.deepocean.DeepOceanSubmarineEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DeepOceanSubmarineRenderer
        extends MobRenderer<DeepOceanSubmarineEntity, DeepOceanSubmarineModel> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
            "textures/entity/deep_ocean/submarine_model.png");

    public DeepOceanSubmarineRenderer(EntityRendererProvider.Context context) {
        super(context, new DeepOceanSubmarineModel(context.bakeLayer(DeepOceanSubmarineModel.LAYER_LOCATION)), 0.48F);
    }

    @Override
    public ResourceLocation getTextureLocation(DeepOceanSubmarineEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(DeepOceanSubmarineEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(0.90F, 0.90F, 0.90F);
    }
}
