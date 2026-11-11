package com.piranport.dungeon.instance;

import com.piranport.PiranPort;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.data.CheckpointData;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.data.TerrainType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;


/**
 * 副本战场的可恢复分帧生成器。
 *
 * <p>每 tick 使用 {@link #blocksPerTick()} 额度（默认 8192）；每次请求区块计 1024。
 * 海洋、屏障、固定节点特征和 POI 都由区块生成器写入。</p>
 */
public final class TerrainGenerationPipeline {
    public static final int MAX_DEPTH = 16;
    private static final int SEA = DungeonConstants.SEA_LEVEL;

    private TerrainGenerationPipeline() {}

    /** 每 tick 的区块准备额度，沿用终端参数 {@code global.terrain.blocks_per_tick}。 */
    public static int blocksPerTick() {
        return ModEquipmentConfig.TERRAIN_BLOCKS_PER_TICK.get();
    }

    /**
     * 取（或创建）某实例 / 某节点的生成游标。
     *
     * <p>WHY 缓存键串：本方法每 tick 会被调用近十次（{@link #tick}、{@link #isReady}、
     * {@link #queuedChunks} 及进度播报），每次都要做字符串拼接 + {@link UUID#nameUUIDFromBytes}（MD5）。
     * 键串只由（实例 ID，节点 ID）决定、与存档内容无关，是纯函数，因此可以长期缓存，命中后零开销。</p>
     *
     * <p>WHY 只缓存键串、不缓存 {@link TerrainGenerationState} 句柄：SavedData 在维度卸载 / 重载后
     * 会换成新的对象，缓存句柄有「写进已作废对象、副本进度丢失」的串档风险。这里始终走
     * {@code getDataStorage().computeIfAbsent} 取当前有效句柄，只把可以安全复用的键串计算省掉。</p>
     */
    public static TerrainGenerationState state(ServerLevel level, DungeonInstance instance, NodeData node) {
        StateKey cacheKey = new StateKey(instance.getInstanceId(), node == null ? null : node.nodeId());
        String key = KEY_CACHE.get(cacheKey);
        if (key == null) {
            key = buildKey(instance, node);
            // 防御性上限：键串无状态、可无损重算，异常增长时直接清空即可，不会影响存档。
            if (KEY_CACHE.size() >= KEY_CACHE_LIMIT) KEY_CACHE.clear();
            KEY_CACHE.put(cacheKey, key);
        }
        return level.getDataStorage().computeIfAbsent(FACTORY, key);
    }

    private static String buildKey(DungeonInstance instance, NodeData node) {
        String key = "piranport_terrain_" + instance.getInstanceId().toString().replace('-', '_');
        if (node != null) {
            key += "_node_" + UUID.nameUUIDFromBytes(node.nodeId().getBytes(StandardCharsets.UTF_8))
                    .toString().replace('-', '_');
        }
        return key;
    }

    /** (实例 ID, 节点 ID) 缓存键；nodeId 为 null 表示共享基底。 */
    private record StateKey(UUID instanceId, String nodeId) {}

    /** SavedData 工厂无状态，提升为常量以免每次 state() 都新建一个对象。 */
    private static final SavedData.Factory<TerrainGenerationState> FACTORY =
            new SavedData.Factory<>(TerrainGenerationState::new, TerrainGenerationState::load, null);
    private static final int KEY_CACHE_LIMIT = 4096;
    private static final Map<StateKey, String> KEY_CACHE = new ConcurrentHashMap<>();

    /** 开始或继续生成；返回 true 仅表示本节点全部方块已准备好。 */
    public static boolean tick(ServerLevel level, DungeonInstance instance, NodeData node) {
        registerPlans(level, instance);
        // 从首次加载区块之前开始计墙钟，不能漏掉第一帧的同步生成开销。
        TerrainGenerationProgress.report(level, instance, node);
        TerrainWorkBudget budget = TerrainWorkBudget.get(level);
        int granted = budget.reserve(level.getServer().getTickCount(), blocksPerTick());
        int used = advance(level, instance, node, granted);
        budget.release(granted - used);
        // 阶段切换和完成仍在本 tick 内播报。
        TerrainGenerationProgress.report(level, instance, node);
        return isReady(level, instance, node);
    }

