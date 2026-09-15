package com.piranport.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
import com.piranport.entity.TorpedoEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * 鱼雷实体模型（发射后航行中的外观），替代原先的物品图标渲染。
 *
 * 轴向约定：雷头朝 -Z，雷尾螺旋桨朝 +Z，原点在雷体正中，
 * 长度 20px（1.25 格）、直径 4px（0.25 格），与实体碰撞箱 0.5x0.25 相称。
 * 朝向换算见 {@link com.piranport.client.TorpedoRenderer}。
 *
 * 贴图 UV 布局由 tools/make_torpedo_texture.py 的 BOXES 表定义，
 * 两边的 texOffs 必须保持一致；改模型时先改脚本再跑一次生成贴图。
 */
public class TorpedoModel extends EntityModel<TorpedoEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "torpedo"), "main");

    /** 螺旋桨每 tick 转过的弧度（约 1 圈 / 16 tick，转太快会糊成一片） */
    private static final float PROPELLER_SPIN_PER_TICK = 0.4f;

    private final ModelPart body;
    private final ModelPart propeller;

    public TorpedoModel(ModelPart root) {
        this.body = root.getChild("body");
        this.propeller = this.body.getChild("propeller");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                // 雷头锥：三段递减的盒子逼近圆锥，配色为铜色战斗部
                .texOffs(20, 11).addBox(-1.0f, -1.0f, -10.0f, 2.0f, 2.0f, 1.0f)
                .texOffs(11, 11).addBox(-1.5f, -1.5f, -9.0f, 3.0f, 3.0f, 1.0f)
                .texOffs(0, 11).addBox(-2.0f, -2.0f, -8.0f, 4.0f, 4.0f, 1.0f)
                // 战斗部 / 中段 / 后段
                .texOffs(21, 0).addBox(-2.0f, -2.0f, -7.0f, 4.0f, 4.0f, 4.0f)
                .texOffs(0, 0).addBox(-2.0f, -2.0f, -3.0f, 4.0f, 4.0f, 6.0f)
                .texOffs(38, 0).addBox(-2.0f, -2.0f, 3.0f, 4.0f, 4.0f, 4.0f)
                // 尾锥
                .texOffs(27, 11).addBox(-1.5f, -1.5f, 7.0f, 3.0f, 3.0f, 2.0f)
                .texOffs(38, 11).addBox(-1.0f, -1.0f, 9.0f, 2.0f, 2.0f, 1.0f)
                // 四片尾鳍，上下共用一段 UV、左右共用一段
                .texOffs(45, 11).addBox(-0.5f, 1.5f, 5.0f, 1.0f, 2.0f, 3.0f)
                .texOffs(45, 11).addBox(-0.5f, -3.5f, 5.0f, 1.0f, 2.0f, 3.0f)
                .texOffs(54, 11).addBox(-3.5f, -0.5f, 5.0f, 2.0f, 1.0f, 3.0f)
                .texOffs(54, 11).addBox(1.5f, -0.5f, 5.0f, 2.0f, 1.0f, 3.0f),
                PartPose.ZERO);

        // 螺旋桨：独立 part，绕自身 Z 轴自转；pivot 落在雷尾末端，盒子坐标相对它给出
        body.addOrReplaceChild("propeller", CubeListBuilder.create()
                .texOffs(0, 17).addBox(-0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 1.0f)
                .texOffs(5, 17).addBox(-2.5f, -0.5f, -0.5f, 5.0f, 1.0f, 1.0f)
                .texOffs(18, 17).addBox(-0.5f, -2.5f, -0.5f, 1.0f, 5.0f, 1.0f),
                PartPose.offset(0.0f, 0.0f, 10.5f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(TorpedoEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.propeller.zRot = ageInTicks * PROPELLER_SPIN_PER_TICK;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer,
                               int packedLight, int packedOverlay, int color) {
        this.body.render(poseStack, consumer, packedLight, packedOverlay, color);
    }
}
