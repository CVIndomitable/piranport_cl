package com.piranport.block;

import com.mojang.serialization.MapCodec;
import com.piranport.block.entity.YubariWaterBucketBlockEntity;
import com.piranport.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class YubariWaterBucketBlock extends BaseEntityBlock {
    public static final MapCodec<YubariWaterBucketBlock> CODEC = simpleCodec(YubariWaterBucketBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public YubariWaterBucketBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new YubariWaterBucketBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide) {
            return createTickerHelper(type, ModBlockEntityTypes.YUBARI_WATER_BUCKET.get(),
                    YubariWaterBucketBlockEntity::serverTick);
        }
        return null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Empty bucket → water bucket
        if (stack.is(Items.BUCKET)) {
            if (!level.isClientSide) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                player.getInventory().placeItemBackInInventory(new ItemStack(Items.WATER_BUCKET));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // Empty glass bottle → water bottle (potion of water)
        if (stack.is(Items.GLASS_BOTTLE)) {
            if (!level.isClientSide) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                ItemStack waterBottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);
                player.getInventory().placeItemBackInInventory(waterBottle);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}
