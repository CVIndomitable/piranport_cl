package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.block.entity.PlaceableFoodBlockEntity;
import com.piranport.registry.ModBlocks;
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
        // 通用容器（盘/碗/蛋糕台）模型不含食物本体，需叠浮空物品图标示意内容物；
        // 专属食物方块（吐司面包等）模型自含本体，跳过图标避免叠影
        var state = blockEntity.getBlockState();
        if (!state.is(ModBlocks.PLATE_FOOD.get())
                && !state.is(ModBlocks.BOWL_FOOD.get())
                && !state.is(ModBlocks.CAKE_FOOD.get())) {
            return;
        }
        var item = BuiltInRegistries.ITEM.get(blockEntity.getFoodItemId());
        if (item == Items.AIR) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.2, 0.5);
        poseStack.scale(0.5f, 0.5f, 0.5f);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                item.getDefaultInstance(), ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource,
                blockEntity.getLevel(), 0
        );
        poseStack.popPose();
    }
}
