package com.piranport.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
import com.piranport.npc.deepocean.DeepOceanBattleshipEntity;
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

public class DeepOceanBattleshipModel extends EntityModel<DeepOceanBattleshipEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "deep_ocean_battleship"), "main");

    private static final float DEG_TO_RAD = Mth.PI / 180.0F;
    private static final int SKIN_COLOR = 0xFFEBDDD8;
    private static final int HAIR_COLOR = 0xFFE6E3EA;
    private static final int HAIR_SHADOW_COLOR = 0xFF8B8790;
    private static final int SUIT_COLOR = 0xFF1A161E;
    private static final int SUIT_DARK_COLOR = 0xFF0F0C12;
    private static final int METAL_COLOR = 0xFF5B555F;
    private static final int METAL_DARK_COLOR = 0xFF2D2931;
    private static final int METAL_EDGE_COLOR = 0xFF827983;
    private static final int BARREL_COLOR = 0xFF756C78;
    private static final int GLOW_COLOR = 0xFFFF3FCC;

    private static final float LEFT_ARM_Z_ROT = 9.0F * DEG_TO_RAD;
    private static final float RIGHT_ARM_Z_ROT = -10.0F * DEG_TO_RAD;
    private static final float LEFT_POD_Y_ROT = 16.0F * DEG_TO_RAD;
    private static final float RIGHT_POD_Y_ROT = -15.0F * DEG_TO_RAD;
    private static final float LEFT_POD_Z_ROT = 6.0F * DEG_TO_RAD;
    private static final float RIGHT_POD_Z_ROT = -7.0F * DEG_TO_RAD;
    private static final float MAW_Y_ROT = 18.0F * DEG_TO_RAD;
    private static final float MAW_Z_ROT = -5.0F * DEG_TO_RAD;
    private static final float LEFT_UPPER_TURRET_Y_ROT = 18.0F * DEG_TO_RAD;
    private static final float RIGHT_UPPER_TURRET_Y_ROT = -18.0F * DEG_TO_RAD;
    private static final float LEFT_LOWER_TURRET_Y_ROT = 24.0F * DEG_TO_RAD;
    private static final float RIGHT_LOWER_TURRET_Y_ROT = -14.0F * DEG_TO_RAD;
    private static final float TAIL_TURRET_Y_ROT = 8.0F * DEG_TO_RAD;

    private final ModelPart bodySkin;
    private final ModelPart bodySuit;
    private final ModelPart bodyGlow;
    private final ModelPart headSkin;
    private final ModelPart headHair;
    private final ModelPart headHairShadow;
    private final ModelPart headArmor;
    private final ModelPart headGlow;
    private final ModelPart leftArmSkin;
    private final ModelPart rightArmSkin;
    private final ModelPart leftArmSuit;
    private final ModelPart rightArmSuit;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart legGlow;
    private final ModelPart skirtArmor;
    private final ModelPart waistDeck;
    private final ModelPart backArmor;
    private final ModelPart backArmorGlow;
    private final ModelPart segmentedTail;
    private final ModelPart tailGlow;
    private final ModelPart leftMaw;
    private final ModelPart mawGlow;
    private final ModelPart leftPod;
    private final ModelPart rightPod;
    private final ModelPart podGlow;
    private final ModelPart leftUpperTurret;
    private final ModelPart rightUpperTurret;
    private final ModelPart leftLowerTurret;
    private final ModelPart rightLowerTurret;
    private final ModelPart tailTurret;
    private final ModelPart turretGlow;

    public DeepOceanBattleshipModel(ModelPart root) {
        ModelPart modelRoot = root.getChild("root");
        this.bodySkin = modelRoot.getChild("body_skin");
        this.bodySuit = modelRoot.getChild("body_suit");
        this.bodyGlow = modelRoot.getChild("body_glow");
        this.headSkin = modelRoot.getChild("head_skin");
        this.headHair = modelRoot.getChild("head_hair");
        this.headHairShadow = modelRoot.getChild("head_hair_shadow");
        this.headArmor = modelRoot.getChild("head_armor");
        this.headGlow = modelRoot.getChild("head_glow");
        this.leftArmSkin = modelRoot.getChild("left_arm_skin");
        this.rightArmSkin = modelRoot.getChild("right_arm_skin");
        this.leftArmSuit = modelRoot.getChild("left_arm_suit");
        this.rightArmSuit = modelRoot.getChild("right_arm_suit");
        this.leftLeg = modelRoot.getChild("left_leg");
        this.rightLeg = modelRoot.getChild("right_leg");
        this.legGlow = modelRoot.getChild("leg_glow");
        this.skirtArmor = modelRoot.getChild("skirt_armor");
        this.waistDeck = modelRoot.getChild("waist_deck");
        this.backArmor = modelRoot.getChild("back_armor");
        this.backArmorGlow = modelRoot.getChild("back_armor_glow");
        this.segmentedTail = modelRoot.getChild("segmented_tail");
        this.tailGlow = modelRoot.getChild("tail_glow");
        this.leftMaw = modelRoot.getChild("left_maw");
        this.mawGlow = modelRoot.getChild("maw_glow");
        this.leftPod = modelRoot.getChild("left_pod");
        this.rightPod = modelRoot.getChild("right_pod");
        this.podGlow = modelRoot.getChild("pod_glow");
        this.leftUpperTurret = modelRoot.getChild("left_upper_turret");
        this.rightUpperTurret = modelRoot.getChild("right_upper_turret");
        this.leftLowerTurret = modelRoot.getChild("left_lower_turret");
        this.rightLowerTurret = modelRoot.getChild("right_lower_turret");
        this.tailTurret = modelRoot.getChild("tail_turret");
        this.turretGlow = modelRoot.getChild("turret_glow");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root",
                CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        addBody(root);
        addHead(root);
        addLimbs(root);
        addArmor(root);
        addMaw(root);
        addPods(root);
        addTurrets(root);

        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addBody(PartDefinition root) {
        root.addOrReplaceChild("body_skin",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.2F, -14.9F, -1.28F, 4.4F, 4.35F, 2.56F, new CubeDeformation(0.04F))
                        .texOffs(0, 8).addBox(-1.7F, -10.9F, -1.12F, 3.4F, 1.9F, 2.24F, new CubeDeformation(0.0F))
                        .texOffs(0, 13).addBox(-1.05F, -18.35F, -0.80F, 2.1F, 1.15F, 1.60F, new CubeDeformation(0.02F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body_suit",
                CubeListBuilder.create()
                        .texOffs(20, 0).addBox(-2.85F, -17.35F, -1.62F, 5.7F, 3.5F, 3.12F, new CubeDeformation(0.06F))
                        .texOffs(20, 7).addBox(-2.55F, -14.15F, -1.76F, 5.1F, 1.1F, 0.42F, new CubeDeformation(0.0F))
                        .texOffs(20, 10).addBox(-2.40F, -11.20F, -1.48F, 4.8F, 1.18F, 0.42F, new CubeDeformation(0.0F))
                        .texOffs(20, 13).addBox(-0.36F, -17.1F, -1.92F, 0.72F, 6.15F, 0.32F, new CubeDeformation(0.0F))
                        .texOffs(34, 7).addBox(-3.15F, -8.95F, -1.24F, 6.3F, 1.65F, 2.48F, new CubeDeformation(0.05F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body_glow",
                CubeListBuilder.create()
                        .texOffs(72, 0).addBox(-0.32F, -16.90F, -2.02F, 0.64F, 1.50F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(72, 3).addBox(-1.30F, -13.55F, -1.88F, 2.6F, 0.22F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(72, 5).addBox(-0.25F, -8.82F, -1.50F, 0.50F, 0.74F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(72, 7).addBox(1.45F, -10.55F, -1.42F, 0.24F, 1.85F, 0.20F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addHead(PartDefinition root) {
        root.addOrReplaceChild("head_skin",
                CubeListBuilder.create()
                        .texOffs(0, 24).addBox(-2.28F, -4.0F, -2.05F, 4.56F, 4.65F, 4.10F, new CubeDeformation(0.03F))
                        .texOffs(0, 34).addBox(-1.50F, -0.05F, -2.38F, 3.0F, 0.78F, 0.68F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -20.25F, -0.24F, -3.0F * DEG_TO_RAD, 0.0F, 0.0F));

        root.addOrReplaceChild("head_hair",
                CubeListBuilder.create()
                        .texOffs(18, 24).addBox(-2.70F, -4.42F, -1.82F, 5.4F, 1.55F, 4.02F, new CubeDeformation(0.11F))
                        .texOffs(18, 30).addBox(-2.96F, -3.18F, -1.36F, 1.30F, 6.55F, 2.16F, new CubeDeformation(0.08F))
                        .texOffs(26, 30).addBox(1.58F, -3.58F, -1.45F, 1.28F, 7.15F, 2.28F, new CubeDeformation(0.08F))
                        .texOffs(34, 30).addBox(-0.90F, -3.95F, -2.50F, 2.55F, 6.20F, 0.74F, new CubeDeformation(0.04F))
                        .texOffs(44, 24).addBox(-0.10F, -4.05F, 1.78F, 1.70F, 8.75F, 0.82F, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(0.0F, -20.25F, -0.24F, -3.0F * DEG_TO_RAD, 0.0F, 0.0F));

        root.addOrReplaceChild("head_hair_shadow",
                CubeListBuilder.create()
                        .texOffs(50, 30).addBox(-2.00F, -2.05F, -2.56F, 1.50F, 3.10F, 0.42F, new CubeDeformation(0.02F))
                        .texOffs(50, 35).addBox(0.28F, -3.48F, -2.56F, 0.72F, 2.42F, 0.42F, new CubeDeformation(0.02F))
                        .texOffs(50, 39).addBox(1.50F, -1.80F, -2.40F, 0.52F, 2.10F, 0.38F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -20.25F, -0.24F, -3.0F * DEG_TO_RAD, 0.0F, 0.0F));

        PartDefinition armor = root.addOrReplaceChild("head_armor",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -23.72F, -0.05F, -3.0F * DEG_TO_RAD, 0.0F, 0.0F));
        armor.addOrReplaceChild("left_shell",
                CubeListBuilder.create()
                        .texOffs(76, 24).addBox(-2.85F, -1.46F, -0.42F, 3.0F, 2.92F, 0.84F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(-2.12F, 0.10F, 0.54F, 0.0F, -12.0F * DEG_TO_RAD, 19.0F * DEG_TO_RAD));
        armor.addOrReplaceChild("right_shell",
                CubeListBuilder.create()
                        .texOffs(76, 31).addBox(-0.15F, -1.46F, -0.42F, 3.0F, 2.92F, 0.84F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(2.12F, 0.10F, 0.54F, 0.0F, 12.0F * DEG_TO_RAD, -19.0F * DEG_TO_RAD));
        armor.addOrReplaceChild("rear_crown",
                CubeListBuilder.create()
                        .texOffs(76, 38).addBox(-2.20F, -0.66F, 0.18F, 4.4F, 1.32F, 1.10F, new CubeDeformation(0.05F)),
                PartPose.offset(0.0F, -0.08F, 1.02F));

        root.addOrReplaceChild("head_glow",
                CubeListBuilder.create()
                        .texOffs(72, 12).addBox(-1.16F, -2.62F, -2.54F, 0.56F, 0.56F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(72, 14).addBox(0.64F, -2.62F, -2.54F, 0.56F, 0.56F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(72, 16).addBox(-0.14F, -3.85F, -2.42F, 0.28F, 3.0F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -20.25F, -0.24F, -3.0F * DEG_TO_RAD, 0.0F, 0.0F));
    }

    private static void addLimbs(PartDefinition root) {
        root.addOrReplaceChild("left_arm_skin",
                CubeListBuilder.create()
                        .texOffs(0, 44).addBox(-1.32F, -0.68F, -0.74F, 1.42F, 4.18F, 1.48F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(-2.88F, -16.42F, 0.04F, -0.22F, 0.0F, LEFT_ARM_Z_ROT));
        root.addOrReplaceChild("right_arm_skin",
                CubeListBuilder.create()
                        .texOffs(7, 44).addBox(-0.10F, -0.68F, -0.74F, 1.42F, 4.18F, 1.48F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(2.88F, -16.42F, 0.04F, -0.22F, 0.0F, RIGHT_ARM_Z_ROT));

        root.addOrReplaceChild("left_arm_suit",
                CubeListBuilder.create()
                        .texOffs(0, 51).addBox(-1.28F, 2.68F, -0.80F, 1.80F, 3.55F, 1.60F, new CubeDeformation(0.03F))
                        .texOffs(0, 57).addBox(-1.48F, 5.72F, -0.92F, 2.08F, 0.82F, 1.84F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(-2.88F, -16.42F, 0.04F, -0.22F, 0.0F, LEFT_ARM_Z_ROT));
        root.addOrReplaceChild("right_arm_suit",
                CubeListBuilder.create()
                        .texOffs(9, 51).addBox(-0.52F, 2.68F, -0.80F, 1.80F, 3.55F, 1.60F, new CubeDeformation(0.03F))
                        .texOffs(9, 57).addBox(-0.60F, 5.72F, -0.92F, 2.08F, 0.82F, 1.84F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(2.88F, -16.42F, 0.04F, -0.22F, 0.0F, RIGHT_ARM_Z_ROT));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(20, 44).addBox(-1.04F, -0.35F, -0.84F, 1.62F, 5.85F, 1.68F, new CubeDeformation(0.04F))
                        .texOffs(20, 52).addBox(-0.90F, 5.00F, -0.78F, 1.40F, 6.05F, 1.56F, new CubeDeformation(0.03F))
                        .texOffs(20, 60).addBox(-1.24F, 10.32F, -1.38F, 2.05F, 0.72F, 2.32F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(-0.82F, -8.65F, 0.03F, -4.0F * DEG_TO_RAD, 0.0F, 2.0F * DEG_TO_RAD));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(31, 44).addBox(-0.58F, -0.35F, -0.84F, 1.62F, 5.85F, 1.68F, new CubeDeformation(0.04F))
                        .texOffs(31, 52).addBox(-0.50F, 5.00F, -0.78F, 1.40F, 6.05F, 1.56F, new CubeDeformation(0.03F))
                        .texOffs(31, 60).addBox(-0.80F, 10.32F, -1.38F, 2.05F, 0.72F, 2.32F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(0.82F, -8.65F, 0.03F, 5.0F * DEG_TO_RAD, 0.0F, -2.0F * DEG_TO_RAD));

        root.addOrReplaceChild("leg_glow",
                CubeListBuilder.create()
                        .texOffs(72, 44).addBox(-1.58F, -6.10F, -0.94F, 0.22F, 3.7F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(72, 49).addBox(1.36F, -3.60F, -0.94F, 0.22F, 3.7F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(72, 54).addBox(-1.72F, 1.96F, -1.10F, 1.08F, 0.24F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(72, 56).addBox(0.64F, 1.96F, -1.10F, 1.08F, 0.24F, 0.20F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addArmor(PartDefinition root) {
        root.addOrReplaceChild("skirt_armor",
                CubeListBuilder.create()
                        .texOffs(44, 44).addBox(-3.95F, -9.18F, -0.92F, 1.82F, 3.82F, 2.06F, new CubeDeformation(0.08F))
                        .texOffs(44, 50).addBox(2.13F, -9.18F, -0.92F, 1.82F, 3.82F, 2.06F, new CubeDeformation(0.08F))
                        .texOffs(54, 44).addBox(-2.60F, -8.80F, 0.68F, 5.2F, 2.72F, 1.12F, new CubeDeformation(0.05F))
                        .texOffs(54, 49).addBox(-0.35F, -9.26F, -1.54F, 0.70F, 1.55F, 0.36F, new CubeDeformation(0.0F))
                        .texOffs(54, 52).addBox(-3.50F, -8.20F, -1.12F, 7.0F, 0.40F, 0.32F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("waist_deck",
                CubeListBuilder.create()
                        .texOffs(88, 44).addBox(-3.3F, -7.05F, -2.18F, 6.6F, 1.18F, 3.4F, new CubeDeformation(0.06F))
                        .texOffs(88, 50).addBox(-3.75F, -6.35F, -2.34F, 7.5F, 0.58F, 1.0F, new CubeDeformation(0.02F))
                        .texOffs(88, 53).addBox(-2.75F, -5.92F, -1.05F, 5.5F, 0.42F, 2.10F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(1.1F, 0.0F, -0.15F, 0.0F, -4.0F * DEG_TO_RAD, -1.0F * DEG_TO_RAD));

        PartDefinition back = root.addOrReplaceChild("back_armor",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -12.0F, 2.75F, 0.0F, 0.0F, 2.0F * DEG_TO_RAD));
        addPlate(back, "upper_cowl", 0.0F, -7.6F, 0.0F, -4.0F * DEG_TO_RAD, 12.8F, 4.0F, 0.82F);
        addPlate(back, "left_arc", -5.5F, -2.8F, 0.1F, 24.0F * DEG_TO_RAD, 4.8F, 9.4F, 0.86F);
        addPlate(back, "right_arc", 5.8F, -2.5F, 0.05F, -26.0F * DEG_TO_RAD, 5.0F, 9.8F, 0.86F);
        addPlate(back, "lower_cowl", 0.5F, 4.4F, -0.08F, 9.0F * DEG_TO_RAD, 10.6F, 3.7F, 0.84F);
        addPlate(back, "inner_shadow", 0.0F, -0.4F, -0.26F, 0.0F, 8.3F, 9.3F, 0.34F);

        root.addOrReplaceChild("back_armor_glow",
                CubeListBuilder.create()
                        .texOffs(72, 72).addBox(-6.85F, -18.90F, 2.34F, 0.28F, 5.5F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(72, 79).addBox(6.50F, -18.50F, 2.34F, 0.28F, 5.2F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(72, 86).addBox(-4.70F, -8.12F, 2.26F, 9.4F, 0.28F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(72, 89).addBox(-2.20F, -3.84F, 2.18F, 4.4F, 0.28F, 0.20F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        PartDefinition tail = root.addOrReplaceChild("segmented_tail",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(2.5F, -6.6F, 2.58F, 0.0F, 0.0F, -12.0F * DEG_TO_RAD));
        addTailSegment(tail, "seg_0", 0.0F, -0.2F, 0.0F, 0.0F, 4.7F, 2.6F);
        addTailSegment(tail, "seg_1", 2.9F, 1.4F, 0.0F, -13.0F * DEG_TO_RAD, 4.2F, 2.5F);
        addTailSegment(tail, "seg_2", 5.15F, 3.35F, 0.0F, -26.0F * DEG_TO_RAD, 3.9F, 2.35F);
        addTailSegment(tail, "seg_3", 6.20F, 5.72F, 0.0F, -43.0F * DEG_TO_RAD, 3.55F, 2.18F);
        addTailSegment(tail, "claw_0", 8.10F, 6.82F, -0.08F, -62.0F * DEG_TO_RAD, 2.4F, 1.55F);

        root.addOrReplaceChild("tail_glow",
                CubeListBuilder.create()
                        .texOffs(92, 72).addBox(0.05F, -7.58F, 2.20F, 0.28F, 4.35F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(92, 78).addBox(3.05F, -5.75F, 2.20F, 0.28F, 3.9F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(92, 84).addBox(5.50F, -3.35F, 2.20F, 0.28F, 3.5F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addPlate(PartDefinition parent, String name, float x, float y, float z,
                                 float zRot, float width, float height, float depth) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(0, 72).addBox(-width * 0.5F, -height * 0.5F, -depth * 0.5F,
                                width, height, depth, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(x, y, z, 0.0F, 0.0F, zRot));
    }

    private static void addTailSegment(PartDefinition parent, String name, float x, float y, float z,
                                       float zRot, float width, float height) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(48, 72).addBox(-width * 0.5F, -height * 0.5F, -0.48F,
                                width, height, 0.96F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(x, y, z, 0.0F, 0.0F, zRot));
    }

    private static void addMaw(PartDefinition root) {
        PartDefinition maw = root.addOrReplaceChild("left_maw",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-5.3F, -15.1F, -0.08F, 0.0F, MAW_Y_ROT, MAW_Z_ROT));
        maw.addOrReplaceChild("head_block",
                CubeListBuilder.create()
                        .texOffs(0, 88).addBox(-2.65F, -2.00F, -2.25F, 5.30F, 4.00F, 4.50F, new CubeDeformation(0.10F)),
                PartPose.ZERO);
        maw.addOrReplaceChild("upper_armor",
                CubeListBuilder.create()
                        .texOffs(20, 88).addBox(-2.95F, -3.10F, -2.48F, 5.9F, 2.15F, 4.96F, new CubeDeformation(0.06F)),
                PartPose.offsetAndRotation(-0.22F, -0.52F, -0.10F, 0.0F, 0.0F, -8.0F * DEG_TO_RAD));
        maw.addOrReplaceChild("lower_jaw",
                CubeListBuilder.create()
                        .texOffs(42, 88).addBox(-2.25F, -0.52F, -2.35F, 4.5F, 1.04F, 4.70F, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(0.35F, 2.03F, -0.05F, 0.0F, 0.0F, 11.0F * DEG_TO_RAD));
        maw.addOrReplaceChild("snout_plate",
                CubeListBuilder.create()
                        .texOffs(62, 88).addBox(-1.38F, -1.15F, -3.95F, 2.76F, 2.30F, 2.10F, new CubeDeformation(0.06F)),
                PartPose.offsetAndRotation(0.08F, -0.12F, -1.22F, -8.0F * DEG_TO_RAD, 0.0F, 0.0F));
        maw.addOrReplaceChild("left_cheek",
                CubeListBuilder.create()
                        .texOffs(78, 88).addBox(-0.62F, -1.85F, -1.95F, 1.24F, 3.70F, 3.90F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(-2.80F, 0.18F, 0.0F, 0.0F, 0.0F, 10.0F * DEG_TO_RAD));
        maw.addOrReplaceChild("right_cheek",
                CubeListBuilder.create()
                        .texOffs(88, 88).addBox(-0.62F, -1.85F, -1.95F, 1.24F, 3.70F, 3.90F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(2.78F, 0.18F, 0.0F, 0.0F, 0.0F, -10.0F * DEG_TO_RAD));
        maw.addOrReplaceChild("crown_plate",
                CubeListBuilder.create()
                        .texOffs(98, 88).addBox(-1.48F, -2.80F, -0.45F, 2.96F, 5.60F, 0.90F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(-0.20F, -2.70F, 0.96F, 0.0F, 0.0F, 38.0F * DEG_TO_RAD));

        root.addOrReplaceChild("maw_glow",
                CubeListBuilder.create()
                        .texOffs(72, 20).addBox(-5.98F, -16.32F, -3.32F, 1.38F, 1.38F, 0.28F, new CubeDeformation(0.0F))
                        .texOffs(72, 23).addBox(-7.28F, -16.88F, -1.72F, 0.48F, 0.62F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(72, 25).addBox(-3.96F, -16.70F, -1.72F, 0.48F, 0.62F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(72, 27).addBox(-6.65F, -17.10F, -3.50F, 2.70F, 0.22F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(72, 29).addBox(-5.45F, -15.72F, -3.54F, 0.32F, 1.45F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addPods(PartDefinition root) {
        root.addOrReplaceChild("left_pod",
                CubeListBuilder.create()
                        .texOffs(0, 104).addBox(-4.55F, -2.25F, -1.60F, 4.90F, 4.50F, 3.20F, new CubeDeformation(0.18F))
                        .texOffs(0, 113).addBox(-6.62F, -1.90F, -1.30F, 2.40F, 3.80F, 2.60F, new CubeDeformation(0.12F))
                        .texOffs(18, 104).addBox(-2.30F, -2.72F, -1.72F, 2.95F, 5.44F, 3.44F, new CubeDeformation(0.10F)),
                PartPose.offsetAndRotation(-6.70F, -14.30F, 1.20F, 0.0F, LEFT_POD_Y_ROT, LEFT_POD_Z_ROT));

        root.addOrReplaceChild("right_pod",
                CubeListBuilder.create()
                        .texOffs(34, 104).addBox(-0.35F, -2.25F, -1.60F, 4.90F, 4.50F, 3.20F, new CubeDeformation(0.18F))
                        .texOffs(34, 113).addBox(4.22F, -1.90F, -1.30F, 2.40F, 3.80F, 2.60F, new CubeDeformation(0.12F))
                        .texOffs(52, 104).addBox(-0.65F, -2.72F, -1.72F, 2.95F, 5.44F, 3.44F, new CubeDeformation(0.10F)),
                PartPose.offsetAndRotation(6.70F, -14.10F, 1.05F, 0.0F, RIGHT_POD_Y_ROT, RIGHT_POD_Z_ROT));

        root.addOrReplaceChild("pod_glow",
                CubeListBuilder.create()
                        .texOffs(72, 34).addBox(-11.45F, -14.52F, -0.66F, 0.32F, 2.65F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(72, 38).addBox(-10.62F, -15.86F, -0.68F, 2.35F, 0.28F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(72, 41).addBox(8.25F, -15.55F, -0.76F, 2.35F, 0.28F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(72, 43).addBox(10.95F, -14.16F, -0.72F, 0.32F, 2.50F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addTurrets(PartDefinition root) {
        addTripleTurret(root, "left_upper_turret", -10.05F, -16.35F, -1.22F,
                -3.0F * DEG_TO_RAD, LEFT_UPPER_TURRET_Y_ROT, -4.0F * DEG_TO_RAD, 1.08F);
        addTripleTurret(root, "right_upper_turret", 10.05F, -16.05F, -1.05F,
                -3.0F * DEG_TO_RAD, RIGHT_UPPER_TURRET_Y_ROT, 4.0F * DEG_TO_RAD, 1.06F);
        addTripleTurret(root, "left_lower_turret", -8.70F, -8.20F, -1.38F,
                -8.0F * DEG_TO_RAD, LEFT_LOWER_TURRET_Y_ROT, -13.0F * DEG_TO_RAD, 1.02F);
        addTripleTurret(root, "right_lower_turret", 3.95F, -6.42F, -1.98F,
                -8.0F * DEG_TO_RAD, RIGHT_LOWER_TURRET_Y_ROT, 1.0F * DEG_TO_RAD, 0.96F);
        addTripleTurret(root, "tail_turret", -2.35F, -3.70F, 2.08F,
                7.0F * DEG_TO_RAD, TAIL_TURRET_Y_ROT, -9.0F * DEG_TO_RAD, 0.86F);

        root.addOrReplaceChild("turret_glow",
                CubeListBuilder.create()
                        .texOffs(72, 104).addBox(-13.72F, -16.95F, -7.98F, 0.24F, 0.16F, 5.95F, new CubeDeformation(0.0F))
                        .texOffs(72, 111).addBox(-12.08F, -17.00F, -8.32F, 0.24F, 0.16F, 6.28F, new CubeDeformation(0.0F))
                        .texOffs(72, 118).addBox(-10.44F, -16.95F, -7.98F, 0.24F, 0.16F, 5.95F, new CubeDeformation(0.0F))
                        .texOffs(91, 104).addBox(10.26F, -16.68F, -7.92F, 0.24F, 0.16F, 5.85F, new CubeDeformation(0.0F))
                        .texOffs(91, 111).addBox(11.88F, -16.74F, -8.20F, 0.24F, 0.16F, 6.12F, new CubeDeformation(0.0F))
                        .texOffs(91, 118).addBox(13.50F, -16.68F, -7.92F, 0.24F, 0.16F, 5.85F, new CubeDeformation(0.0F))
                        .texOffs(110, 104).addBox(-11.86F, -8.70F, -7.68F, 0.22F, 0.16F, 5.48F, new CubeDeformation(0.0F))
                        .texOffs(110, 111).addBox(-10.32F, -8.82F, -8.02F, 0.22F, 0.16F, 5.78F, new CubeDeformation(0.0F))
                        .texOffs(110, 118).addBox(-8.78F, -8.70F, -7.68F, 0.22F, 0.16F, 5.48F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addTripleTurret(PartDefinition root, String name, float x, float y, float z,
                                        float xRot, float yRot, float zRot, float scale) {
        PartDefinition turret = root.addOrReplaceChild(name,
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(x, y, z, xRot, yRot, zRot));
        float houseW = 3.1F * scale;
        float houseH = 2.0F * scale;
        float houseD = 2.25F * scale;
        float barrelL = 6.2F * scale;
        float barrelT = 0.34F * scale;
        float spacing = 1.32F * scale;

        turret.addOrReplaceChild("house",
                CubeListBuilder.create()
                        .texOffs(0, 120).addBox(-houseW * 0.5F, -houseH * 0.5F, -houseD * 0.5F,
                                houseW, houseH, houseD, new CubeDeformation(0.12F * scale)),
                PartPose.ZERO);
        turret.addOrReplaceChild("front_lip",
                CubeListBuilder.create()
                        .texOffs(20, 120).addBox(-houseW * 0.62F, -houseH * 0.30F, -houseD * 0.64F,
                                houseW * 1.24F, houseH * 0.34F, houseD * 0.28F, new CubeDeformation(0.02F)),
                PartPose.ZERO);
        addBarrel(turret, "barrel_left", -spacing, -0.32F * scale, -houseD * 0.44F, barrelL, barrelT);
        addBarrel(turret, "barrel_center", 0.0F, -0.46F * scale, -houseD * 0.48F, barrelL * 1.08F, barrelT);
        addBarrel(turret, "barrel_right", spacing, -0.32F * scale, -houseD * 0.44F, barrelL, barrelT);
    }

    private static void addBarrel(PartDefinition parent, String name, float x, float y, float z,
                                  float length, float thickness) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(40, 120).addBox(-thickness * 0.5F, -thickness * 0.5F, -length,
                                thickness, thickness, length, new CubeDeformation(0.0F)),
                PartPose.offset(x, y, z));
    }

    @Override
    public void setupAnim(DeepOceanBattleshipEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float headYaw = netHeadYaw * DEG_TO_RAD * 0.38F;
        float headPitchRot = -3.0F * DEG_TO_RAD + headPitch * DEG_TO_RAD * 0.30F;
        setHeadRotation(headSkin, headYaw, headPitchRot);
        setHeadRotation(headHair, headYaw, headPitchRot);
        setHeadRotation(headHairShadow, headYaw, headPitchRot);
        setHeadRotation(headArmor, headYaw * 0.72F, headPitchRot);
        setHeadRotation(headGlow, headYaw, headPitchRot);

        float walk = Math.min(limbSwingAmount, 1.0F);
        float armSwing = Mth.cos(limbSwing * 0.6662F) * 0.20F * walk;
        setArmRotation(leftArmSkin, -0.22F - armSwing, LEFT_ARM_Z_ROT);
        setArmRotation(leftArmSuit, leftArmSkin.xRot, LEFT_ARM_Z_ROT);
        setArmRotation(rightArmSkin, -0.22F + armSwing, RIGHT_ARM_Z_ROT);
        setArmRotation(rightArmSuit, rightArmSkin.xRot, RIGHT_ARM_Z_ROT);

        leftLeg.xRot = -4.0F * DEG_TO_RAD + Mth.cos(limbSwing * 0.6662F + Mth.PI) * 0.22F * walk;
        rightLeg.xRot = 5.0F * DEG_TO_RAD + Mth.cos(limbSwing * 0.6662F) * 0.22F * walk;
        leftLeg.zRot = 2.0F * DEG_TO_RAD;
        rightLeg.zRot = -2.0F * DEG_TO_RAD;

        float pulse = Mth.sin(ageInTicks * 0.055F) * 0.045F;
        leftPod.yRot = LEFT_POD_Y_ROT + pulse;
        leftPod.zRot = LEFT_POD_Z_ROT + pulse * 0.30F;
        rightPod.yRot = RIGHT_POD_Y_ROT - pulse;
        rightPod.zRot = RIGHT_POD_Z_ROT - pulse * 0.30F;
        leftMaw.yRot = MAW_Y_ROT + pulse * 0.55F;
        leftMaw.zRot = MAW_Z_ROT + Mth.sin(ageInTicks * 0.070F) * 0.035F;

        backArmor.zRot = 2.0F * DEG_TO_RAD + Mth.sin(ageInTicks * 0.042F) * 0.040F;
        segmentedTail.zRot = -12.0F * DEG_TO_RAD + Mth.sin(ageInTicks * 0.050F) * 0.060F;

        leftUpperTurret.yRot = LEFT_UPPER_TURRET_Y_ROT + pulse * 0.50F;
        rightUpperTurret.yRot = RIGHT_UPPER_TURRET_Y_ROT - pulse * 0.50F;
        leftLowerTurret.yRot = LEFT_LOWER_TURRET_Y_ROT + pulse * 0.65F;
        rightLowerTurret.yRot = RIGHT_LOWER_TURRET_Y_ROT - pulse * 0.55F;
        tailTurret.yRot = TAIL_TURRET_Y_ROT + Mth.sin(ageInTicks * 0.047F) * 0.070F;
    }

    private static void setHeadRotation(ModelPart part, float yRot, float xRot) {
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = 0.0F;
    }

    private static void setArmRotation(ModelPart part, float xRot, float zRot) {
        part.xRot = xRot;
        part.yRot = 0.0F;
        part.zRot = zRot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, int color) {
        backArmor.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        segmentedTail.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_COLOR);
        backArmorGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        tailGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);

        leftPod.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        rightPod.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        leftMaw.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        podGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        mawGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);

        leftUpperTurret.render(poseStack, vertexConsumer, packedLight, packedOverlay, BARREL_COLOR);
        rightUpperTurret.render(poseStack, vertexConsumer, packedLight, packedOverlay, BARREL_COLOR);
        leftLowerTurret.render(poseStack, vertexConsumer, packedLight, packedOverlay, BARREL_COLOR);
        rightLowerTurret.render(poseStack, vertexConsumer, packedLight, packedOverlay, BARREL_COLOR);
        tailTurret.render(poseStack, vertexConsumer, packedLight, packedOverlay, BARREL_COLOR);
        turretGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);

        skirtArmor.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_COLOR);
        waistDeck.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_EDGE_COLOR);
        leftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, SUIT_DARK_COLOR);
        rightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, SUIT_DARK_COLOR);
        legGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);

        bodySkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        bodySuit.render(poseStack, vertexConsumer, packedLight, packedOverlay, SUIT_COLOR);
        bodyGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        leftArmSkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        rightArmSkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        leftArmSuit.render(poseStack, vertexConsumer, packedLight, packedOverlay, SUIT_DARK_COLOR);
        rightArmSuit.render(poseStack, vertexConsumer, packedLight, packedOverlay, SUIT_DARK_COLOR);

        headSkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        headHair.render(poseStack, vertexConsumer, packedLight, packedOverlay, HAIR_COLOR);
        headHairShadow.render(poseStack, vertexConsumer, packedLight, packedOverlay, HAIR_SHADOW_COLOR);
        headArmor.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_COLOR);
        headGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
    }
}
