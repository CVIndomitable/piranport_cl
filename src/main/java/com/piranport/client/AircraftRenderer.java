package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.entity.AircraftEntity;
import com.piranport.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Placeholder renderer — displays the aircraft item spinning at the entity position. */
public class AircraftRenderer extends EntityRenderer<AircraftEntity> {

    public AircraftRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(AircraftEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                (entity.tickCount + partialTick) * 3.0f));
        poseStack.scale(0.5f, 0.5f, 0.5f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                getDisplayStack(entity),
                ItemDisplayContext.GROUND,
                packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, entity.level(), entity.getId());

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static ItemStack getDisplayStack(AircraftEntity entity) {
        return switch (entity.getAircraftType()) {
            case FIGHTER        -> new ItemStack(ModItems.FIGHTER_SQUADRON.get());
            case DIVE_BOMBER    -> new ItemStack(ModItems.DIVE_BOMBER_SQUADRON.get());
            case TORPEDO_BOMBER -> new ItemStack(ModItems.TORPEDO_BOMBER_SQUADRON.get());
            case LEVEL_BOMBER   -> new ItemStack(ModItems.LEVEL_BOMBER_SQUADRON.get());
        };
    }

    @Override
    public ResourceLocation getTextureLocation(AircraftEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
