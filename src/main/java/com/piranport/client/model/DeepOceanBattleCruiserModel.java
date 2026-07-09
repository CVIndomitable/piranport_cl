package com.piranport.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
import com.piranport.npc.deepocean.DeepOceanBattleCruiserEntity;
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

public class DeepOceanBattleCruiserModel extends EntityModel<DeepOceanBattleCruiserEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "deep_ocean_battle_cruiser"), "main");

    private static final float DEG_TO_RAD = Mth.PI / 180.0F;
    private static final int SKIN_COLOR = 0xFFE9DCD8;
    private static final int HAIR_COLOR = 0xFFE4E1E7;
    private static final int HAIR_SHADOW_COLOR = 0xFF8A8790;
    private static final int SUIT_COLOR = 0xFF19151C;
    private static final int SUIT_DARK_COLOR = 0xFF0D0B10;
    private static final int METAL_COLOR = 0xFF5C5660;
    private static final int METAL_DARK_COLOR = 0xFF2B2730;
    private static final int METAL_EDGE_COLOR = 0xFF827A84;
    private static final int BARREL_COLOR = 0xFF736B77;
    private static final int FIN_COLOR = 0xFF536170;
    private static final int GLOW_COLOR = 0xFFFF42C9;

    private static final float LEFT_ARM_Z_ROT = 11.0F * DEG_TO_RAD;
    private static final float RIGHT_ARM_Z_ROT = -9.0F * DEG_TO_RAD;
    private static final float MAW_Y_ROT = 22.0F * DEG_TO_RAD;
    private static final float MAW_Z_ROT = -8.0F * DEG_TO_RAD;
    private static final float RIGHT_FIN_Y_ROT = -24.0F * DEG_TO_RAD;
    private static final float RIGHT_FIN_Z_ROT = -17.0F * DEG_TO_RAD;
    private static final float FORE_TURRET_Y_ROT = 24.0F * DEG_TO_RAD;
    private static final float AFT_TURRET_Y_ROT = -21.0F * DEG_TO_RAD;
    private static final float SHOULDER_TURRET_Y_ROT = -29.0F * DEG_TO_RAD;

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
    private final ModelPart waistArmor;
    private final ModelPart backShell;
    private final ModelPart backShellGlow;
    private final ModelPart crescentTail;
    private final ModelPart tailGlow;
    private final ModelPart leftMaw;
    private final ModelPart rightFin;
    private final ModelPart finGlow;
    private final ModelPart foreTurret;
    private final ModelPart aftTurret;
    private final ModelPart shoulderTurret;
    private final ModelPart turretGlow;

    public DeepOceanBattleCruiserModel(ModelPart root) {
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
        this.waistArmor = modelRoot.getChild("waist_armor");
        this.backShell = modelRoot.getChild("back_shell");
        this.backShellGlow = modelRoot.getChild("back_shell_glow");
        this.crescentTail = modelRoot.getChild("crescent_tail");
        this.tailGlow = modelRoot.getChild("tail_glow");
        this.leftMaw = modelRoot.getChild("left_maw");
        this.rightFin = modelRoot.getChild("right_fin");
        this.finGlow = modelRoot.getChild("fin_glow");
        this.foreTurret = modelRoot.getChild("fore_turret");
        this.aftTurret = modelRoot.getChild("aft_turret");
        this.shoulderTurret = modelRoot.getChild("shoulder_turret");
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
        addBackShell(root);
        addSideArmor(root);
        addTurrets(root);

        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addBody(PartDefinition root) {
        root.addOrReplaceChild("body_skin",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.10F, -14.65F, -1.25F, 4.20F, 4.35F, 2.50F, new CubeDeformation(0.04F))
                        .texOffs(0, 8).addBox(-1.62F, -10.72F, -1.05F, 3.24F, 1.85F, 2.10F, new CubeDeformation(0.0F))
                        .texOffs(0, 13).addBox(-0.96F, -18.12F, -0.72F, 1.92F, 1.10F, 1.44F, new CubeDeformation(0.02F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body_suit",
                CubeListBuilder.create()
                        .texOffs(20, 0).addBox(-2.65F, -17.12F, -1.55F, 5.30F, 3.32F, 3.00F, new CubeDeformation(0.05F))
                        .texOffs(20, 7).addBox(-2.28F, -14.05F, -1.70F, 4.56F, 1.05F, 0.42F, new CubeDeformation(0.0F))
                        .texOffs(20, 10).addBox(-2.20F, -11.08F, -1.43F, 4.40F, 1.18F, 0.40F, new CubeDeformation(0.0F))
                        .texOffs(20, 13).addBox(-0.32F, -16.94F, -1.82F, 0.64F, 6.05F, 0.32F, new CubeDeformation(0.0F))
                        .texOffs(34, 7).addBox(-2.82F, -8.82F, -1.14F, 5.64F, 1.45F, 2.28F, new CubeDeformation(0.04F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body_glow",
                CubeListBuilder.create()
                        .texOffs(72, 0).addBox(-0.30F, -16.62F, -1.94F, 0.60F, 1.38F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(72, 3).addBox(-1.18F, -13.45F, -1.80F, 2.36F, 0.22F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(72, 5).addBox(1.25F, -10.62F, -1.38F, 0.22F, 1.65F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(72, 8).addBox(-0.22F, -8.70F, -1.38F, 0.44F, 0.66F, 0.20F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addHead(PartDefinition root) {
        root.addOrReplaceChild("head_skin",
                CubeListBuilder.create()
                        .texOffs(0, 24).addBox(-2.20F, -4.0F, -1.98F, 4.40F, 4.55F, 3.96F, new CubeDeformation(0.03F))
                        .texOffs(0, 34).addBox(-1.38F, -0.08F, -2.28F, 2.76F, 0.78F, 0.64F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -19.88F, -0.22F, -3.0F * DEG_TO_RAD, 0.0F, 0.0F));

        root.addOrReplaceChild("head_hair",
                CubeListBuilder.create()
                        .texOffs(18, 24).addBox(-2.56F, -4.36F, -1.76F, 5.12F, 1.48F, 3.90F, new CubeDeformation(0.10F))
                        .texOffs(18, 30).addBox(-2.94F, -3.08F, -1.32F, 1.24F, 6.70F, 2.08F, new CubeDeformation(0.08F))
                        .texOffs(26, 30).addBox(1.58F, -3.48F, -1.40F, 1.20F, 7.38F, 2.18F, new CubeDeformation(0.08F))
                        .texOffs(34, 30).addBox(-1.15F, -3.88F, -2.42F, 2.62F, 5.80F, 0.72F, new CubeDeformation(0.04F))
                        .texOffs(44, 24).addBox(0.58F, -2.10F, 1.78F, 1.25F, 6.20F, 0.72F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(0.0F, -19.88F, -0.22F, -3.0F * DEG_TO_RAD, 0.0F, 0.0F));

        root.addOrReplaceChild("head_hair_shadow",
                CubeListBuilder.create()
                        .texOffs(50, 30).addBox(-1.95F, -1.92F, -2.48F, 1.42F, 2.95F, 0.40F, new CubeDeformation(0.02F))
                        .texOffs(50, 35).addBox(0.25F, -3.36F, -2.48F, 0.68F, 2.28F, 0.40F, new CubeDeformation(0.02F))
                        .texOffs(50, 39).addBox(1.42F, -1.75F, -2.32F, 0.48F, 2.00F, 0.36F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -19.88F, -0.22F, -3.0F * DEG_TO_RAD, 0.0F, 0.0F));

        PartDefinition armor = root.addOrReplaceChild("head_armor",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -23.28F, -0.06F, -3.0F * DEG_TO_RAD, 0.0F, 0.0F));
        armor.addOrReplaceChild("left_shell",
                CubeListBuilder.create()
                        .texOffs(76, 24).addBox(-2.70F, -1.40F, -0.40F, 2.84F, 2.80F, 0.80F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(-2.02F, 0.08F, 0.50F, 0.0F, -14.0F * DEG_TO_RAD, 20.0F * DEG_TO_RAD));
        armor.addOrReplaceChild("right_shell",
                CubeListBuilder.create()
                        .texOffs(76, 31).addBox(-0.14F, -1.40F, -0.40F, 2.84F, 2.80F, 0.80F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(2.02F, 0.08F, 0.50F, 0.0F, 14.0F * DEG_TO_RAD, -20.0F * DEG_TO_RAD));
        armor.addOrReplaceChild("rear_crown",
                CubeListBuilder.create()
                        .texOffs(76, 38).addBox(-2.05F, -0.62F, 0.16F, 4.10F, 1.24F, 1.02F, new CubeDeformation(0.05F)),
                PartPose.offset(0.0F, -0.05F, 0.96F));

        root.addOrReplaceChild("head_glow",
                CubeListBuilder.create()
                        .texOffs(72, 12).addBox(-1.08F, -2.58F, -2.46F, 0.52F, 0.52F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(72, 14).addBox(0.60F, -2.58F, -2.46F, 0.52F, 0.52F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(72, 16).addBox(-0.13F, -3.74F, -2.34F, 0.26F, 2.82F, 0.20F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -19.88F, -0.22F, -3.0F * DEG_TO_RAD, 0.0F, 0.0F));
    }

    private static void addLimbs(PartDefinition root) {
        root.addOrReplaceChild("left_arm_skin",
                CubeListBuilder.create()
                        .texOffs(0, 44).addBox(-1.30F, -0.66F, -0.72F, 1.36F, 4.08F, 1.44F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(-2.70F, -16.22F, 0.02F, -0.24F, 0.0F, LEFT_ARM_Z_ROT));
        root.addOrReplaceChild("right_arm_skin",
                CubeListBuilder.create()
                        .texOffs(7, 44).addBox(-0.06F, -0.66F, -0.72F, 1.36F, 4.08F, 1.44F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(2.70F, -16.22F, 0.02F, -0.24F, 0.0F, RIGHT_ARM_Z_ROT));

        root.addOrReplaceChild("left_arm_suit",
                CubeListBuilder.create()
                        .texOffs(0, 51).addBox(-1.24F, 2.58F, -0.78F, 1.72F, 3.38F, 1.56F, new CubeDeformation(0.03F))
                        .texOffs(0, 57).addBox(-1.44F, 5.50F, -0.90F, 2.00F, 0.78F, 1.80F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(-2.70F, -16.22F, 0.02F, -0.24F, 0.0F, LEFT_ARM_Z_ROT));
        root.addOrReplaceChild("right_arm_suit",
                CubeListBuilder.create()
                        .texOffs(9, 51).addBox(-0.48F, 2.58F, -0.78F, 1.72F, 3.38F, 1.56F, new CubeDeformation(0.03F))
                        .texOffs(9, 57).addBox(-0.56F, 5.50F, -0.90F, 2.00F, 0.78F, 1.80F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(2.70F, -16.22F, 0.02F, -0.24F, 0.0F, RIGHT_ARM_Z_ROT));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(20, 44).addBox(-0.98F, -0.34F, -0.80F, 1.55F, 5.55F, 1.60F, new CubeDeformation(0.04F))
                        .texOffs(20, 52).addBox(-0.84F, 4.76F, -0.74F, 1.34F, 5.74F, 1.48F, new CubeDeformation(0.03F))
                        .texOffs(20, 60).addBox(-1.18F, 9.98F, -1.28F, 1.94F, 0.70F, 2.20F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(-0.78F, -8.58F, 0.02F, -4.0F * DEG_TO_RAD, 0.0F, 3.0F * DEG_TO_RAD));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(31, 44).addBox(-0.57F, -0.34F, -0.80F, 1.55F, 5.55F, 1.60F, new CubeDeformation(0.04F))
                        .texOffs(31, 52).addBox(-0.50F, 4.76F, -0.74F, 1.34F, 5.74F, 1.48F, new CubeDeformation(0.03F))
                        .texOffs(31, 60).addBox(-0.74F, 9.98F, -1.28F, 1.94F, 0.70F, 2.20F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(0.78F, -8.58F, 0.02F, 5.0F * DEG_TO_RAD, 0.0F, -3.0F * DEG_TO_RAD));

        root.addOrReplaceChild("leg_glow",
                CubeListBuilder.create()
                        .texOffs(72, 44).addBox(-1.50F, -5.88F, -0.90F, 0.22F, 3.42F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(72, 49).addBox(1.28F, -3.38F, -0.90F, 0.22F, 3.42F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(72, 54).addBox(-1.64F, 1.80F, -1.04F, 1.02F, 0.24F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(72, 56).addBox(0.62F, 1.80F, -1.04F, 1.02F, 0.24F, 0.20F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addArmor(PartDefinition root) {
        root.addOrReplaceChild("waist_armor",
                CubeListBuilder.create()
                        .texOffs(44, 44).addBox(-3.55F, -9.02F, -0.86F, 1.58F, 3.52F, 1.92F, new CubeDeformation(0.08F))
                        .texOffs(44, 50).addBox(1.97F, -9.02F, -0.86F, 1.58F, 3.52F, 1.92F, new CubeDeformation(0.08F))
                        .texOffs(54, 44).addBox(-2.30F, -8.68F, 0.62F, 4.60F, 2.48F, 1.04F, new CubeDeformation(0.05F))
                        .texOffs(54, 49).addBox(-0.32F, -9.10F, -1.42F, 0.64F, 1.36F, 0.34F, new CubeDeformation(0.0F))
                        .texOffs(54, 52).addBox(-3.12F, -8.10F, -1.08F, 6.24F, 0.36F, 0.30F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addBackShell(PartDefinition root) {
        PartDefinition shell = root.addOrReplaceChild("back_shell",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.9F, -12.05F, 2.86F, 0.0F, 0.0F, -7.0F * DEG_TO_RAD));
        addPlate(shell, "upper_dome", 0.6F, -7.10F, 0.0F, -3.0F * DEG_TO_RAD, 11.7F, 3.70F, 0.82F);
        addPlate(shell, "left_curl", -4.80F, -2.45F, 0.10F, 25.0F * DEG_TO_RAD, 4.50F, 8.75F, 0.86F);
        addPlate(shell, "right_curl", 5.20F, -2.15F, 0.04F, -27.0F * DEG_TO_RAD, 4.80F, 9.10F, 0.86F);
        addPlate(shell, "lower_sweep", 0.52F, 4.14F, -0.06F, 10.0F * DEG_TO_RAD, 9.90F, 3.45F, 0.84F);
        addPlate(shell, "inner_shadow", 0.0F, -0.35F, -0.26F, 0.0F, 7.70F, 8.65F, 0.34F);
        addRib(shell, "rib_0", 0.35F, -5.45F, 0.52F, -2.0F * DEG_TO_RAD, 9.40F);
        addRib(shell, "rib_1", 0.10F, -2.58F, 0.54F, 4.0F * DEG_TO_RAD, 8.95F);
        addRib(shell, "rib_2", 0.28F, 0.38F, 0.54F, 8.0F * DEG_TO_RAD, 8.40F);
        addRib(shell, "rib_3", 0.74F, 3.00F, 0.54F, 14.0F * DEG_TO_RAD, 7.20F);

        root.addOrReplaceChild("back_shell_glow",
                CubeListBuilder.create()
                        .texOffs(72, 72).addBox(-5.95F, -18.25F, 2.40F, 0.26F, 5.10F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(72, 79).addBox(6.10F, -17.92F, 2.40F, 0.26F, 4.88F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(72, 86).addBox(-4.32F, -8.00F, 2.32F, 8.64F, 0.26F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(72, 89).addBox(-1.92F, -3.92F, 2.25F, 3.84F, 0.26F, 0.20F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        PartDefinition tail = root.addOrReplaceChild("crescent_tail",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(2.10F, -6.45F, 2.60F, 0.0F, 0.0F, -16.0F * DEG_TO_RAD));
        addTailSegment(tail, "seg_0", 0.00F, -0.10F, 0.0F, 0.0F, 4.30F, 2.34F);
        addTailSegment(tail, "seg_1", 2.60F, 1.22F, 0.0F, -14.0F * DEG_TO_RAD, 3.92F, 2.25F);
        addTailSegment(tail, "seg_2", 4.70F, 3.05F, 0.0F, -28.0F * DEG_TO_RAD, 3.62F, 2.10F);
        addTailSegment(tail, "seg_3", 5.72F, 5.18F, 0.0F, -47.0F * DEG_TO_RAD, 3.25F, 1.92F);
        addTailSegment(tail, "hook", 7.28F, 6.16F, -0.05F, -66.0F * DEG_TO_RAD, 2.12F, 1.42F);

        root.addOrReplaceChild("tail_glow",
                CubeListBuilder.create()
                        .texOffs(92, 72).addBox(-0.05F, -7.38F, 2.24F, 0.26F, 4.05F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(92, 78).addBox(2.78F, -5.70F, 2.24F, 0.26F, 3.60F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(92, 84).addBox(5.05F, -3.52F, 2.24F, 0.26F, 3.10F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(92, 89).addBox(7.03F, -1.08F, 2.24F, 0.24F, 2.25F, 0.22F, new CubeDeformation(0.0F)),
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

    private static void addRib(PartDefinition parent, String name, float x, float y, float z,
                               float zRot, float width) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(30, 72).addBox(-width * 0.5F, -0.16F, -0.14F,
                                width, 0.32F, 0.28F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(x, y, z, 0.0F, 0.0F, zRot));
    }

    private static void addTailSegment(PartDefinition parent, String name, float x, float y, float z,
                                       float zRot, float width, float height) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(48, 72).addBox(-width * 0.5F, -height * 0.5F, -0.46F,
                                width, height, 0.92F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(x, y, z, 0.0F, 0.0F, zRot));
    }

    private static void addSideArmor(PartDefinition root) {
        PartDefinition maw = root.addOrReplaceChild("left_maw",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-5.10F, -14.58F, -0.06F, 0.0F, MAW_Y_ROT, MAW_Z_ROT));
        maw.addOrReplaceChild("hinge",
                CubeListBuilder.create()
                        .texOffs(0, 88).addBox(-1.20F, -2.10F, -1.70F, 2.40F, 4.20F, 3.40F, new CubeDeformation(0.10F)),
                PartPose.ZERO);
        maw.addOrReplaceChild("upper_jaw",
                CubeListBuilder.create()
                        .texOffs(16, 88).addBox(-3.05F, -1.10F, -2.20F, 6.10F, 2.20F, 4.40F, new CubeDeformation(0.06F)),
                PartPose.offsetAndRotation(-1.96F, -1.70F, -0.08F, 0.0F, 0.0F, -11.0F * DEG_TO_RAD));
        maw.addOrReplaceChild("lower_jaw",
                CubeListBuilder.create()
                        .texOffs(40, 88).addBox(-2.72F, -0.52F, -2.05F, 5.44F, 1.04F, 4.10F, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(-1.88F, 1.42F, -0.04F, 0.0F, 0.0F, 9.0F * DEG_TO_RAD));
        maw.addOrReplaceChild("snout",
                CubeListBuilder.create()
                        .texOffs(62, 88).addBox(-1.24F, -1.05F, -3.68F, 2.48F, 2.10F, 1.96F, new CubeDeformation(0.06F)),
                PartPose.offsetAndRotation(-0.10F, -0.06F, -1.18F, -7.0F * DEG_TO_RAD, 0.0F, 0.0F));
        maw.addOrReplaceChild("crown_plate",
                CubeListBuilder.create()
                        .texOffs(78, 88).addBox(-1.25F, -2.56F, -0.42F, 2.50F, 5.12F, 0.84F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(-0.24F, -2.40F, 0.82F, 0.0F, 0.0F, 36.0F * DEG_TO_RAD));

        root.addOrReplaceChild("right_fin",
                CubeListBuilder.create()
                        .texOffs(0, 104).addBox(-1.40F, -4.80F, -0.50F, 2.80F, 9.60F, 1.00F, new CubeDeformation(0.07F))
                        .texOffs(10, 104).addBox(-3.18F, -2.40F, -0.42F, 6.36F, 4.80F, 0.84F, new CubeDeformation(0.05F))
                        .texOffs(28, 104).addBox(-2.60F, 2.05F, -0.38F, 5.20F, 2.44F, 0.76F, new CubeDeformation(0.04F))
                        .texOffs(46, 104).addBox(-0.38F, -5.25F, -0.56F, 0.76F, 10.50F, 1.12F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(5.28F, -10.92F, 1.45F, 0.0F, RIGHT_FIN_Y_ROT, RIGHT_FIN_Z_ROT));

        root.addOrReplaceChild("fin_glow",
                CubeListBuilder.create()
                        .texOffs(72, 20).addBox(-7.32F, -16.42F, -3.20F, 1.22F, 1.22F, 0.26F, new CubeDeformation(0.0F))
                        .texOffs(72, 23).addBox(-8.40F, -16.92F, -1.54F, 0.44F, 0.58F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(72, 25).addBox(-5.42F, -16.78F, -1.54F, 0.44F, 0.58F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(72, 27).addBox(-7.98F, -17.05F, -3.38F, 2.42F, 0.22F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(72, 34).addBox(5.60F, -13.80F, 0.60F, 2.32F, 0.26F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(72, 37).addBox(7.82F, -12.70F, 0.58F, 0.28F, 2.14F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addTurrets(PartDefinition root) {
        addTripleTurret(root, "fore_turret", -8.35F, -17.18F, -1.10F,
                -3.0F * DEG_TO_RAD, FORE_TURRET_Y_ROT, -5.0F * DEG_TO_RAD, 0.98F);
        addTripleTurret(root, "aft_turret", 6.50F, -18.02F, -0.88F,
                -2.0F * DEG_TO_RAD, AFT_TURRET_Y_ROT, 5.0F * DEG_TO_RAD, 0.96F);
        addTripleTurret(root, "shoulder_turret", 7.02F, -13.32F, 0.82F,
                -5.0F * DEG_TO_RAD, SHOULDER_TURRET_Y_ROT, -4.0F * DEG_TO_RAD, 0.86F);

        root.addOrReplaceChild("turret_glow",
                CubeListBuilder.create()
                        .texOffs(72, 104).addBox(-11.58F, -17.70F, -7.66F, 0.24F, 0.16F, 5.48F, new CubeDeformation(0.0F))
                        .texOffs(72, 111).addBox(-10.14F, -17.76F, -7.98F, 0.24F, 0.16F, 5.78F, new CubeDeformation(0.0F))
                        .texOffs(72, 118).addBox(-8.70F, -17.70F, -7.66F, 0.24F, 0.16F, 5.48F, new CubeDeformation(0.0F))
                        .texOffs(91, 104).addBox(6.42F, -18.52F, -7.26F, 0.24F, 0.16F, 5.28F, new CubeDeformation(0.0F))
                        .texOffs(91, 111).addBox(7.82F, -18.58F, -7.52F, 0.24F, 0.16F, 5.58F, new CubeDeformation(0.0F))
                        .texOffs(91, 118).addBox(9.22F, -18.52F, -7.26F, 0.24F, 0.16F, 5.28F, new CubeDeformation(0.0F))
                        .texOffs(110, 104).addBox(6.20F, -13.84F, -5.28F, 0.22F, 0.16F, 4.26F, new CubeDeformation(0.0F))
                        .texOffs(110, 111).addBox(7.42F, -13.90F, -5.50F, 0.22F, 0.16F, 4.52F, new CubeDeformation(0.0F))
                        .texOffs(110, 118).addBox(8.64F, -13.84F, -5.28F, 0.22F, 0.16F, 4.26F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addTripleTurret(PartDefinition root, String name, float x, float y, float z,
                                        float xRot, float yRot, float zRot, float scale) {
        PartDefinition turret = root.addOrReplaceChild(name,
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(x, y, z, xRot, yRot, zRot));
        float houseW = 2.90F * scale;
        float houseH = 1.82F * scale;
        float houseD = 2.08F * scale;
        float barrelL = 5.90F * scale;
        float barrelT = 0.32F * scale;
        float spacing = 1.22F * scale;

        turret.addOrReplaceChild("house",
                CubeListBuilder.create()
                        .texOffs(0, 120).addBox(-houseW * 0.5F, -houseH * 0.5F, -houseD * 0.5F,
                                houseW, houseH, houseD, new CubeDeformation(0.11F * scale)),
                PartPose.ZERO);
        turret.addOrReplaceChild("front_lip",
                CubeListBuilder.create()
                        .texOffs(20, 120).addBox(-houseW * 0.62F, -houseH * 0.30F, -houseD * 0.64F,
                                houseW * 1.24F, houseH * 0.32F, houseD * 0.28F, new CubeDeformation(0.02F)),
                PartPose.ZERO);
        addBarrel(turret, "barrel_left", -spacing, -0.30F * scale, -houseD * 0.44F, barrelL, barrelT);
        addBarrel(turret, "barrel_center", 0.0F, -0.42F * scale, -houseD * 0.48F, barrelL * 1.08F, barrelT);
        addBarrel(turret, "barrel_right", spacing, -0.30F * scale, -houseD * 0.44F, barrelL, barrelT);
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
    public void setupAnim(DeepOceanBattleCruiserEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float headYaw = netHeadYaw * DEG_TO_RAD * 0.40F;
        float headPitchRot = -3.0F * DEG_TO_RAD + headPitch * DEG_TO_RAD * 0.31F;
        setHeadRotation(headSkin, headYaw, headPitchRot);
        setHeadRotation(headHair, headYaw, headPitchRot);
        setHeadRotation(headHairShadow, headYaw, headPitchRot);
        setHeadRotation(headArmor, headYaw * 0.74F, headPitchRot);
        setHeadRotation(headGlow, headYaw, headPitchRot);

        float walk = Math.min(limbSwingAmount, 1.0F);
        float armSwing = Mth.cos(limbSwing * 0.6662F) * 0.23F * walk;
        setArmRotation(leftArmSkin, -0.24F - armSwing, LEFT_ARM_Z_ROT);
        setArmRotation(leftArmSuit, leftArmSkin.xRot, LEFT_ARM_Z_ROT);
        setArmRotation(rightArmSkin, -0.24F + armSwing, RIGHT_ARM_Z_ROT);
        setArmRotation(rightArmSuit, rightArmSkin.xRot, RIGHT_ARM_Z_ROT);

        leftLeg.xRot = -4.0F * DEG_TO_RAD + Mth.cos(limbSwing * 0.6662F + Mth.PI) * 0.24F * walk;
        rightLeg.xRot = 5.0F * DEG_TO_RAD + Mth.cos(limbSwing * 0.6662F) * 0.24F * walk;
        leftLeg.zRot = 3.0F * DEG_TO_RAD;
        rightLeg.zRot = -3.0F * DEG_TO_RAD;

        float pulse = Mth.sin(ageInTicks * 0.060F) * 0.050F;
        backShell.zRot = -7.0F * DEG_TO_RAD + Mth.sin(ageInTicks * 0.044F) * 0.038F;
        crescentTail.zRot = -16.0F * DEG_TO_RAD + Mth.sin(ageInTicks * 0.055F) * 0.065F;
        leftMaw.yRot = MAW_Y_ROT + pulse * 0.60F;
        leftMaw.zRot = MAW_Z_ROT + Mth.sin(ageInTicks * 0.073F) * 0.034F;
        rightFin.yRot = RIGHT_FIN_Y_ROT - pulse * 0.46F;
        rightFin.zRot = RIGHT_FIN_Z_ROT - pulse * 0.28F;

        foreTurret.yRot = FORE_TURRET_Y_ROT + pulse * 0.60F;
        aftTurret.yRot = AFT_TURRET_Y_ROT - pulse * 0.55F;
        shoulderTurret.yRot = SHOULDER_TURRET_Y_ROT - Mth.sin(ageInTicks * 0.050F) * 0.066F;
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
        backShell.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        crescentTail.render(poseStack, vertexConsumer, packedLight, packedOverlay, FIN_COLOR);
        backShellGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        tailGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);

        leftMaw.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        rightFin.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_COLOR);
        finGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);

        foreTurret.render(poseStack, vertexConsumer, packedLight, packedOverlay, BARREL_COLOR);
        aftTurret.render(poseStack, vertexConsumer, packedLight, packedOverlay, BARREL_COLOR);
        shoulderTurret.render(poseStack, vertexConsumer, packedLight, packedOverlay, BARREL_COLOR);
        turretGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);

        waistArmor.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_EDGE_COLOR);
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
