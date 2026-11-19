package com.piranport.block;

import com.mojang.serialization.MapCodec;
import com.piranport.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public class TeaBushBlock extends SweetBerryBushBlock {
    private static final MapCodec<SweetBerryBushBlock> TEA_CODEC = simpleCodec(TeaBushBlock::new);

    public TeaBushBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<SweetBerryBushBlock> codec() {
        return TEA_CODEC;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.TEA_LEAVES.get());
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // 茶树没有甜浆果丛的刺。
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(AGE) != MAX_AGE) return InteractionResult.PASS;
        if (!level.isClientSide) {
            BlockState harvested = state.setValue(AGE, 1);
            level.setBlockAndUpdate(pos, harvested);
            popResource(level, pos, new ItemStack(ModItems.TEA_LEAVES.get(), 2 + level.random.nextInt(2)));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, harvested));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
