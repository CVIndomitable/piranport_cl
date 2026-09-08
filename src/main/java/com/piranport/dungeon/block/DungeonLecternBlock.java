package com.piranport.dungeon.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 副本讲台方块。整合版 §2.2：钥匙插在讲台上，玩家不携带进副本。
 *
 * <p>交互流程：
 * <ul>
 *   <li>右键：BE 无钥匙 → 玩家背包有钥匙则插入；BE 有钥匙 → 进入副本（阶段 3 改为打开 ContinueScreen）</li>
 *   <li>Shift+右键：取出钥匙到玩家背包</li>
 * </ul>
 *
 * <p>阶段 2（P1-A）范围：BE 持有钥匙的持久化与插入/取出。阶段 3（P1-B）会替换为"打开 ContinueScreen"。
 */
public class DungeonLecternBlock extends BaseEntityBlock {
    public static final MapCodec<DungeonLecternBlock> CODEC = simpleCodec(DungeonLecternBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public DungeonLecternBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonLecternBlockEntity(pos, state);
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

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof DungeonLecternBlockEntity lecternBE)) {
            return InteractionResult.PASS;
        }

        // Shift+右键：取出钥匙
        if (player.isShiftKeyDown()) {
            if (lecternBE.extractKeyForShiftRightClick(serverPlayer)) {
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        // 右键：BE 无钥匙 → 插入；BE 有钥匙 → 进入副本（阶段 3 改为打开 ContinueScreen）
        if (!lecternBE.hasKey()) {
            if (!lecternBE.tryInsertKey(serverPlayer)) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("block.piranport.dungeon_lectern.no_key"));
            }
            return InteractionResult.CONSUME;
        }

        // 整合版 §3.1：BE 有钥匙 → 弹"继续/从头开始"对话框（OpenContinueScreenPayload）
        // 客户端打开 DungeonContinueScreen（独立 Screen，无 Menu）。
        // 已通关节点为 0 时仍弹框，但 ContinueScreen 显示"直接进入"按钮。
        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
        com.piranport.dungeon.instance.DungeonInstanceManager mgr =
                com.piranport.dungeon.instance.DungeonInstanceManager.get(serverLevel);
        String stageDisplay = "(unknown)";
        int clearedCount = 0;
        UUID instId = lecternBE.getDungeonInstanceUuid();
        if (instId != null) {
            com.piranport.dungeon.instance.DungeonInstance inst = mgr.getInstance(instId);
            if (inst != null) {
                clearedCount = inst.getClearedNodes().size();
                com.piranport.dungeon.data.StageData stage =
                        com.piranport.dungeon.data.DungeonRegistry.INSTANCE.getStage(inst.getStageId());
                if (stage != null) {
                    stageDisplay = stage.displayName();
                }
            }
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                new com.piranport.dungeon.network.OpenContinueScreenPayload(pos, stageDisplay, clearedCount));
        return InteractionResult.CONSUME;
    }
}
