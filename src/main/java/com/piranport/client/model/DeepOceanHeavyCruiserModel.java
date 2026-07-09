package com.piranport.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
import com.piranport.npc.deepocean.DeepOceanHeavyCruiserEntity;
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

public class DeepOceanHeavyCruiserModel extends EntityModel<DeepOceanHeavyCruiserEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "deep_ocean_heavy_cruiser"), "main");

    private static final float DEG_TO_RAD = Mth.PI / 180.0F;
    private static final int SKIN_COLOR = 0xFFE8DED9;
    private static final int HAIR_COLOR = 0xFFD0CED5;
    private static final int HAIR_SHADOW_COLOR = 0xFF8E8B94;
    private static final int DARK_COLOR = 0xFF18151B;
    private static final int SUIT_COLOR = 0xFF252128;
    private static final int METAL_COLOR = 0xFF49444D;
    private static final int METAL_DARK_COLOR = 0xFF302C34;
    private static final int GLOW_COLOR = 0xFFFF45C8;
    private static final int BARREL_COLOR = 0xFF756E78;
    private static final int MOUTH_COLOR = 0xFF4C1F33;

    private static final float LEFT_ARM_Z_ROT = 9.0F * DEG_TO_RAD;
    private static final float RIGHT_ARM_Z_ROT = -11.0F * DEG_TO_RAD;
    private static final float LEFT_SHIELD_Y_ROT = 16.0F * DEG_TO_RAD;
    private static final float RIGHT_SHIELD_Y_ROT = -20.0F * DEG_TO_RAD;
    private static final float LEFT_SHIELD_Z_ROT = 12.0F * DEG_TO_RAD;
    private static final float RIGHT_SHIELD_Z_ROT = -7.0F * DEG_TO_RAD;
    private static final float LEFT_TURRET_Y_ROT = 12.0F * DEG_TO_RAD;
    private static final float RIGHT_TURRET_Y_ROT = -16.0F * DEG_TO_RAD;
    private static final float REAR_TURRET_Y_ROT = -24.0F * DEG_TO_RAD;

    private final ModelPart bodySkin;
    private final ModelPart bodyDark;
    private final ModelPart bodyGlow;
    private final ModelPart headSkin;
    private final ModelPart headHair;
    private final ModelPart headHairShadow;
    private final ModelPart headArmor;
    private final ModelPart headGlow;
    private final ModelPart headMouth;
    private final ModelPart leftArmSkin;
    private final ModelPart rightArmSkin;
    private final ModelPart leftArmDark;
    private final ModelPart rightArmDark;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart legGlow;
    private final ModelPart skirtArmor;
    private final ModelPart tailRing;
    private final ModelPart tailGlow;
    private final ModelPart leftShield;
    private final ModelPart rightShield;
    private final ModelPart shieldGlow;
    private final ModelPart leftTurret;
    private final ModelPart rightTurret;
    private final ModelPart rearTurret;
    private final ModelPart turretGlow;

    public DeepOceanHeavyCruiserModel(ModelPart root) {
        ModelPart modelRoot = root.getChild("root");
        this.bodySkin = modelRoot.getChild("body_skin");
        this.bodyDark = modelRoot.getChild("body_dark");
        this.bodyGlow = modelRoot.getChild("body_glow");
        this.headSkin = modelRoot.getChild("head_skin");
        this.headHair = modelRoot.getChild("head_hair");
        this.headHairShadow = modelRoot.getChild("head_hair_shadow");
        this.headArmor = modelRoot.getChild("head_armor");
        this.headGlow = modelRoot.getChild("head_glow");
        this.headMouth = modelRoot.getChild("head_mouth");
        this.leftArmSkin = modelRoot.getChild("left_arm_skin");
        this.rightArmSkin = modelRoot.getChild("right_arm_skin");
        this.leftArmDark = modelRoot.getChild("left_arm_dark");
        this.rightArmDark = modelRoot.getChild("right_arm_dark");
        this.leftLeg = modelRoot.getChild("left_leg");
        this.rightLeg = modelRoot.getChild("right_leg");
        this.legGlow = modelRoot.getChild("leg_glow");
        this.skirtArmor = modelRoot.getChild("skirt_armor");
        this.tailRing = modelRoot.getChild("tail_ring");
        this.tailGlow = modelRoot.getChild("tail_glow");
        this.leftShield = modelRoot.getChild("left_shield");
        this.rightShield = modelRoot.getChild("right_shield");
        this.shieldGlow = modelRoot.getChild("shield_glow");
        this.leftTurret = modelRoot.getChild("left_turret");
        this.rightTurret = modelRoot.getChild("right_turret");
        this.rearTurret = modelRoot.getChild("rear_turret");
        this.turretGlow = modelRoot.getChild("turret_glow");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root",
                CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        addBody(root);
        addHead(root);
        addLimbs(root);
        addSkirt(root);
        addTailRing(root);
        addShields(root);
        addTurrets(root);

        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addBody(PartDefinition root) {
        root.addOrReplaceChild("body_skin",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.15F, -14.6F, -1.34F, 4.3F, 4.2F, 2.65F, new CubeDeformation(0.04F))
                        .texOffs(0, 8).addBox(-1.85F, -10.8F, -1.18F, 3.7F, 1.9F, 2.35F, new CubeDeformation(0.0F))
                        .texOffs(0, 13).addBox(-1.05F, -18.2F, -0.82F, 2.1F, 1.2F, 1.65F, new CubeDeformation(0.02F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body_dark",
                CubeListBuilder.create()
                        .texOffs(20, 0).addBox(-2.7F, -17.3F, -1.62F, 5.4F, 3.55F, 3.15F, new CubeDeformation(0.05F))
                        .texOffs(20, 7).addBox(-2.35F, -14.05F, -1.72F, 4.7F, 1.0F, 0.42F, new CubeDeformation(0.0F))
                        .texOffs(20, 10).addBox(-2.45F, -11.05F, -1.52F, 4.9F, 1.25F, 0.46F, new CubeDeformation(0.0F))
                        .texOffs(20, 13).addBox(-2.55F, -9.25F, -1.38F, 5.1F, 1.0F, 2.55F, new CubeDeformation(0.04F))
                        .texOffs(36, 0).addBox(-0.35F, -17.05F, -1.86F, 0.7F, 6.35F, 0.34F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body_glow",
                CubeListBuilder.create()
                        .texOffs(64, 0).addBox(-0.36F, -16.65F, -1.95F, 0.72F, 0.34F, 0.26F, new CubeDeformation(0.0F))
                        .texOffs(64, 2).addBox(-0.28F, -14.75F, -1.98F, 0.56F, 1.75F, 0.26F, new CubeDeformation(0.0F))
                        .texOffs(64, 5).addBox(-1.45F, -10.25F, -1.61F, 2.9F, 0.24F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(64, 7).addBox(-0.24F, -8.72F, -1.48F, 0.48F, 0.62F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addHead(PartDefinition root) {
        root.addOrReplaceChild("head_skin",
                CubeListBuilder.create()
                        .texOffs(0, 24).addBox(-2.35F, -4.1F, -2.08F, 4.7F, 4.75F, 4.16F, new CubeDeformation(0.03F))
                        .texOffs(0, 34).addBox(-1.68F, -0.12F, -2.38F, 3.36F, 0.85F, 0.72F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -19.2F, -0.22F, -4.0F * DEG_TO_RAD, 0.0F, 0.0F));

        root.addOrReplaceChild("head_hair",
                CubeListBuilder.create()
                        .texOffs(18, 24).addBox(-2.75F, -4.45F, -1.82F, 5.5F, 1.55F, 4.05F, new CubeDeformation(0.11F))
                        .texOffs(18, 30).addBox(-3.0F, -3.12F, -1.40F, 1.36F, 6.2F, 2.2F, new CubeDeformation(0.08F))
                        .texOffs(26, 30).addBox(1.52F, -3.5F, -1.48F, 1.38F, 6.8F, 2.35F, new CubeDeformation(0.08F))
                        .texOffs(34, 30).addBox(-1.58F, -3.95F, -2.46F, 2.55F, 4.7F, 0.78F, new CubeDeformation(0.04F))
                        .texOffs(44, 24).addBox(1.04F, -2.55F, -2.36F, 1.22F, 3.20F, 0.62F, new CubeDeformation(0.03F))
                        .texOffs(44, 30).addBox(-2.20F, -1.00F, -2.40F, 1.20F, 2.70F, 0.55F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(0.0F, -19.2F, -0.22F, -4.0F * DEG_TO_RAD, 0.0F, 0.0F));

        root.addOrReplaceChild("head_hair_shadow",
                CubeListBuilder.create()
                        .texOffs(48, 30).addBox(-1.92F, -1.95F, -2.54F, 1.35F, 2.95F, 0.45F, new CubeDeformation(0.02F))
                        .texOffs(48, 35).addBox(0.15F, -3.38F, -2.52F, 0.72F, 2.35F, 0.45F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(0.0F, -19.2F, -0.22F, -4.0F * DEG_TO_RAD, 0.0F, 0.0F));

        PartDefinition armor = root.addOrReplaceChild("head_armor",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -22.7F, -0.10F, -4.0F * DEG_TO_RAD, 0.0F, 0.0F));

        armor.addOrReplaceChild("left_shell",
                CubeListBuilder.create()
                        .texOffs(72, 24).addBox(-2.9F, -1.6F, -0.44F, 3.05F, 3.2F, 0.88F, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(-2.16F, 0.1F, 0.52F, 0.0F, -13.0F * DEG_TO_RAD, 20.0F * DEG_TO_RAD));
        armor.addOrReplaceChild("right_shell",
                CubeListBuilder.create()
                        .texOffs(72, 31).addBox(-0.15F, -1.6F, -0.44F, 3.05F, 3.2F, 0.88F, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(2.16F, 0.1F, 0.52F, 0.0F, 13.0F * DEG_TO_RAD, -20.0F * DEG_TO_RAD));
        armor.addOrReplaceChild("rear_crown",
                CubeListBuilder.create()
                        .texOffs(72, 38).addBox(-2.35F, -0.75F, 0.25F, 4.7F, 1.5F, 1.05F, new CubeDeformation(0.05F)),
                PartPose.offset(0.0F, -0.2F, 1.05F));

        root.addOrReplaceChild("head_glow",
                CubeListBuilder.create()
                        .texOffs(64, 12).addBox(0.58F, -2.68F, -2.56F, 0.72F, 0.72F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(64, 14).addBox(1.08F, -3.70F, -2.36F, 0.32F, 1.20F, 0.25F, new CubeDeformation(0.0F))
                        .texOffs(64, 16).addBox(-0.14F, -4.00F, -2.42F, 0.30F, 3.35F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(64, 20).addBox(-1.62F, -0.25F, -2.55F, 0.22F, 0.62F, 0.18F, new CubeDeformation(0.0F))
                        .texOffs(64, 22).addBox(1.46F, -0.10F, -2.55F, 0.22F, 0.54F, 0.18F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -19.2F, -0.22F, -4.0F * DEG_TO_RAD, 0.0F, 0.0F));

        root.addOrReplaceChild("head_mouth",
                CubeListBuilder.create()
                        .texOffs(72, 12).addBox(0.38F, -0.18F, -2.61F, 0.70F, 0.34F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(72, 14).addBox(0.50F, 0.08F, -2.63F, 0.46F, 0.16F, 0.18F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -19.2F, -0.22F, -4.0F * DEG_TO_RAD, 0.0F, 0.0F));
    }

    private static void addLimbs(PartDefinition root) {
        root.addOrReplaceChild("left_arm_skin",
                CubeListBuilder.create()
                        .texOffs(0, 44).addBox(-1.35F, -0.65F, -0.76F, 1.45F, 4.35F, 1.52F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(-2.85F, -16.3F, 0.02F, -0.25F, 0.0F, LEFT_ARM_Z_ROT));
        root.addOrReplaceChild("right_arm_skin",
                CubeListBuilder.create()
                        .texOffs(7, 44).addBox(-0.10F, -0.65F, -0.76F, 1.45F, 4.35F, 1.52F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(2.85F, -16.3F, 0.02F, -0.25F, 0.0F, RIGHT_ARM_Z_ROT));

        root.addOrReplaceChild("left_arm_dark",
                CubeListBuilder.create()
                        .texOffs(0, 51).addBox(-1.28F, 2.9F, -0.80F, 1.8F, 3.3F, 1.6F, new CubeDeformation(0.03F))
                        .texOffs(0, 56).addBox(-1.50F, 5.62F, -0.92F, 2.1F, 0.8F, 1.84F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(-2.85F, -16.3F, 0.02F, -0.25F, 0.0F, LEFT_ARM_Z_ROT));
        root.addOrReplaceChild("right_arm_dark",
                CubeListBuilder.create()
                        .texOffs(9, 51).addBox(-0.52F, 2.9F, -0.80F, 1.8F, 3.3F, 1.6F, new CubeDeformation(0.03F))
                        .texOffs(9, 56).addBox(-0.62F, 5.62F, -0.92F, 2.1F, 0.8F, 1.84F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(2.85F, -16.3F, 0.02F, -0.25F, 0.0F, RIGHT_ARM_Z_ROT));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(20, 44).addBox(-1.05F, -0.35F, -0.85F, 1.65F, 5.8F, 1.7F, new CubeDeformation(0.04F))
                        .texOffs(20, 52).addBox(-0.92F, 5.00F, -0.78F, 1.44F, 6.0F, 1.56F, new CubeDeformation(0.03F))
                        .texOffs(20, 60).addBox(-1.20F, 10.30F, -1.35F, 1.95F, 0.70F, 2.25F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(-0.82F, -8.6F, 0.03F, -5.0F * DEG_TO_RAD, 0.0F, 3.0F * DEG_TO_RAD));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(31, 44).addBox(-0.60F, -0.35F, -0.85F, 1.65F, 5.8F, 1.7F, new CubeDeformation(0.04F))
                        .texOffs(31, 52).addBox(-0.52F, 5.00F, -0.78F, 1.44F, 6.0F, 1.56F, new CubeDeformation(0.03F))
                        .texOffs(31, 60).addBox(-0.75F, 10.30F, -1.35F, 1.95F, 0.70F, 2.25F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(0.82F, -8.6F, 0.03F, 6.0F * DEG_TO_RAD, 0.0F, -3.0F * DEG_TO_RAD));

        root.addOrReplaceChild("leg_glow",
                CubeListBuilder.create()
                        .texOffs(64, 44).addBox(-1.62F, -6.2F, -0.92F, 0.22F, 3.6F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(64, 49).addBox(1.38F, -3.5F, -0.92F, 0.22F, 3.6F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(64, 54).addBox(-1.78F, 2.08F, -1.10F, 1.1F, 0.26F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(64, 56).addBox(0.68F, 2.08F, -1.10F, 1.1F, 0.26F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addSkirt(PartDefinition root) {
        root.addOrReplaceChild("skirt_armor",
                CubeListBuilder.create()
                        .texOffs(44, 44).addBox(-3.75F, -9.05F, -0.88F, 1.75F, 3.75F, 2.0F, new CubeDeformation(0.08F))
                        .texOffs(44, 50).addBox(2.00F, -9.05F, -0.88F, 1.75F, 3.75F, 2.0F, new CubeDeformation(0.08F))
                        .texOffs(54, 44).addBox(-2.40F, -8.72F, 0.72F, 4.8F, 2.65F, 1.05F, new CubeDeformation(0.05F))
                        .texOffs(54, 49).addBox(-0.34F, -9.16F, -1.48F, 0.68F, 1.45F, 0.36F, new CubeDeformation(0.0F))
                        .texOffs(54, 52).addBox(-3.30F, -8.15F, -1.10F, 6.6F, 0.38F, 0.32F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addTailRing(PartDefinition root) {
        PartDefinition tail = root.addOrReplaceChild("tail_ring",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -8.2F, 2.45F, 0.0F, 0.0F, -4.0F * DEG_TO_RAD));

        addSegment(tail, "left_upper", -3.45F, -0.7F, 0.0F, 34.0F * DEG_TO_RAD, 4.4F, 0.30F);
        addSegment(tail, "left_lower", -4.15F, 1.75F, 0.0F, -18.0F * DEG_TO_RAD, 4.8F, 0.32F);
        addSegment(tail, "bottom_sweep", -0.75F, 3.12F, 0.0F, 8.0F * DEG_TO_RAD, 6.2F, 0.34F);
        addSegment(tail, "right_lower", 3.70F, 2.0F, 0.0F, 28.0F * DEG_TO_RAD, 4.65F, 0.32F);
        addSegment(tail, "right_extension", 5.85F, 0.55F, 0.0F, -8.0F * DEG_TO_RAD, 3.9F, 0.34F);

        root.addOrReplaceChild("tail_glow",
                CubeListBuilder.create()
                        .texOffs(86, 0).addBox(-5.55F, -7.15F, 2.34F, 0.28F, 3.9F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(86, 5).addBox(-2.65F, -5.0F, 2.32F, 0.28F, 4.0F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(86, 10).addBox(3.85F, -4.95F, 2.32F, 0.28F, 3.8F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(86, 15).addBox(5.85F, -7.90F, 2.32F, 0.28F, 3.5F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addSegment(PartDefinition parent, String name, float x, float y, float z,
                                   float zRot, float length, float thickness) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(86, 24).addBox(-length * 0.5F, -thickness * 0.5F, -thickness * 0.5F,
                                length, thickness, thickness, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(x, y, z, 0.0F, 0.0F, zRot));
    }

    private static void addShields(PartDefinition root) {
        root.addOrReplaceChild("left_shield",
                CubeListBuilder.create()
                        .texOffs(0, 72).addBox(-4.15F, -5.35F, -0.44F, 8.3F, 7.35F, 0.88F, new CubeDeformation(0.06F))
                        .texOffs(0, 82).addBox(-3.42F, 1.80F, -0.42F, 6.2F, 3.15F, 0.84F, new CubeDeformation(0.05F))
                        .texOffs(22, 72).addBox(-0.72F, -5.95F, -0.51F, 1.44F, 8.3F, 1.02F, new CubeDeformation(0.03F))
                        .texOffs(30, 72).addBox(-4.48F, -0.72F, -0.56F, 8.96F, 1.44F, 1.12F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(-5.00F, -13.0F, 1.85F, 0.0F, LEFT_SHIELD_Y_ROT, LEFT_SHIELD_Z_ROT));

        root.addOrReplaceChild("right_shield",
                CubeListBuilder.create()
                        .texOffs(0, 88).addBox(-3.85F, -4.55F, -0.44F, 7.7F, 6.85F, 0.88F, new CubeDeformation(0.06F))
                        .texOffs(0, 97).addBox(-2.80F, 2.00F, -0.42F, 5.8F, 3.05F, 0.84F, new CubeDeformation(0.05F))
                        .texOffs(22, 88).addBox(-0.62F, -5.10F, -0.51F, 1.24F, 7.9F, 1.02F, new CubeDeformation(0.03F))
                        .texOffs(30, 88).addBox(-4.20F, -0.65F, -0.56F, 8.4F, 1.30F, 1.12F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(5.25F, -12.65F, 1.72F, 0.0F, RIGHT_SHIELD_Y_ROT, RIGHT_SHIELD_Z_ROT));

        root.addOrReplaceChild("shield_glow",
                CubeListBuilder.create()
                        .texOffs(64, 72).addBox(-9.90F, -14.0F, 1.02F, 1.25F, 1.25F, 0.28F, new CubeDeformation(0.0F))
                        .texOffs(64, 75).addBox(-10.55F, -13.48F, 1.00F, 2.55F, 0.24F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(64, 77).addBox(-9.30F, -14.72F, 1.00F, 0.24F, 2.55F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(64, 84).addBox(-10.48F, -14.25F, 1.01F, 1.95F, 0.18F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(64, 86).addBox(-9.18F, -14.32F, 1.01F, 0.18F, 1.82F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(64, 88).addBox(-10.02F, -13.88F, 1.01F, 1.45F, 0.16F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(64, 90).addBox(-9.92F, -13.02F, 1.01F, 1.25F, 0.16F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(64, 80).addBox(8.55F, -13.55F, 0.82F, 2.35F, 0.24F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(64, 82).addBox(9.60F, -14.60F, 0.82F, 0.24F, 2.25F, 0.24F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addTurrets(PartDefinition root) {
        root.addOrReplaceChild("left_turret",
                CubeListBuilder.create()
                        .texOffs(0, 106).addBox(-1.50F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.12F))
                        .texOffs(0, 111).addBox(-1.62F, -0.58F, -6.05F, 0.36F, 0.36F, 5.4F, new CubeDeformation(0.0F))
                        .texOffs(0, 116).addBox(-0.18F, -0.72F, -6.45F, 0.36F, 0.36F, 5.8F, new CubeDeformation(0.0F))
                        .texOffs(0, 121).addBox(1.26F, -0.58F, -6.05F, 0.36F, 0.36F, 5.4F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.78F, -10.95F, -0.62F, -2.0F * DEG_TO_RAD, LEFT_TURRET_Y_ROT, 0.0F));

        root.addOrReplaceChild("right_turret",
                CubeListBuilder.create()
                        .texOffs(20, 106).addBox(-1.48F, -1.0F, -1.0F, 2.96F, 2.0F, 2.0F, new CubeDeformation(0.12F))
                        .texOffs(20, 111).addBox(-1.54F, -0.58F, -6.20F, 0.36F, 0.36F, 5.55F, new CubeDeformation(0.0F))
                        .texOffs(20, 116).addBox(-0.18F, -0.72F, -6.58F, 0.36F, 0.36F, 5.92F, new CubeDeformation(0.0F))
                        .texOffs(20, 121).addBox(1.18F, -0.58F, -6.20F, 0.36F, 0.36F, 5.55F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(6.10F, -11.45F, -0.40F, -2.0F * DEG_TO_RAD, RIGHT_TURRET_Y_ROT, 0.0F));

        root.addOrReplaceChild("rear_turret",
                CubeListBuilder.create()
                        .texOffs(42, 106).addBox(-1.35F, -0.82F, -0.86F, 2.7F, 1.64F, 1.72F, new CubeDeformation(0.10F))
                        .texOffs(42, 111).addBox(-1.28F, -0.45F, -5.10F, 0.32F, 0.32F, 4.65F, new CubeDeformation(0.0F))
                        .texOffs(42, 116).addBox(-0.16F, -0.58F, -5.46F, 0.32F, 0.32F, 5.02F, new CubeDeformation(0.0F))
                        .texOffs(42, 121).addBox(0.96F, -0.45F, -5.10F, 0.32F, 0.32F, 4.65F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(6.80F, -5.05F, 2.92F, 6.0F * DEG_TO_RAD, REAR_TURRET_Y_ROT, 4.0F * DEG_TO_RAD));

        root.addOrReplaceChild("turret_glow",
                CubeListBuilder.create()
                        .texOffs(64, 104).addBox(-7.45F, -11.84F, -6.55F, 0.28F, 0.18F, 5.20F, new CubeDeformation(0.0F))
                        .texOffs(64, 110).addBox(-5.70F, -11.88F, -6.75F, 0.28F, 0.18F, 5.45F, new CubeDeformation(0.0F))
                        .texOffs(64, 116).addBox(5.05F, -12.34F, -6.78F, 0.28F, 0.18F, 5.40F, new CubeDeformation(0.0F))
                        .texOffs(64, 122).addBox(6.72F, -12.38F, -6.98F, 0.28F, 0.18F, 5.60F, new CubeDeformation(0.0F))
                        .texOffs(84, 104).addBox(5.85F, -5.72F, -2.68F, 0.26F, 0.16F, 4.3F, new CubeDeformation(0.0F))
                        .texOffs(84, 110).addBox(7.10F, -5.65F, -2.50F, 0.26F, 0.16F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    @Override
    public void setupAnim(DeepOceanHeavyCruiserEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float headYaw = netHeadYaw * DEG_TO_RAD * 0.42F;
        float headPitchRot = -4.0F * DEG_TO_RAD + headPitch * DEG_TO_RAD * 0.32F;
        setHeadRotation(headSkin, headYaw, headPitchRot);
        setHeadRotation(headHair, headYaw, headPitchRot);
        setHeadRotation(headHairShadow, headYaw, headPitchRot);
        setHeadRotation(headArmor, headYaw * 0.75F, headPitchRot);
        setHeadRotation(headGlow, headYaw, headPitchRot);
        setHeadRotation(headMouth, headYaw, headPitchRot);

        float walk = Math.min(limbSwingAmount, 1.0F);
        float armSwing = Mth.cos(limbSwing * 0.6662F) * 0.22F * walk;
        setArmRotation(leftArmSkin, -0.25F - armSwing, LEFT_ARM_Z_ROT);
        setArmRotation(leftArmDark, leftArmSkin.xRot, LEFT_ARM_Z_ROT);
        setArmRotation(rightArmSkin, -0.25F + armSwing, RIGHT_ARM_Z_ROT);
        setArmRotation(rightArmDark, rightArmSkin.xRot, RIGHT_ARM_Z_ROT);

        leftLeg.xRot = -5.0F * DEG_TO_RAD + Mth.cos(limbSwing * 0.6662F + Mth.PI) * 0.28F * walk;
        rightLeg.xRot = 6.0F * DEG_TO_RAD + Mth.cos(limbSwing * 0.6662F) * 0.28F * walk;
        leftLeg.zRot = 3.0F * DEG_TO_RAD;
        rightLeg.zRot = -3.0F * DEG_TO_RAD;

        float pulse = Mth.sin(ageInTicks * 0.065F) * 0.045F;
        leftShield.yRot = LEFT_SHIELD_Y_ROT + pulse;
        leftShield.zRot = LEFT_SHIELD_Z_ROT + pulse * 0.35F;
        rightShield.yRot = RIGHT_SHIELD_Y_ROT - pulse;
        rightShield.zRot = RIGHT_SHIELD_Z_ROT - pulse * 0.35F;

        leftTurret.yRot = LEFT_TURRET_Y_ROT + pulse * 0.45F;
        rightTurret.yRot = RIGHT_TURRET_Y_ROT - pulse * 0.45F;
        rearTurret.yRot = REAR_TURRET_Y_ROT + Mth.sin(ageInTicks * 0.052F) * 0.07F;
        tailRing.zRot = -4.0F * DEG_TO_RAD + Mth.sin(ageInTicks * 0.055F) * 0.055F;
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
        tailRing.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        tailGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        leftShield.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        rightShield.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        shieldGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        leftTurret.render(poseStack, vertexConsumer, packedLight, packedOverlay, BARREL_COLOR);
        rightTurret.render(poseStack, vertexConsumer, packedLight, packedOverlay, BARREL_COLOR);
        rearTurret.render(poseStack, vertexConsumer, packedLight, packedOverlay, BARREL_COLOR);
        skirtArmor.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_COLOR);

        leftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        rightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        legGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        bodySkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        bodyDark.render(poseStack, vertexConsumer, packedLight, packedOverlay, SUIT_COLOR);
        bodyGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        leftArmSkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        rightArmSkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        leftArmDark.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        rightArmDark.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        headSkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        headMouth.render(poseStack, vertexConsumer, packedLight, packedOverlay, MOUTH_COLOR);
        headHair.render(poseStack, vertexConsumer, packedLight, packedOverlay, HAIR_COLOR);
        headHairShadow.render(poseStack, vertexConsumer, packedLight, packedOverlay, HAIR_SHADOW_COLOR);
        headArmor.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        headGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        turretGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
    }
}
