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
 * 装饰方块 — 港区椅子 (策划 §6 装饰系列)。
 *
 * <p>纯装饰方块：固定碰撞箱、可坐姿态（无功能）、无红石、无下落。
 * 与 {@link ItalianDishKitBlock} 风格保持一致——基色/音色/耐久，无 occlude。
 */
public class PirateChairBlock extends Block {

    // 靠背与椅腿构成的整体碰撞箱（X+Z 全宽，Y 含靠背）
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    // 视觉轮廓仅椅面 + 四腿（无靠背），模拟真实椅子轮廓
    private static final VoxelShape VISUAL = Shapes.or(
            Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0),         // 椅面（板）
            Block.box(0.0, 6.0, 0.0, 2.0, 16.0, 16.0),         // 左腿+左靠背
            Block.box(14.0, 6.0, 0.0, 16.0, 16.0, 16.0));      // 右腿+右靠背

    public PirateChairBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .sound(SoundType.WOOD)
                .strength(2.0f, 6.0f)
                .noOcclusion());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("block.piranport.pirate_chair.tooltip"));
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