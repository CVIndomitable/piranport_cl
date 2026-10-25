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
 * 流星（横须贺 D4Y）鱼雷机实体模型。
 *
 * <p>与 B25/F4F 一致的低多边形 Java 模型：机身、座舱罩、桨毂、滑油散热器用实心盒，
 * 机翼、水平尾翼、垂尾用 0 厚度平面。造型特征是低置机翼、很长的温室座舱罩、
 * 机腹滑油散热器和四叶螺旋桨（0 厚度圆盘，贴图用透明像素切出十字桨叶）。
 *
 * <p>贴图 64×64，UV 由 texOffs + box 尺寸按 {@link ModelPart.Cube} 的展开规则决定。
 * 改任何一条 addBox 的尺寸都会让贴图错位 —— 必须同步改
 * {@code 小工具/dev_draw_ryusei_entity.py} 里的 BOXES 并重新生成贴图。
 * 机头朝 -Z（与 B25/F4F 相同），y 轴向下、机腹底部在 y=0。
 */
public class RyuseiModel<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "ryusei"), "main");

    private final ModelPart bb_main;

    public RyuseiModel(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("bb_main",
                CubeListBuilder.create()
                        // 机身
                        .texOffs(0, 0).addBox(-1.0F, -2.5F, -6.0F, 2.0F, 2.5F, 12.0F, new CubeDeformation(0.0F))
                        // 长条温室座舱罩：覆盖机翼上方直到后座
                        .texOffs(30, 0).addBox(-1.0F, -3.0F, -3.0F, 2.0F, 0.8F, 6.0F, new CubeDeformation(0.0F))
                        // 桨毂
                        .texOffs(48, 0).addBox(-0.5F, -2.0F, -6.8F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        // 四叶螺旋桨：0 厚度圆盘，贴图十字桨叶、四角透明（entityCutout）
                        .texOffs(48, 8).addBox(-1.5F, -3.0F, -6.7F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        // 机腹滑油散热器
                        .texOffs(56, 0).addBox(-0.5F, 0.0F, -5.2F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        // 机翼：低置，0 厚度；中翼 + 左右外翼
                        .texOffs(0, 16).addBox(-2.5F, -1.0F, -2.2F, 5.0F, 0.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(22, 16).addBox(-6.5F, -1.0F, -2.0F, 4.0F, 0.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(42, 16).addBox(2.5F, -1.0F, -2.0F, 4.0F, 0.0F, 5.0F, new CubeDeformation(0.0F))
                        // 水平尾翼：贴在机身中低位置，不是梭鱼那种高置
                        .texOffs(0, 22).addBox(-2.5F, -1.4F, 4.2F, 5.0F, 0.0F, 2.0F, new CubeDeformation(0.0F))
                        // 垂尾
                        .texOffs(55, 6).addBox(0.0F, -5.8F, 4.5F, 0.0F, 3.3F, 2.0F, new CubeDeformation(0.0F))
                        // 尾轮
                        .texOffs(60, 40).addBox(-0.1F, 0.0F, 5.2F, 0.2F, 0.5F, 0.5F, new CubeDeformation(0.0F))
                        // 座舱后天线杆
                        .texOffs(60, 40).addBox(-0.05F, -3.4F, 3.2F, 0.1F, 0.9F, 0.1F, new CubeDeformation(0.0F)),
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
