package com.piranport.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Abyssal portal block — the non-solid "gate" inside a completed portal frame.
 * Reserved for future version; no teleportation logic yet.
 * Similar to nether portal: non-colliding, emits light, unbreakable by hand.
 */
public class AbyssalPortalBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public AbyssalPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(net.minecraft.world.level.block.state.BlockState state,
                               BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
