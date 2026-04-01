package com.piranport.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A thin salt layer block (like carpet) — produced when flowing water
 * evaporates above a lit furnace.
 */
public class SaltChipBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public SaltChipBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
