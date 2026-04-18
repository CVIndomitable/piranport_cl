package com.piranport.dungeon.block;

import com.mojang.serialization.MapCodec;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.FlagshipManager;
import com.piranport.dungeon.lobby.DungeonLobbyManager;
import com.piranport.dungeon.menu.DungeonBookMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DungeonLecternBlock extends Block {
    public static final MapCodec<DungeonLecternBlock> CODEC = simpleCodec(DungeonLecternBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public DungeonLecternBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        // Check if player has a dungeon key
        int keySlot = FlagshipManager.findAnyKeySlot(serverPlayer);
        if (keySlot < 0) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("block.piranport.dungeon_lectern.no_key"));
            return InteractionResult.CONSUME;
        }

        // Leave any other lobby first to avoid the player being a "phantom" member
        // in two lobbies at once (broadcasts go to ghost players, MAX_LOBBY_SIZE inflated).
        GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);
        GlobalPos existing = DungeonLobbyManager.INSTANCE.findLobbyOf(serverPlayer.getUUID());
        if (existing != null && !existing.equals(globalPos)) {
            DungeonLobbyManager.INSTANCE.leaveLobby(existing, serverPlayer.getUUID());
            DungeonLobbyManager.INSTANCE.broadcastLobbyUpdate(serverPlayer.server, existing);
        }
        DungeonLobbyManager.INSTANCE.joinLobby(globalPos, serverPlayer);

        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (containerId, playerInv, p) -> new DungeonBookMenu(containerId, playerInv, pos, keySlot),
                        Component.translatable("container.piranport.dungeon_book")
                ),
                buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeVarInt(keySlot);
                }
        );

        return InteractionResult.CONSUME;
    }
}
