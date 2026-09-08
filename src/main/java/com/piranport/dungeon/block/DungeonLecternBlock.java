package com.piranport.dungeon.block;

import com.mojang.serialization.MapCodec;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.menu.DungeonBookMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

/**
 * 副本讲台方块。整合版 §2.2：钥匙插在讲台上，玩家不携带进副本。
 * 当前阶段（P1-A 占位）：仍走玩家背包拿钥匙 + 打开 DungeonBookMenu。
 * 阶段 2：升级为 BaseEntityBlock，新建 DungeonLecternBlockEntity 持有钥匙。
 */
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

        // 检查玩家背包中是否有任意一把钥匙（整合版 §2.2：过渡期仍以背包为钥匙来源；阶段 2 升级为讲台 BE 持有）
        int keySlot = DungeonKeyItem.findAnyKeySlot(serverPlayer);
        if (keySlot < 0) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("block.piranport.dungeon_lectern.no_key"));
            return InteractionResult.CONSUME;
        }

        // 整合版 §3.1：联机大厅与队长机制已作废（副本/10），不再 joinLobby / broadcastLobbyUpdate。
        // 阶段 3（P1-B）将替换为"打开 DungeonContinueScreen"。

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