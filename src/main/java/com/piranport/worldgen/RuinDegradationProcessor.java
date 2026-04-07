package com.piranport.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * Randomly degrades structure blocks to simulate battle damage / ruins.
 * A configurable probability determines how many blocks are replaced with air or cobblestone.
 */
public class RuinDegradationProcessor extends StructureProcessor {

    public static final MapCodec<RuinDegradationProcessor> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.fieldOf("integrity").forGetter(p -> p.integrity)
    ).apply(inst, RuinDegradationProcessor::new));

    public static final StructureProcessorType<RuinDegradationProcessor> TYPE = () -> CODEC;

    private final float integrity;

    public RuinDegradationProcessor(float integrity) {
        this.integrity = integrity;
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos offset,
            BlockPos pos,
            StructureTemplate.StructureBlockInfo blockInfo,
            StructureTemplate.StructureBlockInfo relativeBlockInfo,
            StructurePlaceSettings settings) {

        RandomSource random = settings.getRandom(relativeBlockInfo.pos());

        // Don't degrade chests, spawner blocks, or air
        if (relativeBlockInfo.state().is(Blocks.CHEST)
                || relativeBlockInfo.state().isAir()
                || relativeBlockInfo.state().is(Blocks.SPAWNER)) {
            return relativeBlockInfo;
        }

        if (random.nextFloat() > integrity) {
            // 70% chance → air, 30% chance → cobblestone (rubble)
            if (random.nextFloat() < 0.7f) {
                return null; // Remove block
            } else {
                return new StructureTemplate.StructureBlockInfo(
                        relativeBlockInfo.pos(), Blocks.COBBLESTONE.defaultBlockState(), null);
            }
        }

        return relativeBlockInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return TYPE;
    }
}
