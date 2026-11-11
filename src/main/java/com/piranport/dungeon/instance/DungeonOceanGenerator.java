package com.piranport.dungeon.instance;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.data.StageData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 海洋基底和固定节点屏障随区块生成。 */
public final class DungeonOceanGenerator extends FlatLevelSource {
    public static final MapCodec<DungeonOceanGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(DungeonOceanGenerator::settings)
    ).apply(instance, instance.stable(DungeonOceanGenerator::new)));
    private static final long OCEAN_SEED = 0x504952414e504f52L;
    private record Placement(BlockPos pos, BlockState state) {}
    private final Map<Long, List<Placement>> plans = new ConcurrentHashMap<>();
    private final Set<UUID> registeredInstances = new HashSet<>();

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

    /** 服务器线程发布不可变计划，生成线程只读取自己的区块。 */
    public synchronized void registerPlans(DungeonInstance instance, StageData stage) {
        if (registeredInstances.contains(instance.getInstanceId())) return;
        Map<Long, List<Placement>> pending = new HashMap<>();
        for (var node : stage.nodes().values()) {
            TerrainGenerationPipeline.planNode(instance, stage, node, (pos, state) -> {
                long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
                pending.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new Placement(pos.immutable(), state));
            });
        }
        pending.forEach((key, placements) -> plans.put(key, List.copyOf(placements)));
        registeredInstances.add(instance.getInstanceId());
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structures, ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap floor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int minY = DungeonConstants.SEA_LEVEL - TerrainGenerationPipeline.MAX_DEPTH;
        int topY = chunk.getMaxBuildHeight() - 1;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getMinBlockX() + x;
                int worldZ = chunk.getPos().getMinBlockZ() + z;
                int floorY = seaFloor(worldX, worldZ);
                boolean wall = isWall(worldX, worldZ);
                for (int y = minY; y <= DungeonConstants.SEA_LEVEL; y++) {
                    BlockState block = (wall ? Blocks.BARRIER : y <= floorY ? Blocks.STONE : Blocks.WATER).defaultBlockState();
                    chunk.setBlockState(pos.set(x, y, z), block, false);
                    floor.update(x, y, z, block);
                    surface.update(x, y, z, block);
                }
                BlockState barrier = Blocks.BARRIER.defaultBlockState();
                if (wall) {
                    for (int y = DungeonConstants.SEA_LEVEL + 1; y < topY; y++) {
                        chunk.setBlockState(pos.set(x, y, z), barrier, false);
                    }
                }
                chunk.setBlockState(pos.set(x, topY, z), barrier, false);
                floor.update(x, topY, z, barrier);
                surface.update(x, topY, z, barrier);
            }
        }
        for (Placement placement : plans.getOrDefault(chunk.getPos().toLong(), List.of())) {
            BlockPos worldPos = placement.pos();
            // 屏障在旧流程中最后写入，节点装饰不得覆盖它。
            if (worldPos.getY() >= minY && (worldPos.getY() == topY || isWall(worldPos.getX(), worldPos.getZ()))) continue;
            BlockState previous = chunk.setBlockState(worldPos, placement.state(), false);
            if (previous != null && previous.hasBlockEntity()) chunk.removeBlockEntity(worldPos);
            if (placement.state().hasBlockEntity()) {
                CompoundTag tag = new CompoundTag();
                tag.putInt("x", worldPos.getX());
                tag.putInt("y", worldPos.getY());
                tag.putInt("z", worldPos.getZ());
                tag.putString("id", "DUMMY");
                chunk.setBlockEntityNbt(tag);
            }
            floor.update(worldPos.getX() & 15, worldPos.getY(), worldPos.getZ() & 15, placement.state());
            surface.update(worldPos.getX() & 15, worldPos.getY(), worldPos.getZ() & 15, placement.state());
        }
        return CompletableFuture.completedFuture(chunk);
    }

    static boolean isWall(int x, int z) {
        int size = DungeonConstants.NODE_AREA_SIZE;
        int localX = Math.floorMod(x, size);
        int localZ = Math.floorMod(z, size);
        return localX == 0 || localX == size - 1 || localZ == 0 || localZ == size - 1;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        if (type.isOpaque().test(Blocks.BARRIER.defaultBlockState())) return level.getMaxBuildHeight();
        return (type.isOpaque().test(Blocks.WATER.defaultBlockState()) ? DungeonConstants.SEA_LEVEL : seaFloor(x, z)) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        BlockState[] states = new BlockState[level.getHeight()];
        int floor = seaFloor(x, z);
        int minY = DungeonConstants.SEA_LEVEL - TerrainGenerationPipeline.MAX_DEPTH;
        for (int i = 0; i < states.length; i++) {
            int y = level.getMinBuildHeight() + i;
            states[i] = (y == level.getMaxBuildHeight() - 1 || (isWall(x, z) && y >= minY) ? Blocks.BARRIER
                    : y < minY || y > DungeonConstants.SEA_LEVEL ? Blocks.AIR
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
