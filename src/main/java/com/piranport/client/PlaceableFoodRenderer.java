package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.block.entity.PlaceableFoodBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PlaceableFoodRenderer implements BlockEntityRenderer<PlaceableFoodBlockEntity> {
    public PlaceableFoodRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(PlaceableFoodBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.isEmpty()) return;
        var item = BuiltInRegistries.ITEM.get(blockEntity.getFoodItemId());
        if (item == Items.AIR) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.2, 0.5);
        poseStack.scale(0.5f, 0.5f, 0.5f);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(item), ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource,
                blockEntity.getLevel(), 0
        );
        poseStack.popPose();
    }
}
