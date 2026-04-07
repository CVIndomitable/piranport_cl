package com.piranport.block;

import com.mojang.serialization.MapCodec;
import com.piranport.block.entity.AbyssalSpawnerBlockEntity;
import com.piranport.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * One-shot spawner placed inside structure templates at enemy spawn marker positions.
 * On first tick (server side), spawns the configured deep ocean entities,
 * assigns them a shared cluster UUID, then self-destructs (replaces itself with air).
 */
public class AbyssalSpawnerBlock extends BaseEntityBlock {

    public static final MapCodec<AbyssalSpawnerBlock> CODEC = simpleCodec(AbyssalSpawnerBlock::new);

    public AbyssalSpawnerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AbyssalSpawnerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntityTypes.ABYSSAL_SPAWNER.get(),
                AbyssalSpawnerBlockEntity::serverTick);
    }
}
