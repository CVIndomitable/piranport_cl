package com.piranport.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
import com.piranport.npc.deepocean.DeepOceanDestroyerEntity;
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

public class DeepOceanDestroyerModel extends EntityModel<DeepOceanDestroyerEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "deep_ocean_destroyer"), "main");

    private static final float DEG_TO_RAD = Mth.PI / 180.0F;
    private static final int SKIN_COLOR = 0xFFE8E0DA;
    private static final int HAIR_COLOR = 0xFF9B9AA0;
    private static final int DARK_COLOR = 0xFF25212A;
    private static final int METAL_COLOR = 0xFF5A545D;
    private static final int FIN_COLOR = 0xFF5C7080;
    private static final int GLOW_COLOR = 0xFFFF4AD8;
    private static final int TEETH_COLOR = 0xFFEFE8E2;

    private static final float LEFT_RIGGING_Y_ROT = 12.0F * DEG_TO_RAD;
    private static final float RIGHT_RIGGING_Y_ROT = -12.0F * DEG_TO_RAD;
    private static final float LEFT_RIGGING_Z_ROT = -5.0F * DEG_TO_RAD;
    private static final float RIGHT_RIGGING_Z_ROT = 5.0F * DEG_TO_RAD;
    private static final float LEFT_ARM_Z_ROT = 8.0F * DEG_TO_RAD;
    private static final float RIGHT_ARM_Z_ROT = -8.0F * DEG_TO_RAD;

    private final ModelPart bodySkin;
    private final ModelPart bodyDark;
    private final ModelPart bodyGlow;
    private final ModelPart headSkin;
    private final ModelPart headHair;
    private final ModelPart headGlow;
    private final ModelPart teeth;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart tentacles;
    private final ModelPart backShell;
    private final ModelPart shieldGlow;
    private final ModelPart leftRigging;
    private final ModelPart rightRigging;
    private final ModelPart leftRiggingGlow;
    private final ModelPart rightRiggingGlow;
    private final ModelPart ghostTail;

    public DeepOceanDestroyerModel(ModelPart root) {
        ModelPart modelRoot = root.getChild("root");
        this.bodySkin = modelRoot.getChild("body_skin");
        this.bodyDark = modelRoot.getChild("body_dark");
        this.bodyGlow = modelRoot.getChild("body_glow");
        this.headSkin = modelRoot.getChild("head_skin");
        this.headHair = modelRoot.getChild("head_hair");
        this.headGlow = modelRoot.getChild("head_glow");
        this.teeth = modelRoot.getChild("teeth");
        this.leftArm = modelRoot.getChild("left_arm");
        this.rightArm = modelRoot.getChild("right_arm");
        this.tentacles = modelRoot.getChild("tentacles");
        this.backShell = modelRoot.getChild("back_shell");
        this.shieldGlow = modelRoot.getChild("shield_glow");
        this.leftRigging = modelRoot.getChild("left_rigging");
        this.rightRigging = modelRoot.getChild("right_rigging");
        this.leftRiggingGlow = modelRoot.getChild("left_rigging_glow");
        this.rightRiggingGlow = modelRoot.getChild("right_rigging_glow");
        this.ghostTail = modelRoot.getChild("ghost_tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root",
                CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        addBody(root);
        addHead(root);
        addArms(root);
        addTentacles(root);
        addBackShell(root);
        addShieldGlow(root);
        addSideRigging(root);
        addGhostTail(root);

        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addBody(PartDefinition root) {
        root.addOrReplaceChild("body_skin",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.6F, -17.4F, -1.35F, 5.2F, 7.0F, 2.7F, new CubeDeformation(0.05F))
                        .texOffs(0, 12).addBox(-2.1F, -10.8F, -1.20F, 4.2F, 2.0F, 2.4F, new CubeDeformation(0.0F))
                        .texOffs(0, 17).addBox(-1.8F, -8.9F, -1.05F, 3.6F, 1.2F, 2.1F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body_dark",
                CubeListBuilder.create()
                        .texOffs(24, 0).addBox(-2.9F, -15.2F, -1.62F, 5.8F, 1.0F, 0.35F, new CubeDeformation(0.0F))
                        .texOffs(24, 2).addBox(-2.45F, -12.1F, -1.63F, 4.9F, 1.0F, 0.35F, new CubeDeformation(0.0F))
                        .texOffs(24, 4).addBox(-2.2F, -8.5F, -1.35F, 4.4F, 1.0F, 0.40F, new CubeDeformation(0.0F))
                        .texOffs(24, 6).addBox(-0.35F, -17.1F, -1.68F, 0.70F, 5.8F, 0.35F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body_glow",
                CubeListBuilder.create()
                        .texOffs(64, 0).addBox(-0.85F, -13.9F, -1.92F, 1.7F, 2.2F, 0.35F, new CubeDeformation(0.0F))
                        .texOffs(64, 4).addBox(-0.35F, -8.95F, -1.48F, 0.70F, 0.35F, 0.25F, new CubeDeformation(0.0F))
                        .texOffs(64, 6).addBox(-0.55F, -16.95F, -1.95F, 1.10F, 0.30F, 0.28F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addHead(PartDefinition root) {
        root.addOrReplaceChild("head_skin",
                CubeListBuilder.create()
                        .texOffs(0, 24).addBox(-2.35F, -4.25F, -2.05F, 4.7F, 4.8F, 4.1F, new CubeDeformation(0.03F))
                        .texOffs(0, 34).addBox(-2.15F, -0.30F, -2.28F, 4.3F, 1.1F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -19.1F, -0.25F));

        root.addOrReplaceChild("head_hair",
                CubeListBuilder.create()
                        .texOffs(20, 24).addBox(-2.65F, -4.55F, -1.80F, 5.3F, 1.5F, 4.0F, new CubeDeformation(0.10F))
                        .texOffs(20, 30).addBox(1.25F, -3.50F, -1.55F, 1.55F, 6.9F, 2.4F, new CubeDeformation(0.08F))
                        .texOffs(30, 30).addBox(-2.85F, -3.15F, -1.45F, 1.45F, 5.8F, 2.2F, new CubeDeformation(0.08F))
                        .texOffs(40, 24).addBox(-0.45F, -4.25F, -2.45F, 2.3F, 4.2F, 0.8F, new CubeDeformation(0.03F)),
                PartPose.offset(0.0F, -19.1F, -0.25F));

        root.addOrReplaceChild("head_glow",
                CubeListBuilder.create()
                        .texOffs(64, 10).addBox(-1.10F, -2.85F, -2.52F, 2.2F, 1.65F, 0.28F, new CubeDeformation(0.0F))
                        .texOffs(64, 14).addBox(1.42F, -3.40F, -2.18F, 0.38F, 1.30F, 0.35F, new CubeDeformation(0.0F))
                        .texOffs(64, 16).addBox(-1.80F, -3.15F, -2.18F, 0.38F, 1.20F, 0.35F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -19.1F, -0.25F));

        root.addOrReplaceChild("teeth",
                CubeListBuilder.create()
                        .texOffs(72, 10).addBox(-1.75F, -0.05F, -2.84F, 3.5F, 0.35F, 0.25F, new CubeDeformation(0.0F))
                        .texOffs(72, 12).addBox(-1.55F, 0.35F, -2.82F, 3.1F, 0.25F, 0.25F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -19.1F, -0.25F));
    }

    private static void addArms(PartDefinition root) {
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(0, 42).addBox(-1.40F, -0.75F, -0.90F, 1.55F, 4.7F, 1.8F, new CubeDeformation(0.02F))
                        .texOffs(0, 49).addBox(-1.20F, 3.60F, -0.75F, 1.8F, 3.0F, 1.5F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(-2.85F, -16.1F, 0.05F, -0.18F, 0.0F, LEFT_ARM_Z_ROT));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(8, 42).addBox(-0.15F, -0.75F, -0.90F, 1.55F, 4.7F, 1.8F, new CubeDeformation(0.02F))
                        .texOffs(8, 49).addBox(-0.60F, 3.60F, -0.75F, 1.8F, 3.0F, 1.5F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(2.85F, -16.1F, 0.05F, -0.18F, 0.0F, RIGHT_ARM_Z_ROT));
    }

    private static void addTentacles(PartDefinition root) {
        root.addOrReplaceChild("tentacles",
                CubeListBuilder.create()
                        .texOffs(48, 0).addBox(-4.45F, -13.5F, -0.45F, 2.2F, 2.7F, 1.8F, new CubeDeformation(0.28F))
                        .texOffs(48, 6).addBox(2.35F, -13.0F, -0.45F, 2.0F, 3.0F, 1.8F, new CubeDeformation(0.28F))
                        .texOffs(48, 12).addBox(-3.60F, -9.25F, -0.55F, 2.35F, 2.5F, 1.9F, new CubeDeformation(0.24F))
                        .texOffs(48, 18).addBox(1.30F, -9.10F, -0.55F, 2.55F, 2.7F, 1.9F, new CubeDeformation(0.24F))
                        .texOffs(48, 24).addBox(-1.55F, -6.95F, -0.45F, 3.10F, 2.2F, 1.7F, new CubeDeformation(0.22F))
                        .texOffs(48, 30).addBox(-3.55F, -5.20F, -0.25F, 2.45F, 1.7F, 1.4F, new CubeDeformation(0.20F))
                        .texOffs(48, 35).addBox(1.10F, -5.25F, -0.25F, 2.45F, 1.7F, 1.4F, new CubeDeformation(0.20F)),
                PartPose.ZERO);
    }

    private static void addBackShell(PartDefinition root) {
        PartDefinition shell = root.addOrReplaceChild("back_shell",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, -12.8F, 3.05F));

        shell.addOrReplaceChild("upper_plate",
                CubeListBuilder.create()
                        .texOffs(0, 64).addBox(-7.4F, -9.0F, 0.0F, 14.8F, 8.2F, 0.9F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(0.2F, -0.6F, 0.0F, 0.0F, 0.0F, -7.0F * DEG_TO_RAD));

        shell.addOrReplaceChild("left_plate",
                CubeListBuilder.create()
                        .texOffs(32, 64).addBox(-4.6F, -7.2F, 0.0F, 6.8F, 14.2F, 0.9F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(-5.3F, 0.0F, 0.0F, 0.0F, 0.0F, 20.0F * DEG_TO_RAD));

        shell.addOrReplaceChild("right_plate",
                CubeListBuilder.create()
                        .texOffs(56, 64).addBox(-2.2F, -7.2F, 0.0F, 6.8F, 14.2F, 0.9F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(5.3F, 0.0F, 0.0F, 0.0F, 0.0F, -20.0F * DEG_TO_RAD));

        shell.addOrReplaceChild("lower_plate",
                CubeListBuilder.create()
                        .texOffs(80, 64).addBox(-5.7F, -0.8F, 0.0F, 11.4F, 7.0F, 0.9F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(-0.1F, 5.0F, 0.0F, 0.0F, 0.0F, 9.0F * DEG_TO_RAD));

        shell.addOrReplaceChild("inner_shadow",
                CubeListBuilder.create()
                        .texOffs(0, 78).addBox(-5.2F, -6.1F, -0.18F, 10.4F, 12.2F, 0.35F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addShieldGlow(PartDefinition root) {
        PartDefinition glow = root.addOrReplaceChild("shield_glow",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, -12.8F, 2.76F));

        addSpoke(glow, "spoke_0", 0.0F, 6.7F);
        addSpoke(glow, "spoke_1", 24.0F * DEG_TO_RAD, 7.4F);
        addSpoke(glow, "spoke_2", -24.0F * DEG_TO_RAD, 7.4F);
        addSpoke(glow, "spoke_3", 48.0F * DEG_TO_RAD, 7.6F);
        addSpoke(glow, "spoke_4", -48.0F * DEG_TO_RAD, 7.6F);
        addSpoke(glow, "spoke_5", 76.0F * DEG_TO_RAD, 7.0F);
        addSpoke(glow, "spoke_6", -76.0F * DEG_TO_RAD, 7.0F);

        glow.addOrReplaceChild("lower_arc",
                CubeListBuilder.create()
                        .texOffs(72, 18).addBox(-4.9F, 3.8F, -0.08F, 9.8F, 0.38F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 9.0F * DEG_TO_RAD));

        glow.addOrReplaceChild("cross_arc",
                CubeListBuilder.create()
                        .texOffs(72, 20).addBox(-6.2F, -1.6F, -0.08F, 12.4F, 0.34F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addSpoke(PartDefinition parent, String name, float zRot, float length) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(64, 18).addBox(-0.17F, -length, -0.08F, 0.34F, length, 0.22F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.5F, 0.0F, 0.0F, 0.0F, zRot));
    }

    private static void addSideRigging(PartDefinition root) {
        root.addOrReplaceChild("left_rigging",
                CubeListBuilder.create()
                        .texOffs(0, 96).addBox(-4.9F, -1.05F, -0.85F, 3.6F, 2.1F, 1.7F, new CubeDeformation(0.10F))
                        .texOffs(0, 101).addBox(-7.1F, -0.22F, -0.22F, 3.3F, 0.44F, 0.44F, new CubeDeformation(0.0F))
                        .texOffs(18, 96).addBox(-1.55F, -1.30F, -0.72F, 2.15F, 2.6F, 1.44F, new CubeDeformation(0.05F))
                        .texOffs(30, 96).addBox(-5.3F, -2.10F, 0.00F, 3.4F, 0.00F, 2.8F, new CubeDeformation(0.0F))
                        .texOffs(30, 100).addBox(-5.3F, 2.10F, 0.00F, 3.4F, 0.00F, 2.8F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.8F, -11.3F, 1.1F, 0.0F, LEFT_RIGGING_Y_ROT, LEFT_RIGGING_Z_ROT));

        root.addOrReplaceChild("right_rigging",
                CubeListBuilder.create()
                        .texOffs(0, 106).addBox(1.3F, -1.05F, -0.85F, 3.6F, 2.1F, 1.7F, new CubeDeformation(0.10F))
                        .texOffs(0, 111).addBox(3.8F, -0.22F, -0.22F, 3.3F, 0.44F, 0.44F, new CubeDeformation(0.0F))
                        .texOffs(18, 106).addBox(-0.60F, -1.30F, -0.72F, 2.15F, 2.6F, 1.44F, new CubeDeformation(0.05F))
                        .texOffs(30, 106).addBox(1.9F, -2.10F, 0.00F, 3.4F, 0.00F, 2.8F, new CubeDeformation(0.0F))
                        .texOffs(30, 110).addBox(1.9F, 2.10F, 0.00F, 3.4F, 0.00F, 2.8F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.8F, -11.3F, 1.1F, 0.0F, RIGHT_RIGGING_Y_ROT, RIGHT_RIGGING_Z_ROT));

        root.addOrReplaceChild("left_rigging_glow",
                CubeListBuilder.create()
                        .texOffs(64, 26).addBox(-6.9F, -0.08F, -0.38F, 3.0F, 0.16F, 0.16F, new CubeDeformation(0.0F))
                        .texOffs(64, 28).addBox(-4.55F, -0.78F, -0.96F, 2.25F, 0.22F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(64, 30).addBox(-0.62F, -0.35F, -0.88F, 0.38F, 0.70F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.8F, -11.3F, 1.1F, 0.0F, LEFT_RIGGING_Y_ROT, LEFT_RIGGING_Z_ROT));

        root.addOrReplaceChild("right_rigging_glow",
                CubeListBuilder.create()
                        .texOffs(64, 34).addBox(3.9F, -0.08F, -0.38F, 3.0F, 0.16F, 0.16F, new CubeDeformation(0.0F))
                        .texOffs(64, 36).addBox(2.30F, -0.78F, -0.96F, 2.25F, 0.22F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(64, 38).addBox(0.24F, -0.35F, -0.88F, 0.38F, 0.70F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.8F, -11.3F, 1.1F, 0.0F, RIGHT_RIGGING_Y_ROT, RIGHT_RIGGING_Z_ROT));
    }

    private static void addGhostTail(PartDefinition root) {
        root.addOrReplaceChild("ghost_tail",
                CubeListBuilder.create()
                        .texOffs(96, 0).addBox(-1.35F, -3.0F, 0.0F, 2.7F, 5.5F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(104, 0).addBox(-2.35F, -1.0F, 0.0F, 4.7F, 2.2F, 0.20F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.6F, -21.0F, 4.0F, 0.0F, 0.0F, -27.0F * DEG_TO_RAD));
    }

    @Override
    public void setupAnim(DeepOceanDestroyerEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float headYaw = netHeadYaw * DEG_TO_RAD * 0.45F;
        float headPitchRot = headPitch * DEG_TO_RAD * 0.35F;
        setHeadRotation(headSkin, headYaw, headPitchRot);
        setHeadRotation(headHair, headYaw, headPitchRot);
        setHeadRotation(headGlow, headYaw, headPitchRot);
        setHeadRotation(teeth, headYaw, headPitchRot);

        float walk = Math.min(limbSwingAmount, 1.0F);
        rightArm.xRot = -0.18F + Mth.cos(limbSwing * 0.6662F) * 0.24F * walk;
        leftArm.xRot = -0.18F + Mth.cos(limbSwing * 0.6662F + Mth.PI) * 0.24F * walk;
        rightArm.zRot = RIGHT_ARM_Z_ROT - Mth.sin(ageInTicks * 0.05F) * 0.04F;
        leftArm.zRot = LEFT_ARM_Z_ROT + Mth.sin(ageInTicks * 0.05F) * 0.04F;

        float rigPulse = Mth.sin(ageInTicks * 0.07F) * 0.045F;
        setRiggingRotation(leftRigging, LEFT_RIGGING_Y_ROT + rigPulse, LEFT_RIGGING_Z_ROT + rigPulse * 0.35F);
        setRiggingRotation(leftRiggingGlow, leftRigging.yRot, leftRigging.zRot);
        setRiggingRotation(rightRigging, RIGHT_RIGGING_Y_ROT - rigPulse, RIGHT_RIGGING_Z_ROT - rigPulse * 0.35F);
        setRiggingRotation(rightRiggingGlow, rightRigging.yRot, rightRigging.zRot);

        float shellSway = Mth.sin(ageInTicks * 0.035F) * 0.025F;
        backShell.zRot = shellSway;
        shieldGlow.zRot = shellSway;
        ghostTail.zRot = -27.0F * DEG_TO_RAD + Mth.sin(ageInTicks * 0.10F) * 0.10F;
    }

    private static void setHeadRotation(ModelPart part, float yRot, float xRot) {
        part.yRot = yRot;
        part.xRot = xRot;
        part.zRot = 0.0F;
    }

    private static void setRiggingRotation(ModelPart part, float yRot, float zRot) {
        part.xRot = 0.0F;
        part.yRot = yRot;
        part.zRot = zRot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, int color) {
        backShell.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_COLOR);
        shieldGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        ghostTail.render(poseStack, vertexConsumer, packedLight, packedOverlay, FIN_COLOR);
        tentacles.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        leftRigging.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_COLOR);
        rightRigging.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_COLOR);
        bodySkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        bodyDark.render(poseStack, vertexConsumer, packedLight, packedOverlay, DARK_COLOR);
        bodyGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        leftArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        rightArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        headSkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        headHair.render(poseStack, vertexConsumer, packedLight, packedOverlay, HAIR_COLOR);
        teeth.render(poseStack, vertexConsumer, packedLight, packedOverlay, TEETH_COLOR);
        headGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        leftRiggingGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        rightRiggingGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
    }
}
