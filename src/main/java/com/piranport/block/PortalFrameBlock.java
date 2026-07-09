package com.piranport.block;

import com.piranport.advancement.ModAdvancements;
import com.piranport.registry.ModBlocks;
import com.piranport.registry.ModItems;
import com.piranport.worldgen.AbyssalDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Abyssal portal frame block — found in portal ruin structures.
 * Right-click with a portal activation core to open a two-block-high portal.
 */
public class PortalFrameBlock extends Block {

    public PortalFrameBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.PORTAL_ACTIVATION_CORE.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel)
                || serverLevel.getServer().getLevel(AbyssalDimensions.ABYSSAL_WORLD) == null) {
            player.displayClientMessage(Component.translatable("message.piranport.abyssal_portal.missing_dimension"), true);
            return ItemInteractionResult.FAIL;
        }

        BlockPos portalBase = findPortalBase(level, pos, hit.getDirection());
        if (portalBase == null) {
            player.displayClientMessage(Component.translatable("message.piranport.abyssal_portal.no_space"), true);
            return ItemInteractionResult.FAIL;
        }

        level.setBlock(portalBase, ModBlocks.ABYSSAL_PORTAL.get().defaultBlockState(), 3);
        level.setBlock(portalBase.above(), ModBlocks.ABYSSAL_PORTAL.get().defaultBlockState(), 3);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "story/activate_abyssal_portal");
        }
        player.displayClientMessage(Component.translatable("message.piranport.abyssal_portal.activated"), true);
        return ItemInteractionResult.SUCCESS;
    }

    private static BlockPos findPortalBase(Level level, BlockPos framePos, Direction clickedFace) {
        BlockPos first = clickedFace.getAxis().isHorizontal()
                ? framePos.relative(clickedFace)
                : framePos.above();
        if (canPlacePortalAt(level, first)) {
            return first;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = framePos.relative(direction);
            if (canPlacePortalAt(level, candidate)) {
                return candidate;
            }
        }

        BlockPos above = framePos.above();
        return canPlacePortalAt(level, above) ? above : null;
    }

    private static boolean canPlacePortalAt(Level level, BlockPos base) {
        return canReplace(level.getBlockState(base)) && canReplace(level.getBlockState(base.above()));
    }

    private static boolean canReplace(BlockState state) {
        return state.isAir()
                || state.is(Blocks.WATER)
                || state.is(ModBlocks.ABYSSAL_PORTAL.get());
    }
}
