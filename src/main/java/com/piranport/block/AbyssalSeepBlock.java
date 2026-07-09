package com.piranport.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class AbyssalSeepBlock extends Block {
    public AbyssalSeepBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.55,
                    pos.getY() + 1.02,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.55,
                    0.0, 0.015, 0.0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP,
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.75,
                    pos.getY() + 1.05,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.75,
                    0.0, 0.03, 0.0);
        }
    }
}
