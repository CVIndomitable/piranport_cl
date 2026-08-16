package com.piranport.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * 装饰方块 — 港区木桌 (策划 §6 装饰系列)。
 *
 * <p>纯装饰：14 像素高桌面 + 桌腿轮廓，noOcclusion，红色木色匹配港区氛围。
 */
public class PirateTableBlock extends Block {

    // 整体碰撞箱（占满空间）
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    // 视觉：桌面 + 四腿
    private static final VoxelShape VISUAL = makeVisual();

    public PirateTableBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .sound(SoundType.WOOD)
                .strength(2.0f, 6.0f)
                .noOcclusion());
    }

    private static VoxelShape makeVisual() {
        VoxelShape top = Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0);
        VoxelShape leftBack = Block.box(0.0, 0.0, 0.0, 2.0, 14.0, 2.0);
        VoxelShape rightBack = Block.box(14.0, 0.0, 0.0, 16.0, 14.0, 2.0);
        VoxelShape leftFront = Block.box(0.0, 0.0, 14.0, 2.0, 14.0, 16.0);
        VoxelShape rightFront = Block.box(14.0, 0.0, 14.0, 16.0, 14.0, 16.0);
        return Shapes.or(top, leftBack, rightBack, leftFront, rightFront);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("block.piranport.pirate_table.tooltip"));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos,
                               CollisionContext context) {
        return VISUAL;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos,
                                        CollisionContext context) {
        return SHAPE;
    }
}