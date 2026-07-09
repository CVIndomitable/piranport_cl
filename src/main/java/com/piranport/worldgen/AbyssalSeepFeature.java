package com.piranport.worldgen;

import com.piranport.PiranPort;
import com.piranport.registry.ModBlocks;
import com.mojang.serialization.Codec;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Creates a small overworld abyssal pocket without replacing the global overworld biome source.
 */
public class AbyssalSeepFeature extends Feature<NoneFeatureConfiguration> {
    private static final int PATCH_RADIUS_BLOCKS = 16;
    private static final ResourceKey<Biome> ABYSSAL_OCEAN = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "abyssal_ocean"));

    public AbyssalSeepFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        BlockPos floor = findSeaFloor(level, origin);
        if (floor == null || !level.ensureCanWrite(floor)) {
            return false;
        }

        level.setBlock(floor, ModBlocks.ABYSSAL_SEEP.get().defaultBlockState(), 2);
        patchBiomePocket(level, floor, PATCH_RADIUS_BLOCKS);
        return true;
    }

    private static BlockPos findSeaFloor(WorldGenLevel level, BlockPos origin) {
        int x = origin.getX();
        int z = origin.getZ();
        int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        int minY = level.getMinBuildHeight() + 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = Math.min(top, level.getMaxBuildHeight() - 2); y >= minY; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir()
                    && !state.getFluidState().is(FluidTags.WATER)
                    && level.getFluidState(cursor.above()).is(FluidTags.WATER)) {
                return cursor.immutable();
            }
        }
        return null;
    }

    private static void patchBiomePocket(WorldGenLevel level, BlockPos center, int radiusBlocks) {
        Holder<Biome> abyssal = level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getHolder(ABYSSAL_OCEAN)
                .orElse(null);
        if (abyssal == null) {
            return;
        }

        int minChunkX = Math.floorDiv(center.getX() - radiusBlocks, 16);
        int maxChunkX = Math.floorDiv(center.getX() + radiusBlocks, 16);
        int minChunkZ = Math.floorDiv(center.getZ() - radiusBlocks, 16);
        int maxChunkZ = Math.floorDiv(center.getZ() + radiusBlocks, 16);
        int radiusSq = radiusBlocks * radiusBlocks;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.BIOMES, false);
                if (chunk == null) {
                    continue;
                }
                patchChunkBiomes(level, chunk, center, radiusSq, abyssal);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void patchChunkBiomes(WorldGenLevel level, ChunkAccess chunk, BlockPos center,
                                         int radiusSq, Holder<Biome> abyssal) {
        ChunkPos chunkPos = chunk.getPos();
        int qBaseX = QuartPos.fromBlock(chunkPos.getMinBlockX());
        int qBaseZ = QuartPos.fromBlock(chunkPos.getMinBlockZ());
        int qMinY = QuartPos.fromBlock(level.getMinBuildHeight());
        int qHeight = QuartPos.fromBlock(level.getHeight());
        Holder<Biome>[] original = new Holder[4 * qHeight * 4];

        for (int y = 0; y < qHeight; y++) {
            int qy = qMinY + y;
            for (int z = 0; z < 4; z++) {
                int qz = qBaseZ + z;
                for (int x = 0; x < 4; x++) {
                    int qx = qBaseX + x;
                    original[index(x, y, z)] = chunk.getNoiseBiome(qx, qy, qz);
                }
            }
        }

        BiomeResolver resolver = (qx, qy, qz, sampler) -> {
            if (isInsidePocket(qx, qz, center, radiusSq)) {
                return abyssal;
            }
            int x = qx - qBaseX;
            int y = qy - qMinY;
            int z = qz - qBaseZ;
            if (x >= 0 && x < 4 && y >= 0 && y < qHeight && z >= 0 && z < 4) {
                Holder<Biome> old = original[index(x, y, z)];
                if (old != null) {
                    return old;
                }
            }
            return chunk.getNoiseBiome(qx, qy, qz);
        };

        chunk.fillBiomesFromNoise(resolver, Climate.empty());
        chunk.setUnsaved(true);
        Arrays.fill(original, null);
    }

    private static boolean isInsidePocket(int qx, int qz, BlockPos center, int radiusSq) {
        int blockX = QuartPos.toBlock(qx) + 2;
        int blockZ = QuartPos.toBlock(qz) + 2;
        int dx = blockX - center.getX();
        int dz = blockZ - center.getZ();
        return dx * dx + dz * dz <= radiusSq;
    }

    private static int index(int x, int y, int z) {
        return (y * 4 + z) * 4 + x;
    }
}
