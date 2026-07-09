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
 * Kitchen Goddess (女灶神) model: Alex-shaped body with hair accessories and apron.
 */
public class KitchenGoddessModel extends PlayerModel<ShipGirlEntity> {
    public static final int SKIN_ID = 9857;
    private static final float PLAYER_UV_SCALE = 0.25F;

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "kitchen_goddess"), "main");
    public static final ResourceLocation TEXTURE_LOCATION =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/shipgirl/kitchen_goddess.png");

    private final ModelPart hairAccessory;
    private final ModelPart apron;
    private final ModelPart ponytail;

    public KitchenGoddessModel(ModelPart root) {
        super(root, true);
        this.hairAccessory = this.head.getChild("hair_accessory");
        this.apron = this.body.getChild("apron");
        this.ponytail = this.head.getChild("ponytail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                playerBox(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                        CubeDeformation.NONE, PLAYER_UV_SCALE, PLAYER_UV_SCALE),
                PartPose.ZERO);
        root.addOrReplaceChild("hat",
                playerBox(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                        new CubeDeformation(0.5F), PLAYER_UV_SCALE, PLAYER_UV_SCALE),
                PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body",
                playerBox(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F,
                        CubeDeformation.NONE, PLAYER_UV_SCALE, PLAYER_UV_SCALE),
                PartPose.ZERO);
        root.addOrReplaceChild("right_arm",
                playerBox(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F,
                        CubeDeformation.NONE, PLAYER_UV_SCALE, PLAYER_UV_SCALE),
                PartPose.offset(-5.0F, 2.5F, 0.0F));
        root.addOrReplaceChild("left_arm",
                playerBox(32, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F,
                        CubeDeformation.NONE, PLAYER_UV_SCALE, PLAYER_UV_SCALE),
                PartPose.offset(5.0F, 2.5F, 0.0F));
        root.addOrReplaceChild("right_leg",
                playerBox(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                        CubeDeformation.NONE, PLAYER_UV_SCALE, PLAYER_UV_SCALE),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                playerBox(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                        CubeDeformation.NONE, PLAYER_UV_SCALE, PLAYER_UV_SCALE),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("right_sleeve",
                playerBox(40, 32).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.25F), PLAYER_UV_SCALE, PLAYER_UV_SCALE),
                PartPose.offset(-5.0F, 2.5F, 0.0F));
        root.addOrReplaceChild("left_sleeve",
                playerBox(48, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.25F), PLAYER_UV_SCALE, PLAYER_UV_SCALE),
                PartPose.offset(5.0F, 2.5F, 0.0F));
        root.addOrReplaceChild("right_pants",
                playerBox(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.25F), PLAYER_UV_SCALE, PLAYER_UV_SCALE),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_pants",
                playerBox(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.25F), PLAYER_UV_SCALE, PLAYER_UV_SCALE),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("jacket",
                playerBox(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.25F), PLAYER_UV_SCALE, PLAYER_UV_SCALE),
                PartPose.ZERO);
        root.addOrReplaceChild("ear", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("cloak", CubeListBuilder.create(), PartPose.ZERO);

        // Hair accessory on top of head (decorative pin/flower)
        head.addOrReplaceChild("hair_accessory",
                CubeListBuilder.create()
                        .texOffs(192, 0).addBox(-2.0F, -9.5F, -1.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(192, 4).addBox(-1.5F, -10.0F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        // Ponytail behind head
        head.addOrReplaceChild("ponytail",
                CubeListBuilder.create()
                        .texOffs(192, 16).addBox(-1.5F, -6.0F, 4.0F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(192, 27).addBox(-1.0F, 2.0F, 4.5F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        // Apron tied to body
        body.addOrReplaceChild("apron",
                CubeListBuilder.create()
                        .texOffs(192, 48).addBox(-4.2F, 2.0F, -2.6F, 8.4F, 10.0F, 0.4F, new CubeDeformation(0.0F))
                        .texOffs(192, 64).addBox(-3.0F, 0.5F, -2.5F, 6.0F, 1.5F, 0.3F, new CubeDeformation(0.0F))
                        .texOffs(192, 70).addBox(-4.5F, 0.8F, -0.5F, 1.0F, 0.8F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(192, 70).addBox(3.5F, 0.8F, -0.5F, 1.0F, 0.8F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 256, 256);
    }

    private static CubeListBuilder playerBox(int u, int v) {
        return CubeListBuilder.create().texOffs(u, v);
    }

    @Override
    public void setupAnim(ShipGirlEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Ponytail gentle sway
        float ponytailSway = Mth.sin(ageInTicks * 0.1F) * 0.05F;
        this.ponytail.xRot = ponytailSway;
        this.ponytail.zRot = Mth.cos(ageInTicks * 0.08F) * 0.03F;

        // Hair accessory slight bob (using yRot for subtle animation)
        this.hairAccessory.yRot = Mth.sin(ageInTicks * 0.12F) * 0.02F;

        // Apron slight movement when walking
        this.apron.xRot = Mth.cos(limbSwing * 0.6F) * limbSwingAmount * 0.1F;
    }
}

