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
import net.minecraft.world.level.block.RenderShape;
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

    /**
     * 右键只作诊断提示，不再承担激活职责。
     *
     * <p>按《副本/17》定稿，进入方式是<b>走进传送门</b>：门框成立且底边挂着持钥匙的讲台时，
     * 门常开，无需手动 ignition。此前的 Shift+右键 activatePortal 是旧口径残留，
     * 会形成第二条与权威入口并行的路径，已删除。</p>
     */
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
        if (!(be instanceof DungeonPortalBlockEntity portalBE)) {
            return InteractionResult.PASS;
        }
        // 给玩家可见反馈，避免"点了没反应"再次成为困惑来源。
        serverPlayer.displayClientMessage(
                Component.translatable(portalBE.findKeyedLectern() == null
                        ? "block.piranport.dungeon_portal.waiting_for_activation"
                        : "block.piranport.dungeon_portal.walk_in_hint"), true);
        return InteractionResult.CONSUME;
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

    /**
     * 用方块模型渲染，而不是交给 BlockEntity 渲染器。
     *
     * <p>BaseEntityBlock 默认返回 {@link RenderShape#INVISIBLE}——那是在"BE 自带渲染器"
     * 的前提下的约定。本 BE 没有渲染器，不覆写的话区块渲染阶段会直接跳过方块模型，
     * 表现就是"只有碰撞箱、方块透明"。mod 里其他 BaseEntityBlock 都覆写了这一项。
     *
     * <p>客户端日志里的「'piranport:blockstates/dungeon_portal.json' missing model for variant」
     * 与这一项是同一个症状的两个成因，必须同时修：只补 blockstate/模型仍会因 INVISIBLE 被跳过。</p>
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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