    /** 在请求任何节点区块前发布整个实例的计划，避免邻接区块提前生成时缺少地形。 */
    public static void registerPlans(ServerLevel level, DungeonInstance instance) {
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        if (stage != null && level.getChunkSource().getGenerator() instanceof DungeonOceanGenerator generator) {
            generator.registerPlans(instance, stage);
        }
    }

    /** 调度器提供额度；节点区块全部准备完成后才放行。 */
    public static int advance(ServerLevel level, DungeonInstance instance, NodeData node, int budget) {
        if (budget <= 0) return 0;
        TerrainGenerationState local = nodeState(level, instance, node);
        int remaining = budget;
        while (remaining > 0) {
            if (local.phase() == TerrainGenerationState.Phase.READY) break;
            int used = switch (local.phase()) {
                case BASE -> processBase(level, local, remaining);
                case FEATURES, POI -> { local.nextPhase(); yield 0; }
                case BOUNDARY -> { local.markReady(); yield 0; }
                case READY -> 0;
            };
            remaining -= used;
        }
        return budget - remaining;
    }

    public static boolean isReady(ServerLevel level, DungeonInstance instance, NodeData node) {
        return nodeState(level, instance, node).phase() == TerrainGenerationState.Phase.READY;
    }

    private static TerrainGenerationState nodeState(ServerLevel level, DungeonInstance instance, NodeData node) {
        TerrainGenerationState shared = state(level, instance, null);
        TerrainGenerationState local = state(level, instance, node);
        BlockPos center = instance.getNodeSpawnPos(node.nodeId());
        long seed = instance.getInstanceId().getMostSignificantBits() ^ instance.getInstanceId().getLeastSignificantBits();
        TerrainType terrain = node.terrainType() == null ? TerrainType.T1_OCEAN : node.terrainType();
        // 旧共享游标离开 BASE 即代表整图海床已铺完，即使当时仍在建边界。
        boolean sharedBaseComplete = shared.initialized() && shared.phase() != TerrainGenerationState.Phase.BASE;
        local.initializeNode(center.getX(), center.getZ(), seed ^ node.nodeId().hashCode() * 31L,
                terrain.ordinal(), sharedBaseComplete, instance.hasEnteredNode(node.nodeId()));
        return local;
    }

    /** 返回节点尚需准备的区块数。 */
    public static long queuedChunks(ServerLevel level, DungeonInstance instance, NodeData node) {
        TerrainGenerationState local = nodeState(level, instance, node);
        return local.phase() == TerrainGenerationState.Phase.BASE
                ? Math.max(0, nodeChunkCount() - local.cursor()) : 0;
    }

    /** 节点区块准备比例（0~1）。 */
    public static double buildFraction(ServerLevel level, DungeonInstance instance, NodeData node) {
        long total = nodeChunkCount();
        if (total <= 0) return 1.0;
        double remaining = queuedChunks(level, instance, node);
        return Math.max(0.0, Math.min(1.0, 1.0 - remaining / total));
    }

    public static String progressBar(double fraction) {
        int filled = (int) Math.floor(Math.max(0, Math.min(1, fraction)) * 10);
        return "[" + "|".repeat(filled) + ".".repeat(10 - filled) + "]";
    }

    static long nodeChunkCount() {
        long side = DungeonConstants.NODE_AREA_SIZE / 16;
        return side * side;
    }

