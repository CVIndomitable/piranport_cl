package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.block.entity.CuttingBoardBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CuttingBoardRenderer implements BlockEntityRenderer<CuttingBoardBlockEntity> {
    public CuttingBoardRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(CuttingBoardBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack stack = blockEntity.getStoredItem();
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.15, 0.5);
        poseStack.scale(0.5f, 0.5f, 0.5f);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource,
                blockEntity.getLevel(), 0
        );
        poseStack.popPose();
    }
}
