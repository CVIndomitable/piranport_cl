package com.piranport.dungeon.block;

import com.piranport.PiranPort;
import com.piranport.dungeon.block.entity.DungeonPortalBlockEntity;
import com.piranport.dungeon.event.DungeonEventHandler;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.registry.ModBlockEntityTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Dungeon portal block - part of a multi-block portal structure.
 * When the portal frame is complete (4x5 frame surrounding a 2x3 opening),
 * stepping into the opening teleports players to the next dungeon node.
 */
public class DungeonPortalBlock extends BaseEntityBlock {
    protected static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public DungeonPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(DungeonPortalBlock::new);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Only handle sneaking right-click for activation
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof DungeonPortalBlockEntity portalBE)) {
            return InteractionResult.PASS;
        }

        // Check if the portal structure is complete
        if (portalBE.isStructureComplete()) {
            // Try to activate the portal if we have the right item
            ItemStack stack = serverPlayer.getMainHandItem();
            if (stack.isEmpty()) {
                stack = serverPlayer.getOffhandItem();
            }

            // Look for dungeon key or activation core
            // For now, we'll just check if structure is ready and show message
            if (portalBE.isReadyForActivation()) {
                portalBE.activatePortal(serverPlayer);
                return InteractionResult.CONSUME;
            } else {
                serverPlayer.displayClientMessage(
                        Component.translatable("block.piranport.dungeon_portal.waiting_for_activation"), true);
                return InteractionResult.CONSUME;
            }
        } else {
            serverPlayer.displayClientMessage(
                    Component.translatable("block.piranport.dungeon_portal.incomplete_structure"), true);
            return InteractionResult.CONSUME;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonPortalBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null :
                createTickerHelper(type, ModBlockEntityTypes.DUNGEON_PORTAL.get(),
                        DungeonPortalBlockEntity::tick);
    }

    /**
     * Called when a neighboring block changes - used to detect when portal structure is complete
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DungeonPortalBlockEntity portalBE) {
                portalBE.checkStructure();
            }
        }
        super.neighborChanged(state, level, pos, blockIn, fromPos, isMoving);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Delegate to BlockEntity for teleportation logic
        if (!level.isClientSide && level.hasChunkAt(pos)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DungeonPortalBlockEntity portalBE) {
                portalBE.entityInside(level, pos, state, entity);
            }
        }
        super.entityInside(state, level, pos, entity);
    }
}