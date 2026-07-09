package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piranport.PiranPort;
import com.piranport.entity.CannonProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/**
 * Stable renderer for fast cannon projectiles.
 *
 * <p>The vanilla thrown-item renderer rotates small sprites aggressively at high
 * speed, which makes shells appear to wobble even when their smoke trail is
 * stable. This renderer keeps the item sprite aligned to velocity instead.
 */
public class CannonProjectileRenderer extends EntityRenderer<CannonProjectileEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/item/projectile_bullet.png");

    private final ItemRenderer itemRenderer;

    public CannonProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.05f;
    }

    @Override
    public void render(CannonProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        Vec3 velocity = entity.getDeltaMovement();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (velocity.lengthSqr() > 1.0e-6) {
            float yaw = (float) (Mth.atan2(velocity.x, velocity.z) * Mth.RAD_TO_DEG);
            float pitch = (float) (Mth.atan2(velocity.y, horizontal) * Mth.RAD_TO_DEG);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        }

        poseStack.scale(1.35f, 1.35f, 1.35f);
        itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CannonProjectileEntity entity) {
        return TEXTURE;
    }
}
