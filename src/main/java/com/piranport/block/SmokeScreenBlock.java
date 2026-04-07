package com.piranport.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Smoke screen block — opaque texture, transparent rendering, no collision,
 * indestructible (except creative), blocks mob line of sight,
 * auto-removes after 120 seconds (2400 ticks).
 */
public class SmokeScreenBlock extends Block {

    /** Ticks before auto-removal: 120 seconds × 20 ticks = 2400 */
    private static final int LIFETIME_TICKS = 2400;

    public SmokeScreenBlock(Properties properties) {
        super(properties);
    }

    /* ---- Shape: no collision, full-cube outline ---- */

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    /**
     * Visual shape is used for line-of-sight raycasts (ClipContext.Block.VISUAL).
     * Return full cube so mobs cannot see through smoke.
     */
    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.block();
    }

    /* ---- Rendering ---- */

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
}
