package com.piranport.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
import com.piranport.npc.deepocean.DeepOceanLightCarrierEntity;
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

public class DeepOceanLightCarrierModel extends EntityModel<DeepOceanLightCarrierEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "deep_ocean_light_carrier"), "main");

    private static final float DEG_TO_RAD = Mth.PI / 180.0F;
    private static final int SKIN_COLOR = 0xFFEADDD9;
    private static final int HAIR_COLOR = 0xFFD9D7DA;
    private static final int HAIR_SHADOW_COLOR = 0xFF9E9AA2;
    private static final int DRESS_COLOR = 0xFF211D25;
    private static final int DRESS_SHADOW_COLOR = 0xFF121016;
    private static final int FRILL_COLOR = 0xFFE7DFDC;
    private static final int METAL_COLOR = 0xFF37323B;
    private static final int METAL_DARK_COLOR = 0xFF17151B;
    private static final int METAL_EDGE_COLOR = 0xFF625B66;
    private static final int GLOW_COLOR = 0xFFFF45C8;
    private static final int SHOE_COLOR = 0xFF2A242E;

    private static final float LEFT_ARM_Z_ROT = 8.0F * DEG_TO_RAD;
    private static final float RIGHT_ARM_Z_ROT = -8.0F * DEG_TO_RAD;
    private static final float LEFT_DECK_Y_ROT = 14.0F * DEG_TO_RAD;
    private static final float RIGHT_DECK_Y_ROT = -14.0F * DEG_TO_RAD;
    private static final float LEFT_DECK_Z_ROT = -7.0F * DEG_TO_RAD;
    private static final float RIGHT_DECK_Z_ROT = 6.0F * DEG_TO_RAD;
    private static final float LEFT_POD_Y_ROT = 15.0F * DEG_TO_RAD;
    private static final float RIGHT_POD_Y_ROT = -15.0F * DEG_TO_RAD;

    private final ModelPart bodySkin;
    private final ModelPart bodyDress;
    private final ModelPart bodyShadow;
    private final ModelPart bodyGlow;
    private final ModelPart skirtFrill;
    private final ModelPart headSkin;
    private final ModelPart headHair;
    private final ModelPart headHairShadow;
    private final ModelPart faceGlow;
    private final ModelPart leftArmSkin;
    private final ModelPart rightArmSkin;
    private final ModelPart leftArmDark;
    private final ModelPart rightArmDark;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart legGlow;
    private final ModelPart shoes;
    private final ModelPart topDeck;
    private final ModelPart topDeckEdge;
    private final ModelPart topDeckGlow;
    private final ModelPart leftFlightDeck;
    private final ModelPart rightFlightDeck;
    private final ModelPart leftFlightDeckEdge;
    private final ModelPart rightFlightDeckEdge;
    private final ModelPart flightDeckGlow;
    private final ModelPart leftPod;
    private final ModelPart rightPod;
    private final ModelPart podGlow;
    private final ModelPart tail;
    private final ModelPart tailGlow;

    public DeepOceanLightCarrierModel(ModelPart root) {
        ModelPart modelRoot = root.getChild("root");
        this.bodySkin = modelRoot.getChild("body_skin");
        this.bodyDress = modelRoot.getChild("body_dress");
        this.bodyShadow = modelRoot.getChild("body_shadow");
        this.bodyGlow = modelRoot.getChild("body_glow");
        this.skirtFrill = modelRoot.getChild("skirt_frill");
        this.headSkin = modelRoot.getChild("head_skin");
        this.headHair = modelRoot.getChild("head_hair");
        this.headHairShadow = modelRoot.getChild("head_hair_shadow");
        this.faceGlow = modelRoot.getChild("face_glow");
        this.leftArmSkin = modelRoot.getChild("left_arm_skin");
        this.rightArmSkin = modelRoot.getChild("right_arm_skin");
        this.leftArmDark = modelRoot.getChild("left_arm_dark");
        this.rightArmDark = modelRoot.getChild("right_arm_dark");
        this.leftLeg = modelRoot.getChild("left_leg");
        this.rightLeg = modelRoot.getChild("right_leg");
        this.legGlow = modelRoot.getChild("leg_glow");
        this.shoes = modelRoot.getChild("shoes");
        this.topDeck = modelRoot.getChild("top_deck");
        this.topDeckEdge = modelRoot.getChild("top_deck_edge");
        this.topDeckGlow = modelRoot.getChild("top_deck_glow");
        this.leftFlightDeck = modelRoot.getChild("left_flight_deck");
        this.rightFlightDeck = modelRoot.getChild("right_flight_deck");
        this.leftFlightDeckEdge = modelRoot.getChild("left_flight_deck_edge");
        this.rightFlightDeckEdge = modelRoot.getChild("right_flight_deck_edge");
        this.flightDeckGlow = modelRoot.getChild("flight_deck_glow");
        this.leftPod = modelRoot.getChild("left_pod");
        this.rightPod = modelRoot.getChild("right_pod");
        this.podGlow = modelRoot.getChild("pod_glow");
        this.tail = modelRoot.getChild("tail");
        this.tailGlow = modelRoot.getChild("tail_glow");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root",
                CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        addBody(root);
        addHead(root);
        addArms(root);
        addLegs(root);
        addTopDeck(root);
        addFlightDecks(root);
        addSidePods(root);
        addTail(root);

        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addBody(PartDefinition root) {
        root.addOrReplaceChild("body_skin",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.85F, -15.5F, -1.18F, 3.7F, 3.65F, 2.35F, new CubeDeformation(0.04F))
                        .texOffs(0, 7).addBox(-1.65F, -11.95F, -1.02F, 3.3F, 1.75F, 2.04F, new CubeDeformation(0.0F))
                        .texOffs(0, 12).addBox(-0.75F, -17.0F, -0.72F, 1.5F, 1.25F, 1.45F, new CubeDeformation(0.02F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body_dress",
                CubeListBuilder.create()
                        .texOffs(20, 0).addBox(-2.25F, -16.65F, -1.42F, 4.5F, 5.25F, 2.84F, new CubeDeformation(0.05F))
                        .texOffs(20, 8).addBox(-2.55F, -11.55F, -1.34F, 5.1F, 1.20F, 2.68F, new CubeDeformation(0.03F))
                        .texOffs(20, 12).addBox(-3.15F, -10.45F, -1.22F, 6.3F, 2.15F, 2.44F, new CubeDeformation(0.05F))
                        .texOffs(38, 0).addBox(-0.28F, -16.4F, -1.74F, 0.56F, 5.8F, 0.28F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body_shadow",
                CubeListBuilder.create()
                        .texOffs(42, 0).addBox(-2.05F, -16.35F, -1.72F, 4.1F, 0.85F, 0.36F, new CubeDeformation(0.0F))
                        .texOffs(42, 2).addBox(-1.85F, -13.6F, -1.73F, 3.7F, 0.72F, 0.36F, new CubeDeformation(0.0F))
                        .texOffs(42, 4).addBox(-2.65F, -10.1F, -1.40F, 5.3F, 0.48F, 0.28F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body_glow",
                CubeListBuilder.create()
                        .texOffs(64, 0).addBox(-0.23F, -16.05F, -1.98F, 0.46F, 4.3F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(64, 5).addBox(-0.82F, -11.0F, -1.52F, 1.64F, 0.24F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(64, 7).addBox(-0.28F, -8.92F, -1.36F, 0.56F, 0.42F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("skirt_frill",
                CubeListBuilder.create()
                        .texOffs(0, 18).addBox(-3.35F, -8.5F, -1.28F, 6.7F, 0.52F, 2.56F, new CubeDeformation(0.02F))
                        .texOffs(0, 21).addBox(-2.72F, -8.05F, -1.18F, 0.54F, 0.62F, 2.36F, new CubeDeformation(0.0F))
                        .texOffs(6, 21).addBox(-1.36F, -8.05F, -1.18F, 0.54F, 0.62F, 2.36F, new CubeDeformation(0.0F))
                        .texOffs(12, 21).addBox(0.82F, -8.05F, -1.18F, 0.54F, 0.62F, 2.36F, new CubeDeformation(0.0F))
                        .texOffs(18, 21).addBox(2.18F, -8.05F, -1.18F, 0.54F, 0.62F, 2.36F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addHead(PartDefinition root) {
        root.addOrReplaceChild("head_skin",
                CubeListBuilder.create()
                        .texOffs(0, 28).addBox(-2.20F, -4.02F, -2.02F, 4.4F, 4.55F, 4.04F, new CubeDeformation(0.03F))
                        .texOffs(0, 38).addBox(-1.48F, -0.12F, -2.34F, 2.96F, 0.74F, 0.64F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -19.05F, -0.24F));

        root.addOrReplaceChild("head_hair",
                CubeListBuilder.create()
                        .texOffs(18, 28).addBox(-2.58F, -4.36F, -1.78F, 5.16F, 1.42F, 3.95F, new CubeDeformation(0.10F))
                        .texOffs(18, 34).addBox(-2.76F, -3.10F, -1.42F, 1.25F, 5.7F, 2.18F, new CubeDeformation(0.08F))
                        .texOffs(26, 34).addBox(1.50F, -3.45F, -1.42F, 1.25F, 6.35F, 2.28F, new CubeDeformation(0.08F))
                        .texOffs(34, 34).addBox(-0.88F, -4.05F, -2.44F, 2.55F, 4.05F, 0.74F, new CubeDeformation(0.03F))
                        .texOffs(44, 28).addBox(-2.00F, -3.58F, -2.34F, 1.15F, 3.35F, 0.64F, new CubeDeformation(0.03F)),
                PartPose.offset(0.0F, -19.05F, -0.24F));

        root.addOrReplaceChild("head_hair_shadow",
                CubeListBuilder.create()
                        .texOffs(50, 28).addBox(-1.62F, -3.90F, -2.52F, 0.82F, 3.45F, 0.42F, new CubeDeformation(0.02F))
                        .texOffs(50, 34).addBox(0.24F, -4.08F, -2.50F, 0.78F, 3.10F, 0.42F, new CubeDeformation(0.02F))
                        .texOffs(50, 40).addBox(1.12F, -3.52F, -2.42F, 0.54F, 2.35F, 0.36F, new CubeDeformation(0.02F)),
                PartPose.offset(0.0F, -19.05F, -0.24F));

        root.addOrReplaceChild("face_glow",
                CubeListBuilder.create()
                        .texOffs(64, 12).addBox(-1.28F, -2.46F, -2.54F, 0.70F, 0.70F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(64, 14).addBox(0.58F, -2.46F, -2.54F, 0.70F, 0.70F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(64, 16).addBox(-0.45F, -1.08F, -2.55F, 0.90F, 0.20F, 0.20F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -19.05F, -0.24F));
    }

    private static void addArms(PartDefinition root) {
        root.addOrReplaceChild("left_arm_skin",
                CubeListBuilder.create()
                        .texOffs(0, 46).addBox(-1.22F, -0.60F, -0.70F, 1.34F, 3.65F, 1.40F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(-2.38F, -15.95F, -0.05F, -0.16F, 0.0F, LEFT_ARM_Z_ROT));

        root.addOrReplaceChild("right_arm_skin",
                CubeListBuilder.create()
                        .texOffs(6, 46).addBox(-0.12F, -0.60F, -0.70F, 1.34F, 3.65F, 1.40F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(2.38F, -15.95F, -0.05F, -0.16F, 0.0F, RIGHT_ARM_Z_ROT));

        root.addOrReplaceChild("left_arm_dark",
                CubeListBuilder.create()
                        .texOffs(0, 52).addBox(-1.18F, 2.65F, -0.74F, 1.62F, 2.85F, 1.48F, new CubeDeformation(0.03F))
                        .texOffs(0, 58).addBox(-0.25F, 3.85F, -1.64F, 4.45F, 0.78F, 1.32F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(-2.38F, -15.95F, -0.05F, -0.16F, 0.0F, LEFT_ARM_Z_ROT));

        root.addOrReplaceChild("right_arm_dark",
                CubeListBuilder.create()
                        .texOffs(12, 52).addBox(-0.44F, 2.65F, -0.74F, 1.62F, 2.85F, 1.48F, new CubeDeformation(0.03F))
                        .texOffs(12, 58).addBox(-4.20F, 3.72F, -1.66F, 4.45F, 0.78F, 1.32F, new CubeDeformation(0.02F)),
                PartPose.offsetAndRotation(2.38F, -15.95F, -0.05F, -0.16F, 0.0F, RIGHT_ARM_Z_ROT));
    }

    private static void addLegs(PartDefinition root) {
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(26, 46).addBox(-0.92F, -0.15F, -0.78F, 1.42F, 5.35F, 1.56F, new CubeDeformation(0.04F))
                        .texOffs(26, 54).addBox(-0.82F, 4.90F, -0.72F, 1.25F, 5.55F, 1.44F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(-0.72F, -8.0F, 0.02F, -3.0F * DEG_TO_RAD, 0.0F, 2.5F * DEG_TO_RAD));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(34, 46).addBox(-0.50F, -0.15F, -0.78F, 1.42F, 5.35F, 1.56F, new CubeDeformation(0.04F))
                        .texOffs(34, 54).addBox(-0.42F, 4.90F, -0.72F, 1.25F, 5.55F, 1.44F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(0.72F, -8.0F, 0.02F, 4.0F * DEG_TO_RAD, 0.0F, -2.5F * DEG_TO_RAD));

        root.addOrReplaceChild("leg_glow",
                CubeListBuilder.create()
                        .texOffs(64, 46).addBox(-1.37F, -6.50F, -0.88F, 0.20F, 3.10F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(64, 51).addBox(1.17F, -4.05F, -0.88F, 0.20F, 3.20F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(64, 56).addBox(-1.45F, 1.98F, -1.02F, 0.82F, 0.22F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(64, 58).addBox(0.63F, 1.98F, -1.02F, 0.82F, 0.22F, 0.20F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("shoes",
                CubeListBuilder.create()
                        .texOffs(44, 46).addBox(-1.58F, 2.00F, -1.24F, 1.82F, 0.70F, 2.04F, new CubeDeformation(0.04F))
                        .texOffs(44, 50).addBox(0.24F, 2.00F, -1.24F, 1.82F, 0.70F, 2.04F, new CubeDeformation(0.04F))
                        .texOffs(64, 61).addBox(-1.54F, 2.50F, -1.36F, 1.70F, 0.24F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(64, 63).addBox(0.32F, 2.50F, -1.36F, 1.70F, 0.24F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addTopDeck(PartDefinition root) {
        PartDefinition deck = root.addOrReplaceChild("top_deck",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -24.0F, 0.10F, 0.0F, 0.0F, -2.0F * DEG_TO_RAD));

        deck.addOrReplaceChild("front_span",
                CubeListBuilder.create()
                        .texOffs(0, 70).addBox(-4.75F, -0.36F, -2.55F, 9.50F, 0.72F, 0.88F, new CubeDeformation(0.04F)),
                PartPose.ZERO);
        deck.addOrReplaceChild("back_span",
                CubeListBuilder.create()
                        .texOffs(0, 74).addBox(-4.70F, -0.36F, 1.65F, 9.40F, 0.72F, 0.88F, new CubeDeformation(0.04F)),
                PartPose.ZERO);
        deck.addOrReplaceChild("left_span",
                CubeListBuilder.create()
                        .texOffs(22, 70).addBox(-4.75F, -0.34F, -1.88F, 1.10F, 0.68F, 3.80F, new CubeDeformation(0.04F)),
                PartPose.ZERO);
        deck.addOrReplaceChild("right_span",
                CubeListBuilder.create()
                        .texOffs(30, 70).addBox(3.65F, -0.34F, -1.88F, 1.10F, 0.68F, 3.80F, new CubeDeformation(0.04F)),
                PartPose.ZERO);

        PartDefinition edge = root.addOrReplaceChild("top_deck_edge",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -24.0F, 0.10F, 0.0F, 0.0F, -2.0F * DEG_TO_RAD));

        edge.addOrReplaceChild("front_edge",
                CubeListBuilder.create()
                        .texOffs(42, 70).addBox(-4.95F, -0.50F, -2.72F, 9.90F, 0.42F, 0.30F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
        edge.addOrReplaceChild("back_edge",
                CubeListBuilder.create()
                        .texOffs(42, 73).addBox(-4.95F, -0.50F, 2.42F, 9.90F, 0.42F, 0.30F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
        edge.addOrReplaceChild("left_edge",
                CubeListBuilder.create()
                        .texOffs(42, 76).addBox(-5.05F, -0.50F, -2.35F, 0.32F, 0.42F, 4.70F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
        edge.addOrReplaceChild("right_edge",
                CubeListBuilder.create()
                        .texOffs(50, 76).addBox(4.73F, -0.50F, -2.35F, 0.32F, 0.42F, 4.70F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("top_deck_glow",
                CubeListBuilder.create()
                        .texOffs(64, 68).addBox(-3.85F, -0.56F, -2.92F, 2.35F, 0.24F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(64, 71).addBox(1.42F, -0.56F, -2.92F, 2.35F, 0.24F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(64, 74).addBox(-4.98F, -0.55F, -1.82F, 0.24F, 0.24F, 2.94F, new CubeDeformation(0.0F))
                        .texOffs(64, 78).addBox(4.72F, -0.55F, -1.82F, 0.24F, 0.24F, 2.94F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -24.0F, 0.10F, 0.0F, 0.0F, -2.0F * DEG_TO_RAD));
    }

    private static void addFlightDecks(PartDefinition root) {
        root.addOrReplaceChild("left_flight_deck",
                CubeListBuilder.create()
                        .texOffs(0, 84).addBox(-2.18F, -10.90F, -0.42F, 4.36F, 20.8F, 0.84F, new CubeDeformation(0.04F))
                        .texOffs(12, 84).addBox(-1.62F, 9.30F, -0.40F, 3.24F, 2.00F, 0.80F, new CubeDeformation(0.03F))
                        .texOffs(22, 84).addBox(-1.80F, -12.45F, -0.40F, 3.60F, 2.00F, 0.80F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(-6.20F, -14.20F, 2.18F, 0.0F, LEFT_DECK_Y_ROT, LEFT_DECK_Z_ROT));

        root.addOrReplaceChild("right_flight_deck",
                CubeListBuilder.create()
                        .texOffs(0, 108).addBox(-2.18F, -10.50F, -0.42F, 4.36F, 20.2F, 0.84F, new CubeDeformation(0.04F))
                        .texOffs(12, 108).addBox(-1.62F, 9.10F, -0.40F, 3.24F, 1.92F, 0.80F, new CubeDeformation(0.03F))
                        .texOffs(22, 108).addBox(-1.80F, -12.00F, -0.40F, 3.60F, 1.92F, 0.80F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(6.45F, -14.00F, 2.12F, 0.0F, RIGHT_DECK_Y_ROT, RIGHT_DECK_Z_ROT));

        root.addOrReplaceChild("left_flight_deck_edge",
                CubeListBuilder.create()
                        .texOffs(34, 84).addBox(-2.46F, -11.00F, -0.55F, 0.42F, 21.2F, 1.10F, new CubeDeformation(0.0F))
                        .texOffs(40, 84).addBox(2.04F, -11.00F, -0.55F, 0.42F, 21.2F, 1.10F, new CubeDeformation(0.0F))
                        .texOffs(46, 84).addBox(-2.10F, -11.24F, -0.58F, 4.20F, 0.48F, 1.16F, new CubeDeformation(0.0F))
                        .texOffs(46, 88).addBox(-2.10F, 9.82F, -0.58F, 4.20F, 0.48F, 1.16F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-6.20F, -14.20F, 2.18F, 0.0F, LEFT_DECK_Y_ROT, LEFT_DECK_Z_ROT));

        root.addOrReplaceChild("right_flight_deck_edge",
                CubeListBuilder.create()
                        .texOffs(34, 108).addBox(-2.46F, -10.60F, -0.55F, 0.42F, 20.6F, 1.10F, new CubeDeformation(0.0F))
                        .texOffs(40, 108).addBox(2.04F, -10.60F, -0.55F, 0.42F, 20.6F, 1.10F, new CubeDeformation(0.0F))
                        .texOffs(46, 108).addBox(-2.10F, -10.86F, -0.58F, 4.20F, 0.48F, 1.16F, new CubeDeformation(0.0F))
                        .texOffs(46, 112).addBox(-2.10F, 9.42F, -0.58F, 4.20F, 0.48F, 1.16F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(6.45F, -14.00F, 2.12F, 0.0F, RIGHT_DECK_Y_ROT, RIGHT_DECK_Z_ROT));

        root.addOrReplaceChild("flight_deck_glow",
                CubeListBuilder.create()
                        .texOffs(76, 84).addBox(-7.86F, -24.20F, 1.50F, 0.26F, 15.10F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(80, 84).addBox(-5.95F, -20.05F, 1.46F, 0.24F, 7.60F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(84, 84).addBox(-8.55F, -6.00F, 1.44F, 1.92F, 0.32F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(76, 104).addBox(7.74F, -24.00F, 1.50F, 0.26F, 15.00F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(80, 104).addBox(5.62F, -19.55F, 1.46F, 0.24F, 7.20F, 0.24F, new CubeDeformation(0.0F))
                        .texOffs(84, 104).addBox(6.48F, -5.96F, 1.44F, 1.90F, 0.32F, 0.24F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addSidePods(PartDefinition root) {
        root.addOrReplaceChild("left_pod",
                CubeListBuilder.create()
                        .texOffs(92, 72).addBox(-2.00F, -1.10F, -1.05F, 3.25F, 2.20F, 2.10F, new CubeDeformation(0.12F))
                        .texOffs(92, 78).addBox(-2.85F, -0.28F, -0.24F, 1.90F, 0.56F, 0.48F, new CubeDeformation(0.0F))
                        .texOffs(104, 72).addBox(0.72F, -1.36F, -0.82F, 1.42F, 2.72F, 1.64F, new CubeDeformation(0.06F)),
                PartPose.offsetAndRotation(-3.80F, -11.05F, 1.02F, 0.0F, LEFT_POD_Y_ROT, -4.0F * DEG_TO_RAD));

        root.addOrReplaceChild("right_pod",
                CubeListBuilder.create()
                        .texOffs(92, 84).addBox(-1.25F, -1.10F, -1.05F, 3.25F, 2.20F, 2.10F, new CubeDeformation(0.12F))
                        .texOffs(92, 90).addBox(0.95F, -0.28F, -0.24F, 1.90F, 0.56F, 0.48F, new CubeDeformation(0.0F))
                        .texOffs(104, 84).addBox(-2.14F, -1.36F, -0.82F, 1.42F, 2.72F, 1.64F, new CubeDeformation(0.06F)),
                PartPose.offsetAndRotation(3.95F, -11.10F, 1.00F, 0.0F, RIGHT_POD_Y_ROT, 4.0F * DEG_TO_RAD));

        root.addOrReplaceChild("pod_glow",
                CubeListBuilder.create()
                        .texOffs(76, 72).addBox(-5.78F, -11.24F, 0.02F, 1.55F, 0.22F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(76, 75).addBox(-3.32F, -11.78F, 0.01F, 0.24F, 1.22F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(76, 78).addBox(4.24F, -11.24F, 0.02F, 1.55F, 0.22F, 0.22F, new CubeDeformation(0.0F))
                        .texOffs(76, 81).addBox(3.04F, -11.78F, 0.01F, 0.24F, 1.22F, 0.22F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addTail(PartDefinition root) {
        PartDefinition tail = root.addOrReplaceChild("tail",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.15F, -7.50F, 2.78F, 0.0F, 0.0F, -6.0F * DEG_TO_RAD));

        addSegment(tail, "base", -0.30F, -0.10F, 0.0F, -8.0F * DEG_TO_RAD, 4.60F, 0.42F);
        addSegment(tail, "middle", -3.75F, 1.00F, 0.0F, -18.0F * DEG_TO_RAD, 4.80F, 0.40F);
        addSegment(tail, "lower", -6.85F, 2.74F, 0.0F, 6.0F * DEG_TO_RAD, 3.90F, 0.38F);

        tail.addOrReplaceChild("tail_fin",
                CubeListBuilder.create()
                        .texOffs(98, 104).addBox(-1.85F, -1.25F, -0.22F, 3.70F, 2.50F, 0.44F, new CubeDeformation(0.03F))
                        .texOffs(98, 110).addBox(-0.78F, -2.40F, -0.20F, 1.56F, 2.15F, 0.40F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(-8.98F, 2.66F, 0.0F, 0.0F, 0.0F, -42.0F * DEG_TO_RAD));

        root.addOrReplaceChild("tail_glow",
                CubeListBuilder.create()
                        .texOffs(76, 114).addBox(-2.55F, -7.32F, 2.58F, 0.22F, 3.20F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(76, 119).addBox(-5.72F, -5.82F, 2.56F, 0.22F, 2.80F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(80, 114).addBox(-8.26F, -4.84F, 2.54F, 0.22F, 2.16F, 0.20F, new CubeDeformation(0.0F))
                        .texOffs(84, 114).addBox(-8.92F, -4.74F, 2.52F, 1.28F, 0.22F, 0.20F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addSegment(PartDefinition parent, String name, float x, float y, float z,
                                   float zRot, float length, float thickness) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(98, 96).addBox(-length * 0.5F, -thickness * 0.5F, -thickness * 0.5F,
                                length, thickness, thickness, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(x, y, z, 0.0F, 0.0F, zRot));
    }

    @Override
    public void setupAnim(DeepOceanLightCarrierEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float headYaw = netHeadYaw * DEG_TO_RAD * 0.42F;
        float headPitchRot = headPitch * DEG_TO_RAD * 0.30F;
        setHeadRotation(headSkin, headYaw, headPitchRot);
        setHeadRotation(headHair, headYaw, headPitchRot);
        setHeadRotation(headHairShadow, headYaw, headPitchRot);
        setHeadRotation(faceGlow, headYaw, headPitchRot);

        float walk = Math.min(limbSwingAmount, 1.0F);
        float armSwing = Mth.cos(limbSwing * 0.6662F) * 0.12F * walk;
        setArmRotation(leftArmSkin, -0.16F - armSwing, LEFT_ARM_Z_ROT);
        setArmRotation(leftArmDark, leftArmSkin.xRot, LEFT_ARM_Z_ROT);
        setArmRotation(rightArmSkin, -0.16F + armSwing, RIGHT_ARM_Z_ROT);
        setArmRotation(rightArmDark, rightArmSkin.xRot, RIGHT_ARM_Z_ROT);

        leftLeg.xRot = -3.0F * DEG_TO_RAD + Mth.cos(limbSwing * 0.6662F + Mth.PI) * 0.24F * walk;
        rightLeg.xRot = 4.0F * DEG_TO_RAD + Mth.cos(limbSwing * 0.6662F) * 0.24F * walk;

        float pulse = Mth.sin(ageInTicks * 0.060F) * 0.040F;
        setDeckRotation(leftFlightDeck, LEFT_DECK_Y_ROT + pulse, LEFT_DECK_Z_ROT + pulse * 0.35F);
        setDeckRotation(leftFlightDeckEdge, leftFlightDeck.yRot, leftFlightDeck.zRot);
        setDeckRotation(rightFlightDeck, RIGHT_DECK_Y_ROT - pulse, RIGHT_DECK_Z_ROT - pulse * 0.35F);
        setDeckRotation(rightFlightDeckEdge, rightFlightDeck.yRot, rightFlightDeck.zRot);
        flightDeckGlow.zRot = Mth.sin(ageInTicks * 0.040F) * 0.015F;

        float haloBob = Mth.sin(ageInTicks * 0.075F) * 0.08F;
        setTopDeckPose(topDeck, haloBob, -2.0F * DEG_TO_RAD + Mth.sin(ageInTicks * 0.030F) * 0.025F);
        setTopDeckPose(topDeckEdge, haloBob, topDeck.zRot);
        setTopDeckPose(topDeckGlow, haloBob, topDeck.zRot);

        leftPod.yRot = LEFT_POD_Y_ROT + pulse * 0.50F;
        rightPod.yRot = RIGHT_POD_Y_ROT - pulse * 0.50F;
        tail.zRot = -6.0F * DEG_TO_RAD + Mth.sin(ageInTicks * 0.070F) * 0.080F;
        tailGlow.zRot = tail.zRot * 0.35F;
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

    private static void setDeckRotation(ModelPart part, float yRot, float zRot) {
        part.xRot = 0.0F;
        part.yRot = yRot;
        part.zRot = zRot;
    }

    private static void setTopDeckPose(ModelPart part, float bob, float zRot) {
        part.y = -24.0F + bob;
        part.xRot = 0.0F;
        part.yRot = 0.0F;
        part.zRot = zRot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, int color) {
        tail.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_COLOR);
        tailGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        leftFlightDeck.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        rightFlightDeck.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        leftFlightDeckEdge.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_EDGE_COLOR);
        rightFlightDeckEdge.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_EDGE_COLOR);
        flightDeckGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        topDeck.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_DARK_COLOR);
        topDeckEdge.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_EDGE_COLOR);
        topDeckGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        leftPod.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_COLOR);
        rightPod.render(poseStack, vertexConsumer, packedLight, packedOverlay, METAL_COLOR);
        podGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        leftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, DRESS_COLOR);
        rightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, DRESS_COLOR);
        legGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        shoes.render(poseStack, vertexConsumer, packedLight, packedOverlay, SHOE_COLOR);
        skirtFrill.render(poseStack, vertexConsumer, packedLight, packedOverlay, FRILL_COLOR);
        bodySkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        bodyDress.render(poseStack, vertexConsumer, packedLight, packedOverlay, DRESS_COLOR);
        bodyShadow.render(poseStack, vertexConsumer, packedLight, packedOverlay, DRESS_SHADOW_COLOR);
        bodyGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
        leftArmSkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        rightArmSkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        leftArmDark.render(poseStack, vertexConsumer, packedLight, packedOverlay, DRESS_SHADOW_COLOR);
        rightArmDark.render(poseStack, vertexConsumer, packedLight, packedOverlay, DRESS_SHADOW_COLOR);
        headSkin.render(poseStack, vertexConsumer, packedLight, packedOverlay, SKIN_COLOR);
        headHair.render(poseStack, vertexConsumer, packedLight, packedOverlay, HAIR_COLOR);
        headHairShadow.render(poseStack, vertexConsumer, packedLight, packedOverlay, HAIR_SHADOW_COLOR);
        faceGlow.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, GLOW_COLOR);
    }
}
