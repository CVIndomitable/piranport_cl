package com.piranport.item;

import com.piranport.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Smoke Canister (发烟筒) — on use, places smoke screen blocks
 * at the player's head position, replacing air / grass / existing smoke.
 * Consumes 1 durability per use.
 */
public class SmokeCandleItem extends Item {

    public SmokeCandleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            BlockPos headPos = player.blockPosition().above(); // head-level
            BlockState smokeState = ModBlocks.SMOKE_SCREEN.get().defaultBlockState();

            boolean placed = false;
            // Place smoke in a 3×3×2 area centred on the player head
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        BlockPos target = headPos.offset(dx, dy, dz);
                        if (canReplace(level, target)) {
                            level.setBlock(target, smokeState, Block.UPDATE_ALL);
                            placed = true;
                        }
                    }
                }
            }

            if (placed) {
                stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Only replace air, short grass, tall grass, fern, and existing smoke. */
    private static boolean canReplace(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        return state.isAir()
                || block == Blocks.SHORT_GRASS
                || block == Blocks.TALL_GRASS
                || block == Blocks.FERN
                || block == Blocks.LARGE_FERN
                || block == ModBlocks.SMOKE_SCREEN.get();
    }
}
