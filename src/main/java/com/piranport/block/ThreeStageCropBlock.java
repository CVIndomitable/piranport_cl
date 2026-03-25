package com.piranport.block;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.function.Supplier;

/** 3阶段作物方块 (age 0-2)。用于生菜、大蒜。 */
public class ThreeStageCropBlock extends CropBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 2);
    private final Supplier<? extends ItemLike> seedSupplier;

    public ThreeStageCropBlock(BlockBehaviour.Properties props, Supplier<? extends ItemLike> seedSupplier) {
        super(props);
        this.seedSupplier = seedSupplier;
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return 2;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seedSupplier.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,
            net.minecraft.world.level.block.state.BlockState> builder) {
        builder.add(AGE);
    }
}
