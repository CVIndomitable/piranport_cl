package com.piranport.client.model;

import com.piranport.PiranPort;
import com.piranport.npc.shipgirl.ShipGirlEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 独角兽舰娘实体模型。
 *
 * <p>以立绘的三个识别层级重做：银蓝长发、花环礼服、竖琴舰装。
 * 细节使用体块、薄片和阶梯式发束表达，保持 YSM 参考风格而不依赖通用舰装层。</p>
 */
public class UnicornModel extends PlayerModel<ShipGirlEntity> {
    public static final int SKIN_ID = 18;
    private static final float SKIN_UV_SCALE = 2.0F;

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "unicorn"), "main");
    public static final ResourceLocation TEXTURE_LOCATION =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/shipgirl/unicorn.png");

    private final ModelPart hairMotion;
    private final ModelPart leftHair;
    private final ModelPart centerHair;
    private final ModelPart rightHair;
    private final ModelPart frontDress;
    private final ModelPart leftDress;
    private final ModelPart rightDress;
    private final ModelPart backDress;
    private final ModelPart leftRibbon;
    private final ModelPart rightRibbon;
    private final ModelPart harp;
    private final ModelPart leftBird;
    private final ModelPart rightBird;
    private final ModelPart leftBirdWings;
    private final ModelPart rightBirdWings;
    private final ModelPart leftForearm;
    private final ModelPart rightForearm;

    public UnicornModel(ModelPart root) {
        super(root, true);
        this.hairMotion = this.head.getChild("hair_motion");
        this.leftHair = this.hairMotion.getChild("left_hair");
        this.centerHair = this.hairMotion.getChild("center_hair");
        this.rightHair = this.hairMotion.getChild("right_hair");
        this.frontDress = this.body.getChild("front_dress");
        this.leftDress = this.body.getChild("left_dress");
        this.rightDress = this.body.getChild("right_dress");
        this.backDress = this.body.getChild("back_dress");
        this.leftRibbon = this.body.getChild("left_ribbon");
        this.rightRibbon = this.body.getChild("right_ribbon");
        this.harp = this.body.getChild("harp");
        this.leftBird = this.body.getChild("left_bird");
        this.rightBird = this.body.getChild("right_bird");
        this.leftBirdWings = this.leftBird.getChild("wings");
        this.rightBirdWings = this.rightBird.getChild("wings");
        this.leftForearm = this.leftArm.getChild("forearm");
        this.rightForearm = this.rightArm.getChild("forearm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                skin(0, 0).addBox(-3.25F, -7.2F, -3.28F, 6.5F, 6.9F, 6.55F,
                        new CubeDeformation(-0.32F), SKIN_UV_SCALE, SKIN_UV_SCALE),
                PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body",
                swatch(128, 32)
                        .addBox(-2.35F, 0.5F, -1.05F, 4.7F, 9.0F, 2.1F, CubeDeformation.NONE)
                        .texOffs(160, 32).addBox(-2.0F, 0.2F, -1.28F, 4.0F, 0.85F, 0.3F,
                                CubeDeformation.NONE)
                        .texOffs(192, 32).addBox(-1.12F, 1.0F, -1.55F, 2.24F, 6.9F, 0.28F,
                                CubeDeformation.NONE),
                PartPose.ZERO);

        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(),
                PartPose.offset(-3.15F, 2.3F, 0.0F));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(),
                PartPose.offset(3.15F, 2.3F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
                swatch(224, 0).addBox(-0.9F, 0.0F, -0.96F, 1.8F, 9.2F, 1.92F, CubeDeformation.NONE),
                PartPose.offset(-1.18F, 12.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
                swatch(224, 0).addBox(-0.9F, 0.0F, -0.96F, 1.8F, 9.2F, 1.92F, CubeDeformation.NONE),
                PartPose.offset(1.18F, 12.0F, 0.0F));

        root.addOrReplaceChild("right_sleeve", CubeListBuilder.create(), PartPose.offset(-3.15F, 2.3F, 0.0F));
        root.addOrReplaceChild("left_sleeve", CubeListBuilder.create(), PartPose.offset(3.15F, 2.3F, 0.0F));
        root.addOrReplaceChild("right_pants", CubeListBuilder.create(), PartPose.offset(-1.18F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_pants", CubeListBuilder.create(), PartPose.offset(1.18F, 12.0F, 0.0F));
        root.addOrReplaceChild("jacket", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("ear", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("cloak", CubeListBuilder.create(), PartPose.ZERO);

        addHeadAndHair(head, body);
        addDress(body);
        addArms(rightArm, leftArm);
        addLegs(rightLeg, leftLeg);
        addHarp(body);
        addBirds(body);

        return LayerDefinition.create(mesh, 256, 256);
    }

    private static void addHeadAndHair(PartDefinition head, PartDefinition body) {
        PartDefinition cap = head.addOrReplaceChild("hair_cap", swatch(128, 0), PartPose.ZERO);
        cap.addOrReplaceChild("top_mass",
                swatch(128, 0).addBox(-3.35F, -7.75F, -3.0F, 6.7F, 1.35F, 6.4F, CubeDeformation.NONE),
                PartPose.ZERO);
        cap.addOrReplaceChild("back_mass",
                swatch(160, 0).addBox(-3.35F, -7.45F, 2.45F, 6.7F, 7.0F, 0.82F, CubeDeformation.NONE),
                PartPose.ZERO);
        cap.addOrReplaceChild("left_mass",
                swatch(192, 0).addBox(-3.5F, -7.4F, -2.35F, 0.68F, 7.0F, 4.95F, CubeDeformation.NONE),
                PartPose.ZERO);
        cap.addOrReplaceChild("right_mass",
                swatch(192, 0).addBox(2.82F, -7.4F, -2.35F, 0.68F, 7.0F, 4.95F, CubeDeformation.NONE),
                PartPose.ZERO);
        cap.addOrReplaceChild("forehead_lock",
                swatch(160, 0)
                        .addBox(-3.02F, -7.58F, -3.58F, 6.04F, 0.92F, 0.34F, CubeDeformation.NONE)
                        .addBox(-3.45F, -6.78F, -3.5F, 0.46F, 4.0F, 0.34F, CubeDeformation.NONE)
                        .addBox(2.99F, -6.78F, -3.5F, 0.46F, 4.0F, 0.34F, CubeDeformation.NONE),
                PartPose.ZERO);

        PartDefinition bangs = head.addOrReplaceChild("bangs", CubeListBuilder.create(), PartPose.ZERO);
        float[] bangX = {-2.65F, -1.78F, -0.88F, 0.02F, 0.92F, 1.8F, 2.68F};
        float[] bangLength = {2.1F, 2.85F, 3.55F, 3.9F, 3.45F, 2.75F, 2.05F};
        float[] bangTilt = {0.22F, 0.13F, 0.06F, 0.0F, -0.06F, -0.13F, -0.22F};
        for (int i = 0; i < bangX.length; i++) {
            bangs.addOrReplaceChild("bang_" + i,
                    swatch(i % 2 == 0 ? 160 : 128)
                            .addBox(-0.38F, 0.0F, -0.2F, 0.76F, bangLength[i], 0.44F, CubeDeformation.NONE),
                    PartPose.offsetAndRotation(bangX[i], -6.3F, -3.72F, -0.045F, 0.0F, bangTilt[i]));
        }

        PartDefinition sideLocks = head.addOrReplaceChild("side_locks", CubeListBuilder.create(), PartPose.ZERO);
        addHairSegment(sideLocks, "left_lock_upper", -3.48F, -4.65F, -2.9F, 1.0F, 5.2F, 0.76F, 0.1F, 160);
        addHairSegment(sideLocks, "left_lock_lower", -3.35F, 0.15F, -2.2F, 0.96F, 5.8F, 0.78F, 0.22F, 128);
        addHairSegment(sideLocks, "right_lock_upper", 3.48F, -4.65F, -2.9F, 1.0F, 4.9F, 0.76F, -0.1F, 160);
        addHairSegment(sideLocks, "right_lock_lower", 3.35F, 0.15F, -2.2F, 0.9F, 5.2F, 0.74F, -0.2F, 128);

        PartDefinition wreath = head.addOrReplaceChild("flower_wreath",
                swatch(128, 96)
                        .addBox(-3.15F, -8.05F, -3.0F, 6.3F, 0.26F, 0.34F, CubeDeformation.NONE)
                        .addBox(-2.98F, -8.25F, -2.82F, 0.25F, 0.75F, 0.25F, CubeDeformation.NONE)
                        .addBox(2.73F, -8.25F, -2.82F, 0.25F, 0.75F, 0.25F, CubeDeformation.NONE),
                PartPose.rotation(-0.06F, 0.0F, 0.0F));
        addFlower(wreath, "flower_left", -2.5F, -8.12F, -3.18F, 128, 64, 0.82F);
        addFlower(wreath, "flower_left_small", -1.2F, -8.27F, -3.2F, 224, 64, 0.62F);
        addFlower(wreath, "flower_center", 0.05F, -8.35F, -3.22F, 192, 64, 0.74F);
        addFlower(wreath, "flower_right_small", 1.3F, -8.25F, -3.19F, 160, 64, 0.62F);
        addFlower(wreath, "flower_right", 2.52F, -8.1F, -3.16F, 128, 64, 0.82F);

        PartDefinition hairMotion = head.addOrReplaceChild("hair_motion", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition leftHair = hairMotion.addOrReplaceChild("left_hair", CubeListBuilder.create(), PartPose.rotation(0.04F, -0.02F, 0.0F));
        PartDefinition centerHair = hairMotion.addOrReplaceChild("center_hair", CubeListBuilder.create(), PartPose.rotation(0.055F, 0.0F, 0.0F));
        PartDefinition rightHair = hairMotion.addOrReplaceChild("right_hair", CubeListBuilder.create(), PartPose.rotation(0.04F, 0.02F, 0.0F));

        addHairSegment(leftHair, "far", -3.15F, 0.0F, -0.82F, 1.3F, 9.2F, 0.96F, 0.16F, 192);
        addHairSegment(leftHair, "outer", -2.18F, 0.25F, -0.94F, 1.55F, 11.0F, 1.08F, 0.1F, 128);
        addHairSegment(leftHair, "inner", -1.12F, 0.35F, -1.02F, 1.24F, 10.4F, 0.9F, 0.04F, 160);
        addHairSegment(centerHair, "left", -0.55F, 0.55F, -0.75F, 0.92F, 6.6F, 0.72F, 0.02F, 128);
        addHairSegment(centerHair, "center", 0.35F, 0.3F, -0.7F, 0.96F, 7.0F, 0.76F, -0.02F, 160);
        addHairSegment(rightHair, "inner", 1.3F, 0.35F, -0.72F, 1.08F, 7.1F, 0.82F, -0.05F, 160);
        addHairSegment(rightHair, "outer", 2.28F, 0.25F, -0.68F, 1.32F, 8.2F, 0.94F, -0.12F, 128);

        PartDefinition backHair = body.addOrReplaceChild("back_hair", CubeListBuilder.create(),
                PartPose.offset(0.0F, -0.4F, 1.55F));
        addBackHairPanel(backHair, "back_left_outer", -3.15F, 0.05F, 0.0F, 1.25F, 13.4F, 0.16F, 192);
        addBackHairPanel(backHair, "back_left_inner", -2.0F, 0.0F, 0.16F, 1.4F, 16.0F, 0.09F, 128);
        addBackHairPanel(backHair, "back_center", -0.55F, 0.0F, 0.24F, 1.65F, 17.2F, 0.0F, 160);
        addBackHairPanel(backHair, "back_right_inner", 1.05F, 0.0F, 0.16F, 1.4F, 16.0F, -0.09F, 128);
        addBackHairPanel(backHair, "back_right_outer", 2.35F, 0.05F, 0.0F, 1.25F, 13.4F, -0.16F, 192);

        PartDefinition leftSideFlow = body.addOrReplaceChild("left_side_flow", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-3.55F, -0.2F, -1.05F, 0.035F, 0.0F, 0.08F));
        addHairSegment(leftSideFlow, "upper", 0.0F, 0.0F, 0.0F, 1.3F, 8.2F, 1.0F, 0.12F, 192);
        addHairSegment(leftSideFlow, "lower", -0.45F, 8.0F, 0.1F, 1.55F, 9.2F, 1.12F, 0.22F, 128);
        addHairSegment(leftSideFlow, "tip", -0.88F, 16.9F, 0.16F, 1.1F, 6.5F, 0.92F, 0.32F, 160);
        PartDefinition rightSideFlow = body.addOrReplaceChild("right_side_flow", CubeListBuilder.create(),
                PartPose.offsetAndRotation(3.45F, -0.2F, -1.0F, 0.035F, 0.0F, -0.08F));
        addHairSegment(rightSideFlow, "upper", 0.0F, 0.0F, 0.0F, 1.25F, 7.5F, 0.98F, -0.1F, 192);
        addHairSegment(rightSideFlow, "lower", 0.42F, 7.3F, 0.1F, 1.35F, 8.2F, 1.02F, -0.2F, 128);
    }

    private static void addDress(PartDefinition body) {
        PartDefinition bodice = body.addOrReplaceChild("bodice", CubeListBuilder.create(), PartPose.ZERO);
        bodice.addOrReplaceChild("front_white",
                swatch(128, 32).addBox(-2.2F, 0.65F, -1.4F, 4.4F, 7.05F, 0.36F, CubeDeformation.NONE),
                PartPose.ZERO);
        bodice.addOrReplaceChild("blue_inlay",
                swatch(160, 32).addBox(-1.12F, 0.45F, -1.62F, 2.24F, 6.9F, 0.28F, CubeDeformation.NONE),
                PartPose.ZERO);
        bodice.addOrReplaceChild("collar",
                swatch(192, 32).addBox(-2.0F, 0.25F, -1.72F, 4.0F, 1.0F, 0.28F, CubeDeformation.NONE),
                PartPose.ZERO);
        bodice.addOrReplaceChild("waist_band",
                swatch(160, 96).addBox(-2.65F, 7.6F, -1.55F, 5.3F, 0.58F, 3.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        for (int i = 0; i < 4; i++) {
            bodice.addOrReplaceChild("button_" + i,
                    swatch(192, 64).addBox(-0.16F, -0.16F, -0.13F, 0.32F, 0.32F, 0.26F, CubeDeformation.NONE),
                    PartPose.offset(0.0F, 2.4F + i * 1.22F, -1.98F));
        }
        addFlower(bodice, "waist_flower_left", -1.55F, 8.05F, -1.8F, 128, 64, 0.46F);
        addFlower(bodice, "waist_flower_center", 0.0F, 8.1F, -1.82F, 192, 64, 0.52F);
        addFlower(bodice, "waist_flower_right", 1.55F, 8.05F, -1.8F, 224, 64, 0.46F);

        PartDefinition frontDress = body.addOrReplaceChild("front_dress", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 8.05F, -1.55F, -0.04F, 0.0F, 0.0F));
        frontDress.addOrReplaceChild("center_panel",
                swatch(128, 32)
                        .addBox(-1.85F, 0.0F, -0.25F, 3.7F, 4.8F, 0.5F, CubeDeformation.NONE)
                        .texOffs(160, 32).addBox(-2.65F, 4.6F, -0.3F, 5.3F, 2.45F, 0.62F, CubeDeformation.NONE)
                        .texOffs(128, 32).addBox(-3.55F, 6.75F, -0.34F, 7.1F, 1.2F, 0.7F, CubeDeformation.NONE)
                        .texOffs(192, 32).addBox(-2.35F, 4.25F, -0.42F, 4.7F, 0.34F, 0.66F)
                        .texOffs(192, 32).addBox(-3.7F, 7.05F, -0.46F, 7.4F, 0.56F, 0.82F),
                PartPose.ZERO);
        int[] hemU = {128, 192, 224, 160};
        float[] hemX = {-2.45F, -0.85F, 0.85F, 2.45F};
        for (int i = 0; i < hemX.length; i++) {
            addFlower(frontDress, "hem_flower_" + i, hemX[i],
                    6.95F + (i % 2) * 0.12F, -0.78F, hemU[i],
                    64, 0.5F + (i % 2) * 0.05F);
        }

        PartDefinition leftDress = body.addOrReplaceChild("left_dress", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-2.18F, 8.0F, -0.05F, 0.0F, -0.06F, 0.16F));
        leftDress.addOrReplaceChild("panel",
                swatch(160, 32)
                        .addBox(-2.25F, 0.0F, -1.48F, 3.05F, 6.2F, 0.46F, CubeDeformation.NONE)
                        .texOffs(160, 32).addBox(-3.35F, 6.2F, -1.34F, 4.35F, 1.95F, 0.6F, CubeDeformation.NONE)
                        .texOffs(128, 32).addBox(-4.0F, 8.1F, -1.16F, 5.25F, 1.0F, 0.72F, CubeDeformation.NONE)
                        .texOffs(192, 32).addBox(-3.55F, 8.35F, -1.62F, 4.8F, 0.58F, 0.78F),
                PartPose.ZERO);
        addFlower(leftDress, "flower_upper", -2.35F, 2.2F, -1.82F, 192, 64, 0.54F);
        addFlower(leftDress, "flower_lower", -2.95F, 7.65F, -1.92F, 128, 64, 0.7F);

        PartDefinition rightDress = body.addOrReplaceChild("right_dress", CubeListBuilder.create(),
                PartPose.offsetAndRotation(2.18F, 8.0F, -0.05F, 0.0F, 0.06F, -0.16F));
        rightDress.addOrReplaceChild("panel",
                swatch(160, 32)
                        .addBox(-0.8F, 0.0F, -1.48F, 3.05F, 6.0F, 0.46F, CubeDeformation.NONE)
                        .texOffs(160, 32).addBox(-0.95F, 6.0F, -1.34F, 4.0F, 1.75F, 0.6F, CubeDeformation.NONE)
                        .texOffs(128, 32).addBox(-0.95F, 7.75F, -1.16F, 4.9F, 0.95F, 0.72F, CubeDeformation.NONE)
                        .texOffs(192, 32).addBox(-0.7F, 8.0F, -1.62F, 4.7F, 0.58F, 0.78F),
                PartPose.ZERO);
        addFlower(rightDress, "flower_upper", 2.35F, 2.2F, -1.82F, 224, 64, 0.54F);
        addFlower(rightDress, "flower_lower", 2.9F, 7.25F, -1.92F, 160, 64, 0.7F);

        PartDefinition backDress = body.addOrReplaceChild("back_dress", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 8.1F, 1.48F, 0.075F, 0.0F, 0.0F));
        backDress.addOrReplaceChild("panel",
                swatch(160, 32)
                        .addBox(-2.9F, 0.0F, -0.18F, 5.8F, 6.7F, 0.42F, CubeDeformation.NONE)
                        .texOffs(192, 32).addBox(-3.35F, 6.3F, -0.3F, 6.7F, 0.68F, 0.66F),
                PartPose.ZERO);
        backDress.addOrReplaceChild("lace",
                swatch(128, 32).addBox(-3.35F, 6.65F, -1.48F, 6.7F, 0.62F, 2.9F, CubeDeformation.NONE),
                PartPose.ZERO);

        PartDefinition leftRibbon = body.addOrReplaceChild("left_ribbon", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-3.15F, 8.25F, 1.2F, 0.08F, 0.0F, 0.2F));
        addRibbon(leftRibbon, "upper", 0.0F, 0.0F, 0.0F, 0.72F, 7.4F, 160, 32, -0.08F);
        addRibbon(leftRibbon, "tip", -0.28F, 6.65F, 0.0F, 0.54F, 5.1F, 192, 32, 0.14F);
        PartDefinition rightRibbon = body.addOrReplaceChild("right_ribbon", CubeListBuilder.create(),
                PartPose.offsetAndRotation(3.15F, 8.25F, 1.2F, 0.08F, 0.0F, -0.2F));
        addRibbon(rightRibbon, "upper", 0.0F, 0.0F, 0.0F, 0.72F, 7.4F, 160, 32, 0.08F);
        addRibbon(rightRibbon, "tip", 0.28F, 6.65F, 0.0F, 0.54F, 5.1F, 192, 32, -0.14F);
    }

    private static void addArms(PartDefinition rightArm, PartDefinition leftArm) {
        addArm(rightArm, true);
        addArm(leftArm, false);
    }

    private static void addArm(PartDefinition arm, boolean right) {
        arm.addOrReplaceChild("upper_arm",
                swatch(224, 0)
                        .addBox(-0.68F, -1.1F, -0.82F, 1.36F, 4.7F, 1.64F, CubeDeformation.NONE)
                        .texOffs(128, 32).addBox(-0.82F, -1.3F, -0.96F, 1.64F, 1.12F, 1.92F,
                                CubeDeformation.NONE),
                PartPose.ZERO);
        PartDefinition forearm = arm.addOrReplaceChild("forearm",
                swatch(224, 0)
                        .addBox(-0.64F, 0.0F, -0.78F, 1.28F, 3.8F, 1.56F, CubeDeformation.NONE)
                        .texOffs(128, 32).addBox(-0.78F, 0.48F, -0.92F, 1.56F, 3.1F, 1.84F,
                                CubeDeformation.NONE)
                        .texOffs(192, 32).addBox(-0.9F, 0.08F, -1.04F, 1.8F, 0.6F, 2.08F,
                                CubeDeformation.NONE),
                PartPose.offset(0.0F, 3.55F, 0.0F));
        addFlower(forearm, "cuff_flower", right ? -0.76F : 0.76F, 0.9F, -1.1F,
                right ? 192 : 224, 64, 0.4F);
    }

    private static void addLegs(PartDefinition rightLeg, PartDefinition leftLeg) {
        addLeg(rightLeg, true);
        addLeg(leftLeg, false);
    }

    private static void addLeg(PartDefinition leg, boolean right) {
        leg.addOrReplaceChild("stocking",
                swatch(128, 32).addBox(-0.94F, 7.0F, -1.0F, 1.88F, 4.7F, 2.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        for (int i = 0; i < 4; i++) {
            leg.addOrReplaceChild("garter_" + i,
                    swatch(i % 2 == 0 ? 128 : 224, 96)
                            .addBox(-1.02F, -0.12F, -1.14F, 2.04F, 0.22F, 0.24F, CubeDeformation.NONE),
                    PartPose.offsetAndRotation(0.0F, 7.25F + i * 1.05F, 0.0F,
                            0.0F, 0.0F, (right ? -1.0F : 1.0F) * (i % 2 == 0 ? 0.22F : -0.22F)));
        }
        leg.addOrReplaceChild("shoe",
                swatch(128, 32)
                        .addBox(-1.16F, 9.0F, -1.55F, 2.32F, 2.8F, 2.75F, CubeDeformation.NONE)
                        .texOffs(192, 32).addBox(-1.25F, 8.75F, -1.38F, 2.5F, 0.52F, 2.64F,
                                CubeDeformation.NONE)
                        .texOffs(224, 128).addBox(-0.78F, 9.8F, -1.78F, 1.56F, 0.32F, 0.42F,
                                CubeDeformation.NONE)
                        .texOffs(160, 160).addBox(-0.95F, 11.35F, -1.4F, 1.9F, 0.45F, 2.5F,
                                CubeDeformation.NONE),
                PartPose.rotation(0.0F, right ? -0.04F : 0.04F, 0.0F));
        addFlower(leg, "ankle_flower", right ? -0.98F : 0.98F, 9.0F, -1.34F,
                right ? 224 : 128, 64, 0.44F);
    }

    private static void addHarp(PartDefinition body) {
        PartDefinition harp = body.addOrReplaceChild("harp", CubeListBuilder.create(),
                PartPose.offsetAndRotation(5.25F, -0.9F, -2.0F, 0.02F, -0.08F, -0.05F));
        harp.addOrReplaceChild("pillar",
                swatch(192, 96)
                        .addBox(-0.42F, 0.0F, -0.48F, 0.84F, 14.2F, 0.96F, CubeDeformation.NONE)
                        .texOffs(224, 32).addBox(-0.56F, -0.18F, -0.58F, 1.12F, 0.78F, 1.16F,
                                CubeDeformation.NONE)
                        .texOffs(160, 96).addBox(-0.55F, 13.55F, -0.58F, 1.1F, 0.85F, 1.16F,
                                CubeDeformation.NONE),
                PartPose.ZERO);
        addHarpBeam(harp, "beam_low", -0.05F, 1.35F, 0.0F, 4.25F, -0.14F);
        addHarpBeam(harp, "beam_mid", -2.7F, 3.25F, 0.0F, 3.15F, -0.38F);
        addHarpBeam(harp, "beam_high", -4.9F, 5.75F, 0.0F, 2.35F, -0.72F);
        addHarpBeam(harp, "beam_top", -5.85F, 8.25F, 0.0F, 1.55F, -1.02F);

        PartDefinition soundBox = harp.addOrReplaceChild("sound_box", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-5.85F, 11.3F, 0.0F, 0.0F, 0.0F, 0.22F));
        soundBox.addOrReplaceChild("main",
                swatch(224, 96)
                        .addBox(-0.25F, -0.66F, -0.55F, 6.35F, 1.32F, 1.1F, CubeDeformation.NONE)
                        .texOffs(224, 32).addBox(-0.4F, -0.86F, -0.65F, 6.6F, 0.26F, 1.3F)
                        .texOffs(192, 96).addBox(0.95F, 0.42F, -0.6F, 5.0F, 0.9F, 1.2F),
                PartPose.ZERO);
        int[][] stringSwatches = {{160, 96}, {128, 64}, {192, 128}, {192, 64}, {160, 96}, {224, 64}, {192, 128}, {160, 64}};
        for (int i = 0; i < stringSwatches.length; i++) {
            float x = -5.35F + i * 0.72F;
            float top = 5.45F - i * 0.3F;
            soundBox.addOrReplaceChild("string_" + i,
                    swatch(stringSwatches[i][0], stringSwatches[i][1])
                            .addBox(-0.065F, 0.0F, -0.1F, 0.13F, 7.8F + i * 0.42F, 0.2F,
                                    CubeDeformation.NONE),
                    PartPose.offsetAndRotation(x, top - 11.3F, -0.08F, 0.0F, 0.0F, -0.018F));
        }
        PartDefinition ornament = harp.addOrReplaceChild("unicorn_ornament", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -0.65F, 0.0F, 0.0F, 0.0F, -0.06F));
        ornament.addOrReplaceChild("head",
                swatch(128, 32)
                        .addBox(-0.34F, -0.6F, -0.5F, 1.5F, 1.2F, 1.0F, CubeDeformation.NONE)
                        .addBox(0.95F, -0.28F, -0.38F, 0.75F, 0.64F, 0.76F, CubeDeformation.NONE),
                PartPose.ZERO);
        ornament.addOrReplaceChild("horn",
                swatch(160, 96).addBox(-0.1F, -1.5F, -0.1F, 0.2F, 1.6F, 0.2F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(1.0F, -0.38F, -0.16F, 0.0F, 0.0F, 0.5F));
        for (int i = 0; i < 4; i++) {
            ornament.addOrReplaceChild("wing_" + i,
                    swatch(i % 2 == 0 ? 160 : 192, 96)
                            .addBox(-1.55F - i * 0.18F, -0.16F, -0.32F, 1.7F + i * 0.12F,
                                    0.32F, 0.64F, CubeDeformation.NONE),
                    PartPose.offsetAndRotation(-0.15F, -0.05F + i * 0.3F, 0.0F,
                            0.0F, 0.0F, -0.35F - i * 0.15F));
        }
    }

    private static void addBirds(PartDefinition body) {
        addBird(body, "left_bird", -7.1F, -1.8F, -1.65F, 1.0F);
        addBird(body, "right_bird", 8.1F, 2.0F, -1.7F, -1.0F);
    }

    private static void addBird(PartDefinition body, String name, float x, float y, float z, float facing) {
        PartDefinition bird = body.addOrReplaceChild(name,
                swatch(128, 160)
                        .addBox(-0.68F, -0.32F, -0.36F, 1.36F, 0.64F, 0.72F, CubeDeformation.NONE)
                        .texOffs(128, 160).addBox(facing > 0 ? 0.3F : -0.78F, -0.52F, -0.3F,
                                0.5F, 0.48F, 0.6F, CubeDeformation.NONE)
                        .texOffs(160, 96).addBox(facing > 0 ? 0.78F : -1.38F, -0.34F, -0.1F,
                                0.6F, 0.14F, 0.2F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(x, y, z, 0.0F, facing > 0 ? -0.2F : 0.2F, 0.0F));
        bird.addOrReplaceChild("wings",
                swatch(224, 128)
                        .addBox(-0.16F, -0.12F, -1.55F, 0.58F, 0.24F, 1.45F, CubeDeformation.NONE)
                        .addBox(-0.16F, -0.12F, 0.1F, 0.58F, 0.24F, 1.45F, CubeDeformation.NONE),
                PartPose.ZERO);
    }

    private static void addHairSegment(PartDefinition parent, String name, float x, float y, float z,
                                       float width, float length, float depth, float zRot, int u) {
        PartDefinition segment = parent.addOrReplaceChild(name,
                swatch(u, 0).addBox(-width * 0.5F, 0.0F, -depth * 0.5F, width, length, depth,
                        CubeDeformation.NONE),
                PartPose.offsetAndRotation(x, y, z, 0.035F, 0.0F, zRot));
        segment.addOrReplaceChild("tip",
                swatch(u == 192 ? 160 : 192, 0)
                        .addBox(-width * 0.38F, 0.0F, -depth * 0.42F, width * 0.76F, length * 0.7F,
                                depth * 0.84F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, length - 0.18F, 0.0F, 0.055F, 0.0F, zRot * 0.45F));
    }

    private static void addBackHairPanel(PartDefinition parent, String name, float x, float y, float z,
                                         float width, float length, float zRot, int u) {
        PartDefinition panel = parent.addOrReplaceChild(name,
                swatch(u, 0)
                        .addBox(-width * 0.5F, 0.0F, -0.38F, width, length, 0.76F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(x, y, z, 0.045F, 0.0F, zRot));
        float tipLength = Math.max(3.0F, length * 0.38F);
        panel.addOrReplaceChild("tip",
                swatch(u == 192 ? 160 : 192, 0)
                        .addBox(-width * 0.38F, 0.0F, -0.31F, width * 0.76F, tipLength, 0.62F,
                                CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, length - 0.25F, 0.0F, 0.08F, 0.0F, zRot * 0.45F));
    }

    private static void addFlower(PartDefinition parent, String name, float x, float y, float z,
                                  int u, int v, float scale) {
        PartDefinition flower = parent.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(x, y, z));
        for (int i = 0; i < 4; i++) {
            flower.addOrReplaceChild("petal_" + i,
                    swatch(u, v).addBox(-0.58F * scale, -0.16F * scale, -0.15F,
                            1.16F * scale, 0.32F * scale, 0.3F, CubeDeformation.NONE),
                    PartPose.rotation(0.0F, 0.0F, i * Mth.HALF_PI));
        }
        flower.addOrReplaceChild("center",
                swatch(192, 64).addBox(-0.19F * scale, -0.19F * scale, -0.22F,
                        0.38F * scale, 0.38F * scale, 0.44F, CubeDeformation.NONE),
                PartPose.ZERO);
    }

    private static void addRibbon(PartDefinition parent, String name, float x, float y, float z,
                                  float width, float length, int u, int v, float zRot) {
        parent.addOrReplaceChild(name,
                swatch(u, v).addBox(-width * 0.5F, 0.0F, -0.16F, width, length, 0.32F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(x, y, z, 0.05F, 0.0F, zRot));
    }

    private static void addHarpBeam(PartDefinition parent, String name, float x, float y, float z,
                                    float length, float zRot) {
        parent.addOrReplaceChild(name,
                swatch(224, 32)
                        .addBox(-length, -0.28F, -0.36F, length, 0.56F, 0.72F, CubeDeformation.NONE)
                        .texOffs(160, 160).addBox(-length + 0.08F, -0.36F, -0.42F,
                                length - 0.08F, 0.16F, 0.84F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(x, y, z, 0.0F, 0.0F, zRot));
    }

    private static CubeListBuilder skin(int u, int v) {
        return CubeListBuilder.create().texOffs(u * 2, v * 2);
    }

    private static CubeListBuilder swatch(int u) {
        return CubeListBuilder.create().texOffs(u, 0);
    }

    private static CubeListBuilder swatch(int u, int v) {
        return CubeListBuilder.create().texOffs(u, v);
    }

    @Override
    public void setupAnim(ShipGirlEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        float idle = Mth.sin(ageInTicks * 0.07F);
        float sway = Mth.sin(ageInTicks * 0.045F + 1.1F);
        float walk = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
        float flow = idle * 0.012F + walk * 0.045F;

        this.hairMotion.xRot = flow * 0.18F;
        this.leftHair.xRot = 0.04F + flow;
        this.leftHair.zRot = idle * 0.018F - walk * 0.024F;
        this.centerHair.xRot = 0.055F + flow * 0.85F;
        this.centerHair.zRot = sway * 0.01F;
        this.rightHair.xRot = 0.04F + flow;
        this.rightHair.zRot = -idle * 0.018F + walk * 0.024F;

        this.frontDress.xRot = -0.04F + walk * 0.022F;
        this.leftDress.xRot = walk * 0.035F;
        this.leftDress.zRot = 0.16F + idle * 0.01F;
        this.rightDress.xRot = -walk * 0.035F;
        this.rightDress.zRot = -0.16F - idle * 0.01F;
        this.backDress.xRot = 0.075F + flow * 0.65F;
        this.leftRibbon.xRot = 0.08F + flow * 1.2F;
        this.leftRibbon.zRot = 0.2F + idle * 0.03F;
        this.rightRibbon.xRot = 0.08F + flow * 1.1F;
        this.rightRibbon.zRot = -0.2F - idle * 0.03F;

        this.leftArm.xRot = 0.0F;
        this.leftArm.yRot = 0.0F;
        this.leftArm.zRot = 0.56F - idle * 0.012F + walk * 0.025F;
        this.rightArm.xRot = 0.0F;
        this.rightArm.yRot = 0.0F;
        this.rightArm.zRot = -0.42F + idle * 0.012F - walk * 0.025F;
        this.leftForearm.xRot = -0.12F - idle * 0.01F;
        this.leftForearm.yRot = 0.0F;
        this.leftForearm.zRot = 0.72F + walk * 0.02F;
        this.rightForearm.xRot = -0.12F + idle * 0.01F;
        this.rightForearm.yRot = 0.0F;
        this.rightForearm.zRot = 0.62F - walk * 0.02F;
        this.leftLeg.zRot = 0.12F + walk * 0.025F;
        this.rightLeg.zRot = -0.16F - walk * 0.025F;

        this.harp.zRot = -0.06F + sway * 0.008F;
        this.leftBird.y = -1.8F + idle * 0.18F;
        this.leftBird.zRot = sway * 0.05F;
        this.rightBird.y = 2.0F + sway * 0.16F;
        this.rightBird.zRot = -idle * 0.045F;
        float wingBeat = Mth.sin(ageInTicks * 1.35F) * 0.58F;
        this.leftBirdWings.xRot = wingBeat;
        this.rightBirdWings.xRot = -wingBeat;
    }
}