    private static int processBase(ServerLevel level, TerrainGenerationState state, int budget) {
        long total = nodeChunkCount();
        int used = 0;
        long cursor = state.cursor();
        int chunksPerSide = DungeonConstants.NODE_AREA_SIZE / 16;
        int minChunkX = (state.startX() - DungeonConstants.NODE_AREA_SIZE / 2) >> 4;
        int minChunkZ = (state.startZ() - DungeonConstants.NODE_AREA_SIZE / 2) >> 4;
        while (cursor < total && used < budget) {
            int chunk = (int) cursor;
            int cost = Math.min(1024, budget - used);
            level.getChunk(minChunkX + chunk % chunksPerSide, minChunkZ + chunk / chunksPerSide);
            cursor++;
            used += cost;
        }
        state.advance(cursor - state.cursor());
        if (state.cursor() >= total) state.nextPhase();
        return used;
    }

    static void planNode(DungeonInstance instance, StageData stage, NodeData node,
                         BiConsumer<BlockPos, BlockState> add) {
        BlockPos spawn = instance.getNodeSpawnPos(node.nodeId());
        long seed = instance.getInstanceId().getMostSignificantBits() ^ instance.getInstanceId().getLeastSignificantBits();
        TerrainGenerationState state = new TerrainGenerationState();
        TerrainType terrain = node.terrainType() == null ? TerrainType.T1_OCEAN : node.terrainType();
        state.initialize(spawn.getX(), spawn.getZ(), seed ^ node.nodeId().hashCode() * 31L, terrain.ordinal());
        for (long index = 0; index < featureCount(terrain); index++) {
            placeFeature(add, state, terrain, index);
        }
        var checkpoints = stage.checkpoints().stream().filter(c -> node.nodeId().equals(c.nodeId())).toList();
        long total = 75L + 28L * checkpoints.size();
        for (long index = 0; index < total; index++) {
            if (index < 75) {
                int cell = (int) (index / 3), layer = (int) (index % 3);
                int dx = cell % 5 - 2;
                int dz = cell / 5 - 2;
                add.accept(spawn.offset(dx, layer - 1, dz),
                        (layer == 0 ? Blocks.OAK_PLANKS : Blocks.AIR).defaultBlockState());
            } else {
                int checkpointIndex = (int) ((index - 75) / 28);
                int localIndex = (int) ((index - 75) % 28);
                CheckpointData checkpoint = checkpoints.get(checkpointIndex);
                if (checkpoint != null) {
                    Block block = BuiltInRegistries.BLOCK.getOptional(
                            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "dungeon_checkpoint"))
                            .orElse(Blocks.LODESTONE);
                    BlockPos pos = new BlockPos(spawn.getX() + checkpoint.posX(), checkpoint.posY(),
                            spawn.getZ() + checkpoint.posZ());
                    if (localIndex == 27) {
                        add.accept(pos, block.defaultBlockState());
                    } else {
                        int cell = localIndex / 3, layer = localIndex % 3;
                        add.accept(pos.offset(cell % 3 - 1, layer - 1, cell / 3 - 1),
                                (layer == 0 ? Blocks.SMOOTH_STONE : Blocks.AIR).defaultBlockState());
                    }
                }
            }
        }
        // 战斗节点的玩家落点在中心西侧，不是中心的标记平台。
        BlockPos playerSpawn = instance.getPlayerSpawnPos(node.nodeId());
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos floor = new BlockPos(playerSpawn.getX() + dx, SEA, playerSpawn.getZ() + dz);
                add.accept(floor, Blocks.OAK_PLANKS.defaultBlockState());
                add.accept(floor.above(), Blocks.AIR.defaultBlockState());
            }
        }
        add.accept(playerSpawn.above(), Blocks.AIR.defaultBlockState());
    }

    static int depthAt(long seed, int x, int z) {
        int cellX = Math.floorDiv(x, 32), cellZ = Math.floorDiv(z, 32);
        double tx = Math.floorMod(x, 32) / 32.0, tz = Math.floorMod(z, 32) / 32.0;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        double north = noiseDepth(seed, cellX, cellZ) * (1 - tx) + noiseDepth(seed, cellX + 1, cellZ) * tx;
        double south = noiseDepth(seed, cellX, cellZ + 1) * (1 - tx) + noiseDepth(seed, cellX + 1, cellZ + 1) * tx;
        return (int) Math.round(north * (1 - tz) + south * tz);
    }

    private static int noiseDepth(long seed, int x, int z) {
        long n = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        n ^= n >>> 33;
        n *= 0xff51afd7ed558ccdL;
        n ^= n >>> 33;
        return 6 + (int) Math.floorMod(n, 7); // 6-12 格，足够潜艇航行和反潜玩法
    }

    private static long featureCount(TerrainType terrain) {
        return switch (terrain) {
            case T1_OCEAN -> 0;
            case T2_ISLAND_REEFS -> 3L * 169;
            case T3_WRECKAGE -> 2L * 256;
            case T4_ISLAND_CHAIN -> 2L * 64 * 64;
            case T5_FORTRESS_REEF -> 36L * 49 + 289;
            case T6_PORT_RUINS -> 5L * 64 + 25 * 2 + 49;
        };
    }

    private static void placeFeature(BiConsumer<BlockPos, BlockState> add, TerrainGenerationState state, TerrainType terrain, long index) {
        int cx = state.startX();
        int cz = state.startZ();
        Block block = Blocks.STONE;
        int x = cx, z = cz, y = SEA - 1;
        switch (terrain) {
            case T2_ISLAND_REEFS -> {
                int island = (int) (index / 169), local = (int) (index % 169);
                int radius = 3 + (int) Math.floorMod(state.seed() + island, 4);
                int ox = (int) Math.floorMod(state.seed() / 7 + island * 37L, DungeonConstants.NODE_AREA_SIZE - 24) - DungeonConstants.NODE_AREA_SIZE / 2 + 12;
                int oz = (int) Math.floorMod(state.seed() / 11 + island * 53L, DungeonConstants.NODE_AREA_SIZE - 24) - DungeonConstants.NODE_AREA_SIZE / 2 + 12;
                int dx = local % 13 - 6, dz0 = local / 13 - 6;
                if (dx * dx + dz0 * dz0 > radius * radius) return;
                x += ox + dx; z += oz + dz0; y = SEA;
                block = (local % 5 == 0) ? Blocks.SAND : Blocks.STONE;
            }
            case T3_WRECKAGE -> {
                int band = (int) (index / 256), local = (int) (index % 256);
                int areaSize = DungeonConstants.NODE_AREA_SIZE;
                x += local % areaSize - areaSize / 2;
                z += band * (areaSize / 3) - areaSize / 6 + local / areaSize;
                y = SEA - 1;
                block = local % 7 == 0 ? Blocks.CHEST : Blocks.OAK_PLANKS;
            }
            case T4_ISLAND_CHAIN -> {
                int chain = (int) (index / 4096), local = (int) (index % 4096);
                int i = local / 64, across = local % 64;
                x += (chain == 0 ? -32 : 32) + (int) (Math.sin(i * .3) * 5) + across % 7 - 3;
                z += i - 32; y = SEA; block = across % 9 == 0 ? Blocks.SAND : Blocks.STONE;
            }
            case T5_FORTRESS_REEF -> {
                if (index < 36L * 49) {
                    int ring = (int) (index / 49), local = (int) (index % 49);
                    double angle = ring * Math.PI / 18;
                    x += (int) (Math.cos(angle) * 34) + local % 7 - 3;
                    z += (int) (Math.sin(angle) * 34) + local / 7 - 3;
                } else {
                    int local = (int) (index - 36L * 49);
                    x += local % 17 - 8; z += local / 17 - 8;
                }
                y = SEA - 1; block = index % 11 == 0 ? Blocks.STONE_BRICKS : Blocks.STONE;
            }
            case T6_PORT_RUINS -> {
                int local = (int) index;
                x += local < 320 ? local % 5 - 2 : local % 14 - 7;
                z += local < 320 ? local / 5 - 32 : local / 14 + 16;
                y = SEA - 1; block = local % 13 == 0 ? Blocks.OAK_LOG : Blocks.OAK_PLANKS;
            }
            default -> { return; }
        }
        add.accept(new BlockPos(x, y, z), block.defaultBlockState());
    }

}
