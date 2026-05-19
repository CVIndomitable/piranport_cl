package com.piranport.block;

import com.mojang.serialization.MapCodec;
import com.piranport.block.entity.SmokeScreenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Smoke screen block — opaque texture, transparent rendering, no collision,
 * indestructible (except creative), blocks mob line of sight,
 * auto-removes after 120 seconds (2400 ticks).
 */
public class SmokeScreenBlock extends BaseEntityBlock {

    /** Ticks before auto-removal: 120 seconds × 20 ticks = 2400 */
    private static final int LIFETIME_TICKS = 2400;

    public static final MapCodec<SmokeScreenBlock> CODEC = simpleCodec(SmokeScreenBlock::new);

    public SmokeScreenBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmokeScreenBlockEntity(pos, state);
    }

    /* ---- Shape: no collision, full-cube outline ---- */

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.block();
    }

    /* ---- Invisibility while inside smoke ---- */

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            // 40 ticks (2s), refreshed each tick → lasts until 2s after leaving smoke
            living.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
        }
    }

    /* ---- Rendering ---- */

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    /* ---- Indestructible (except creative) ---- */

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return player.getAbilities().instabuild ? 1.0f : 0.0f;
    }

    /* ---- Auto-removal via scheduled tick ---- */

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, LIFETIME_TICKS);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.removeBlock(pos, false);
        }
    }

    /* ---- BlockEntity cleanup ---- */

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                        BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
