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
 * 天河（双发陆攻）实体模型。
 *
 * <p>比单发机大一圈：机身长 14 格，翼展 18 格，两台发动机短舱各带一副
 * 四叶螺旋桨（0 厚度圆盘，贴图透明像素切出桨叶），机头是玻璃领航舱。
 *
 * <p>机翼和水平尾翼用 0.1 格厚度而不是纯平面：机背深绿、机腹浅灰蓝是
 * 两套颜色，0 厚度会让上下两个面共面闪烁（z-fighting）。
 *
 * <p>贴图 64×64，UV 由 texOffs + box 尺寸按 {@link ModelPart.Cube} 的展开规则决定。
 * 改任何一条 addBox 的尺寸都会让贴图错位 —— 必须同步改
 * {@code 小工具/dev_draw_tenga_entity.py} 里的 BOXES 并重新生成贴图。
 * 机头朝 -Z（与 B25/F4F 相同），y 轴向下、机腹底部在 y=0。
 */
public class TengaModel<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "tenga"), "main");

    private final ModelPart bb_main;

    public TengaModel(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("bb_main",
                CubeListBuilder.create()
                        // 机身
                        .texOffs(0, 0).addBox(-1.0F, -2.6F, -7.0F, 2.0F, 2.6F, 14.0F, new CubeDeformation(0.0F))
                        // 玻璃机鼻
                        .texOffs(48, 0).addBox(-1.0F, -2.2F, -7.8F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        // 长条座舱罩
                        .texOffs(32, 0).addBox(-1.0F, -3.3F, -6.4F, 2.0F, 0.7F, 5.0F, new CubeDeformation(0.0F))
                        // 左右发动机短舱
                        .texOffs(32, 8).addBox(-3.5F, -2.2F, -4.2F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 8).addBox(1.5F, -2.2F, -4.2F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                        // 左右四叶螺旋桨（0 厚度圆盘，透明角）
                        .texOffs(44, 8).addBox(-4.0F, -2.7F, -4.4F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(50, 8).addBox(1.0F, -2.7F, -4.4F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        // 左右桨毂
                        .texOffs(56, 8).addBox(-3.0F, -1.7F, -4.6F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 8).addBox(2.0F, -1.7F, -4.6F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        // 机翼：0.1 厚度，机背深绿/机腹浅灰蓝
                        .texOffs(0, 17).addBox(-3.0F, -1.2F, -3.6F, 6.0F, 0.1F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(26, 17).addBox(-9.0F, -1.2F, -3.4F, 6.0F, 0.1F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(26, 26).addBox(3.0F, -1.2F, -3.4F, 6.0F, 0.1F, 6.0F, new CubeDeformation(0.0F))
                        // 水平尾翼
                        .texOffs(0, 24).addBox(-3.0F, -1.5F, 5.2F, 6.0F, 0.1F, 2.0F, new CubeDeformation(0.0F))
                        // 垂尾
                        .texOffs(57, 0).addBox(0.0F, -5.6F, 5.2F, 0.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        // 尾轮
                        .texOffs(60, 40).addBox(-0.1F, 0.0F, 6.0F, 0.2F, 0.4F, 0.4F, new CubeDeformation(0.0F)),
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
