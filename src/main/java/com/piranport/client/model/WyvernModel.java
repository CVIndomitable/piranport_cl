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
 * 飞龙（Westland Wyvern）实体模型。
 *
 * <p>长机头 + 红色桨毂 + 四叶桨、深蓝灰机翼、银色机身、翼下火箭。
 * 机头朝 -Z，y 轴向下；机翼/平尾用 0.1 格厚度避免上下异色时 z-fighting。
 *
 * <p>BOXES 由 小工具/dev_draw_<机型>_entity.py 与贴图同源生成，改模型必须同时重新生成贴图。
 */
public class WyvernModel<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "wyvern"), "main");

    private final ModelPart bb_main;

    public WyvernModel(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("bb_main",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -2.5F, -7.0F, 2.0F, 2.5F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 0).addBox(-1.0F, -3.1F, -2.5F, 2.0F, 0.6F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(46, 0).addBox(-0.5F, -2.0F, -8.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(46, 6).addBox(-1.5F, -3.0F, -7.9F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(50, 10).addBox(-0.7F, 0.0F, -6.0F, 1.4F, 0.7F, 1.8F, new CubeDeformation(0.0F))
                        .texOffs(0, 17).addBox(-2.5F, -1.2F, -2.8F, 5.0F, 0.1F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(22, 17).addBox(-7.5F, -1.2F, -2.6F, 5.0F, 0.1F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(22, 17).addBox(2.5F, -1.2F, -2.6F, 5.0F, 0.1F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 24).addBox(-2.5F, -1.6F, 4.4F, 5.0F, 0.1F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 0).addBox(0.0F, -5.6F, 4.7F, 0.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 12).addBox(-5.2F, 0.0F, -1.6F, 0.3F, 0.3F, 1.6F, new CubeDeformation(0.0F))
                        .texOffs(44, 12).addBox(-4.3F, 0.0F, -1.6F, 0.3F, 0.3F, 1.6F, new CubeDeformation(0.0F))
                        .texOffs(44, 12).addBox(3.9F, 0.0F, -1.6F, 0.3F, 0.3F, 1.6F, new CubeDeformation(0.0F))
                        .texOffs(44, 12).addBox(4.8F, 0.0F, -1.6F, 0.3F, 0.3F, 1.6F, new CubeDeformation(0.0F))
                        .texOffs(60, 40).addBox(-0.1F, 0.0F, 6.2F, 0.2F, 0.4F, 0.4F, new CubeDeformation(0.0F)),
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
