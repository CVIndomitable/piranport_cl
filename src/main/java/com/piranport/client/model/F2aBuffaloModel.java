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
 * F2A 水牛实体模型。
 *
 * <p>蓝灰机身、黄色机翼（战前美军黄翼涂装）、银色发动机罩、三星徽、
 * 三叶螺旋桨。机头朝 -Z，y 轴向下。
 *
 * <p>BOXES 由 小工具/dev_draw_<机型>_entity.py 与贴图同源生成，改模型必须同时重新生成贴图。
 */
public class F2aBuffaloModel<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "f2a_buffalo"), "main");

    private final ModelPart bb_main;

    public F2aBuffaloModel(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("bb_main",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -2.5F, -6.0F, 2.0F, 2.5F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(30, 0).addBox(-1.1F, -2.7F, -6.8F, 2.2F, 2.6F, 1.6F, new CubeDeformation(0.0F))
                        .texOffs(40, 0).addBox(-1.0F, -3.2F, -2.5F, 2.0F, 0.6F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(50, 0).addBox(-0.5F, -2.0F, -7.3F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 6).addBox(-1.5F, -3.0F, -7.2F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 16).addBox(-2.2F, -1.2F, -2.4F, 4.5F, 0.1F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(19, 16).addBox(-6.8F, -1.2F, -2.3F, 4.5F, 0.1F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(19, 16).addBox(2.2F, -1.2F, -2.3F, 4.5F, 0.1F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 21).addBox(-2.2F, -1.4F, 4.2F, 4.5F, 0.1F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 16).addBox(0.0F, -5.4F, 4.5F, 0.0F, 2.8F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(60, 40).addBox(-0.1F, 0.0F, 5.4F, 0.2F, 0.4F, 0.4F, new CubeDeformation(0.0F)),
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
