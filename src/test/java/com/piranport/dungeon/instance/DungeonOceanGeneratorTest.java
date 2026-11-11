package com.piranport.dungeon.instance;

import com.mojang.serialization.Lifecycle;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("mc-bootstrap")
class DungeonOceanGeneratorTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        Bootstrap.bootStrap();
    }

    @Test
    void generatesOceanAndCorrectHeightmapsAcrossChunkBoundary() {
        Biome biome = new Biome.BiomeBuilder().temperature(0.5f).downfall(0.5f)
                .specialEffects(new BiomeSpecialEffects.Builder().fogColor(0).waterColor(0)
                        .waterFogColor(0).skyColor(0).build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY).generationSettings(BiomeGenerationSettings.EMPTY).build();
        Registry<Biome> biomes = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        Registry.register(biomes, "plains", biome);
        var settings = new FlatLevelGeneratorSettings(Optional.empty(), Holder.direct(biome), List.of());
        var generator = new DungeonOceanGenerator(settings);
        LevelHeightAccessor height = LevelHeightAccessor.create(-64, 384);
        for (int chunkX : new int[]{-1, 0}) {
            var chunk = new ProtoChunk(new ChunkPos(chunkX, 2), UpgradeData.EMPTY, height, biomes, null);
            assertFalse(TerrainGenerationPipeline.generatedOceanColumn(chunk, new BlockPos.MutableBlockPos(), chunkX * 16, 32));
            generator.fillFromNoise(null, null, null, chunk).join();
            for (int x = chunkX * 16; x < chunkX * 16 + 16; x++) {
                for (int z = 32; z < 48; z++) {
                    int floor = DungeonOceanGenerator.seaFloor(x, z);
                    assertTrue(floor >= 51 && floor <= 57);
                    assertTrue(TerrainGenerationPipeline.generatedOceanColumn(chunk, new BlockPos.MutableBlockPos(), x, z));
                    assertEquals(Blocks.AIR.defaultBlockState(), chunk.getBlockState(new BlockPos(x, 46, z)));
                    assertEquals(Blocks.STONE.defaultBlockState(), chunk.getBlockState(new BlockPos(x, floor, z)));
                    assertEquals(Blocks.WATER.defaultBlockState(), chunk.getBlockState(new BlockPos(x, 63, z)));
                    assertEquals(floor + 1, generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, height, null));
                    assertEquals(64, generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, null));
                    var column = generator.getBaseColumn(x, z, height, null);
                    for (int y = 46; y <= 64; y++) {
                        assertEquals(chunk.getBlockState(new BlockPos(x, y, z)), column.getBlock(y));
                    }
                    assertEquals(floor, chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG).getFirstAvailable(x & 15, z & 15) - 1);
                    assertEquals(64, chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG).getFirstAvailable(x & 15, z & 15));
                }
            }
            int x = chunkX * 16;
            chunk.setBlockState(new BlockPos(x, 63, 32), Blocks.AIR.defaultBlockState(), false);
            assertFalse(TerrainGenerationPipeline.generatedOceanColumn(chunk, new BlockPos.MutableBlockPos(), x, 32));
        }
    }
}
