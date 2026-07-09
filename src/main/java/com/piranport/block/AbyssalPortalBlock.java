package com.piranport.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Abyssal portal block — the non-solid "gate" inside a completed portal frame.
 * Portal block for travelling between the overworld and the abyssal world.
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

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            AbyssalPortalTeleporter.teleport(player);
        }
    }
}
