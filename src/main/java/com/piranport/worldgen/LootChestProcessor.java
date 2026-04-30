package com.piranport.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * Structure processor that injects a loot table reference into chest block entities
 * during structure generation. Configured via the processor list JSON with a
 * {@code loot_table} parameter.
 */
public class LootChestProcessor extends StructureProcessor {

    public static final MapCodec<LootChestProcessor> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("loot_table").forGetter(p -> p.lootTable)
    ).apply(inst, LootChestProcessor::new));

    public static final StructureProcessorType<LootChestProcessor> TYPE =
            () -> CODEC;

    private final ResourceLocation lootTable;

    public LootChestProcessor(ResourceLocation lootTable) {
        this.lootTable = lootTable;
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo process(
            LevelReader level,
            BlockPos offset,
            BlockPos pos,
            StructureTemplate.StructureBlockInfo blockInfo,
            StructureTemplate.StructureBlockInfo relativeBlockInfo,
            StructurePlaceSettings settings,
            @Nullable StructureTemplate template) {

        if (relativeBlockInfo.state().is(Blocks.CHEST)) {
            CompoundTag nbt = relativeBlockInfo.nbt() != null
                    ? relativeBlockInfo.nbt().copy()
                    : new CompoundTag();
            nbt.putString("LootTable", lootTable.toString());
            nbt.putLong("LootTableSeed", settings.getRandom(relativeBlockInfo.pos()).nextLong());
            return new StructureTemplate.StructureBlockInfo(
                    relativeBlockInfo.pos(), relativeBlockInfo.state(), nbt);
        }

        return relativeBlockInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return TYPE;
    }
}
