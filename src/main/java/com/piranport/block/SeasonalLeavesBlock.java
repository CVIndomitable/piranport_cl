package com.piranport.block;

import com.piranport.config.ModCommonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class SeasonalLeavesBlock extends LeavesBlock {
    public static final EnumProperty<Season> SEASON = EnumProperty.create("season", Season.class);

    public SeasonalLeavesBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(SEASON, Season.SUMMER));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (!ModCommonConfig.SEASONAL_LEAF_COLOR_ENABLED.get()) {
            return;
        }

        BlockState current = level.getBlockState(pos);
        if (!current.is(this)) {
            return;
        }

        Season target = Season.fromDayTime(level.getDayTime());
        if (current.getValue(SEASON) != target) {
            level.setBlock(pos, current.setValue(SEASON, target), 3);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SEASON);
    }

    public enum Season implements StringRepresentable {
        SPRING("spring"),
        SUMMER("summer"),
        AUTUMN("autumn"),
        WINTER("winter");

        private static final long TICKS_PER_DAY = 24000L;
        private static final long DAYS_PER_SEASON = 90L;
        private final String serializedName;

        Season(String serializedName) {
            this.serializedName = serializedName;
        }

        public static Season fromDayTime(long dayTime) {
            long day = Math.floorDiv(Math.max(0L, dayTime), TICKS_PER_DAY);
            int index = (int) ((day / DAYS_PER_SEASON) % values().length);
            return values()[index];
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
