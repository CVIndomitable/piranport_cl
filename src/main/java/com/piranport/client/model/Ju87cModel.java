package com.piranport.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Ju 87C 斯图卡实体模型。
 *
 * <p>造型重点：倒海鸥翼用三段不同高度的翼面近似、固定起落架轮罩、
 * 黄色发动机罩、三叶螺旋桨、长条座舱罩。机头朝 -Z，y 轴向下。
 *
 * <p>BOXES 由 小工具/dev_draw_<机型>_entity.py 与贴图同源生成，改模型必须同时重新生成贴图。
 */
public class Ju87cModel<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "ju87c"), "main");

    private final ModelPart bb_main;

    public Ju87cModel(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("bb_main",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -2.5F, -6.0F, 2.0F, 2.5F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(30, 0).addBox(-1.0F, -3.0F, -2.5F, 2.0F, 0.5F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 0).addBox(-0.5F, -2.0F, -6.8F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 8).addBox(-1.5F, -3.0F, -6.7F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(50, 0).addBox(-0.5F, 0.0F, -5.2F, 1.0F, 0.8F, 1.5F, new CubeDeformation(0.0F))
                        .texOffs(0, 16).addBox(-2.5F, -1.5F, -2.6F, 5.0F, 0.1F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(22, 16).addBox(-4.5F, -0.9F, -2.5F, 2.0F, 0.1F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(22, 16).addBox(2.5F, -0.9F, -2.5F, 2.0F, 0.1F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 16).addBox(-7.5F, -1.5F, -2.5F, 3.0F, 0.1F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 16).addBox(4.5F, -1.5F, -2.5F, 3.0F, 0.1F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 22).addBox(-2.5F, -1.6F, 4.2F, 5.0F, 0.1F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 0).addBox(0.0F, -5.4F, 4.5F, 0.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 6).addBox(-3.2F, -0.5F, -2.5F, 0.7F, 1.2F, 2.4F, new CubeDeformation(0.0F))
                        .texOffs(56, 6).addBox(2.5F, -0.5F, -2.5F, 0.7F, 1.2F, 2.4F, new CubeDeformation(0.0F))
                        .texOffs(60, 40).addBox(-3.1F, 0.7F, -2.0F, 0.5F, 0.4F, 0.8F, new CubeDeformation(0.0F))
                        .texOffs(60, 40).addBox(2.6F, 0.7F, -2.0F, 0.5F, 0.4F, 0.8F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 64, 64);
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
