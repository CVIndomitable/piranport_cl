package com.piranport.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
import com.piranport.npc.deepocean.DeepOceanSupplyEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class DeepOceanSupplyModel extends EntityModel<DeepOceanSupplyEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "deep_ocean_supply"), "main");

    private static final float DEG_TO_RAD = Mth.PI / 180.0F;
    private static final int SKIN_COLOR = 0xFFEADBD5;
    private static final int HAIR_COLOR = 0xFFF2F0EC;
    private static final int HAIR_SHADOW_COLOR = 0xFFC5C2C0;
    private static final int CLOTH_COLOR = 0xFFE5E0D8;
    private static final int DARK_COLOR = 0xFF201A22;
    private static final int METAL_COLOR = 0xFF4E4851;
    private static final int METAL_DARK_COLOR = 0xFF2A252D;
    private static final int METAL_LIGHT_COLOR = 0xFF756B73;
    private static final int GLOW_COLOR = 0xFFFF3D5C;
    private static final int EYE_GLOW_COLOR = 0xFFFFB34B;
    private static final int TEETH_COLOR = 0xFFEFE9E0;
    private static final int TONGUE_COLOR = 0xFFFF8A78;

    private static final float LEFT_ARM_Z_ROT = 8.0F * DEG_TO_RAD;
    private static final float RIGHT_ARM_Z_ROT = -7.0F * DEG_TO_RAD;
    private static final float CRANE_Z_ROT = 8.0F * DEG_TO_RAD;

    private final ModelPart hullBase;
    private final ModelPart hullRim;
    private final ModelPart hullPanels;
    private final ModelPart hullMarking;
    private final ModelPart hullMouth;
    private final ModelPart hullTeeth;
    private final ModelPart hullTongue;
    private final ModelPart hullGlow;
    private final ModelPart bodySkin;
    private final ModelPart bodyCloth;
    private final ModelPart bodyStripe;
    private final ModelPart headSkin;
    private final ModelPart headHair;
    private final ModelPart hairShadow;
    private final ModelPart headBand;
    private final ModelPart faceMask;
    private final ModelPart eyesGlow;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftFoot;
    private final ModelPart rightFoot;
    private final ModelPart leftHairTail;
    private final ModelPart rightHairTail;
    private final ModelPart scarfRibbon;
    private final ModelPart craneBase;
    private final ModelPart craneBoom;
    private final ModelPart craneGlow;
    private final ModelPart craneHook;
    private final ModelPart trussFrame;
    private final ModelPart chain;

    public DeepOceanSupplyModel(ModelPart root) {
        ModelPart modelRoot = root.getChild("root");
        this.hullBase = modelRoot.getChild("hull_base");
        this.hullRim = modelRoot.getChild("hull_rim");
        this.hullPanels = modelRoot.getChild("hull_panels");
        this.hullMarking = modelRoot.getChild("hull_marking");
        this.hullMouth = modelRoot.getChild("hull_mouth");
        this.hullTeeth = modelRoot.getChild("hull_teeth");
        this.hullTongue = modelRoot.getChild("hull_tongue");
        this.hullGlow = modelRoot.getChild("hull_glow");
        this.bodySkin = modelRoot.getChild("body_skin");
        this.bodyCloth = modelRoot.getChild("body_cloth");
        this.bodyStripe = modelRoot.getChild("body_stripe");
        this.headSkin = modelRoot.getChild("head_skin");
        this.headHair = modelRoot.getChild("head_hair");
        this.hairShadow = modelRoot.getChild("hair_shadow");
        this.headBand = modelRoot.getChild("head_band");
        this.faceMask = modelRoot.getChild("face_mask");
        this.eyesGlow = modelRoot.getChild("eyes_glow");
        this.leftArm = modelRoot.getChild("left_arm");
        this.rightArm = modelRoot.getChild("right_arm");
        this.leftLeg = modelRoot.getChild("left_leg");
        this.rightLeg = modelRoot.getChild("right_leg");
        this.leftFoot = modelRoot.getChild("left_foot");
        this.rightFoot = modelRoot.getChild("right_foot");
        this.leftHairTail = modelRoot.getChild("left_hair_tail");
        this.rightHairTail = modelRoot.getChild("right_hair_tail");
        this.scarfRibbon = modelRoot.getChild("scarf_ribbon");
        this.craneBase = modelRoot.getChild("crane_base");
        this.craneBoom = modelRoot.getChild("crane_boom");
        this.craneGlow = modelRoot.getChild("crane_glow");
        this.craneHook = modelRoot.getChild("crane_hook");
        this.trussFrame = modelRoot.getChild("truss_frame");
        this.chain = modelRoot.getChild("chain");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root",
                CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        addHull(root);
        addGirl(root);
        addRigging(root);

        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addHull(PartDefinition root) {
        root.addOrReplaceChild("hull_base",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.85F, -9.25F, -4.12F, 11.70F, 7.35F, 8.24F,
                                new CubeDeformation(0.28F))
                        .texOffs(0, 17).addBox(-4.35F, -2.45F, -3.05F, 8.70F, 1.30F, 6.10F,
                                new CubeDeformation(0.18F))
                        .texOffs(0, 26).addBox(-2.55F, -1.45F, -2.00F, 5.10F, 1.35F, 4.00F,
                                new CubeDeformation(0.08F)),
                PartPose.ZERO);

        root.addOrReplaceChild("hull_rim",
                CubeListBuilder.create()
                        .texOffs(36, 0).addBox(-6.55F, -12.20F, -4.70F, 13.10F, 2.85F, 9.40F,
                                new CubeDeformation(0.20F))
                        .texOffs(36, 13).addBox(-6.90F, -10.05F, -4.95F, 13.80F, 0.72F, 9.90F,
                                new CubeDeformation(0.10F))
                        .texOffs(36, 16).addBox(-5.35F, -12.95F, -3.85F, 10.70F, 1.05F, 7.70F,
                                new CubeDeformation(0.08F)),
                PartPose.ZERO);

        root.addOrReplaceChild("hull_panels",
                CubeListBuilder.create()
                        .texOffs(0, 34).addBox(-6.08F, -8.80F, -4.42F, 1.20F, 5.80F, 0.42F,
                                new CubeDeformation(0.0F))
                        .texOffs(5, 34).addBox(4.88F, -8.80F, -4.42F, 1.20F, 5.80F, 0.42F,
                                new CubeDeformation(0.0F))
                        .texOffs(10, 34).addBox(-0.32F, -8.90F, -4.46F, 0.64F, 5.95F, 0.44F,
                                new CubeDeformation(0.0F))
                        .texOffs(14, 34).addBox(-5.70F, -5.85F, -4.50F, 11.40F, 0.58F, 0.46F,
                                new CubeDeformation(0.0F))
                        .texOffs(14, 37).addBox(-5.15F, -9.60F, 4.08F, 10.30F, 1.10F, 0.48F,
                                new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("hull_marking",
                CubeListBuilder.create()
                        .texOffs(76, 0).addBox(1.05F, -9.80F, -4.76F, 2.00F, 0.36F, 0.24F,
                                new CubeDeformation(0.0F))
                        .texOffs(76, 2).addBox(1.05F, -8.72F, -4.76F, 2.00F, 0.36F, 0.24F,
                                new CubeDeformation(0.0F))
                        .texOffs(76, 4).addBox(1.05F, -7.64F, -4.76F, 2.00F, 0.36F, 0.24F,
                                new CubeDeformation(0.0F))
                        .texOffs(76, 6).addBox(0.92F, -9.50F, -4.76F, 0.36F, 0.92F, 0.24F,
                                new CubeDeformation(0.0F))
                        .texOffs(78, 6).addBox(2.74F, -8.42F, -4.76F, 0.36F, 0.92F, 0.24F,
                                new CubeDeformation(0.0F))
                        .texOffs(82, 0).addBox(3.68F, -9.72F, -4.76F, 1.40F, 2.35F, 0.24F,
                                new CubeDeformation(0.0F))
                        .texOffs(86, 0).addBox(3.96F, -9.44F, -4.82F, 0.84F, 1.80F, 0.26F,
                                new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("hull_mouth",
                CubeListBuilder.create()
                        .texOffs(0, 44).addBox(-2.75F, -5.62F, -5.04F, 5.50F, 1.80F, 0.78F,
                                new CubeDeformation(0.08F))
                        .texOffs(0, 48).addBox(-2.20F, -4.20F, -5.26F, 4.40F, 1.35F, 0.86F,
                                new CubeDeformation(0.04F)),
                PartPose.ZERO);

        root.addOrReplaceChild("hull_teeth",
                CubeListBuilder.create()
                        .texOffs(16, 44).addBox(-2.42F, -5.28F, -5.42F, 0.66F, 0.72F, 0.36F,
                                new CubeDeformation(0.0F))
                        .texOffs(20, 44).addBox(-1.56F, -5.18F, -5.42F, 0.66F, 0.82F, 0.36F,
                                new CubeDeformation(0.0F))
                        .texOffs(24, 44).addBox(-0.66F, -5.06F, -5.42F, 0.66F, 0.90F, 0.36F,
                                new CubeDeformation(0.0F))
                        .texOffs(28, 44).addBox(0.28F, -5.08F, -5.42F, 0.66F, 0.86F, 0.36F,
                                new CubeDeformation(0.0F))
                        .texOffs(32, 44).addBox(1.18F, -5.24F, -5.42F, 0.66F, 0.76F, 0.36F,
                                new CubeDeformation(0.0F))
                        .texOffs(36, 44).addBox(1.95F, -5.42F, -5.42F, 0.62F, 0.64F, 0.36F,
                                new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("hull_tongue",
                CubeListBuilder.create()
                        .texOffs(40, 44).addBox(-1.38F, -3.72F, -6.05F, 2.76F, 0.78F, 1.72F,
                                new CubeDeformation(0.16F))
                        .texOffs(40, 48).addBox(-0.86F, -3.42F, -6.74F, 1.72F, 0.48F, 1.05F,
                                new CubeDeformation(0.14F)),
                PartPose.ZERO);

        root.addOrReplaceChild("hull_glow",
                CubeListBuilder.create()
                        .texOffs(64, 28).addBox(-6.94F, -7.14F, -5.10F, 13.88F, 0.34F, 0.28F,
                                new CubeDeformation(0.0F))
                        .texOffs(64, 31).addBox(-7.12F, -7.08F, -3.62F, 0.32F, 0.32F, 7.24F,
                                new CubeDeformation(0.0F))
                        .texOffs(64, 34).addBox(6.80F, -7.08F, -3.62F, 0.32F, 0.32F, 7.24F,
                                new CubeDeformation(0.0F))
                        .texOffs(64, 37).addBox(-4.70F, -7.00F, 4.52F, 9.40F, 0.30F, 0.24F,
                                new CubeDeformation(0.0F))
                        .texOffs(64, 40).addBox(-8.28F, -6.34F, -1.42F, 2.60F, 0.30F, 0.22F,
                                new CubeDeformation(0.0F))
                        .texOffs(64, 42).addBox(5.68F, -6.92F, -1.42F, 2.80F, 0.30F, 0.22F,
                                new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addGirl(PartDefinition root) {
        root.addOrReplaceChild("body_skin",
                CubeListBuilder.create()
                        .texOffs(0, 56).addBox(-1.85F, -18.30F, -1.42F, 3.70F, 3.95F, 2.72F,
                                new CubeDeformation(0.04F))
                        .texOffs(0, 64).addBox(-1.55F, -14.72F, -1.22F, 3.10F, 2.30F, 2.34F,
                                new CubeDeformation(0.02F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body_cloth",
                CubeListBuilder.create()
                        .texOffs(16, 56).addBox(-2.12F, -18.70F, -1.65F, 4.24F, 5.45F, 3.10F,
                                new CubeDeformation(0.05F))
                        .texOffs(16, 66).addBox(-2.42F, -13.70F, -1.38F, 4.84F, 1.35F, 2.76F,
                                new CubeDeformation(0.08F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body_stripe",
                CubeListBuilder.create()
                        .texOffs(34, 56).addBox(-0.30F, -18.45F, -1.94F, 0.60F, 5.60F, 0.32F,
                                new CubeDeformation(0.0F))
                        .texOffs(38, 56).addBox(-2.00F, -17.40F, -1.93F, 4.00F, 0.42F, 0.30F,
                                new CubeDeformation(0.0F))
                        .texOffs(38, 59).addBox(-1.70F, -14.28F, -1.76F, 3.40F, 0.36F, 0.28F,
                                new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("head_skin",
                CubeListBuilder.create()
                        .texOffs(0, 76).addBox(-2.28F, -4.00F, -2.08F, 4.56F, 4.68F, 4.16F,
                                new CubeDeformation(0.03F)),
                PartPose.offset(0.0F, -21.70F, -0.34F));

        root.addOrReplaceChild("head_hair",
                CubeListBuilder.create()
                        .texOffs(18, 76).addBox(-2.68F, -4.42F, -1.82F, 5.36F, 1.44F, 4.06F,
                                new CubeDeformation(0.12F))
                        .texOffs(18, 82).addBox(-2.92F, -3.22F, -1.34F, 1.30F, 6.90F, 2.26F,
                                new CubeDeformation(0.08F))
                        .texOffs(26, 82).addBox(1.62F, -3.38F, -1.40F, 1.30F, 7.22F, 2.34F,
                                new CubeDeformation(0.08F))
                        .texOffs(34, 82).addBox(-1.28F, -3.92F, -2.50F, 2.94F, 4.30F, 0.72F,
                                new CubeDeformation(0.04F)),
                PartPose.offset(0.0F, -21.70F, -0.34F));

        root.addOrReplaceChild("hair_shadow",
                CubeListBuilder.create()
                        .texOffs(44, 76).addBox(-2.88F, -3.86F, -1.70F, 5.76F, 1.06F, 3.92F,
                                new CubeDeformation(0.07F))
                        .texOffs(44, 82).addBox(-1.48F, -1.84F, -2.52F, 0.74F, 2.70F, 0.42F,
                                new CubeDeformation(0.02F))
                        .texOffs(48, 82).addBox(0.84F, -2.84F, -2.52F, 0.66F, 2.78F, 0.42F,
                                new CubeDeformation(0.02F)),
                PartPose.offset(0.0F, -21.70F, -0.34F));

        root.addOrReplaceChild("head_band",
                CubeListBuilder.create()
                        .texOffs(58, 76).addBox(-2.78F, -4.10F, -2.18F, 5.56F, 0.48F, 4.18F,
                                new CubeDeformation(0.04F))
                        .texOffs(58, 82).addBox(-2.72F, -3.28F, -2.34F, 5.44F, 0.42F, 0.42F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -21.70F, -0.34F));

        root.addOrReplaceChild("face_mask",
                CubeListBuilder.create()
                        .texOffs(72, 76).addBox(-2.04F, -0.98F, -2.48F, 4.08F, 1.35F, 0.48F,
                                new CubeDeformation(0.0F))
                        .texOffs(72, 80).addBox(-0.26F, -0.82F, -2.58F, 0.52F, 1.02F, 0.26F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -21.70F, -0.34F));

        root.addOrReplaceChild("eyes_glow",
                CubeListBuilder.create()
                        .texOffs(64, 48).addBox(-1.28F, -1.92F, -2.54F, 0.56F, 0.42F, 0.24F,
                                new CubeDeformation(0.0F))
                        .texOffs(68, 48).addBox(0.72F, -1.92F, -2.54F, 0.56F, 0.42F, 0.24F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -21.70F, -0.34F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(0, 92).addBox(-1.34F, -0.58F, -0.78F, 1.42F, 4.30F, 1.56F,
                                new CubeDeformation(0.02F))
                        .texOffs(0, 99).addBox(-1.46F, 3.02F, -0.86F, 1.82F, 0.74F, 1.72F,
                                new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(-2.36F, -17.10F, -0.08F, -0.26F, 0.0F, LEFT_ARM_Z_ROT));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(8, 92).addBox(-0.08F, -0.58F, -0.78F, 1.42F, 4.30F, 1.56F,
                                new CubeDeformation(0.02F))
                        .texOffs(8, 99).addBox(-0.36F, 3.02F, -0.86F, 1.82F, 0.74F, 1.72F,
                                new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(2.36F, -17.10F, -0.08F, -0.26F, 0.0F, RIGHT_ARM_Z_ROT));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(20, 92).addBox(-3.25F, -11.58F, -3.54F, 2.08F, 1.50F, 5.20F,
                                new CubeDeformation(0.04F))
                        .texOffs(20, 100).addBox(-3.72F, -10.32F, -6.02F, 1.52F, 4.72F, 1.36F,
                                new CubeDeformation(0.04F)),
                PartPose.ZERO);

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(34, 92).addBox(-0.48F, -11.42F, -3.36F, 2.00F, 1.44F, 4.82F,
                                new CubeDeformation(0.04F))
                        .texOffs(34, 100).addBox(0.80F, -10.16F, -5.62F, 1.44F, 4.18F, 1.32F,
                                new CubeDeformation(0.04F)),
                PartPose.ZERO);

        root.addOrReplaceChild("left_foot",
                CubeListBuilder.create()
                        .texOffs(48, 92).addBox(-4.28F, -5.98F, -6.48F, 2.38F, 0.82F, 1.88F,
                                new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -7.0F * DEG_TO_RAD, -8.0F * DEG_TO_RAD));

        root.addOrReplaceChild("right_foot",
                CubeListBuilder.create()
                        .texOffs(48, 97).addBox(0.54F, -6.32F, -6.12F, 2.24F, 0.76F, 1.72F,
                                new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 6.0F * DEG_TO_RAD, 5.0F * DEG_TO_RAD));

        root.addOrReplaceChild("left_hair_tail",
                CubeListBuilder.create()
                        .texOffs(60, 92).addBox(-4.94F, -20.50F, 0.22F, 1.18F, 7.10F, 1.14F,
                                new CubeDeformation(0.06F))
                        .texOffs(66, 92).addBox(-5.72F, -15.10F, -1.36F, 1.02F, 5.94F, 1.02F,
                                new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 10.0F * DEG_TO_RAD));

        root.addOrReplaceChild("right_hair_tail",
                CubeListBuilder.create()
                        .texOffs(72, 92).addBox(3.78F, -20.38F, 0.26F, 1.18F, 7.55F, 1.14F,
                                new CubeDeformation(0.06F))
                        .texOffs(78, 92).addBox(4.72F, -15.20F, -1.18F, 1.02F, 5.70F, 1.02F,
                                new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -9.0F * DEG_TO_RAD));

        root.addOrReplaceChild("scarf_ribbon",
                CubeListBuilder.create()
                        .texOffs(86, 92).addBox(-6.80F, -19.36F, -0.62F, 4.26F, 0.62F, 0.46F,
                                new CubeDeformation(0.02F))
                        .texOffs(86, 95).addBox(-7.90F, -18.80F, -0.62F, 2.76F, 0.52F, 0.42F,
                                new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -16.0F * DEG_TO_RAD));
    }

    private static void addRigging(PartDefinition root) {
        PartDefinition truss = root.addOrReplaceChild("truss_frame",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-6.40F, -15.30F, 2.18F,
                        0.0F, -18.0F * DEG_TO_RAD, 31.0F * DEG_TO_RAD));
        addBeam(truss, "top", 0.0F, -2.10F, 0.0F, 0.0F, 7.20F, 0.34F);
        addBeam(truss, "bottom", 0.0F, 2.10F, 0.0F, 0.0F, 7.20F, 0.34F);
        addBeam(truss, "left_side", -3.60F, 0.0F, 0.0F, 90.0F * DEG_TO_RAD, 4.20F, 0.34F);
        addBeam(truss, "right_side", 3.60F, 0.0F, 0.0F, 90.0F * DEG_TO_RAD, 4.20F, 0.34F);
        addBeam(truss, "diag_a", -1.80F, 0.0F, 0.0F, 30.0F * DEG_TO_RAD, 4.70F, 0.28F);
        addBeam(truss, "diag_b", 1.80F, 0.0F, 0.0F, -30.0F * DEG_TO_RAD, 4.70F, 0.28F);
        addBeam(truss, "diag_c", 0.0F, 0.0F, 0.0F, -31.0F * DEG_TO_RAD, 4.90F, 0.28F);

        root.addOrReplaceChild("crane_base",
                CubeListBuilder.create()
                        .texOffs(92, 0).addBox(4.70F, -17.32F, 2.52F, 1.00F, 5.60F, 1.00F,
                                new CubeDeformation(0.04F))
                        .texOffs(98, 0).addBox(3.82F, -12.24F, 1.92F, 2.76F, 1.20F, 2.20F,
                                new CubeDeformation(0.08F)),
                PartPose.ZERO);

        PartDefinition boom = root.addOrReplaceChild("crane_boom",
                CubeListBuilder.create()
                        .texOffs(92, 10).addBox(-0.30F, -0.36F, -0.36F, 8.82F, 0.72F, 0.72F,
                                new CubeDeformation(0.02F))
                        .texOffs(92, 14).addBox(0.36F, -1.12F, -0.32F, 6.20F, 0.34F, 0.64F,
                                new CubeDeformation(0.0F))
                        .texOffs(92, 17).addBox(0.36F, 0.78F, -0.32F, 6.20F, 0.34F, 0.64F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.32F, -17.68F, 2.96F,
                        0.0F, -10.0F * DEG_TO_RAD, CRANE_Z_ROT));
        addBoomBrace(boom, "brace_a", 1.25F, 0.0F, 28.0F * DEG_TO_RAD);
        addBoomBrace(boom, "brace_b", 3.35F, 0.0F, -28.0F * DEG_TO_RAD);
        addBoomBrace(boom, "brace_c", 5.45F, 0.0F, 28.0F * DEG_TO_RAD);

        root.addOrReplaceChild("crane_glow",
                CubeListBuilder.create()
                        .texOffs(64, 52).addBox(6.08F, -18.44F, 2.42F, 1.10F, 0.26F, 0.28F,
                                new CubeDeformation(0.0F))
                        .texOffs(64, 54).addBox(7.62F, -18.20F, 2.42F, 1.10F, 0.26F, 0.28F,
                                new CubeDeformation(0.0F))
                        .texOffs(64, 56).addBox(9.14F, -17.98F, 2.42F, 1.10F, 0.26F, 0.28F,
                                new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("crane_hook",
                CubeListBuilder.create()
                        .texOffs(112, 0).addBox(12.16F, -16.36F, 2.48F, 0.22F, 3.24F, 0.22F,
                                new CubeDeformation(0.0F))
                        .texOffs(114, 0).addBox(11.62F, -13.22F, 2.34F, 1.32F, 0.34F, 0.50F,
                                new CubeDeformation(0.0F))
                        .texOffs(114, 2).addBox(11.38F, -12.92F, 2.34F, 0.40F, 1.10F, 0.50F,
                                new CubeDeformation(0.0F))
                        .texOffs(118, 2).addBox(12.52F, -12.92F, 2.34F, 0.40F, 0.72F, 0.50F,
                                new CubeDeformation(0.0F))
                        .texOffs(114, 6).addBox(11.66F, -11.96F, 2.34F, 1.02F, 0.34F, 0.50F,
                                new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("chain",
                CubeListBuilder.create()
                        .texOffs(108, 24).addBox(-5.70F, -5.74F, -5.18F, 0.44F, 0.86F, 0.34F,
                                new CubeDeformation(0.0F))
                        .texOffs(110, 24).addBox(-5.26F, -4.98F, -5.18F, 0.76F, 0.38F, 0.34F,
                                new CubeDeformation(0.0F))
                        .texOffs(108, 27).addBox(-4.44F, -4.84F, -5.18F, 0.44F, 0.86F, 0.34F,
                                new CubeDeformation(0.0F))
                        .texOffs(110, 27).addBox(-4.00F, -4.10F, -5.18F, 0.76F, 0.38F, 0.34F,
                                new CubeDeformation(0.0F))
                        .texOffs(108, 30).addBox(-3.20F, -4.00F, -5.18F, 0.44F, 0.78F, 0.34F,
                                new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addBeam(PartDefinition parent, String name, float x, float y, float z,
                                float zRot, float length, float thickness) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(96, 32).addBox(-length * 0.5F, -thickness * 0.5F, -thickness * 0.5F,
                                length, thickness, thickness, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(x, y, z, 0.0F, 0.0F, zRot));
    }

    private static void addBoomBrace(PartDefinition parent, String name, float x, float z, float zRot) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(96, 40).addBox(-0.18F, -1.04F, -0.22F, 0.36F, 2.08F, 0.44F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(x, 0.0F, z, 0.0F, 0.0F, zRot));
    }

    @Override
    public void setupAnim(DeepOceanSupplyEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float headYaw = netHeadYaw * DEG_TO_RAD * 0.42F;
        float headPitchRot = headPitch * DEG_TO_RAD * 0.30F;
        setHeadRotation(headSkin, headYaw, headPitchRot);
        setHeadRotation(headHair, headYaw, headPitchRot);
        setHeadRotation(hairShadow, headYaw, headPitchRot);
        setHeadRotation(headBand, headYaw, headPitchRot);
        setHeadRotation(faceMask, headYaw, headPitchRot);
        setHeadRotation(eyesGlow, headYaw, headPitchRot);

        float walk = Math.min(limbSwingAmount, 1.0F);
        float armSwing = Mth.cos(limbSwing * 0.6662F) * 0.18F * walk;
        leftArm.xRot = -0.26F - armSwing;
        rightArm.xRot = -0.26F + armSwing;
        leftArm.zRot = LEFT_ARM_Z_ROT + Mth.sin(ageInTicks * 0.055F) * 0.035F;
        rightArm.zRot = RIGHT_ARM_Z_ROT - Mth.sin(ageInTicks * 0.055F) * 0.035F;

        float idle = Mth.sin(ageInTicks * 0.055F);
        hullGlow.yRot = idle * 0.018F;
        leftHairTail.zRot = 10.0F * DEG_TO_RAD + idle * 0.055F;
        rightHairTail.zRot = -9.0F * DEG_TO_RAD - idle * 0.052F;
        scarfRibbon.zRot = -16.0F * DEG_TO_RAD + Mth.sin(ageInTicks * 0.075F) * 0.08F;
        craneBoom.zRot = CRANE_Z_ROT + Mth.sin(ageInTicks * 0.035F) * 0.025F;
        craneHook.xRot = Mth.sin(ageInTicks * 0.070F) * 0.040F;
        trussFrame.zRot = 31.0F * DEG_TO_RAD + Mth.sin(ageInTicks * 0.044F) * 0.020F;
    }

    private static void setHeadRotation(ModelPart part, float yRot, float xRot) {
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = 0.0F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, int color) {
        trussFrame.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        craneBase.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        craneBoom.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_COLOR);
        craneGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        craneHook.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);

        hullBase.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        hullRim.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_COLOR);
        hullPanels.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_LIGHT_COLOR);
        hullMarking.render(poseStack, vertexConsumer, packedLight, packedOverlay, TEETH_COLOR);
        hullMouth.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        hullTeeth.render(poseStack, vertexConsumer, packedLight, packedOverlay, TEETH_COLOR);
        hullTongue.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, TONGUE_COLOR);
        hullGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        chain.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);

        leftHairTail.render(poseStack, vertexConsumer, packedLight, packedOverlay, HAIR_COLOR);
        rightHairTail.render(poseStack, vertexConsumer, packedLight, packedOverlay, HAIR_COLOR);
        scarfRibbon.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        leftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        rightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        leftFoot.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        rightFoot.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        bodySkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        bodyCloth.render(poseStack, vertexConsumer, packedLight, packedOverlay, CLOTH_COLOR);
        bodyStripe.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        leftArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        rightArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        headSkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        headHair.render(poseStack, vertexConsumer, packedLight, packedOverlay, HAIR_COLOR);
        hairShadow.render(poseStack, vertexConsumer, packedLight, packedOverlay, HAIR_SHADOW_COLOR);
        headBand.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        faceMask.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        eyesGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, EYE_GLOW_COLOR);
    }
}
