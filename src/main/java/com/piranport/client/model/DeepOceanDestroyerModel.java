package com.piranport.client.model;

import com.piranport.PiranPort;
import com.piranport.npc.deepocean.DeepOceanDestroyerEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 深海驱逐 (No.901 敌驱逐舰) 实体模型.
 *
 * 本文件由 tools/gen_deepocean_model.py 从 tools/deepocean_model_spec.py 生成 ——
 * 请勿手改; 改几何/UV 请改 spec 后重新生成.
 *
 * 立绘依据: 立绘/No901_敌驱逐舰.png
 * 造型规范: docs/YSM舰娘模型美术风格规范.md
 *
 * ── 骨架 (单位 = MC 像素, root 在脚底, 局部 y 向上为负) ──
 *     头顶 -26.3 | 下巴 -21.0 | 颈肩 -19.4 | 胸 -19.4..-12.4
 *     胯  -12.4 | 腿 -8.4..0 | 脚底 0
 *   总高 26.3 / 头高 5.3 ≈ 4.96 头身 (规范 §3.1 推荐 5~6 头身).
 *   注意: 这是相对 root 的**局部**坐标; 渲染时的世界坐标整体 +24.
 *
 * ── 立绘识别锚点 → 三维 (规范 §10) ──
 *   银灰长发 (头顶/刘海/后发/两侧长束, 三级发束 §6.2)
 *   右眼粉色机械眼 + 左眼黑色护目镜 + 口鼻带齿面罩
 *   白皮肤 + 黑白色胸甲; 腿部六边形鳞甲束
 *   舰装核心 = 蓝灰「黑鱼」(鲸): 弧形身体 + 分叉尾鳍 + 机械炮舱 + 粉色电路炮管
 *   身后倾斜黑盾 + 粉色电路; 两侧炮舱组; 环绕的发光眼黑舱
 *
 * ── 视觉优先级 (规范 §2) ──
 *   头/脸/发 > 服装主色块 > 舰装轮廓 > 饰品 > 小机械.
 *   舰装不遮脸与躯干中心 (§12.4); 外轮廓不压过头部 (§8.1).
 *
 * UV 布局: 各 box 的 texOffs 由 spec 自动排布 (shelf packing), 贴图生成器
 * (tools/make_deepocean_destroyer_texture.py) 读同一份 spec 绘制.
 */
