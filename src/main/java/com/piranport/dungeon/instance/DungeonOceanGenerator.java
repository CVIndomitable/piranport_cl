package com.piranport.dungeon.instance;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.piranport.dungeon.DungeonConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.StructureManager;

import java.util.concurrent.CompletableFuture;

/** 空白维度中的海水和缓变海床由区块生成阶段一次写入。 */
public final class DungeonOceanGenerator extends FlatLevelSource {
    public static final MapCodec<DungeonOceanGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(DungeonOceanGenerator::settings)
    ).apply(instance, instance.stable(DungeonOceanGenerator::new)));
    private static final long OCEAN_SEED = 0x504952414e504f52L;

    public DungeonOceanGenerator(FlatLevelGeneratorSettings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<DungeonOceanGenerator> codec() {
        return CODEC;
    }

    static int seaFloor(int x, int z) {
        return DungeonConstants.SEA_LEVEL - TerrainGenerationPipeline.depthAt(OCEAN_SEED, x, z);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structures, ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap floor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int minY = DungeonConstants.SEA_LEVEL - TerrainGenerationPipeline.MAX_DEPTH;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int floorY = seaFloor(chunk.getPos().getMinBlockX() + x, chunk.getPos().getMinBlockZ() + z);
                for (int y = minY; y <= DungeonConstants.SEA_LEVEL; y++) {
                    BlockState block = (y <= floorY ? Blocks.STONE : Blocks.WATER).defaultBlockState();
                    chunk.setBlockState(pos.set(x, y, z), block, false);
                    floor.update(x, y, z, block);
                    surface.update(x, y, z, block);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return (type.isOpaque().test(Blocks.WATER.defaultBlockState()) ? DungeonConstants.SEA_LEVEL : seaFloor(x, z)) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        BlockState[] states = new BlockState[level.getHeight()];
        int floor = seaFloor(x, z);
        int minY = DungeonConstants.SEA_LEVEL - TerrainGenerationPipeline.MAX_DEPTH;
        for (int i = 0; i < states.length; i++) {
            int y = level.getMinBuildHeight() + i;
            states[i] = (y < minY || y > DungeonConstants.SEA_LEVEL ? Blocks.AIR
                    : y <= floor ? Blocks.STONE : Blocks.WATER).defaultBlockState();
        }
        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public int getSeaLevel() {
        return DungeonConstants.SEA_LEVEL;
    }

    @Override
    public int getMinY() {
        return -64;
    }
}
