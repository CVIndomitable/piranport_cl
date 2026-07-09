package com.piranport.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
import com.piranport.npc.deepocean.DeepOceanSubmarineEntity;
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

public class DeepOceanSubmarineModel extends EntityModel<DeepOceanSubmarineEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "deep_ocean_submarine"), "main");

    private static final float DEG_TO_RAD = Mth.PI / 180.0F;
    private static final int SKIN_COLOR = 0xFFE8D6D2;
    private static final int HAIR_COLOR = 0xFFD4CDD2;
    private static final int HAIR_SHADOW_COLOR = 0xFF9A8E98;
    private static final int SUIT_COLOR = 0xFF1A171E;
    private static final int DARK_COLOR = 0xFF24212A;
    private static final int METAL_COLOR = 0xFF4D4750;
    private static final int METAL_DARK_COLOR = 0xFF2E2A32;
    private static final int TENTACLE_COLOR = 0xFF5F5964;
    private static final int TENTACLE_UNDERSIDE_COLOR = 0xFFA9A2AD;
    private static final int GLOW_COLOR = 0xFFFF43C9;

    private static final float HEAD_BASE_X_ROT = -7.0F * DEG_TO_RAD;
    private static final float LEFT_ARM_Z_ROT = 13.0F * DEG_TO_RAD;
    private static final float RIGHT_ARM_Z_ROT = -17.0F * DEG_TO_RAD;
    private static final float LEFT_LEG_X_ROT = -35.0F * DEG_TO_RAD;
    private static final float RIGHT_LEG_X_ROT = -45.0F * DEG_TO_RAD;
    private static final float LEFT_LEG_Z_ROT = 9.0F * DEG_TO_RAD;
    private static final float RIGHT_LEG_Z_ROT = -13.0F * DEG_TO_RAD;
    private static final float LEFT_HULL_Y_ROT = 13.0F * DEG_TO_RAD;
    private static final float RIGHT_HULL_Y_ROT = -18.0F * DEG_TO_RAD;
    private static final float LEFT_HULL_Z_ROT = -8.0F * DEG_TO_RAD;
    private static final float RIGHT_HULL_Z_ROT = 10.0F * DEG_TO_RAD;
    private static final float TORPEDO_Y_ROT = -4.0F * DEG_TO_RAD;
    private static final float TORPEDO_Z_ROT = 7.0F * DEG_TO_RAD;

    private final ModelPart bodySkin;
    private final ModelPart bodySuit;
    private final ModelPart bodyGlow;
    private final ModelPart collar;
    private final ModelPart headSkin;
    private final ModelPart headHair;
    private final ModelPart hairShadow;
    private final ModelPart visorGlow;
    private final ModelPart headArmor;
    private final ModelPart periscopeGlow;
    private final ModelPart leftArmSkin;
    private final ModelPart rightArmSkin;
    private final ModelPart leftGlove;
    private final ModelPart rightGlove;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftFootFin;
    private final ModelPart rightFootFin;
    private final ModelPart footGlow;
    private final ModelPart leftSubHull;
    private final ModelPart rightSubHull;
    private final ModelPart leftSubGlow;
    private final ModelPart rightSubGlow;
    private final ModelPart torpedoRack;
    private final ModelPart torpedoGlow;
    private final ModelPart dorsalFin;
    private final ModelPart tentacles;
    private final ModelPart tentacleUnderside;
    private final ModelPart tentacleGlow;

    public DeepOceanSubmarineModel(ModelPart root) {
        ModelPart modelRoot = root.getChild("root");
        this.bodySkin = modelRoot.getChild("body_skin");
        this.bodySuit = modelRoot.getChild("body_suit");
        this.bodyGlow = modelRoot.getChild("body_glow");
        this.collar = modelRoot.getChild("collar");
        this.headSkin = modelRoot.getChild("head_skin");
        this.headHair = modelRoot.getChild("head_hair");
        this.hairShadow = modelRoot.getChild("hair_shadow");
        this.visorGlow = modelRoot.getChild("visor_glow");
        this.headArmor = modelRoot.getChild("head_armor");
        this.periscopeGlow = modelRoot.getChild("periscope_glow");
        this.leftArmSkin = modelRoot.getChild("left_arm_skin");
        this.rightArmSkin = modelRoot.getChild("right_arm_skin");
        this.leftGlove = modelRoot.getChild("left_glove");
        this.rightGlove = modelRoot.getChild("right_glove");
        this.leftLeg = modelRoot.getChild("left_leg");
        this.rightLeg = modelRoot.getChild("right_leg");
        this.leftFootFin = modelRoot.getChild("left_foot_fin");
        this.rightFootFin = modelRoot.getChild("right_foot_fin");
        this.footGlow = modelRoot.getChild("foot_glow");
        this.leftSubHull = modelRoot.getChild("left_sub_hull");
        this.rightSubHull = modelRoot.getChild("right_sub_hull");
        this.leftSubGlow = modelRoot.getChild("left_sub_glow");
        this.rightSubGlow = modelRoot.getChild("right_sub_glow");
        this.torpedoRack = modelRoot.getChild("torpedo_rack");
        this.torpedoGlow = modelRoot.getChild("torpedo_glow");
        this.dorsalFin = modelRoot.getChild("dorsal_fin");
        this.tentacles = modelRoot.getChild("tentacles");
        this.tentacleUnderside = modelRoot.getChild("tentacle_underside");
        this.tentacleGlow = modelRoot.getChild("tentacle_glow");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root",
                CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        addBody(root);
        addHead(root);
        addLimbs(root);
        addRigging(root);
        addTentacles(root);

        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addBody(PartDefinition root) {
        root.addOrReplaceChild("body_skin",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.15F, -15.3F, -1.25F, 4.3F, 4.35F, 2.45F, new CubeDeformation(0.04F))
                        .texOffs(0, 8).addBox(-1.70F, -11.25F, -1.05F, 3.4F, 2.25F, 2.05F, new CubeDeformation(0.0F))
                        .texOffs(0, 14).addBox(-1.30F, -9.35F, -0.95F, 2.6F, 1.35F, 1.90F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body_suit",
                CubeListBuilder.create()
                        .texOffs(22, 0).addBox(-2.55F, -17.4F, -1.62F, 5.10F, 3.8F, 3.12F, new CubeDeformation(0.04F))
                        .texOffs(22, 8).addBox(-2.48F, -14.20F, -1.70F, 4.96F, 1.05F, 0.42F, new CubeDeformation(0.0F))
                        .texOffs(22, 11).addBox(-2.30F, -12.10F, -1.55F, 4.60F, 0.85F, 0.38F, new CubeDeformation(0.0F))
                        .texOffs(22, 14).addBox(-2.05F, -8.65F, -1.17F, 4.10F, 1.05F, 2.20F, new CubeDeformation(0.03F))
                        .texOffs(38, 0).addBox(-0.35F, -16.85F, -1.90F, 0.70F, 5.65F, 0.32F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body_glow",
                CubeListBuilder.create()
                        .texOffs(64, 0).addBox(-1.22F, -14.62F, -1.98F, 2.44F, 0.24F, 0.25F, new CubeDeformation(0.0F))
                        .texOffs(64, 3).addBox(-0.28F, -16.45F, -2.05F, 0.56F, 4.85F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(64, 9).addBox(-0.95F, -8.75F, -1.32F, 1.90F, 0.26F, 0.24F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("collar",
                CubeListBuilder.create()
                        .texOffs(42, 0).addBox(-3.0F, -18.15F, -1.65F, 6.0F, 1.35F, 3.25F, new CubeDeformation(0.09F))
                        .texOffs(42, 6).addBox(-3.2F, -17.78F, -1.95F, 6.4F, 0.44F, 0.48F, new CubeDeformation(0.0F))
                        .texOffs(42, 8).addBox(-0.34F, -18.55F, -2.06F, 0.68F, 1.45F, 0.40F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addHead(PartDefinition root) {
        root.addOrReplaceChild("head_skin",
                CubeListBuilder.create()
                        .texOffs(0, 24).addBox(-2.25F, -4.05F, -2.02F, 4.5F, 4.65F, 4.04F, new CubeDeformation(0.03F))
                        .texOffs(0, 34).addBox(-1.20F, -0.18F, -2.32F, 2.40F, 0.82F, 0.72F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -20.65F, -0.24F, HEAD_BASE_X_ROT, 0.0F, 0.0F));

        root.addOrReplaceChild("head_hair",
                CubeListBuilder.create()
                        .texOffs(20, 24).addBox(-2.65F, -4.45F, -1.82F, 5.30F, 1.55F, 4.05F, new CubeDeformation(0.10F))
                        .texOffs(20, 30).addBox(-2.85F, -3.25F, -1.55F, 1.30F, 5.85F, 2.38F, new CubeDeformation(0.08F))
                        .texOffs(28, 30).addBox(1.50F, -3.55F, -1.45F, 1.35F, 6.20F, 2.34F, new CubeDeformation(0.08F))
                        .texOffs(36, 30).addBox(-1.42F, -3.70F, -2.45F, 2.95F, 4.35F, 0.76F, new CubeDeformation(0.04F))
                        .texOffs(48, 24).addBox(1.02F, -2.75F, -2.35F, 1.25F, 3.20F, 0.62F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(0.0F, -20.65F, -0.24F, HEAD_BASE_X_ROT, 0.0F, 0.0F));

        root.addOrReplaceChild("hair_shadow",
                CubeListBuilder.create()
                        .texOffs(52, 30).addBox(-2.02F, -2.30F, -2.52F, 1.02F, 3.15F, 0.42F, new CubeDeformation(0.02F))
                        .texOffs(52, 35).addBox(-0.76F, -3.02F, -2.55F, 0.82F, 3.45F, 0.42F, new CubeDeformation(0.02F))
                        .texOffs(52, 40).addBox(0.35F, -3.38F, -2.55F, 0.86F, 3.65F, 0.42F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(0.0F, -20.65F, -0.24F, HEAD_BASE_X_ROT, 0.0F, 0.0F));

        root.addOrReplaceChild("visor_glow",
                CubeListBuilder.create()
                        .texOffs(64, 14).addBox(-1.72F, -2.84F, -2.58F, 3.44F, 0.34F, 0.25F, new CubeDeformation(0.0F))
                        .texOffs(64, 16).addBox(-1.58F, -2.18F, -2.62F, 3.16F, 0.32F, 0.25F, new CubeDeformation(0.0F))
                        .texOffs(64, 18).addBox(-1.30F, -1.54F, -2.64F, 2.60F, 0.30F, 0.25F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -20.65F, -0.24F, HEAD_BASE_X_ROT, 0.0F, 0.0F));

        PartDefinition armor = root.addOrReplaceChild("head_armor",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -24.40F, 0.04F, HEAD_BASE_X_ROT, 0.0F, 0.0F));

        armor.addOrReplaceChild("cap",
                CubeListBuilder.create()
                        .texOffs(80, 24).addBox(-2.12F, -0.58F, -1.45F, 4.24F, 1.16F, 2.90F, new CubeDeformation(0.08F)),
                PartPose.ZERO);
        armor.addOrReplaceChild("periscope_base",
                CubeListBuilder.create()
                        .texOffs(80, 30).addBox(-0.55F, -2.42F, -0.48F, 1.10F, 2.05F, 0.96F, new CubeDeformation(0.03F)),
                PartPose.offset(0.62F, -0.05F, 0.12F));
        armor.addOrReplaceChild("periscope_lens",
                CubeListBuilder.create()
                        .texOffs(80, 34).addBox(-0.74F, -0.32F, -0.44F, 1.48F, 0.64F, 0.88F, new CubeDeformation(0.02F)),
                PartPose.offset(0.62F, -2.25F, -0.62F));

        root.addOrReplaceChild("periscope_glow",
                CubeListBuilder.create()
                        .texOffs(64, 22).addBox(-0.82F, -26.85F, -1.06F, 1.64F, 0.34F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(64, 24).addBox(0.18F, -24.90F, -1.02F, 0.88F, 0.22F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addLimbs(PartDefinition root) {
        root.addOrReplaceChild("left_arm_skin",
                CubeListBuilder.create()
                        .texOffs(0, 46).addBox(-1.35F, -0.62F, -0.76F, 1.48F, 4.15F, 1.52F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(-2.62F, -16.15F, -0.05F, -16.0F * DEG_TO_RAD, 0.0F, LEFT_ARM_Z_ROT));
        root.addOrReplaceChild("right_arm_skin",
                CubeListBuilder.create()
                        .texOffs(8, 46).addBox(-0.13F, -0.62F, -0.76F, 1.48F, 4.15F, 1.52F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(2.62F, -16.15F, -0.05F, -18.0F * DEG_TO_RAD, 0.0F, RIGHT_ARM_Z_ROT));

        root.addOrReplaceChild("left_glove",
                CubeListBuilder.create()
                        .texOffs(0, 53).addBox(-1.48F, 3.02F, -0.86F, 1.90F, 2.55F, 1.72F, new CubeDeformation(0.04F))
                        .texOffs(0, 59).addBox(-1.70F, 5.24F, -1.00F, 2.15F, 0.72F, 2.00F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(-2.62F, -16.15F, -0.05F, -16.0F * DEG_TO_RAD, 0.0F, LEFT_ARM_Z_ROT));
        root.addOrReplaceChild("right_glove",
                CubeListBuilder.create()
                        .texOffs(10, 53).addBox(-0.42F, 3.02F, -0.86F, 1.90F, 2.55F, 1.72F, new CubeDeformation(0.04F))
                        .texOffs(10, 59).addBox(-0.45F, 5.24F, -1.00F, 2.15F, 0.72F, 2.00F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(2.62F, -16.15F, -0.05F, -18.0F * DEG_TO_RAD, 0.0F, RIGHT_ARM_Z_ROT));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(24, 46).addBox(-0.96F, -0.45F, -0.82F, 1.62F, 5.80F, 1.64F, new CubeDeformation(0.04F))
                        .texOffs(24, 54).addBox(-0.82F, 4.95F, -0.74F, 1.34F, 5.40F, 1.48F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(-0.86F, -8.55F, 0.15F, LEFT_LEG_X_ROT, 0.0F, LEFT_LEG_Z_ROT));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(34, 46).addBox(-0.66F, -0.45F, -0.82F, 1.62F, 5.80F, 1.64F, new CubeDeformation(0.04F))
                        .texOffs(34, 54).addBox(-0.52F, 4.95F, -0.74F, 1.34F, 5.40F, 1.48F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(0.86F, -8.50F, 0.15F, RIGHT_LEG_X_ROT, 0.0F, RIGHT_LEG_Z_ROT));

        root.addOrReplaceChild("left_foot_fin",
                CubeListBuilder.create()
                        .texOffs(44, 46).addBox(-1.40F, 8.95F, -1.45F, 2.42F, 1.02F, 2.75F, new CubeDeformation(0.03F))
                        .texOffs(44, 51).addBox(-1.55F, 9.72F, -1.85F, 2.72F, 0.40F, 3.60F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(-0.86F, -8.55F, 0.15F, LEFT_LEG_X_ROT, 0.0F, LEFT_LEG_Z_ROT));
        root.addOrReplaceChild("right_foot_fin",
                CubeListBuilder.create()
                        .texOffs(58, 46).addBox(-1.02F, 8.95F, -1.45F, 2.42F, 1.02F, 2.75F, new CubeDeformation(0.03F))
                        .texOffs(58, 51).addBox(-1.17F, 9.72F, -1.85F, 2.72F, 0.40F, 3.60F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(0.86F, -8.50F, 0.15F, RIGHT_LEG_X_ROT, 0.0F, RIGHT_LEG_Z_ROT));

        root.addOrReplaceChild("foot_glow",
                CubeListBuilder.create()
                        .texOffs(64, 58).addBox(-2.72F, 2.18F, -5.52F, 1.62F, 0.28F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(64, 60).addBox(0.98F, 1.34F, -6.10F, 1.62F, 0.28F, 0.24F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addRigging(PartDefinition root) {
        PartDefinition leftHull = root.addOrReplaceChild("left_sub_hull",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-5.55F, -10.85F, 1.15F, 2.0F * DEG_TO_RAD, LEFT_HULL_Y_ROT, LEFT_HULL_Z_ROT));
        addSubHull(leftHull, false, 0, 72);

        PartDefinition rightHull = root.addOrReplaceChild("right_sub_hull",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(5.85F, -9.45F, -0.35F, -1.0F * DEG_TO_RAD, RIGHT_HULL_Y_ROT, RIGHT_HULL_Z_ROT));
        addSubHull(rightHull, true, 0, 90);

        PartDefinition leftGlow = root.addOrReplaceChild("left_sub_glow",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-5.55F, -10.85F, 1.15F, 2.0F * DEG_TO_RAD, LEFT_HULL_Y_ROT, LEFT_HULL_Z_ROT));
        addSubGlow(leftGlow, 64, 72);

        PartDefinition rightGlow = root.addOrReplaceChild("right_sub_glow",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(5.85F, -9.45F, -0.35F, -1.0F * DEG_TO_RAD, RIGHT_HULL_Y_ROT, RIGHT_HULL_Z_ROT));
        addSubGlow(rightGlow, 64, 84);

        PartDefinition rack = root.addOrReplaceChild("torpedo_rack",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(4.28F, -5.58F, -3.35F, -2.0F * DEG_TO_RAD, TORPEDO_Y_ROT, TORPEDO_Z_ROT));
        addTorpedoTube(rack, "upper_left", -1.12F, -0.82F, 0.0F, 0, 108);
        addTorpedoTube(rack, "upper_right", 1.12F, -0.82F, 0.0F, 14, 108);
        addTorpedoTube(rack, "lower_left", -1.12F, 0.82F, 0.12F, 28, 108);
        addTorpedoTube(rack, "lower_right", 1.12F, 0.82F, 0.12F, 42, 108);
        rack.addOrReplaceChild("rack_plate",
                CubeListBuilder.create()
                        .texOffs(58, 108).addBox(-3.10F, -2.15F, 1.95F, 6.2F, 4.3F, 0.78F, new CubeDeformation(0.05F)),
                PartPose.ZERO);

        root.addOrReplaceChild("torpedo_glow",
                CubeListBuilder.create()
                        .texOffs(64, 96).addBox(1.68F, -6.55F, -8.75F, 0.22F, 0.18F, 7.25F, new CubeDeformation(0.0F))
                        .texOffs(64, 104).addBox(3.88F, -6.55F, -8.75F, 0.22F, 0.18F, 7.25F, new CubeDeformation(0.0F))
                        .texOffs(82, 96).addBox(1.68F, -4.88F, -8.60F, 0.22F, 0.18F, 7.05F, new CubeDeformation(0.0F))
                        .texOffs(82, 104).addBox(3.88F, -4.88F, -8.60F, 0.22F, 0.18F, 7.05F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("dorsal_fin",
                CubeListBuilder.create()
                        .texOffs(96, 72).addBox(-4.15F, -4.55F, 0.0F, 8.3F, 8.8F, 0.76F, new CubeDeformation(0.04F))
                        .texOffs(96, 84).addBox(-2.95F, 3.55F, 0.02F, 5.9F, 2.35F, 0.74F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(0.1F, -16.05F, 2.70F, 0.0F, 0.0F, -6.0F * DEG_TO_RAD));
    }

    private static void addSubHull(PartDefinition parent, boolean mirror, int u, int v) {
        float sideFinX = mirror ? 0.65F : -3.15F;
        parent.addOrReplaceChild("main_hull",
                CubeListBuilder.create()
                        .texOffs(u, v).addBox(-2.25F, -2.05F, -6.45F, 4.5F, 4.1F, 11.8F, new CubeDeformation(0.20F))
                        .texOffs(u + 22, v).addBox(-1.75F, -1.55F, -8.10F, 3.5F, 3.1F, 2.05F, new CubeDeformation(0.10F))
                        .texOffs(u + 38, v).addBox(-1.35F, -1.15F, 4.72F, 2.7F, 2.3F, 2.45F, new CubeDeformation(0.08F)),
                PartPose.ZERO);
        parent.addOrReplaceChild("top_spine",
                CubeListBuilder.create()
                        .texOffs(u, v + 12).addBox(-0.32F, -2.62F, -4.80F, 0.64F, 0.92F, 7.60F, new CubeDeformation(0.02F)),
                PartPose.ZERO);
        parent.addOrReplaceChild("side_fin",
                CubeListBuilder.create()
                        .texOffs(u + 18, v + 12).addBox(sideFinX, -0.24F, -1.90F, 2.50F, 0.48F, 4.85F, new CubeDeformation(0.01F)),
                PartPose.ZERO);
        parent.addOrReplaceChild("lower_fin",
                CubeListBuilder.create()
                        .texOffs(u + 34, v + 12).addBox(-0.72F, 1.52F, -2.55F, 1.44F, 1.04F, 5.15F, new CubeDeformation(0.02F)),
                PartPose.ZERO);
    }

    private static void addSubGlow(PartDefinition parent, int u, int v) {
        parent.addOrReplaceChild("bow_ring",
                CubeListBuilder.create()
                        .texOffs(u, v).addBox(-1.82F, -1.82F, -8.34F, 3.64F, 0.28F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(u, v + 2).addBox(-1.82F, 1.54F, -8.34F, 3.64F, 0.28F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(u, v + 4).addBox(-1.82F, -1.54F, -8.34F, 0.28F, 3.08F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(u + 6, v + 4).addBox(1.54F, -1.54F, -8.34F, 0.28F, 3.08F, 0.24F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
        parent.addOrReplaceChild("center_line",
                CubeListBuilder.create()
                        .texOffs(u, v + 8).addBox(-0.14F, -2.82F, -5.10F, 0.28F, 0.22F, 8.35F, new CubeDeformation(0.0F))
                        .texOffs(u, v + 10).addBox(-0.14F, 2.58F, -4.55F, 0.28F, 0.22F, 7.80F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addTorpedoTube(PartDefinition parent, String name, float x, float y, float z, int u, int v) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(u, v).addBox(-0.54F, -0.54F, -5.55F, 1.08F, 1.08F, 8.45F, new CubeDeformation(0.08F))
                        .texOffs(u, v + 10).addBox(-0.66F, -0.66F, -6.05F, 1.32F, 1.32F, 0.72F, new CubeDeformation(0.04F)),
                PartPose.offset(x, y, z));
    }

    private static void addTentacles(PartDefinition root) {
        PartDefinition tentacles = root.addOrReplaceChild("tentacles",
                CubeListBuilder.create(),
                PartPose.ZERO);
        addTentacleSegment(tentacles, "left_high_0", -3.15F, -14.6F, 2.72F,
                -7.0F * DEG_TO_RAD, -38.0F * DEG_TO_RAD, 5.5F, 1.35F, 0, 0);
        addTentacleSegment(tentacles, "left_high_1", -5.92F, -18.55F, 3.10F,
                -4.0F * DEG_TO_RAD, -72.0F * DEG_TO_RAD, 4.7F, 1.15F, 0, 0);
        addTentacleSegment(tentacles, "right_low_0", 3.78F, -9.30F, 2.36F,
                8.0F * DEG_TO_RAD, 34.0F * DEG_TO_RAD, 4.6F, 1.28F, 0, 0);
        addTentacleSegment(tentacles, "right_low_1", 5.95F, -6.15F, 2.12F,
                10.0F * DEG_TO_RAD, 62.0F * DEG_TO_RAD, 4.2F, 1.06F, 0, 0);
        addTentacleSegment(tentacles, "rear_drop_0", -0.62F, -10.5F, 3.35F,
                13.0F * DEG_TO_RAD, -6.0F * DEG_TO_RAD, 6.0F, 1.30F, 0, 0);
        addTentacleSegment(tentacles, "rear_drop_1", -0.12F, -5.15F, 3.18F,
                9.0F * DEG_TO_RAD, 24.0F * DEG_TO_RAD, 3.9F, 1.02F, 0, 0);

        PartDefinition underside = root.addOrReplaceChild("tentacle_underside",
                CubeListBuilder.create(),
                PartPose.ZERO);
        addTentacleSegment(underside, "left_underside", -4.78F, -16.25F, 2.12F,
                -8.0F * DEG_TO_RAD, -58.0F * DEG_TO_RAD, 3.45F, 0.46F, 34, 0);
        addTentacleSegment(underside, "right_underside", 4.90F, -7.72F, 1.58F,
                7.0F * DEG_TO_RAD, 50.0F * DEG_TO_RAD, 3.10F, 0.42F, 34, 0);
        addTentacleSegment(underside, "rear_underside", -0.08F, -7.30F, 2.58F,
                8.0F * DEG_TO_RAD, 20.0F * DEG_TO_RAD, 3.20F, 0.44F, 34, 0);

        root.addOrReplaceChild("tentacle_glow",
                CubeListBuilder.create()
                        .texOffs(64, 112).addBox(-6.55F, -19.85F, 2.32F, 0.46F, 0.30F, 0.28F, new CubeDeformation(0.0F))
                        .texOffs(64, 114).addBox(-5.68F, -18.08F, 2.28F, 0.42F, 0.28F, 0.26F, new CubeDeformation(0.0F))
                        .texOffs(64, 116).addBox(5.52F, -8.78F, 1.78F, 0.42F, 0.28F, 0.26F, new CubeDeformation(0.0F))
                        .texOffs(64, 118).addBox(6.72F, -6.75F, 1.72F, 0.42F, 0.28F, 0.26F, new CubeDeformation(0.0F))
                        .texOffs(64, 120).addBox(-0.48F, -5.92F, 2.55F, 0.42F, 0.28F, 0.26F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addTentacleSegment(PartDefinition parent, String name, float x, float y, float z,
                                           float xRot, float zRot, float length, float thickness,
                                           int texU, int texV) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(texU, texV).addBox(-thickness * 0.5F, -length, -thickness * 0.5F,
                                thickness, length, thickness, new CubeDeformation(thickness * 0.14F)),
                PartPose.offsetAndRotation(x, y, z, xRot, 0.0F, zRot));
    }

    @Override
    public void setupAnim(DeepOceanSubmarineEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float headYaw = netHeadYaw * DEG_TO_RAD * 0.38F;
        float headPitchRot = HEAD_BASE_X_ROT + headPitch * DEG_TO_RAD * 0.30F;
        setHeadRotation(headSkin, headYaw, headPitchRot);
        setHeadRotation(headHair, headYaw, headPitchRot);
        setHeadRotation(hairShadow, headYaw, headPitchRot);
        setHeadRotation(visorGlow, headYaw, headPitchRot);
        headArmor.xRot = headPitchRot;
        headArmor.yRot = headYaw * 0.68F;
        headArmor.zRot = 0.0F;

        float walk = Math.min(limbSwingAmount, 1.0F);
        float armSwing = Mth.cos(limbSwing * 0.6662F) * 0.18F * walk;
        setArmRotation(leftArmSkin, -16.0F * DEG_TO_RAD - armSwing, LEFT_ARM_Z_ROT);
        setArmRotation(leftGlove, leftArmSkin.xRot, LEFT_ARM_Z_ROT);
        setArmRotation(rightArmSkin, -18.0F * DEG_TO_RAD + armSwing, RIGHT_ARM_Z_ROT);
        setArmRotation(rightGlove, rightArmSkin.xRot, RIGHT_ARM_Z_ROT);

        float finKick = Mth.cos(limbSwing * 0.6662F) * 0.28F * walk;
        leftLeg.xRot = LEFT_LEG_X_ROT - finKick;
        rightLeg.xRot = RIGHT_LEG_X_ROT + finKick;
        leftFootFin.xRot = leftLeg.xRot;
        rightFootFin.xRot = rightLeg.xRot;

        float pulse = Mth.sin(ageInTicks * 0.065F) * 0.045F;
        setHullRotation(leftSubHull, 2.0F * DEG_TO_RAD, LEFT_HULL_Y_ROT + pulse, LEFT_HULL_Z_ROT + pulse * 0.35F);
        setHullRotation(leftSubGlow, leftSubHull.xRot, leftSubHull.yRot, leftSubHull.zRot);
        setHullRotation(rightSubHull, -1.0F * DEG_TO_RAD, RIGHT_HULL_Y_ROT - pulse, RIGHT_HULL_Z_ROT - pulse * 0.35F);
        setHullRotation(rightSubGlow, rightSubHull.xRot, rightSubHull.yRot, rightSubHull.zRot);

        float swimSway = Mth.sin(ageInTicks * 0.055F) * 0.050F;
        torpedoRack.yRot = TORPEDO_Y_ROT - pulse * 0.30F;
        torpedoRack.zRot = TORPEDO_Z_ROT + pulse * 0.25F;
        dorsalFin.zRot = -6.0F * DEG_TO_RAD + swimSway;
        tentacles.zRot = swimSway * 0.65F;
        tentacleUnderside.zRot = tentacles.zRot;
        tentacleGlow.zRot = tentacles.zRot;
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

    private static void setHullRotation(ModelPart part, float xRot, float yRot, float zRot) {
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, int color) {
        tentacles.render(poseStack, vertexConsumer, packedLight, packedOverlay, TENTACLE_COLOR);
        tentacleUnderside.render(poseStack, vertexConsumer, packedLight, packedOverlay, TENTACLE_UNDERSIDE_COLOR);
        dorsalFin.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        leftSubHull.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_COLOR);
        rightSubHull.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_COLOR);
        torpedoRack.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);

        leftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, SUIT_COLOR);
        rightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, SUIT_COLOR);
        leftFootFin.render(poseStack, vertexConsumer, packedLight, packedOverlay, GLOW_COLOR);
        rightFootFin.render(poseStack, vertexConsumer, packedLight, packedOverlay, GLOW_COLOR);
        bodySkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        bodySuit.render(poseStack, vertexConsumer, packedLight, packedOverlay, SUIT_COLOR);
        collar.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        leftArmSkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        rightArmSkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        leftGlove.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        rightGlove.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        headSkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        headHair.render(poseStack, vertexConsumer, packedLight, packedOverlay, HAIR_COLOR);
        hairShadow.render(poseStack, vertexConsumer, packedLight, packedOverlay, HAIR_SHADOW_COLOR);
        headArmor.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);

        bodyGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        visorGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        periscopeGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        leftSubGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        rightSubGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        torpedoGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        footGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        tentacleGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
    }
}
