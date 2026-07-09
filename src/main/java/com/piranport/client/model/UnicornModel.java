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
 * Unicorn standby model: Alex-shaped body with high-resolution UVs plus carrier rigging.
 */
public class UnicornModel extends PlayerModel<ShipGirlEntity> {
    public static final int SKIN_ID = 18;
    private static final float PLAYER_UV_SCALE = 0.25F;

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "unicorn"), "main");
    public static final ResourceLocation TEXTURE_LOCATION =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/shipgirl/unicorn.png");

    private final ModelPart flightDeck;
    private final ModelPart supportRigging;

    public UnicornModel(ModelPart root) {
        super(root, true);
        this.flightDeck = this.body.getChild("unicorn_flight_deck");
        this.supportRigging = this.body.getChild("unicorn_support_rigging");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
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

        body.addOrReplaceChild("unicorn_flight_deck",
                CubeListBuilder.create()
                        .texOffs(192, 0).addBox(4.4F, 2.4F, -1.2F, 2.2F, 12.0F, 2.4F, new CubeDeformation(0.02F))
                        .texOffs(192, 0).addBox(4.1F, 10.6F, -1.6F, 2.8F, 4.0F, 3.2F, new CubeDeformation(0.02F))
                        .texOffs(192, 0).addBox(3.8F, 1.4F, -1.0F, 0.8F, 11.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(192, 0).addBox(6.4F, 3.6F, -0.2F, 1.8F, 1.0F, 0.4F, new CubeDeformation(0.0F))
                        .texOffs(192, 0).addBox(6.4F, 7.2F, -0.2F, 1.8F, 1.0F, 0.4F, new CubeDeformation(0.0F)),
                PartPose.rotation(0.0F, 0.0F, -0.12F));

        body.addOrReplaceChild("unicorn_support_rigging",
                CubeListBuilder.create()
                        .texOffs(192, 64).addBox(-5.8F, 4.0F, 2.6F, 1.2F, 9.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(192, 64).addBox(4.6F, 4.0F, 2.6F, 1.2F, 9.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(192, 64).addBox(-5.0F, 11.2F, 2.7F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
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

        float sway = Mth.sin(ageInTicks * 0.08F) * 0.02F;
        this.flightDeck.zRot = -0.12F + sway;
        this.supportRigging.zRot = sway * 0.6F;
    }
}
