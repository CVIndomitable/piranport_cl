package com.piranport.dungeon.instance;

import com.mojang.serialization.Lifecycle;
import com.piranport.dungeon.data.CheckpointData;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.data.TerrainType;
import com.piranport.dungeon.data.TestNodes;
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
import java.util.Set;
import java.util.UUID;

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
        for (int chunkX : new int[]{1, 2}) {
            var chunk = new ProtoChunk(new ChunkPos(chunkX, 2), UpgradeData.EMPTY, height, biomes, null);
            generator.fillFromNoise(null, null, null, chunk).join();
            for (int x = chunkX * 16; x < chunkX * 16 + 16; x++) {
                for (int z = 32; z < 48; z++) {
                    int floor = DungeonOceanGenerator.seaFloor(x, z);
                    assertTrue(floor >= 51 && floor <= 57);
                    assertEquals(Blocks.AIR.defaultBlockState(), chunk.getBlockState(new BlockPos(x, 46, z)));
                    assertEquals(Blocks.STONE.defaultBlockState(), chunk.getBlockState(new BlockPos(x, floor, z)));
                    assertEquals(Blocks.WATER.defaultBlockState(), chunk.getBlockState(new BlockPos(x, 63, z)));
                    assertEquals(320, generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, height, null));
                    assertEquals(320, generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, null));
                    var column = generator.getBaseColumn(x, z, height, null);
                    for (int y = 46; y <= 64; y++) {
                        assertEquals(chunk.getBlockState(new BlockPos(x, y, z)), column.getBlock(y));
                    }
                    assertEquals(320, chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG).getFirstAvailable(x & 15, z & 15));
                    assertEquals(320, chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG).getFirstAvailable(x & 15, z & 15));
                }
            }
        }
        var edge = new ProtoChunk(new ChunkPos(-1, 2), UpgradeData.EMPTY, height, biomes, null);
        generator.fillFromNoise(null, null, null, edge).join();
        for (int y = 47; y <= 319; y++) {
            assertEquals(Blocks.BARRIER.defaultBlockState(), edge.getBlockState(new BlockPos(-1, y, 32)));
        }
        assertEquals(Blocks.WATER.defaultBlockState(), edge.getBlockState(new BlockPos(-2, 63, 32)));
        assertEquals(Blocks.BARRIER.defaultBlockState(), edge.getBlockState(new BlockPos(-2, 319, 32)));
    }

    @Test
    void generatesNodeFeaturesAndBlockEntityMarkersInTheirChunks() {
        Biome biome = new Biome.BiomeBuilder().temperature(0.5f).downfall(0.5f)
                .specialEffects(new BiomeSpecialEffects.Builder().fogColor(0).waterColor(0)
                        .waterFogColor(0).skyColor(0).build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY).generationSettings(BiomeGenerationSettings.EMPTY).build();
        Registry<Biome> biomes = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        Registry.register(biomes, "plains", biome);
        var generator = new DungeonOceanGenerator(new FlatLevelGeneratorSettings(Optional.empty(), Holder.direct(biome), List.of()));
        var node = TestNodes.plain("A", NodeData.NodeType.BATTLE, null, List.of(), List.of(), "", 0, 0,
                "", TerrainType.T3_WRECKAGE, Set.of(), null);
        var stage = new StageData("test", "chapter", "test", Map.of("A", node), List.of(), "A",
                List.of(), List.of(), List.of(new CheckpointData("cp", "A", 2, 64, 2, "north", "any")),
                Set.of(), null, Set.of(), StageData.VictoryObjectives.EMPTY);
        DungeonRegistry.INSTANCE.load(Map.of(), Map.of("test", stage), Map.of());
        try {
            var instance = new DungeonInstance(UUID.randomUUID(), "test", 0);
            generator.registerPlans(instance, stage);
            var height = LevelHeightAccessor.create(-64, 384);
            BlockPos spawn = instance.getNodeSpawnPos("A");
            var platform = new ProtoChunk(new ChunkPos(spawn), UpgradeData.EMPTY, height, biomes, null);
            generator.fillFromNoise(null, null, null, platform).join();
            assertEquals(Blocks.OAK_PLANKS.defaultBlockState(), platform.getBlockState(spawn.below()));
            assertEquals(Blocks.AIR.defaultBlockState(), platform.getBlockState(spawn));
            assertEquals(Blocks.LODESTONE.defaultBlockState(), platform.getBlockState(spawn.offset(2, 0, 2)));
            BlockPos playerSpawn = instance.getPlayerSpawnPos("A");
            var playerPlatform = new ProtoChunk(new ChunkPos(playerSpawn), UpgradeData.EMPTY, height, biomes, null);
            generator.fillFromNoise(null, null, null, playerPlatform).join();
            assertEquals(Blocks.OAK_PLANKS.defaultBlockState(), playerPlatform.getBlockState(playerSpawn.below()));
            assertEquals(Blocks.AIR.defaultBlockState(), playerPlatform.getBlockState(playerSpawn));

            BlockPos chest = new BlockPos(spawn.getX() - 256 + 7, 62, spawn.getZ() - 512 / 6);
            var wreck = new ProtoChunk(new ChunkPos(chest), UpgradeData.EMPTY, height, biomes, null);
            generator.fillFromNoise(null, null, null, wreck).join();
            assertEquals(Blocks.CHEST.defaultBlockState(), wreck.getBlockState(chest));
            assertNotNull(wreck.getBlockEntityNbt(chest));
            assertEquals("DUMMY", wreck.getBlockEntityNbt(chest).getString("id"));

            var restarted = new DungeonOceanGenerator(new FlatLevelGeneratorSettings(Optional.empty(), Holder.direct(biome), List.of()));
            restarted.registerPlans(instance, stage);
            var afterRestart = new ProtoChunk(new ChunkPos(chest), UpgradeData.EMPTY, height, biomes, null);
            restarted.fillFromNoise(null, null, null, afterRestart).join();
            assertEquals(wreck.getBlockState(chest), afterRestart.getBlockState(chest));
            assertEquals("DUMMY", afterRestart.getBlockEntityNbt(chest).getString("id"));
        } finally {
            DungeonRegistry.INSTANCE.load(Map.of(), Map.of(), Map.of());
        }
    }

    @Test
    void measuresOceanAndBarrierFillForOneNode() {
        Biome biome = new Biome.BiomeBuilder().temperature(0.5f).downfall(0.5f)
                .specialEffects(new BiomeSpecialEffects.Builder().fogColor(0).waterColor(0)
                        .waterFogColor(0).skyColor(0).build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY).generationSettings(BiomeGenerationSettings.EMPTY).build();
        Registry<Biome> biomes = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        Registry.register(biomes, "plains", biome);
        var generator = new DungeonOceanGenerator(new FlatLevelGeneratorSettings(Optional.empty(), Holder.direct(biome), List.of()));
        var height = LevelHeightAccessor.create(-64, 384);
        long start = System.nanoTime();
        for (int z = 64; z < 96; z++) {
            for (int x = 64; x < 96; x++) {
                var chunk = new ProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, height, biomes, null);
                generator.fillFromNoise(null, null, null, chunk).join();
            }
        }
        System.out.printf(java.util.Locale.ROOT, "1024 chunks noise fill only: %.3f seconds%n",
                (System.nanoTime() - start) / 1_000_000_000.0);
    }
}
