package com.piranport.block;

import com.mojang.serialization.MapCodec;
import com.piranport.block.entity.ReloadFacilityBlockEntity;
import com.piranport.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ReloadFacilityBlock extends BaseEntityBlock {
    public static final MapCodec<ReloadFacilityBlock> CODEC = simpleCodec(ReloadFacilityBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * 碰撞形状 = 四条立柱（模型里 4x4 的角柱）。
     * 模型是"四角立柱 + 顶部横梁的开放龙门吊"，中间是给舰娘站进去的作业区，
     * 所以碰撞体不能是整个立方体——否则玩家会被挡在门外，站不进中间，
     * 而模型看起来明明是通的。横梁在 2 格高处，玩家身高 1.8 格会自然从下面穿过。
     */
    private static final VoxelShape COLLISION_SHAPE = Shapes.or(
            Block.box(0, 0, 0, 4, 16, 4),      // 前左柱
            Block.box(12, 0, 0, 16, 16, 4),    // 前右柱
            Block.box(0, 0, 12, 4, 16, 16),    // 后左柱
            Block.box(12, 0, 12, 16, 16, 16)   // 后右柱
    );

    /**
     * 交互/选取形状 = 整块占地，并向上覆盖到模型顶部（模型最高 30.5/16 格）。
     *
     * <p>模型是空心的，只按立柱做射线检测时，玩家对着中间的空隙或第二格高的横梁
     * 右键会直接穿透，很不方便。方块放置只占一格，但模型伸进上方一格；把 getShape
     * 扩到 30.5 高后，射线穿过上方那格时也会命中本方块位置，右键照样能开界面。
     * 碰撞仍用 {@link #COLLISION_SHAPE}，玩家照常可以站进龙门吊中间。</p>
     */
    private static final VoxelShape INTERACTION_SHAPE = Block.box(0, 0, 0, 16, 30.5, 16);

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public ReloadFacilityBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return INTERACTION_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext ctx) {
        return COLLISION_SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReloadFacilityBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide) {
            return createTickerHelper(type, ModBlockEntityTypes.RELOAD_FACILITY.get(),
                    ReloadFacilityBlockEntity::serverTick);
        }
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ReloadFacilityBlockEntity facility) {
                ((ServerPlayer) player).openMenu(facility, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ReloadFacilityBlockEntity facility) {
                for (int i = 0; i < ReloadFacilityBlockEntity.TOTAL_SLOTS; i++) {
                    net.minecraft.world.item.ItemStack stack = facility.getItemHandler().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // Comparator support — signal strength based on output slot
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ReloadFacilityBlockEntity facility) {
            return facility.getComparatorOutput();
        }
        return 0;
    }
}