public class DeepOceanDestroyerModel extends EntityModel<DeepOceanDestroyerEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "deep_ocean_destroyer"), "main");

    private static final float DEG = Mth.PI / 180.0F;
    private static final float ARM_OUT = 4.0F * DEG;
    // 从 spec 的 pivot 直接取, 不手抄 —— 骨架一改这里自动跟着变.
    private static final float BODY_PIVOT_Y = -19.4FF;
    private static final float HIPS_PIVOT_Y = -12.4FF;

    public final ModelPart body = new ModelPart();
    public final ModelPart hips = new ModelPart();
    public final ModelPart head = new ModelPart();
    public final ModelPart hair = new ModelPart();
    public final ModelPart rightArm = new ModelPart();
    public final ModelPart leftArm = new ModelPart();
    public final ModelPart leftLeg = new ModelPart();
    public final ModelPart rightLeg = new ModelPart();
    public final ModelPart chestCore = new ModelPart();
    public final ModelPart faceGlow = new ModelPart();
    public final ModelPart muzzle = new ModelPart();
    public final ModelPart blackFish = new ModelPart();
    public final ModelPart fishMount = new ModelPart();
    public final ModelPart tailStem = new ModelPart();
    public final ModelPart shield = new ModelPart();
    public final ModelPart shieldCircuit = new ModelPart();
    public final ModelPart leftRigging = new ModelPart();
    public final ModelPart rightRigging = new ModelPart();
    public final ModelPart glowPods = new ModelPart();

    public DeepOceanDestroyerModel(ModelPart root) {
        ModelPart r = root.getChild("root");
        this.body = r.getChild("body");
        this.hips = r.getChild("hips");
        this.head = r.getChild("head");
        this.hair = r.getChild("hair");
        this.rightArm = r.getChild("rightArm");
        this.leftArm = r.getChild("leftArm");
        this.leftLeg = r.getChild("leftLeg");
        this.rightLeg = r.getChild("rightLeg");
        this.chestCore = r.getChild("chestCore");
        this.faceGlow = r.getChild("faceGlow");
        this.muzzle = r.getChild("muzzle");
        this.blackFish = r.getChild("blackFish");
        this.fishMount = r.getChild("fishMount");
        this.tailStem = r.getChild("tailStem");
        this.shield = r.getChild("shield");
        this.shieldCircuit = r.getChild("shieldCircuit");
        this.leftRigging = r.getChild("leftRigging");
        this.rightRigging = r.getChild("rightRigging");
        this.glowPods = r.getChild("glowPods");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root",
                CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.8F, 2.0F)
                        .texOffs(8, 0).addBox(-2.4F, 1.6F, -1.4F, 4.8F, 7.0F, 2.8F)
                , PartPose.offsetAndRotation(0.0F, -19.4F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("hips",
                CubeListBuilder.create()
                        .texOffs(24, 0).addBox(-2.2F, 0.0F, -1.3F, 4.4F, 4.0F, 2.6F)
                , PartPose.offsetAndRotation(0.0F, -12.4F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(38, 0).addBox(-2.4F, -4.7F, -2.1F, 4.8F, 4.7F, 4.2F)
                        .texOffs(56, 0).addBox(-1.9F, 0.0F, -1.9F, 3.8F, 0.8F, 1.8F)
                        .texOffs(68, 0).addBox(-1.7F, 0.8F, -1.6F, 3.4F, 1.2F, 3.0F)
                , PartPose.offsetAndRotation(0.0F, -21.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("hair",
                CubeListBuilder.create()
                        .texOffs(80, 0).addBox(-2.55F, -5.2F, -2.2F, 5.1F, 1.0F, 4.4F)
                        .texOffs(98, 0).addBox(-2.45F, -4.5F, -2.7F, 4.9F, 3.6F, 1.0F)
                        .texOffs(110, 0).addBox(-2.45F, -4.8F, 1.5F, 4.9F, 6.4F, 1.6F)
                        .texOffs(0, 10).addBox(2.4F, -4.8F, -1.7F, 1.7F, 7.2F, 3.4F)
                        .texOffs(10, 10).addBox(-4.1F, -4.8F, -1.7F, 1.7F, 7.2F, 3.4F)
                , PartPose.offsetAndRotation(0.0F, -21.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("rightArm",
                CubeListBuilder.create()
                        .texOffs(20, 10).addBox(0.0F, 0.0F, -1.0F, 1.9F, 3.4F, 2.0F)
                , PartPose.offsetAndRotation(2.9F, -19.0F, 0.0F, 0.0F, 0.0F, 0.0698F));
        root.addOrReplaceChild("leftArm",
                CubeListBuilder.create()
                        .texOffs(28, 10).addBox(-1.9F, 0.0F, -1.0F, 1.9F, 3.4F, 2.0F)
                        .texOffs(36, 10).addBox(-1.9F, 3.4F, -0.9F, 1.9F, 2.8F, 1.8F)
                , PartPose.offsetAndRotation(-2.9F, -19.0F, 0.0F, 0.0F, 0.0F, -0.0698F));
        root.addOrReplaceChild("leftLeg",
                CubeListBuilder.create()
                        .texOffs(44, 10).addBox(-1.05F, 0.0F, -1.05F, 2.1F, 3.6F, 2.1F)
                , PartPose.offsetAndRotation(-1.15F, -8.4F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("rightLeg",
                CubeListBuilder.create()
                        .texOffs(52, 10).addBox(-1.05F, 0.0F, -1.05F, 2.1F, 3.6F, 2.1F)
                        .texOffs(60, 10).addBox(-1.0F, 4.2F, -1.0F, 2.0F, 4.2F, 2.0F)
                , PartPose.offsetAndRotation(1.15F, -8.4F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("chestCore",
                CubeListBuilder.create()
                        .texOffs(68, 10).addBox(-0.9F, -0.9F, -1.6F, 1.8F, 1.8F, 0.8F)
                , PartPose.offsetAndRotation(0.0F, -14.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("faceGlow",
                CubeListBuilder.create()
                        .texOffs(74, 10).addBox(-2.6F, -4.0F, -2.6F, 2.6F, 2.6F, 1.0F)
                        .texOffs(82, 10).addBox(-2.0F, -3.4F, -3.1F, 1.4F, 1.4F, 0.6F)
                , PartPose.offsetAndRotation(0.0F, -21.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("muzzle",
                CubeListBuilder.create()
                        .texOffs(86, 10).addBox(-1.8F, -2.2F, -2.6F, 3.6F, 1.0F, 0.6F)
                , PartPose.offsetAndRotation(0.0F, -21.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("blackFish",
                CubeListBuilder.create()
                        .texOffs(96, 10).addBox(-1.5F, -2.6F, -1.1F, 3.0F, 5.2F, 2.2F)
                        .texOffs(106, 10).addBox(-2.6F, 1.6F, -1.1F, 3.0F, 2.0F, 2.2F)
                , PartPose.offsetAndRotation(-3.4F, -18.0F, 1.6F, 0.1047F, 0.3665F, -0.2967F));
        root.getChild("blackFish").addOrReplaceChild("fishMount",
                CubeListBuilder.create()
                        .texOffs(0, 20).addBox(-2.0F, 0.0F, -1.5F, 4.0F, 3.0F, 3.0F)
                        .texOffs(14, 20).addBox(-0.5F, 1.0F, -6.4F, 1.0F, 1.0F, 5.0F)
                , PartPose.offsetAndRotation(1.4F, 4.6F, -0.9F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("tailStem",
                CubeListBuilder.create()
                        .texOffs(26, 20).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F)
                        .texOffs(30, 20).addBox(-3.0F, -5.6F, -0.5F, 6.0F, 2.0F, 1.0F)
                , PartPose.offsetAndRotation(-4.4F, -22.4F, 2.4F, 0.0F, 0.0F, 0.3316F));
        root.addOrReplaceChild("shield",
                CubeListBuilder.create()
                        .texOffs(44, 20).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 3.5F, 1.0F)
                        .texOffs(62, 20).addBox(-4.0F, -0.5F, 0.0F, 3.0F, 7.0F, 1.0F)
                        .texOffs(70, 20).addBox(1.0F, -0.5F, 0.0F, 3.0F, 7.0F, 1.0F)
                        .texOffs(78, 20).addBox(-3.0F, 6.5F, 0.0F, 6.0F, 3.0F, 1.0F)
                        .texOffs(92, 20).addBox(-3.0F, -3.0F, -0.5F, 6.0F, 6.0F, 1.0F)
                , PartPose.offsetAndRotation(0.2F, -20.4F, 2.5F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("shieldCircuit",
                CubeListBuilder.create()
                        .texOffs(106, 20).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F)
                        .texOffs(110, 20).addBox(-3.0F, 1.0F, -0.5F, 6.0F, 1.0F, 1.0F)
                        .texOffs(0, 28).addBox(-3.5F, -1.0F, -0.5F, 7.0F, 1.0F, 1.0F)
                , PartPose.offsetAndRotation(0.2F, -20.4F, 2.32F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leftRigging",
                CubeListBuilder.create()
                        .texOffs(16, 28).addBox(-2.6F, -1.2F, -1.2F, 2.6F, 2.4F, 2.4F)
                        .texOffs(26, 28).addBox(-4.8F, -0.6F, -0.5F, 2.2F, 1.0F, 1.0F)
                        .texOffs(32, 28).addBox(0.0F, -1.5F, -1.1F, 2.8F, 3.0F, 2.2F)
                        .texOffs(42, 28).addBox(-2.6F, -1.6F, 0.5F, 2.6F, 1.2F, 3.0F)
                , PartPose.offsetAndRotation(-2.9F, -14.4F, 0.6F, 0.0F, 0.1745F, -0.0524F));
        root.addOrReplaceChild("rightRigging",
                CubeListBuilder.create()
                        .texOffs(54, 28).addBox(0.0F, -1.2F, -1.2F, 2.6F, 2.4F, 2.4F)
                        .texOffs(64, 28).addBox(2.6F, -0.6F, -0.5F, 2.2F, 1.0F, 1.0F)
                        .texOffs(70, 28).addBox(-2.8F, -1.5F, -1.1F, 2.8F, 3.0F, 2.2F)
                        .texOffs(80, 28).addBox(0.0F, -1.6F, 0.5F, 2.6F, 1.2F, 3.0F)
                , PartPose.offsetAndRotation(2.9F, -14.4F, 0.6F, 0.0F, -0.1745F, 0.0524F));
        root.addOrReplaceChild("glowPods",
                CubeListBuilder.create()
                        .texOffs(92, 28).addBox(-3.1F, -1.6F, -1.0F, 1.8F, 2.0F, 1.0F)
                        .texOffs(98, 28).addBox(1.3F, -1.3F, -1.0F, 1.8F, 2.0F, 1.0F)
                        .texOffs(104, 28).addBox(-2.6F, 1.4F, -1.0F, 1.8F, 2.0F, 1.0F)
                        .texOffs(110, 28).addBox(0.8F, 1.6F, -1.0F, 1.8F, 2.0F, 1.0F)
                        .texOffs(116, 28).addBox(-3.1F, -1.1F, -1.6F, 1.8F, 1.0F, 1.0F)
                        .texOffs(122, 28).addBox(1.3F, -0.8F, -1.6F, 1.8F, 1.0F, 1.0F)
                        .texOffs(0, 33).addBox(-2.6F, 1.9F, -1.6F, 1.8F, 1.0F, 1.0F)
                        .texOffs(6, 33).addBox(0.8F, 2.1F, -1.6F, 1.8F, 1.0F, 1.0F)
                , PartPose.offsetAndRotation(0.0F, -13.0F, 1.4F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(DeepOceanDestroyerEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float headYaw = netHeadYaw * DEG * 0.42F;
        float headPitch = headPitch * DEG * 0.34F;
        // 头部与头发同转 (头发是头的子件, 直接叠同样的偏转)
        applyHead(head, headYaw, headPitch);
        applyHead(hair, headYaw, headPitch);

        // 呼吸: 胸腔微小起伏, 不让全身伸缩 (规范 §11.2)
        float breath = Mth.sin(ageInTicks * 0.055F) * 0.012F;
        body.y = BODY_PIVOT_Y + breath;
        hips.y = HIPS_PIVOT_Y + breath * 0.5F;

        // 行走: 手臂反相摆动, 幅度克制 (规范 §11.3)
        float walk = Math.min(limbSwingAmount, 1.0F);
        leftArm.xRot = Mth.cos(limbSwing * 0.62F + Mth.PI) * 0.34F * walk;
        rightArm.xRot = Mth.cos(limbSwing * 0.62F) * 0.34F * walk;
        leftArm.zRot = -ARM_OUT + Mth.sin(ageInTicks * 0.04F) * 0.03F;
        rightArm.zRot = ARM_OUT - Mth.sin(ageInTicks * 0.04F) * 0.03F;

        // 双腿反相, 膝盖有重心转移感
        leftLeg.xRot = Mth.cos(limbSwing * 0.62F) * 0.42F * walk;
        rightLeg.xRot = Mth.cos(limbSwing * 0.62F + Mth.PI) * 0.42F * walk;

        // 舰装: 更迟缓、幅度更小 (规范 §11.1 第 4 层)
        float rigSway = Mth.sin(ageInTicks * 0.035F) * 0.035F;
        leftRigging.zRot = -3.0F * DEG + rigSway;
        rightRigging.zRot = 3.0F * DEG - rigSway;
        blackFish.zRot = -17.0F * DEG + Mth.sin(ageInTicks * 0.048F) * 0.045F;
        blackFish.yRot = 21.0F * DEG + Mth.sin(ageInTicks * 0.041F) * 0.05F;
        tailStem.zRot = 19.0F * DEG + Mth.sin(ageInTicks * 0.075F) * 0.10F;

        // 盾: 极轻微回弹
        float shieldSway = Mth.sin(ageInTicks * 0.03F) * 0.02F;
        shield.zRot = shieldSway;
        shieldCircuit.zRot = shieldSway;

        // 发光件: 呼吸式脉动 (只改大小不改位置)
        float pulse = 1.0F + Mth.sin(ageInTicks * 0.09F) * 0.06F;
        chestCore.xScale = chestCore.yScale = chestCore.zScale = pulse;
    }

    private static void applyHead(ModelPart p, float yaw, float pitch) {
        p.yRot = yaw;
        p.xRot = pitch;
        p.zRot = 0.0F;
    }
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               int packedLight, int packedOverlay, int color) {
        // 按视觉优先级从后往前画: 舰装 → 人物 → 发光件
        for (ModelPart p : new ModelPart[]{shield, shieldCircuit, glowPods,
                leftRigging, rightRigging, tailStem, blackFish,
                leftLeg, rightLeg, hips, body, leftArm, rightArm, hair, head,
                chestCore, faceGlow}) {
            p.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
    }
}
