package com.piranport.block;

import com.mojang.serialization.MapCodec;
import com.piranport.block.entity.PlaceableFoodBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PlaceableFoodBlock extends BaseEntityBlock {
    private final VoxelShape shape;

    public PlaceableFoodBlock(VoxelShape shape, BlockBehaviour.Properties props) {
        super(props);
        this.shape = shape;
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        throw new UnsupportedOperationException("PlaceableFoodBlock is not world-gen serializable");
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlaceableFoodBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PlaceableFoodBlockEntity foodBE && !foodBE.isEmpty()) {
            if (!level.isClientSide) {
                foodBE.eat(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    // === Subclasses with shape-specific codecs ===

    public static class Plate extends PlaceableFoodBlock {
        public static final MapCodec<Plate> CODEC = simpleCodec(Plate::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Plate(BlockBehaviour.Properties props) { super(Block.box(0, 0, 0, 16, 4, 16), props); }
    }

    public static class Bowl extends PlaceableFoodBlock {
        public static final MapCodec<Bowl> CODEC = simpleCodec(Bowl::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Bowl(BlockBehaviour.Properties props) { super(Block.box(2, 0, 2, 14, 6, 14), props); }
    }

    public static class Cake extends PlaceableFoodBlock {
        public static final MapCodec<Cake> CODEC = simpleCodec(Cake::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Cake(BlockBehaviour.Properties props) { super(Block.box(1, 0, 1, 15, 8, 15), props); }
    }
}
