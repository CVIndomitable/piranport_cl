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
 * XF5U 飞行圆饼实体模型。
 *
 * <p>扁平圆盘机身、两侧发动机短舱 + 大直径四叶桨、双垂尾、中央气泡座舱、
 * 美军星徽。机头朝 -Z，y 轴向下。
 *
 * <p>BOXES 由 小工具/dev_draw_<机型>_entity.py 与贴图同源生成，改模型必须同时重新生成贴图。
 */
public class Xf5uModel<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "xf5u"), "main");

    private final ModelPart bb_main;

    public Xf5uModel(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("bb_main",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -2.2F, -4.0F, 7.0F, 2.2F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 0).addBox(-1.0F, -3.0F, -1.5F, 2.0F, 0.8F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 0).addBox(-3.4F, -1.8F, -4.5F, 1.6F, 1.6F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 0).addBox(1.8F, -1.8F, -4.5F, 1.6F, 1.6F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 6).addBox(-4.4F, -2.6F, -4.7F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(50, 6).addBox(1.1F, -2.6F, -4.7F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(54, 0).addBox(-3.1F, -1.7F, -4.9F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(54, 0).addBox(2.1F, -1.7F, -4.9F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 12).addBox(-1.8F, -4.5F, 3.0F, 0.0F, 2.3F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 12).addBox(1.8F, -4.5F, 3.0F, 0.0F, 2.3F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 12).addBox(-2.5F, -2.0F, 4.0F, 5.0F, 0.1F, 1.5F, new CubeDeformation(0.0F)),
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
