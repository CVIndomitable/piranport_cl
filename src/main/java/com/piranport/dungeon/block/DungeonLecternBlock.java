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

import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.network.OpenContinueScreenPayload;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * 副本书台（讲台）方块。策划决策/副本/17 §三：书台本身就是入口，入口传送门已作废。
 *
 * <p>交互（§3.2，与铁砧/工作台同级）：
 * <ul>
 *   <li>右键（空手或手持物品）：书台空且手持钥匙 → 插入并立即创建实例；否则按纹路分流——
 *       白提示要钥匙、红提示建造中/满员、绿打开"最新记录点 / 从头开始"进本界面。</li>
 *   <li>空手潜行右键：撤下钥匙；建造中锁定，提示"建造中，钥匙已锁定"。</li>
 *   <li>手持物品潜行右键：对手中物品使用，不触发书台（原版语义）。</li>
 *   <li>任何方式破坏书台：钥匙掉落，绑定不清（{@link #onRemove}）。</li>
 * </ul>
 *
 * <p>外观：{@link #HAS_KEY} 切模型（空台面 / 插钥匙），{@link #PATTERN} 驱动纹路面
 * BlockColor 染色（白/红/绿）。两者都由 {@link DungeonLecternBlockEntity} 服务端回写。
 */
public class DungeonLecternBlock extends BaseEntityBlock {
    public static final MapCodec<DungeonLecternBlock> CODEC = simpleCodec(DungeonLecternBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** 台面上是否插着钥匙。仅用于外观分层，不参与任何逻辑判定（逻辑一律问 BE）。 */
    public static final BooleanProperty HAS_KEY = BooleanProperty.create("has_key");

    /** 纹路三态（副本/17 §3.1），只驱动纹路面 BlockColor 染色；逻辑仍问 BE 的 status。 */
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<LecternPattern> PATTERN =
            net.minecraft.world.level.block.state.properties.EnumProperty.create("pattern", LecternPattern.class);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public DungeonLecternBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_KEY, false)
                .setValue(PATTERN, LecternPattern.WHITE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_KEY, PATTERN);
    }

    /**
     * 书台被移除（玩家破坏、爆炸、指令替换）时掉落钥匙（副本/17 §3.2）。
     *
     * <p>钥匙只活在 BE 的 {@code keyStack} 里，没有掉落表兜底；此前只在 {@code playerWillDestroy}
     * 交还，爆炸破坏会让钥匙与整个实例进度一起蒸发。改挂 {@code onRemove} 后覆盖所有移除路径。
     * 建造中破坏同样掉落——这是锁定期间唯一的取出途径。钥匙上的 instanceId 不清，
     * 换一座书台插回去仍指向同一实例，不新建。
     *
     * <p>同方块换状态（HAS_KEY / PATTERN 回写）也会进 onRemove，必须用方块是否变化过滤。
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof DungeonLecternBlockEntity lectern && lectern.hasKey()) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0,
                    pos.getZ() + 0.5, lectern.takeKeyForDrop());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
     * 手持物品右键（副本/17 §3.2，与铁砧/工作台同级）：先走书台，不先用手中物品。
     *
     * <ul>
     *   <li>非潜行：手持钥匙且书台空 → 插入；否则按书台状态提示或打开进本界面。</li>
     *   <li>潜行：交还给物品本身（原版语义：潜行右键对着方块使用手中物品）。</li>
     * </ul>
     *
     * <p>引擎在「潜行且至少一只手非空」时会整块跳过 useItemOn/useWithoutItem，潜行持物天然
     * 落到物品使用，这里的潜行分支只是兜底。
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isSecondaryUseActive()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 副手触发时不重复开界面：主手那次已经处理过书台。
        if (hand == InteractionHand.OFF_HAND && !(stack.getItem() instanceof com.piranport.dungeon.key.DungeonKeyItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level.getBlockEntity(pos) instanceof DungeonLecternBlockEntity lecternBE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!lecternBE.hasKey() && stack.getItem() instanceof com.piranport.dungeon.key.DungeonKeyItem
                && lecternBE.tryInsertKey(player, hand)) {
            return ItemInteractionResult.SUCCESS;
        }
        openLectern(serverPlayer, (ServerLevel) level, pos, lecternBE);
        return ItemInteractionResult.SUCCESS;
    }

    /**
     * 空手右键：普通右键 = 打开书台（进本）；潜行右键 = 撤下钥匙（2026-10-01 项目所有者定）。
     * 建造中钥匙锁定，不能撤下。
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level.getBlockEntity(pos) instanceof DungeonLecternBlockEntity lecternBE)) {
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive()) {
            if (!lecternBE.hasKey()) {
                serverPlayer.displayClientMessage(
                        Component.translatable("block.piranport.dungeon_lectern.no_key_to_take"), true);
                return InteractionResult.CONSUME;
            }
            lecternBE.refreshStatus();
            if (!LecternPattern.canWithdraw(lecternBE.getStatus())) {
                serverPlayer.displayClientMessage(
                        Component.translatable("block.piranport.dungeon_lectern.key_locked"), true);
                return InteractionResult.CONSUME;
            }
            lecternBE.extractKeyForShiftRightClick(serverPlayer);
            return InteractionResult.CONSUME;
        }
        openLectern(serverPlayer, (ServerLevel) level, pos, lecternBE);
        return InteractionResult.CONSUME;
    }

    /** 按纹路三态分流：白=提示要钥匙，红=提示建造中/满员/钥匙无效，绿=打开继续界面。 */
    private static void openLectern(ServerPlayer player, ServerLevel level, BlockPos pos,
                                    DungeonLecternBlockEntity lecternBE) {
        lecternBE.refreshStatus();
        String blocked = switch (lecternBE.getStatus()) {
            case EMPTY -> "block.piranport.dungeon_lectern.no_key";
            case BUILDING -> "block.piranport.dungeon_lectern.building";
            case FULL -> "dungeon.piranport.entry_blocked.instance_full";
            case INVALID -> "block.piranport.dungeon_lectern.invalid_key";
            case READY -> null;
        };
        if (blocked != null) {
            player.displayClientMessage(Component.translatable(blocked), true);
            return;
        }
        // 整合版 §3.1：有进度弹"最新记录点 / 从头开始"；无进度 ContinueScreen 显示"直接进入"。
        DungeonInstanceManager mgr = DungeonInstanceManager.get(level);
        String stageDisplay = lecternBE.getLabelStage().isEmpty() ? "(unknown)" : lecternBE.getLabelStage();
        int clearedCount = 0;
        UUID instId = com.piranport.dungeon.key.DungeonKeyItem.getInstanceId(lecternBE.getKeyStack());
        DungeonInstance inst = instId == null ? null : mgr.getInstance(instId);
        if (inst != null) clearedCount = inst.getClearedNodes().size();
        PacketDistributor.sendToPlayer(player, new OpenContinueScreenPayload(pos, stageDisplay, clearedCount));
    }
}
