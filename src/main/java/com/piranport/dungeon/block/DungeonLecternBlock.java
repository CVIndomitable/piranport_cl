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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
 *
 * <p>外观：{@link #HAS_KEY} 驱动 blockstate 切模型——空台面 vs 台面上插着钥匙。
 * 该属性由 {@link DungeonLecternBlockEntity} 在增删钥匙时同步，因此模型状态与 BE 里的钥匙保持一致。
 */
public class DungeonLecternBlock extends BaseEntityBlock {
    public static final MapCodec<DungeonLecternBlock> CODEC = simpleCodec(DungeonLecternBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** 台面上是否插着钥匙。仅用于外观分层，不参与任何逻辑判定（逻辑一律问 BE）。 */
    public static final BooleanProperty HAS_KEY = BooleanProperty.create("has_key");

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public DungeonLecternBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_KEY, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_KEY);
    }

    /**
     * 抽取钥匙前先脱掉 HAS_KEY，避免区块被卸载时带着"已插钥匙"的状态存盘，
     * 下次加载模型显示插着钥匙但 BE 里其实已经空了。
     */
    @Override
    public BlockState playerWillDestroy(net.minecraft.world.level.Level level, BlockPos pos,
                                        BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(HAS_KEY)) {
            level.setBlock(pos, state.setValue(HAS_KEY, false), Block.UPDATE_ALL);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /**
     * 用方块模型渲染，而不是交给 BlockEntity 渲染器。
     *
     * <p>BaseEntityBlock 默认返回 {@link RenderShape#INVISIBLE}——那是在"BE 自带渲染器"
     * 的前提下的约定。本 BE 没有渲染器，不覆写的话区块渲染阶段会直接跳过方块模型，
     * 表现就是"只有碰撞箱、方块透明"。mod 里其他 BaseEntityBlock 都覆写了这一项。
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonLecternBlockEntity(pos, state);
    }

    /**
     * 服务端 ticker：只用于承接 {@code onLoad} 里调度的那次复检（见 BE#onLoad）。
     * 常规插入/取出走 BE 直接同步，不依赖 tick。
     */
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof DungeonLecternBlockEntity lecternBE) {
                lecternBE.tickFromScheduledUpdate();
            }
        };
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
        } else {
            // instId 为 null：钥匙尚未绑定实例（首次插入），从钥匙的 stageId 回退获取名称
            // 注意：钥匙的 stageId 可能是 chapter ID（如 "chapter_1"），需先解析为实际关卡 ID
            if (lecternBE.getKeyStack().getItem() instanceof com.piranport.dungeon.key.DungeonKeyItem) {
                String keyStageId = com.piranport.dungeon.key.DungeonKeyItem.getStageId(lecternBE.getKeyStack());
                if (!keyStageId.isEmpty()) {
                    // chapter_1 → 取该章节第一个 stage（如 "1-1"）
                    if (keyStageId.startsWith("chapter_")) {
                        com.piranport.dungeon.data.ChapterData chapter =
                                com.piranport.dungeon.data.DungeonRegistry.INSTANCE.getChapter(keyStageId);
                        if (chapter != null && !chapter.stages().isEmpty()) {
                            keyStageId = chapter.stages().get(0);
                        }
                    }
                    com.piranport.dungeon.data.StageData fallbackStage =
                            com.piranport.dungeon.data.DungeonRegistry.INSTANCE.getStage(keyStageId);
                    if (fallbackStage != null) {
                        stageDisplay = fallbackStage.displayName();
                    }
                }
            }
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                new com.piranport.dungeon.network.OpenContinueScreenPayload(pos, stageDisplay, clearedCount));
        return InteractionResult.CONSUME;
    }
}
