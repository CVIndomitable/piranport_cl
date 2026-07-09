package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.PiranPort;
import com.piranport.client.model.DeepOceanBattleshipModel;
import com.piranport.npc.deepocean.DeepOceanBattleshipEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DeepOceanBattleshipRenderer
        extends MobRenderer<DeepOceanBattleshipEntity, DeepOceanBattleshipModel> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
            "textures/entity/deep_ocean/battleship_model.png");

    public DeepOceanBattleshipRenderer(EntityRendererProvider.Context context) {
        super(context, new DeepOceanBattleshipModel(context.bakeLayer(DeepOceanBattleshipModel.LAYER_LOCATION)), 0.82F);
    }

    @Override
    public ResourceLocation getTextureLocation(DeepOceanBattleshipEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(DeepOceanBattleshipEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(1.12F, 1.12F, 1.12F);
    }
}
