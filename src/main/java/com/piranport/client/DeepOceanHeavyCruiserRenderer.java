package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.PiranPort;
import com.piranport.client.model.DeepOceanHeavyCruiserModel;
import com.piranport.npc.deepocean.DeepOceanHeavyCruiserEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DeepOceanHeavyCruiserRenderer
        extends MobRenderer<DeepOceanHeavyCruiserEntity, DeepOceanHeavyCruiserModel> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
            "textures/entity/deep_ocean/heavy_cruiser_model.png");

    public DeepOceanHeavyCruiserRenderer(EntityRendererProvider.Context context) {
        super(context, new DeepOceanHeavyCruiserModel(context.bakeLayer(DeepOceanHeavyCruiserModel.LAYER_LOCATION)), 0.68F);
    }

    @Override
    public ResourceLocation getTextureLocation(DeepOceanHeavyCruiserEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(DeepOceanHeavyCruiserEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(1.08F, 1.08F, 1.08F);
    }
}
