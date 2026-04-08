package com.piranport.client.model;

import com.piranport.PiranPort;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * F4F Wildcat entity model (ported from Blockbench via sheropshire).
 * Used for all aircraft types EXCEPT LEVEL_BOMBER.
 */
public class F4FModel<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "f4f"), "main");

    private final ModelPart bb_main;

    public F4FModel(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("bb_main",
                CubeListBuilder.create()
                        .texOffs(0, 3).addBox(-1.0F, -2.0F, -5.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-1.0F, -3.0F, -2.25F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(0.0F, -4.0F, 4.0F, 0.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 7).addBox(0.0F, -2.75F, -1.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(4, 2).addBox(0.0F, -3.0F, 3.0F, 0.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-6.0F, -1.0F, -3.5F, 12.0F, 0.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 3).addBox(-3.0F, -1.75F, 3.0F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(6, 11).addBox(-0.5F, -3.0F, -2.25F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 3).addBox(-0.5F, -2.0F, 1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 5).addBox(-0.5F, -1.5F, 3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, int color) {
        bb_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
