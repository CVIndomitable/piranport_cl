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
 * Unicorn's floral harp dress, built with fine YSM-style layered geometry.
 */
public class UnicornModel extends PlayerModel<ShipGirlEntity> {
    public static final int SKIN_ID = 18;
    private static final float SKIN_UV_SCALE = 2.0F;

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "unicorn"), "main");
    public static final ResourceLocation TEXTURE_LOCATION =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/shipgirl/unicorn.png");

    private final ModelPart hairRoot;
    private final ModelPart leftHair;
    private final ModelPart centerHair;
    private final ModelPart rightHair;
    private final ModelPart frontSkirt;
    private final ModelPart leftSkirt;
    private final ModelPart rightSkirt;
    private final ModelPart backSkirt;
    private final ModelPart leftRibbon;
    private final ModelPart rightRibbon;
    private final ModelPart rightForearm;
    private final ModelPart leftForearm;
    private final ModelPart harp;
    private final ModelPart leftBird;
    private final ModelPart rightBird;
    private final ModelPart leftBirdWings;
    private final ModelPart rightBirdWings;

    public UnicornModel(ModelPart root) {
        super(root, true);
        this.hairRoot = this.body.getChild("hair_root");
        this.leftHair = this.hairRoot.getChild("left_flow");
        this.centerHair = this.hairRoot.getChild("center_flow");
        this.rightHair = this.hairRoot.getChild("right_flow");
        this.frontSkirt = this.body.getChild("front_skirt");
        this.leftSkirt = this.body.getChild("left_skirt");
        this.rightSkirt = this.body.getChild("right_skirt");
        this.backSkirt = this.body.getChild("back_skirt");
        this.leftRibbon = this.body.getChild("left_ribbon");
        this.rightRibbon = this.body.getChild("right_ribbon");
        this.rightForearm = this.rightArm.getChild("forearm");
        this.leftForearm = this.leftArm.getChild("forearm");
        this.harp = this.body.getChild("harp");
        this.leftBird = this.body.getChild("left_bird");
        this.rightBird = this.body.getChild("right_bird");
        this.leftBirdWings = this.leftBird.getChild("wings");
        this.rightBirdWings = this.rightBird.getChild("wings");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                skin(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                        new CubeDeformation(-0.45F), SKIN_UV_SCALE, SKIN_UV_SCALE),
                PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body",
                swatch(128, 32).addBox(-2.7F, 0.9F, -1.25F, 5.4F, 10.1F, 2.5F,
                        CubeDeformation.NONE),
                PartPose.ZERO);
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(),
                PartPose.offset(-3.25F, 2.25F, 0.0F));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(),
                PartPose.offset(3.25F, 2.25F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
                swatch(224, 0).addBox(-1.1F, 0.0F, -1.2F, 2.2F, 11.8F, 2.4F,
                        CubeDeformation.NONE),
                PartPose.offset(-1.3F, 12.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
                swatch(224, 0).addBox(-1.1F, 0.0F, -1.2F, 2.2F, 11.8F, 2.4F,
                        CubeDeformation.NONE),
                PartPose.offset(1.3F, 12.0F, 0.0F));

        root.addOrReplaceChild("right_sleeve", CubeListBuilder.create(), PartPose.offset(-3.25F, 2.25F, 0.0F));
        root.addOrReplaceChild("left_sleeve", CubeListBuilder.create(), PartPose.offset(3.25F, 2.25F, 0.0F));
        root.addOrReplaceChild("right_pants", CubeListBuilder.create(), PartPose.offset(-1.3F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_pants", CubeListBuilder.create(), PartPose.offset(1.3F, 12.0F, 0.0F));
        root.addOrReplaceChild("jacket", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("ear", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("cloak", CubeListBuilder.create(), PartPose.ZERO);

        addHeadAndHair(head, body);
        addDress(body);
        addArms(rightArm, leftArm);
        addLegs(rightLeg, leftLeg);
        addHarp(body);
        addHummingbirds(body);

        return LayerDefinition.create(mesh, 256, 256);
    }

    private static void addHeadAndHair(PartDefinition head, PartDefinition body) {
        head.addOrReplaceChild("hair_shell",
                swatch(128, 0)
                        .addBox(-3.72F, -7.82F, -3.62F, 7.44F, 1.55F, 7.24F, CubeDeformation.NONE)
                        .texOffs(160, 0).addBox(-3.55F, -7.68F, -3.84F, 7.1F, 1.92F, 0.42F,
                                CubeDeformation.NONE)
                        .texOffs(160, 0).addBox(-3.72F, -7.68F, 2.82F, 7.44F, 7.53F, 0.82F,
                                CubeDeformation.NONE)
                        .texOffs(192, 0).addBox(-3.88F, -7.68F, -2.9F, 0.72F, 7.53F, 5.8F,
                                CubeDeformation.NONE)
                        .addBox(3.16F, -7.68F, -2.9F, 0.72F, 7.53F, 5.8F, CubeDeformation.NONE),
                PartPose.ZERO);

        PartDefinition rearScalp = head.addOrReplaceChild("rear_scalp", CubeListBuilder.create(), PartPose.ZERO);
        float[] rearX = {-3.05F, -2.05F, -1.02F, 0.0F, 1.02F, 2.05F, 3.05F};
        float[] rearLength = {6.25F, 6.85F, 7.15F, 7.35F, 7.1F, 6.8F, 6.2F};
        for (int i = 0; i < rearX.length; i++) {
            rearScalp.addOrReplaceChild("back_strand_" + i,
                    swatch(i % 3 == 0 ? 192 : i % 2 == 0 ? 128 : 160, 0)
                            .addBox(-0.48F, 0.0F, -0.2F, 0.96F, rearLength[i], 0.4F,
                                    CubeDeformation.NONE),
                    PartPose.offsetAndRotation(rearX[i], -7.45F, 3.55F, 0.025F, 0.0F,
                            (i - 3) * 0.025F));
        }
        float[] sideZ = {-2.45F, -0.85F, 0.8F, 2.35F};
        for (int i = 0; i < sideZ.length; i++) {
            rearScalp.addOrReplaceChild("left_side_strand_" + i,
                    swatch(i % 2 == 0 ? 160 : 192, 0)
                            .addBox(-0.2F, 0.0F, -0.48F, 0.4F, 6.7F + i * 0.18F, 0.96F,
                                    CubeDeformation.NONE),
                    PartPose.offsetAndRotation(-3.82F, -7.35F, sideZ[i], 0.02F, 0.0F, 0.035F));
            rearScalp.addOrReplaceChild("right_side_strand_" + i,
                    swatch(i % 2 == 0 ? 160 : 192, 0)
                            .addBox(-0.2F, 0.0F, -0.48F, 0.4F, 6.7F + i * 0.18F, 0.96F,
                                    CubeDeformation.NONE),
                    PartPose.offsetAndRotation(3.82F, -7.35F, sideZ[i], 0.02F, 0.0F, -0.035F));
        }

        PartDefinition bangs = head.addOrReplaceChild("bangs", CubeListBuilder.create(), PartPose.ZERO);
        float[] hairlineX = {-2.85F, -1.9F, -0.95F, 0.0F, 0.95F, 1.9F, 2.85F};
        float[] hairlineY = {-5.95F, -6.15F, -5.82F, -6.22F, -5.88F, -6.12F, -5.94F};
        for (int i = 0; i < hairlineX.length; i++) {
            bangs.addOrReplaceChild("hairline_" + i,
                    swatch(i % 2 == 0 ? 160 : 128, 0)
                            .addBox(-0.5F, -0.28F, -0.18F, 1.0F, 0.56F, 0.36F,
                                    CubeDeformation.NONE),
                    PartPose.offsetAndRotation(hairlineX[i], hairlineY[i], -3.69F,
                            0.0F, 0.0F, (i - 3) * 0.055F));
        }
        float[] bangX = {-3.05F, -2.2F, -1.35F, -0.48F, 0.38F, 1.25F, 2.12F, 2.98F};
        float[] bangLength = {2.3F, 3.0F, 2.6F, 3.7F, 3.2F, 2.6F, 3.0F, 2.3F};
        float[] bangTilt = {0.18F, 0.11F, 0.07F, 0.03F, -0.04F, -0.08F, -0.12F, -0.18F};
        for (int i = 0; i < bangX.length; i++) {
            bangs.addOrReplaceChild("strand_" + i,
                    swatch(i % 3 == 1 ? 128 : 160, 0)
                            .addBox(-0.36F, 0.0F, -0.22F, 0.72F, bangLength[i], 0.44F,
                                    CubeDeformation.NONE),
                    PartPose.offsetAndRotation(bangX[i], -6.85F, -3.58F, -0.035F, 0.0F, bangTilt[i]));
        }

        PartDefinition tufts = head.addOrReplaceChild("hair_tufts", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 4; i++) {
            float topX = -2.25F + i * 1.5F;
            tufts.addOrReplaceChild("top_" + i,
                    swatch(i % 2 == 0 ? 128 : 160, 0)
                            .addBox(-0.34F, -1.35F, -0.32F, 0.68F, 1.55F, 0.64F,
                                    CubeDeformation.NONE),
                    PartPose.offsetAndRotation(topX, -7.72F, -0.65F, -0.16F, 0.0F,
                            i < 2 ? -0.2F + i * 0.08F : 0.12F + (i - 2) * 0.08F));
        }
        for (int i = 0; i < 3; i++) {
            float y = -6.25F + i * 1.75F;
            tufts.addOrReplaceChild("left_" + i,
                    swatch(i % 2 == 0 ? 192 : 160, 0)
                            .addBox(-0.34F, -0.2F, -0.34F, 0.68F, 2.05F, 0.68F,
                                    CubeDeformation.NONE),
                    PartPose.offsetAndRotation(-3.62F, y, 0.35F, 0.04F, 0.0F, 0.34F + i * 0.05F));
            tufts.addOrReplaceChild("right_" + i,
                    swatch(i % 2 == 0 ? 192 : 160, 0)
                            .addBox(-0.34F, -0.2F, -0.34F, 0.68F, 2.05F, 0.68F,
                                    CubeDeformation.NONE),
                    PartPose.offsetAndRotation(3.62F, y, 0.35F, 0.04F, 0.0F, -0.34F - i * 0.05F));
        }

        PartDefinition leftLock = head.addOrReplaceChild("left_lock", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-3.52F, -4.8F, -2.65F, 0.04F, 0.08F, 0.12F));
        addHairStrand(leftLock, "outer", -0.2F, 0.0F, 0.0F, 1.15F, 5.3F, 4.0F, 0.08F, 160);
        addHairStrand(leftLock, "inner", 0.75F, 0.75F, -0.25F, 0.72F, 4.7F, 3.1F, -0.04F, 128);
        PartDefinition rightLock = head.addOrReplaceChild("right_lock", CubeListBuilder.create(),
                PartPose.offsetAndRotation(3.52F, -4.8F, -2.65F, 0.04F, -0.08F, -0.12F));
        addHairStrand(rightLock, "outer", 0.2F, 0.0F, 0.0F, 1.15F, 5.3F, 4.0F, -0.08F, 160);
        addHairStrand(rightLock, "inner", -0.75F, 0.75F, -0.25F, 0.72F, 4.7F, 3.1F, 0.04F, 128);

        PartDefinition crown = head.addOrReplaceChild("flower_crown",
                swatch(128, 96)
                        .addBox(-3.35F, -8.28F, -3.15F, 6.7F, 0.22F, 0.3F, CubeDeformation.NONE)
                        .addBox(-3.1F, -8.55F, -2.95F, 0.24F, 0.95F, 0.26F, CubeDeformation.NONE)
                        .addBox(2.82F, -8.55F, -2.95F, 0.24F, 0.95F, 0.26F, CubeDeformation.NONE),
                PartPose.rotation(-0.08F, 0.0F, 0.0F));
        addFlower(crown, "flower_l", -2.75F, -8.35F, -3.28F, 128, 64, 0.82F);
        addFlower(crown, "flower_l2", -1.55F, -8.52F, -3.33F, 224, 64, 0.62F);
        addFlower(crown, "flower_c", -0.2F, -8.65F, -3.38F, 192, 64, 0.72F);
        addFlower(crown, "flower_r2", 1.2F, -8.5F, -3.34F, 160, 64, 0.65F);
        addFlower(crown, "flower_r", 2.62F, -8.32F, -3.28F, 128, 64, 0.95F);

        PartDefinition sideFlowers = head.addOrReplaceChild("side_flowers", CubeListBuilder.create(), PartPose.ZERO);
        addFlower(sideFlowers, "temple", -3.72F, -3.5F, -3.05F, 128, 64, 0.7F);
        addFlower(sideFlowers, "ear", -3.82F, -2.25F, -2.8F, 192, 64, 0.62F);
        addFlower(sideFlowers, "lower", -3.7F, -0.95F, -2.55F, 224, 64, 0.56F);
        for (int i = 0; i < 5; i++) {
            sideFlowers.addOrReplaceChild("braid_" + i,
                    swatch(i % 2 == 0 ? 160 : 192, 0)
                            .addBox(-0.38F, -0.28F, -0.32F, 0.76F, 0.56F, 0.64F, CubeDeformation.NONE),
                    PartPose.offsetAndRotation(-3.62F + (i % 2) * 0.22F, -0.1F + i * 0.72F, -2.35F,
                            0.0F, 0.0F, i % 2 == 0 ? 0.34F : -0.34F));
        }

        PartDefinition hairRoot = body.addOrReplaceChild("hair_root", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition upperHair = hairRoot.addOrReplaceChild("upper_hair", CubeListBuilder.create(), PartPose.ZERO);
        float[] upperX = {-3.1F, -2.05F, -1.0F, 0.0F, 1.0F, 2.05F, 3.1F};
        for (int i = 0; i < upperX.length; i++) {
            upperHair.addOrReplaceChild("panel_" + i,
                    swatch(i % 3 == 0 ? 192 : i % 2 == 0 ? 128 : 160, 0)
                            .addBox(-0.58F, 0.0F, -0.35F, 1.16F, 6.4F + (i % 3) * 0.65F, 0.7F,
                                    CubeDeformation.NONE),
                    PartPose.offsetAndRotation(upperX[i], -0.8F, 2.95F, 0.055F, 0.0F,
                            (i - 3) * 0.022F));
        }

        PartDefinition leftFlow = hairRoot.addOrReplaceChild("left_flow", CubeListBuilder.create(),
                PartPose.rotation(0.045F, -0.025F, 0.0F));
        addHairStrand(leftFlow, "far", -3.35F, 4.4F, 3.05F, 1.0F, 8.2F, 6.4F, 0.16F, 192);
        addHairStrand(leftFlow, "outer", -2.55F, 4.0F, 3.15F, 1.15F, 9.0F, 6.2F, 0.1F, 128);
        addHairStrand(leftFlow, "inner", -1.62F, 4.7F, 3.22F, 0.92F, 8.1F, 5.5F, 0.04F, 160);

        PartDefinition centerFlow = hairRoot.addOrReplaceChild("center_flow", CubeListBuilder.create(),
                PartPose.rotation(0.055F, 0.0F, 0.0F));
        addHairStrand(centerFlow, "left", -0.72F, 4.9F, 3.28F, 0.95F, 8.7F, 5.2F, 0.025F, 128);
        addHairStrand(centerFlow, "center", 0.0F, 4.5F, 3.35F, 1.1F, 9.1F, 5.8F, 0.0F, 160);
        addHairStrand(centerFlow, "right", 0.72F, 4.9F, 3.28F, 0.95F, 8.7F, 5.2F, -0.025F, 128);

        PartDefinition rightFlow = hairRoot.addOrReplaceChild("right_flow", CubeListBuilder.create(),
                PartPose.rotation(0.045F, 0.025F, 0.0F));
        addHairStrand(rightFlow, "inner", 1.62F, 4.7F, 3.22F, 0.92F, 8.1F, 5.5F, -0.04F, 160);
        addHairStrand(rightFlow, "outer", 2.55F, 4.0F, 3.15F, 1.15F, 9.0F, 6.2F, -0.1F, 128);
        addHairStrand(rightFlow, "far", 3.35F, 4.4F, 3.05F, 1.0F, 8.2F, 6.4F, -0.16F, 192);
    }

    private static void addDress(PartDefinition body) {
        body.addOrReplaceChild("bodice_layers",
                swatch(128, 32)
                        .addBox(-2.55F, 0.55F, -1.62F, 5.1F, 7.9F, 0.34F, CubeDeformation.NONE)
                        .texOffs(160, 32).addBox(-2.1F, 0.25F, -1.82F, 4.2F, 1.25F, 0.32F,
                                CubeDeformation.NONE)
                        .texOffs(192, 32).addBox(-2.5F, 6.7F, -1.88F, 5.0F, 1.45F, 0.38F,
                                CubeDeformation.NONE)
                        .texOffs(224, 0).addBox(-1.4F, -0.05F, -1.88F, 2.8F, 2.7F, 0.3F,
                                CubeDeformation.NONE)
                        .texOffs(224, 64).addBox(-0.42F, 1.55F, -2.02F, 0.84F, 1.0F, 0.25F,
                                CubeDeformation.NONE),
                PartPose.ZERO);

        PartDefinition collar = body.addOrReplaceChild("lace_collar", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 7; i++) {
            float x = -2.1F + i * 0.7F;
            collar.addOrReplaceChild("petal_" + i,
                    swatch(192, 32).addBox(-0.5F, -0.18F, -0.16F, 1.0F, 0.36F, 0.32F,
                            CubeDeformation.NONE),
                    PartPose.offsetAndRotation(x, 1.15F + Math.abs(i - 3) * 0.18F, -2.05F,
                            0.0F, 0.0F, (i - 3) * 0.13F));
        }

        PartDefinition waist = body.addOrReplaceChild("waist_garland",
                swatch(160, 96)
                        .addBox(-2.8F, 7.85F, -1.86F, 5.6F, 0.42F, 3.72F, CubeDeformation.NONE)
                        .texOffs(128, 96).addBox(-2.7F, 8.32F, -2.02F, 5.4F, 0.2F, 0.24F,
                                CubeDeformation.NONE),
                PartPose.ZERO);
        addFlower(waist, "garland_l", -2.05F, 8.32F, -2.17F, 128, 64, 0.58F);
        addFlower(waist, "garland_l2", -1.0F, 8.38F, -2.18F, 192, 64, 0.52F);
        addFlower(waist, "garland_c", 0.05F, 8.38F, -2.19F, 224, 64, 0.55F);
        addFlower(waist, "garland_r2", 1.05F, 8.36F, -2.18F, 160, 64, 0.52F);
        addFlower(waist, "garland_r", 2.1F, 8.3F, -2.17F, 128, 64, 0.58F);

        PartDefinition corset = body.addOrReplaceChild("corset_detail", CubeListBuilder.create(), PartPose.ZERO);
        corset.addOrReplaceChild("left_seam",
                swatch(160, 32).addBox(-0.11F, 0.0F, -0.1F, 0.22F, 4.4F, 0.2F,
                        CubeDeformation.NONE),
                PartPose.offsetAndRotation(-1.55F, 2.65F, -1.93F, 0.0F, 0.0F, -0.08F));
        corset.addOrReplaceChild("right_seam",
                swatch(160, 32).addBox(-0.11F, 0.0F, -0.1F, 0.22F, 4.4F, 0.2F,
                        CubeDeformation.NONE),
                PartPose.offsetAndRotation(1.55F, 2.65F, -1.93F, 0.0F, 0.0F, 0.08F));
        for (int i = 0; i < 3; i++) {
            corset.addOrReplaceChild("button_" + i,
                    swatch(160, 96).addBox(-0.15F, -0.15F, -0.12F, 0.3F, 0.3F, 0.24F,
                            CubeDeformation.NONE),
                    PartPose.offset(0.0F, 3.2F + i * 1.25F, -2.02F));
        }

        PartDefinition frontSkirt = body.addOrReplaceChild("front_skirt", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 8.25F, -1.7F, -0.045F, 0.0F, 0.0F));
        frontSkirt.addOrReplaceChild("center_panel",
                swatch(128, 32)
                        .addBox(-2.25F, 0.0F, -0.22F, 4.5F, 10.5F, 0.44F, CubeDeformation.NONE)
                        .texOffs(192, 32).addBox(-2.48F, 4.15F, -0.3F, 4.96F, 0.62F, 0.58F,
                                CubeDeformation.NONE)
                        .addBox(-2.7F, 9.55F, -0.34F, 5.4F, 0.95F, 0.66F, CubeDeformation.NONE),
                PartPose.ZERO);
        for (int i = 0; i < 5; i++) {
            addFlower(frontSkirt, "front_flower_" + i, -1.75F + i * 0.88F,
                    8.8F + Math.abs(i - 2) * 0.2F, -0.58F,
                    new int[] {128, 192, 224, 160, 128}[i], 64, 0.48F + (i % 2) * 0.08F);
        }

        PartDefinition leftSkirt = body.addOrReplaceChild("left_skirt", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-2.35F, 8.15F, -0.2F, 0.0F, -0.05F, 0.16F));
        leftSkirt.addOrReplaceChild("front_fold",
                swatch(160, 32)
                        .addBox(-2.5F, 0.0F, -1.65F, 2.75F, 10.8F, 0.38F, CubeDeformation.NONE)
                        .texOffs(192, 32).addBox(-2.72F, 9.75F, -1.75F, 3.05F, 0.9F, 0.58F,
                                CubeDeformation.NONE),
                PartPose.rotation(0.025F, -0.05F, 0.0F));
        leftSkirt.addOrReplaceChild("side_fold",
                swatch(128, 32)
                        .addBox(-2.68F, 0.15F, -1.45F, 0.38F, 10.45F, 3.0F, CubeDeformation.NONE),
                PartPose.rotation(0.0F, 0.0F, 0.0F));
        addFlower(leftSkirt, "side_flower_top", -2.8F, 2.0F, -1.72F, 192, 64, 0.62F);
        addFlower(leftSkirt, "side_flower_low", -2.75F, 7.6F, -1.72F, 128, 64, 0.7F);

        PartDefinition rightSkirt = body.addOrReplaceChild("right_skirt", CubeListBuilder.create(),
                PartPose.offsetAndRotation(2.35F, 8.15F, -0.2F, 0.0F, 0.05F, -0.16F));
        rightSkirt.addOrReplaceChild("front_fold",
                swatch(160, 32)
                        .addBox(-0.25F, 0.0F, -1.65F, 2.75F, 10.8F, 0.38F, CubeDeformation.NONE)
                        .texOffs(192, 32).addBox(-0.33F, 9.75F, -1.75F, 3.05F, 0.9F, 0.58F,
                                CubeDeformation.NONE),
                PartPose.rotation(0.025F, 0.05F, 0.0F));
        rightSkirt.addOrReplaceChild("side_fold",
                swatch(128, 32)
                        .addBox(2.3F, 0.15F, -1.45F, 0.38F, 10.45F, 3.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        addFlower(rightSkirt, "side_flower_top", 2.8F, 2.0F, -1.72F, 224, 64, 0.62F);
        addFlower(rightSkirt, "side_flower_low", 2.75F, 7.6F, -1.72F, 160, 64, 0.7F);

        PartDefinition backSkirt = body.addOrReplaceChild("back_skirt", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 8.2F, 1.65F, 0.07F, 0.0F, 0.0F));
        backSkirt.addOrReplaceChild("back_panel",
                swatch(160, 32)
                        .addBox(-3.05F, 0.0F, -0.16F, 6.1F, 11.0F, 0.36F, CubeDeformation.NONE)
                        .texOffs(192, 32).addBox(-3.35F, 9.95F, -0.22F, 6.7F, 0.98F, 0.52F,
                                CubeDeformation.NONE),
                PartPose.ZERO);

        // H15: petticoat 原本挂在 body 上 Y=18.35，超出 body 高度 12 直接浮空。
        // 改为挂在 backSkirt 之下，使用相对 backSkirt 的局部坐标；back_panel 顶部约 Y=10.95，
        // petticoat 摆放在裙摆下沿作为装饰花边，避免穿模/浮空。
        backSkirt.addOrReplaceChild("petticoat",
                swatch(192, 32)
                        .addBox(-3.15F, 10.95F, -1.72F, 6.3F, 1.0F, 3.44F, CubeDeformation.NONE)
                        .texOffs(128, 32).addBox(-2.82F, 11.78F, -1.5F, 5.64F, 0.58F, 3.0F,
                                CubeDeformation.NONE),
                PartPose.ZERO);

        PartDefinition leftRibbon = body.addOrReplaceChild("left_ribbon", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-3.15F, 8.3F, 1.92F, 0.1F, 0.0F, 0.18F));
        addRibbon(leftRibbon, "upper", 0.0F, 0.0F, 0.0F, 0.75F, 6.8F, 160, 32, -0.06F);
        addRibbon(leftRibbon, "lower", -0.25F, 6.2F, 0.0F, 0.55F, 5.4F, 192, 32, 0.12F);
        PartDefinition rightRibbon = body.addOrReplaceChild("right_ribbon", CubeListBuilder.create(),
                PartPose.offsetAndRotation(3.15F, 8.3F, 1.92F, 0.1F, 0.0F, -0.18F));
        addRibbon(rightRibbon, "upper", 0.0F, 0.0F, 0.0F, 0.75F, 6.8F, 160, 32, 0.06F);
        addRibbon(rightRibbon, "lower", 0.25F, 6.2F, 0.0F, 0.55F, 5.4F, 192, 32, -0.12F);
    }

    private static void addArms(PartDefinition rightArm, PartDefinition leftArm) {
        addArm(rightArm, true);
        addArm(leftArm, false);
    }

    private static void addArm(PartDefinition arm, boolean right) {
        arm.addOrReplaceChild("upper_arm",
                swatch(224, 0)
                        .addBox(-0.85F, -1.45F, -1.0F, 1.7F, 5.5F, 2.0F, CubeDeformation.NONE)
                        .texOffs(128, 32).addBox(-0.98F, -1.62F, -1.12F, 1.96F, 1.45F, 2.24F,
                                CubeDeformation.NONE),
                PartPose.ZERO);
        PartDefinition forearm = arm.addOrReplaceChild("forearm",
                swatch(224, 0)
                        .addBox(-0.8F, 0.0F, -0.92F, 1.6F, 5.35F, 1.84F, CubeDeformation.NONE)
                        .texOffs(128, 32).addBox(-0.95F, 0.48F, -1.08F, 1.9F, 4.82F, 2.16F,
                                CubeDeformation.NONE)
                        .texOffs(192, 32).addBox(-1.08F, 0.2F, -1.18F, 2.16F, 0.7F, 2.36F,
                                CubeDeformation.NONE)
                        .texOffs(160, 96).addBox(-0.98F, 4.6F, -1.12F, 1.96F, 0.48F, 2.24F,
                                CubeDeformation.NONE),
                PartPose.offset(0.0F, 4.0F, 0.0F));
        addFlower(forearm, "glove_flower", right ? -0.92F : 0.92F, 1.15F, -1.28F,
                right ? 192 : 224, 64, 0.42F);
    }

    private static void addLegs(PartDefinition rightLeg, PartDefinition leftLeg) {
        addLegDetails(rightLeg, true);
        addLegDetails(leftLeg, false);
    }

    private static void addLegDetails(PartDefinition leg, boolean right) {
        for (int i = 0; i < 4; i++) {
            leg.addOrReplaceChild("vine_" + i,
                    swatch(i % 2 == 0 ? 128 : 224, 96)
                            .addBox(-1.14F, -0.12F, -0.13F, 2.28F, 0.24F, 0.26F, CubeDeformation.NONE),
                    PartPose.offsetAndRotation(0.0F, 1.65F + i * 1.65F, -1.3F, 0.0F, 0.0F,
                            (right ? -1.0F : 1.0F) * (i % 2 == 0 ? 0.58F : -0.58F)));
        }
        leg.addOrReplaceChild("shoe",
                swatch(128, 32)
                        .addBox(-1.28F, 8.6F, -1.78F, 2.56F, 3.25F, 3.15F, CubeDeformation.NONE)
                        .texOffs(192, 32).addBox(-1.4F, 8.4F, -1.48F, 2.8F, 0.6F, 2.96F,
                                CubeDeformation.NONE)
                        .texOffs(224, 128).addBox(-0.88F, 9.45F, -1.95F, 1.76F, 0.38F, 0.48F,
                                CubeDeformation.NONE)
                        .texOffs(160, 160).addBox(-1.05F, 11.35F, -1.48F, 2.1F, 0.52F, 2.7F,
                                CubeDeformation.NONE),
                PartPose.rotation(0.0F, right ? -0.035F : 0.035F, 0.0F));
        addFlower(leg, "ankle_flower", right ? -1.12F : 1.12F, 8.35F, -1.42F,
                right ? 224 : 128, 64, 0.46F);
    }

    private static void addHarp(PartDefinition body) {
        PartDefinition harp = body.addOrReplaceChild("harp", CubeListBuilder.create(),
                PartPose.offsetAndRotation(8.0F, -1.8F, -1.6F, 0.02F, -0.12F, -0.035F));
        harp.addOrReplaceChild("pillar",
                swatch(128, 32)
                        .addBox(-0.36F, 0.0F, -0.42F, 0.72F, 15.2F, 0.84F, CubeDeformation.NONE)
                        .texOffs(160, 96).addBox(-0.5F, -0.15F, -0.54F, 1.0F, 0.75F, 1.08F,
                                CubeDeformation.NONE)
                        .addBox(-0.52F, 14.45F, -0.56F, 1.04F, 0.82F, 1.12F,
                                CubeDeformation.NONE),
                PartPose.ZERO);

        addHarpBeam(harp, "neck_1", -0.05F, 0.35F, 0.0F, 3.0F, -0.12F);
        addHarpBeam(harp, "neck_2", -2.8F, 0.92F, 0.0F, 2.8F, -0.28F);
        addHarpBeam(harp, "neck_3", -5.15F, 2.15F, 0.0F, 2.0F, -0.52F);
        addHarpBeam(harp, "neck_4", -6.25F, 3.75F, 0.0F, 1.7F, -0.82F);

        PartDefinition soundBox = harp.addOrReplaceChild("sound_box",
                swatch(224, 32)
                        .addBox(-0.25F, -0.62F, -0.52F, 6.9F, 1.24F, 1.04F, CubeDeformation.NONE)
                        .texOffs(160, 96).addBox(-0.45F, -0.82F, -0.62F, 7.2F, 0.32F, 1.24F,
                                CubeDeformation.NONE)
                        .texOffs(224, 96).addBox(1.2F, 0.35F, -0.58F, 5.45F, 1.25F, 1.16F,
                                CubeDeformation.NONE)
                        .addBox(2.15F, 1.3F, -0.52F, 4.45F, 0.82F, 1.04F,
                                CubeDeformation.NONE)
                        .texOffs(192, 96).addBox(3.2F, 1.92F, -0.44F, 3.3F, 0.48F, 0.88F,
                                CubeDeformation.NONE),
                PartPose.offsetAndRotation(-6.2F, 12.6F, 0.0F, 0.0F, 0.0F, 0.25F));
        addFlower(soundBox, "harp_flower_l", 1.15F, -1.0F, -0.78F, 128, 64, 0.48F);
        addFlower(soundBox, "harp_flower_r", 5.7F, 0.0F, -0.78F, 192, 64, 0.45F);

        int[][] colors = {{128, 96}, {192, 64}, {224, 64}, {160, 64}, {224, 128}, {128, 64}, {192, 64}, {224, 64}};
        for (int i = 0; i < colors.length; i++) {
            float x = -5.75F + i * 0.67F;
            float top = 4.2F - i * 0.32F;
            float height = 7.3F + i * 0.42F;
            harp.addOrReplaceChild("string_" + i,
                    swatch(colors[i][0], colors[i][1])
                            .addBox(-0.075F, 0.0F, -0.11F, 0.15F, height, 0.22F, CubeDeformation.NONE),
                    PartPose.offsetAndRotation(x, top, -0.06F, 0.0F, 0.0F, -0.018F));
        }

        PartDefinition unicorn = harp.addOrReplaceChild("unicorn_ornament", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -0.65F, 0.0F, 0.0F, 0.0F, -0.06F));
        unicorn.addOrReplaceChild("head",
                swatch(128, 32)
                        .addBox(-0.35F, -0.65F, -0.52F, 1.55F, 1.3F, 1.04F, CubeDeformation.NONE)
                        .addBox(0.92F, -0.32F, -0.4F, 0.85F, 0.72F, 0.8F, CubeDeformation.NONE)
                        .texOffs(192, 128).addBox(1.55F, -0.15F, -0.44F, 0.16F, 0.16F, 0.12F,
                                CubeDeformation.NONE),
                PartPose.ZERO);
        unicorn.addOrReplaceChild("horn",
                swatch(160, 96).addBox(-0.12F, -1.45F, -0.12F, 0.24F, 1.55F, 0.24F,
                        CubeDeformation.NONE),
                PartPose.offsetAndRotation(1.05F, -0.45F, -0.2F, 0.0F, 0.0F, 0.48F));
        for (int i = 0; i < 5; i++) {
            unicorn.addOrReplaceChild("wing_" + i,
                    swatch(i % 2 == 0 ? 160 : 192, 96)
                            .addBox(-1.75F - i * 0.18F, -0.18F, -0.35F, 1.9F + i * 0.12F,
                                    0.36F, 0.7F, CubeDeformation.NONE),
                    PartPose.offsetAndRotation(-0.15F, -0.05F + i * 0.28F, 0.0F,
                            0.0F, 0.0F, -0.38F - i * 0.14F));
        }
    }

    private static void addHummingbirds(PartDefinition body) {
        addHummingbird(body, "left_bird", -7.2F, -2.8F, -0.8F, 1.0F);
        addHummingbird(body, "right_bird", 9.0F, 3.2F, 1.8F, -1.0F);
    }

    private static void addHummingbird(PartDefinition body, String name, float x, float y, float z, float facing) {
        PartDefinition bird = body.addOrReplaceChild(name,
                swatch(128, 160)
                        .addBox(-0.6F, -0.28F, -0.32F, 1.2F, 0.56F, 0.64F, CubeDeformation.NONE)
                        .texOffs(128, 96).addBox(facing > 0 ? 0.35F : -0.8F, -0.48F, -0.3F,
                                0.45F, 0.45F, 0.6F, CubeDeformation.NONE)
                        .texOffs(160, 96).addBox(facing > 0 ? 0.75F : -1.35F, -0.32F, -0.09F,
                                0.6F, 0.12F, 0.18F, CubeDeformation.NONE)
                        .texOffs(160, 64).addBox(facing > 0 ? -0.95F : 0.45F, 0.0F, -0.12F,
                                0.55F, 0.18F, 0.24F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(x, y, z, 0.0F, facing > 0 ? -0.2F : 0.2F, 0.0F));
        bird.addOrReplaceChild("wings",
                swatch(224, 128)
                        .addBox(-0.15F, -0.12F, -1.45F, 0.55F, 0.24F, 1.35F, CubeDeformation.NONE)
                        .addBox(-0.15F, -0.12F, 0.1F, 0.55F, 0.24F, 1.35F, CubeDeformation.NONE),
                PartPose.rotation(0.0F, 0.0F, 0.0F));
    }

    private static void addHairStrand(PartDefinition parent, String name, float x, float y, float z,
                                      float width, float upperLength, float tipLength, float zRot, int u) {
        PartDefinition strand = parent.addOrReplaceChild(name,
                swatch(u, 0).addBox(-width * 0.5F, 0.0F, -0.38F, width, upperLength, 0.76F,
                        CubeDeformation.NONE),
                PartPose.offsetAndRotation(x, y, z, 0.045F, 0.0F, zRot));
        strand.addOrReplaceChild("tip",
                swatch(u == 192 ? 160 : 192, 0)
                        .addBox(-width * 0.4F, 0.0F, -0.31F, width * 0.8F, tipLength, 0.62F,
                                CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, upperLength - 0.2F, 0.0F, 0.065F, 0.0F, zRot * 0.45F));
    }

    private static void addFlower(PartDefinition parent, String name, float x, float y, float z,
                                  int u, int v, float scale) {
        PartDefinition flower = parent.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(x, y, z));
        for (int i = 0; i < 4; i++) {
            flower.addOrReplaceChild("petal_" + i,
                    swatch(u, v).addBox(-0.58F * scale, -0.17F * scale, -0.16F,
                            1.16F * scale, 0.34F * scale, 0.32F, CubeDeformation.NONE),
                    PartPose.rotation(0.0F, 0.0F, i * Mth.HALF_PI));
        }
        flower.addOrReplaceChild("center",
                swatch(192, 64).addBox(-0.2F * scale, -0.2F * scale, -0.23F,
                        0.4F * scale, 0.4F * scale, 0.46F, CubeDeformation.NONE),
                PartPose.ZERO);
    }

    private static void addRibbon(PartDefinition parent, String name, float x, float y, float z,
                                  float width, float length, int u, int v, float zRot) {
        parent.addOrReplaceChild(name,
                swatch(u, v).addBox(-width * 0.5F, 0.0F, -0.18F, width, length, 0.36F,
                        CubeDeformation.NONE),
                PartPose.offsetAndRotation(x, y, z, 0.06F, 0.0F, zRot));
    }

    private static void addHarpBeam(PartDefinition parent, String name, float x, float y, float z,
                                    float length, float zRot) {
        parent.addOrReplaceChild(name,
                swatch(224, 32)
                        .addBox(-length, -0.32F, -0.4F, length, 0.64F, 0.8F, CubeDeformation.NONE)
                        .texOffs(160, 160).addBox(-length + 0.08F, -0.4F, -0.46F,
                                length - 0.08F, 0.18F, 0.92F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(x, y, z, 0.0F, 0.0F, zRot));
    }

    private static CubeListBuilder skin(int u, int v) {
        return CubeListBuilder.create().texOffs(u * 2, v * 2);
    }

    private static CubeListBuilder swatch(int u, int v) {
        return CubeListBuilder.create().texOffs(u, v);
    }

    @Override
    public void setupAnim(ShipGirlEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        float idle = Mth.sin(ageInTicks * 0.075F);
        float secondary = Mth.sin(ageInTicks * 0.052F + 1.2F);
        float walk = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
        float flow = idle * 0.018F + walk * 0.055F;

        this.hairRoot.xRot = flow * 0.25F;
        this.leftHair.xRot = 0.045F + flow;
        this.leftHair.zRot = idle * 0.018F - walk * 0.025F;
        this.centerHair.xRot = 0.055F + flow * 0.9F;
        this.centerHair.zRot = secondary * 0.009F;
        this.rightHair.xRot = 0.045F + flow;
        this.rightHair.zRot = -idle * 0.018F + walk * 0.025F;

        this.frontSkirt.xRot = -0.045F + walk * 0.025F;
        this.leftSkirt.xRot = walk * 0.05F;
        this.leftSkirt.zRot = 0.16F + idle * 0.009F;
        this.rightSkirt.xRot = -walk * 0.05F;
        this.rightSkirt.zRot = -0.16F - idle * 0.009F;
        this.backSkirt.xRot = 0.07F + flow * 0.75F;
        this.leftRibbon.xRot = 0.1F + flow * 1.4F;
        this.leftRibbon.zRot = 0.18F + idle * 0.035F;
        this.rightRibbon.xRot = 0.1F + flow * 1.25F;
        this.rightRibbon.zRot = -0.18F - idle * 0.035F;

        this.rightForearm.xRot = 0.0F;
        this.rightForearm.yRot = 0.0F;
        this.rightForearm.zRot = 0.0F;
        this.leftForearm.xRot = 0.0F;
        this.leftForearm.yRot = 0.0F;
        this.leftForearm.zRot = 0.0F;
        boolean relaxed = entity.getMainHandItem().isEmpty()
                && entity.getOffhandItem().isEmpty()
                && limbSwingAmount < 0.1F
                && !entity.isAggressive();
        if (relaxed) {
            this.rightArm.zRot = -0.055F + idle * 0.008F;
            this.leftArm.zRot = 0.055F - idle * 0.008F;
            this.rightForearm.xRot = -0.13F + idle * 0.012F;
            this.leftForearm.xRot = -0.13F - idle * 0.012F;
        }

        this.harp.zRot = -0.035F + secondary * 0.008F;
        this.leftBird.y = -2.8F + idle * 0.22F;
        this.leftBird.zRot = secondary * 0.06F;
        this.rightBird.y = 3.2F + secondary * 0.18F;
        this.rightBird.zRot = -idle * 0.05F;
        float wingBeat = Mth.sin(ageInTicks * 1.45F) * 0.72F;
        this.leftBirdWings.xRot = wingBeat;
        this.rightBirdWings.xRot = -wingBeat;
    }
}
