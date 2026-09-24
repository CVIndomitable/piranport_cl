package com.piranport.dungeon.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 *   <li>右键：BE 无钥匙 → 玩家背包有钥匙则插入；BE 有钥匙 → 进入副本（打开 ContinueScreen）</li>
 *   <li>Shift+右键：取出钥匙到玩家背包。<b>两条方法各覆盖一半场景</b>：
 *       {@link #useItemOn} 管"潜行 + 手上拿着东西"，{@link #useWithoutItem} 管"潜行 + 主手空手"。
 *       缺一不可——主手空手时引擎不会调用 useItemOn（见 {@link #useWithoutItem} 的潜行分支说明），
 *       而潜行且手上有东西时引擎会整块跳过 useWithoutItem（见 {@link #useItemOn} 的 flag1 说明）</li>
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
     * 破坏前把插着的钥匙交还玩家。
     *
     * <h2>为什么必须在这里掉落（260924 审查 P0-6）</h2>
     * <p>讲台的钥匙只活在 BlockEntity 的 {@code keyStack} 字段里，BE 没有 {@code onRemove}
     * 之类钩子，且 {@code data/piranport/loot_table/} 下<b>没有</b> {@code dungeon_lectern}
     * 掉落表——破坏后方块与钥匙双双蒸发。</p>
     *
     * <p>后果比"丢一件道具"严重得多：{@code DungeonEntryService.enter} 的唯一入口要求
     * 「讲台方块存在且 {@code hasKey()}」，而实例存档里的 {@code lecternPos} 只被读取做距离
     * 判定、<b>没有任何一处用它重建讲台</b>。所以拆掉讲台后，该实例的
     * {@code ClearedNodes} 等进度就成了谁也读不到的孤儿数据——钥匙、方块、可达性三者全灭。
     * 玩家只是顺手挖了个自己放下去的讲台，就永久失去一整个副本的进度。</p>
     *
     * <p>交还语义与 {@code extractKeyForShiftRightClick} 完全一致（背包满则落地），
     * 因此走同一个方法即可，不重复实现。</p>
     */
    @Override
    public BlockState playerWillDestroy(net.minecraft.world.level.Level level, BlockPos pos,
                                        BlockState state, Player player) {
        if (!level.isClientSide()) {
            // 先归还钥匙：extractKeyForShiftRightClick 内部会清 keyStack + setChanged，
            // 并保留 dungeonInstanceUuid（副本可被其他玩家继续，与主动取钥匙同一约定）。
            if (level.getBlockEntity(pos) instanceof DungeonLecternBlockEntity lectern
                    && lectern.hasKey()) {
                lectern.extractKeyForShiftRightClick(player);
            }
            // 再脱掉 HAS_KEY，避免区块被卸载时带着"已插钥匙"的状态存盘，
            // 下次加载模型显示插着钥匙但 BE 里其实已经空了。
            if (state.getValue(HAS_KEY)) {
                level.setBlock(pos, state.setValue(HAS_KEY, false), Block.UPDATE_ALL);
            }
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

    /**
     * 手持物品时的交互入口：潜行取钥匙，非潜行交回 {@link #useWithoutItem}。
     *
     * <p><b>什么时候会走到这里：</b>只有"主手/副手拿着物品、且物品对空手交互没有抢先消费"时，
     * 引擎才会调用本方法。主手空手右键时 {@code handleUseItemOn} 会直接跳过整个
     * {@code gameMode.useItemOn}（客户端 {@code Minecraft#startUseItem} 也只在
     * {@code itemstack} 非空时才调 {@code gameMode.useItemOn}），那条路径由
     * {@link #useWithoutItem} 的潜行分支兜底——两处必须同时存在，覆盖"潜行取钥匙"的全部手势。
     *
     * <p><b>为什么潜行分支不能只写在这里：</b>当玩家潜行且两手中至少有一手非空时，
     * {@code ServerPlayerGameMode.useItemOn} 会算出 {@code flag1 = true}，随即将 {@code useItemOn}
     * 与紧随其后的 {@code useWithoutItem} <b>整块跳过</b>（1.21.1 引擎行为，见该方法的
     * flag/flag1 判定）。反过来，主手空手时又走不到本方法。所以两边都要有，不能二选一。
     *
     * <p>{@code flag1} 只拦 {@code getUseBlock().isDefault()} 这一支；一旦本方法返回
     * {@code consumesAction() == true}，引擎在越过 flag1 前就已处理完毕，故不必也不该关心潜行状态以外的事。
     *
     * <p>非潜行时一律返回 {@link ItemInteractionResult#PASS_TO_DEFAULT_BLOCK_INTERACTION}，
     * 把"插入钥匙 / 打开继续界面"交回 {@link #useWithoutItem}——那条路径本来就没被 flag1 拦住。
     * 注意 {@code PASS_TO_DEFAULT_BLOCK_INTERACTION} 只在主手触发时才会续接 useWithoutItem，
     * 副手触发不会；这与原版行为一致，无需额外处理。
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        // 非潜行 → 交回 useWithoutItem 走"插入/进入副本"，本方法不消费
        if (!player.isSecondaryUseActive()) {
            if (!level.isClientSide() && stack.getItem() instanceof com.piranport.dungeon.key.DungeonKeyItem) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof DungeonLecternBlockEntity lecternBE && !lecternBE.hasKey()
                        && lecternBE.tryInsertKey(player, hand)) {
                    return ItemInteractionResult.SUCCESS;
                }
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            // 客户端只做预测：返回成功让手臂/音效有反馈，真正的取钥匙在服务端执行。
            // 与服务端同构的判定（都只看潜行状态）保证两端不会出现预测-回滚。
            return ItemInteractionResult.sidedSuccess(true);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof DungeonLecternBlockEntity lecternBE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 潜行 + 手上拿着东西 = 取下钥匙（唯一能绕开引擎 flag1 闸门的入口）
        if (lecternBE.extractKeyForShiftRightClick(serverPlayer)) {
            return ItemInteractionResult.SUCCESS;
        }
        // 讲台上没钥匙：给出可见反馈（静默失败是本次 bug 的原始症状，不能再留），
        // 并返回"不消费"让引擎继续处理手上物品（与"蹲下+右键不触发方块交互"的原版预期一致）
        serverPlayer.sendSystemMessage(
                Component.translatable("block.piranport.dungeon_lectern.no_key_to_take"));
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        // 客户端预测：判定只看"潜行 + 是不是主手"这类本地已知信息，与服务端完全同构，
        // 因此不会出现"客户端预测成功、服务端拒绝"的预测-回滚闪烁。
        // 引擎在本方法返回 consumesAction() 后即为这一步发送 ServerboundUseItemPacket，
        // 所以非潜行分支照旧返回 SUCCESS（原行为，不改）。
        if (level.isClientSide()) {
            return player.isSecondaryUseActive()
                    ? InteractionResult.sidedSuccess(true)
                    : InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof DungeonLecternBlockEntity lecternBE)) {
            return InteractionResult.PASS;
        }

        // ===== 潜行分支：取钥匙 =====
        // 主手空手 + 潜行时，引擎压根不会调用 useItemOn（handleUseItemOn 的 itemstack 为空时
        // 直接不发包；客户端 startUseItem 也只在 itemstack 非空时才调 gameMode.useItemOn），
        // 只能落到这里——这就是"蹲下右键取不下钥匙"在主手空手时的根因。
        // useItemOn 只覆盖"潜行 + 手上拿着东西"那一条路径，两条路共用同一个取钥匙方法。
        if (player.isSecondaryUseActive()) {
            if (lecternBE.extractKeyForShiftRightClick(serverPlayer)) {
                return InteractionResult.sidedSuccess(false);
            }
            // 讲台上没钥匙：可见反馈，然后不消费，保持"潜行右键不触发方块常规交互"的原版语义
            serverPlayer.sendSystemMessage(
                    Component.translatable("block.piranport.dungeon_lectern.no_key_to_take"));
            return InteractionResult.PASS;
        }

        // 非潜行 → 右键：BE 无钥匙 → 插入；BE 有钥匙 → 进入副本（阶段 3 改为打开 ContinueScreen）
        if (!lecternBE.hasKey()) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("block.piranport.dungeon_lectern.no_key"));
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
            // instId 为 null：钥匙尚未绑定实例（首次插入），从钥匙的 stageId 回退获取名称。
            // 解析走 DungeonKeyItem.resolveStageId —— 与 DungeonEntryService.enter 的入口判定
            // 共用同一个方法，避免"对话框显示了章节名、点进去却说副本不存在"。
            if (lecternBE.getKeyStack().getItem() instanceof com.piranport.dungeon.key.DungeonKeyItem) {
                com.piranport.dungeon.data.StageData fallbackStage =
                        com.piranport.dungeon.data.DungeonRegistry.INSTANCE.getStage(
                                com.piranport.dungeon.key.DungeonKeyItem.resolveStageId(lecternBE.getKeyStack()));
                if (fallbackStage != null) {
                    stageDisplay = fallbackStage.displayName();
                }
            }
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                new com.piranport.dungeon.network.OpenContinueScreenPayload(pos, stageDisplay, clearedCount));
        return InteractionResult.CONSUME;
    }
}
