package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.PiranPort;
import com.piranport.client.model.DeepOceanBattleCruiserModel;
import com.piranport.npc.deepocean.DeepOceanBattleCruiserEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DeepOceanBattleCruiserRenderer
        extends MobRenderer<DeepOceanBattleCruiserEntity, DeepOceanBattleCruiserModel> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
            "textures/entity/deep_ocean/battle_cruiser_model.png");

    public DeepOceanBattleCruiserRenderer(EntityRendererProvider.Context context) {
        super(context, new DeepOceanBattleCruiserModel(context.bakeLayer(DeepOceanBattleCruiserModel.LAYER_LOCATION)), 0.76F);
    }

    @Override
    public ResourceLocation getTextureLocation(DeepOceanBattleCruiserEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(DeepOceanBattleCruiserEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(1.10F, 1.10F, 1.10F);
    }
}
