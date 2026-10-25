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
 * 梭鱼（Fairey Barracuda）鱼雷机实体模型。
 *
 * <p>低多边形风格与 B25/F4F 一致：机身、座舱罩、桨毂、机腹散热器用实心盒，
 * 机翼、水平尾翼、垂尾用 0 厚度平面。造型特征是肩部高置机翼、
 * 架在大型垂尾上的高置水平尾翼（带两根撑杆）和长条温室座舱罩。
 *
 * <p>贴图 64×64，UV 由 texOffs + box 尺寸按 {@link ModelPart.Cube} 的展开规则决定。
 * 改任何一条 addBox 的尺寸都会让贴图错位 —— 必须同步改
 * {@code 小工具/dev_draw_barracuda_entity.py} 里的 BOXES 并重新生成贴图。
 * 机头朝 -Z（与 B25/F4F 相同），y 轴向下、机腹底部在 y=0。
 */
public class BarracudaModel<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "barracuda"), "main");

    private final ModelPart bb_main;

    public BarracudaModel(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("bb_main",
                CubeListBuilder.create()
                        // 机身：尾锥到机头等宽的长条，给座舱罩和散热器留出安装面
                        .texOffs(0, 0).addBox(-1.0F, -2.5F, -6.0F, 2.0F, 2.5F, 12.0F, new CubeDeformation(0.0F))
                        // 温室座舱罩：高出机身 0.5，贴图画框架分格
                        .texOffs(30, 0).addBox(-1.0F, -3.0F, -2.6F, 2.0F, 0.8F, 5.0F, new CubeDeformation(0.0F))
                        // 桨毂
                        .texOffs(45, 0).addBox(-0.5F, -2.0F, -6.8F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        // 机腹散热器（梭鱼标志性的下巴进气口）
                        .texOffs(45, 4).addBox(-1.0F, 0.0F, -5.8F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        // 机翼：肩部高置，0 厚度；中翼 + 左右外翼，外翼略后掠
                        .texOffs(0, 16).addBox(-2.5F, -2.3F, -3.5F, 5.0F, 0.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(24, 16).addBox(-7.5F, -2.3F, -3.3F, 5.0F, 0.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 16).addBox(2.5F, -2.3F, -3.3F, 5.0F, 0.0F, 6.0F, new CubeDeformation(0.0F))
                        // 高置水平尾翼：架在垂尾中部，而不是贴在机身上
                        .texOffs(0, 21).addBox(-3.0F, -4.9F, 4.4F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F))
                        // 垂尾：0 厚度，从机身上缘一直立到水平尾翼之上
                        .texOffs(55, 0).addBox(0.0F, -6.8F, 4.4F, 0.0F, 4.3F, 2.0F, new CubeDeformation(0.0F))
                        // 水平尾翼撑杆 ×2（原画的 V 形支撑简化为竖直薄片）
                        .texOffs(60, 40).addBox(-1.8F, -4.9F, 5.4F, 0.4F, 2.4F, 0.4F, new CubeDeformation(0.0F))
                        .texOffs(60, 40).addBox(1.4F, -4.9F, 5.4F, 0.4F, 2.4F, 0.4F, new CubeDeformation(0.0F))
                        // 发动机罩两侧排气管
                        .texOffs(60, 40).addBox(-1.15F, -1.6F, -4.6F, 0.3F, 0.6F, 1.4F, new CubeDeformation(0.0F))
                        .texOffs(60, 40).addBox(0.85F, -1.6F, -4.6F, 0.3F, 0.6F, 1.4F, new CubeDeformation(0.0F))
                        // 座舱后天线杆
                        .texOffs(60, 40).addBox(-0.1F, -3.4F, 3.4F, 0.2F, 0.9F, 0.2F, new CubeDeformation(0.0F)),
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
