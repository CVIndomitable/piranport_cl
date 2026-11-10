package com.piranport.dungeon.instance;

import com.piranport.PiranPort;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.data.CheckpointData;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
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


/**
 * 副本战场的可恢复分帧生成器。
 *
 * <p>每次 tick 最多写入 {@link #blocksPerTick()} 个方块（默认 8192，走调试终端
 * {@code global.terrain.blocks_per_tick} 可调）。基底按列深度写入海水和海床，
 * 特征、POI、边界分阶段处理；避免一次调用把 512×512 节点全部同步写完。</p>
 */
public final class TerrainGenerationPipeline {
    public static final int MAP_SIZE = DungeonConstants.MAP_USABLE_SIZE;
    public static final int MAX_DEPTH = 16;
    private static final int SEA = DungeonConstants.SEA_LEVEL;
    private static final int FLAGS = 2 | 16 | 64;

    private TerrainGenerationPipeline() {}

    /** 每 tick 写入上限；默认 8192，可由调试终端 {@code global.terrain.blocks_per_tick} 调整。 */
    public static int blocksPerTick() {
        return ModEquipmentConfig.TERRAIN_BLOCKS_PER_TICK.get();
    }

    /**
     * 取（或创建）某实例 / 某节点的生成游标。
     *
     * <p>WHY 缓存键串：本方法每 tick 会被调用近十次（{@link #tick}、{@link #isReady}、
     * {@link #queuedBlocks} 及进度播报），每次都要做字符串拼接 + {@link UUID#nameUUIDFromBytes}（MD5）。
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
        TerrainWorkBudget budget = TerrainWorkBudget.get(level);
        int granted = budget.reserve(level.getServer().getTickCount(), blocksPerTick());
        int used = advance(level, instance, node, granted);
        budget.release(granted - used);
        // 唯一的对外推进入口：在此统一播报后台建造进度（节流在 TerrainGenerationProgress 内）。
        TerrainGenerationProgress.report(level, instance, node);
        return isReady(level, instance, node);
    }

    /** 调度器提供额度；节点海床、特征和边界都完成后才放行。 */
    public static int advance(ServerLevel level, DungeonInstance instance, NodeData node, int budget) {
        if (budget <= 0) return 0;
        long seed = instance.getInstanceId().getMostSignificantBits() ^ instance.getInstanceId().getLeastSignificantBits();
        TerrainGenerationState local = nodeState(level, instance, node);
        int remaining = budget;
        while (remaining > 0) {
            if (local.phase() == TerrainGenerationState.Phase.READY) break;
            int used = switch (local.phase()) {
                case BASE -> processBase(level, instance, local, seed, remaining);
                case FEATURES -> processFeatures(level, local, remaining);
                case POI -> processPoi(level, instance, node, local, remaining);
                case BOUNDARY -> processBoundary(level, local, remaining);
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

    /** 当前持久化队列尚需写入的近似方块数，入口可用于调试或进度提示。 */
    public static long queuedBlocks(ServerLevel level, DungeonInstance instance, NodeData node) {
        TerrainGenerationState local = nodeState(level, instance, node);
        long localBoundary = boundaryBlockCount(level, DungeonConstants.NODE_AREA_SIZE);
        long features = featureCount(node.terrainType() == null ? TerrainType.T1_OCEAN : node.terrainType());
        long poi = 75 + 28 * checkpointCount(instance, node);
        return remainingBlocks(local.phase(), local.cursor(), features, poi, localBoundary);
    }

    static long remainingBlocks(TerrainGenerationState.Phase phase, long cursor, long features, long poi, long boundary) {
        return switch (phase) {
            case BASE -> Math.max(0, baseBlockCount() - cursor) + features + poi + boundary;
            case FEATURES -> Math.max(0, features - cursor) + poi + boundary;
            case POI -> Math.max(0, poi - cursor) + boundary;
            case BOUNDARY -> Math.max(0, boundary - cursor);
            default -> 0;
        };
    }

    /** 节点地形建造的粗略完成比例（0~1）：分母为全部未开始时的 {@link #queuedBlocks} 口径。 */
    public static double buildFraction(ServerLevel level, DungeonInstance instance, NodeData node) {
        TerrainType terrain = node.terrainType() == null ? TerrainType.T1_OCEAN : node.terrainType();
        long total = baseBlockCount() + featureCount(terrain) + 75L + 28L * checkpointCount(instance, node)
                + boundaryBlockCount(level, DungeonConstants.NODE_AREA_SIZE);
        if (total <= 0) return 1.0;
        double remaining = queuedBlocks(level, instance, node);
        return Math.max(0.0, Math.min(1.0, 1.0 - remaining / total));
    }

    static long baseBlockCount() {
        return (long) DungeonConstants.NODE_AREA_SIZE * DungeonConstants.NODE_AREA_SIZE * (MAX_DEPTH + 1);
    }

    private static int processBase(ServerLevel level, DungeonInstance instance, TerrainGenerationState state,
                                   long seed, int budget) {
        long total = baseBlockCount();
        int used = (int) Math.min(total - state.cursor(), budget);
        // P0-1：同一列（同一 x,z）的 MAX_DEPTH+1 层深度完全相同，depthAt 只需按列算一次。
        // 本方法分帧执行：游标可从任意 index 起步、跨多次调用，故缓存只在本次批量的连续 index 内
        // 按列号复用——换列即重算。结果与逐方块调用 depthAt(seed,x,z) 逐位一致（同一 seed + 坐标）。
        int cachedColumn = -1;
        int cachedDepth = 0;
        for (int i = 0; i < used; i++) {
            long index = state.cursor() + i;
            int layer = (int) (index % (MAX_DEPTH + 1));
            int column = (int) (index / (MAX_DEPTH + 1));
            int x = baseColumnX(state.startX(), column);
            int z = baseColumnZ(state.startZ(), column);
            if (column != cachedColumn) {
                cachedColumn = column;
                cachedDepth = depthAt(seed, x - instance.getUsableMinX(), z - instance.getUsableMinZ());
            }
            int y = SEA - MAX_DEPTH + layer;
            BlockState block = y <= SEA - cachedDepth ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState();
            level.setBlock(new BlockPos(x, y, z), block, FLAGS);
        }
        state.advance(used);
        if (state.cursor() >= total) state.nextPhase();
        return used;
    }

    static int baseColumnX(int centerX, int column) {
        int span = DungeonConstants.NODE_AREA_SIZE;
        return centerX - span / 2 + column % span;
    }

    static int baseColumnZ(int centerZ, int column) {
        int span = DungeonConstants.NODE_AREA_SIZE;
        return centerZ - span / 2 + column / span;
    }

    private static int processFeatures(ServerLevel level, TerrainGenerationState state, int budget) {
        TerrainType terrain = TerrainType.values()[state.terrainOrdinal()];
        long total = featureCount(terrain);
        if (total == 0) {
            state.nextPhase();
            return 0;
        }
        int used = (int) Math.min(total - state.cursor(), budget);
        for (int i = 0; i < used; i++) placeFeature(level, state, terrain, state.cursor() + i);
        state.advance(used);
        if (state.cursor() >= total) state.nextPhase();
        return used;
    }

    private static int processPoi(ServerLevel level, DungeonInstance instance, NodeData node,
                                  TerrainGenerationState state, int budget) {
        long total = 75L + 28 * checkpointCount(instance, node);
        int used = (int) Math.min(total - state.cursor(), budget);
        for (int i = 0; i < used; i++) {
            long index = state.cursor() + i;
            BlockPos spawn = instance.getNodeSpawnPos(node.nodeId());
            if (index < 75) {
                int cell = (int) (index / 3), layer = (int) (index % 3);
                int dx = cell % 5 - 2;
                int dz = cell / 5 - 2;
                level.setBlock(spawn.offset(dx, layer - 1, dz),
                        (layer == 0 ? Blocks.OAK_PLANKS : Blocks.AIR).defaultBlockState(), FLAGS);
            } else {
                int checkpointIndex = (int) ((index - 75) / 28);
                int localIndex = (int) ((index - 75) % 28);
                CheckpointData checkpoint = checkpoint(instance, node, checkpointIndex);
                if (checkpoint != null) {
                    Block block = BuiltInRegistries.BLOCK.getOptional(
                            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "dungeon_checkpoint"))
                            .orElse(Blocks.LODESTONE);
                    BlockPos pos = new BlockPos(spawn.getX() + checkpoint.posX(), checkpoint.posY(),
                            spawn.getZ() + checkpoint.posZ());
                    if (localIndex == 27) {
                        level.setBlock(pos, block.defaultBlockState(), FLAGS);
                    } else {
                        int cell = localIndex / 3, layer = localIndex % 3;
                        level.setBlock(pos.offset(cell % 3 - 1, layer - 1, cell / 3 - 1),
                                (layer == 0 ? Blocks.SMOOTH_STONE : Blocks.AIR).defaultBlockState(), FLAGS);
                    }
                }
            }
        }
        state.advance(used);
        if (state.cursor() >= total) state.nextPhase();
        return used;
    }

    /** 节点屏障从海床底部写到建筑高度上限并加盖，避免从上下方绕出。 */
    private static int processBoundary(ServerLevel level, TerrainGenerationState state, int budget) {
        // 节点局部状态：跨度 = 节点边长，坐标基准 = 出生中心回退半格。
        // 共享基底状态：跨度 = 整张地图，坐标基准本来就是角。
        boolean nodeLocal = state.nodeSpecific();
        int span = nodeLocal ? DungeonConstants.NODE_AREA_SIZE : MAP_SIZE;
        int originX = nodeLocal ? state.startX() - span / 2 : state.startX();
        int originZ = nodeLocal ? state.startZ() - span / 2 : state.startZ();
        int bottomY = barrierBottomY();
        int height = boundaryHeight(level);
        int topY = barrierTopY(level);
        long ring = (long) span * 4 * height;
        long total = ring + (long) span * span;
        int used = (int) Math.min(total - state.cursor(), budget);
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        for (int i = 0; i < used; i++) {
            long index = state.cursor() + i;
            if (index < ring) {
                int side = (int) (index / ((long) span * height));
                int rem = (int) (index % ((long) span * height));
                int offset = rem / height;
                int y = bottomY + rem % height;
                int x = originX + (side < 2 ? offset : side == 2 ? 0 : span - 1);
                int z = originZ + (side < 2 ? side == 0 ? 0 : span - 1 : offset);
                level.setBlock(new BlockPos(x, y, z), barrier, FLAGS);
            } else {
                // 加盖：顶端整层封死（含与围墙重叠的一圈，写屏障是幂等的），
                // 防止玩家从围墙正上方越顶。
                long cap = index - ring;
                int x = originX + (int) (cap % span);
                int z = originZ + (int) (cap / span);
                level.setBlock(new BlockPos(x, topY, z), barrier, FLAGS);
            }
        }
        state.advance(used);
        if (state.cursor() >= total) state.markReady();
        return used;
    }

    /** 屏障环顶端 = 建筑高度上限的最后一层（主世界 y=319，其上放不了方块也飞不上去）。 */
    private static int barrierTopY(ServerLevel level) {
        return level.getMaxBuildHeight() - 1;
    }

    /** 屏障环底端 = 共享基底写入的最低层，与水底齐平，不留水下缝隙。 */
    private static int barrierBottomY() {
        return SEA - MAX_DEPTH;
    }

    private static int boundaryHeight(ServerLevel level) {
        return barrierTopY(level) - barrierBottomY() + 1;
    }

    /** 环体（周长 × 高度）+ 顶盖（整层），供分帧游标与队列估算共用同一口径。 */
    private static long boundaryBlockCount(ServerLevel level, int span) {
        return (long) span * 4 * boundaryHeight(level) + (long) span * span;
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

    private static void placeFeature(ServerLevel level, TerrainGenerationState state, TerrainType terrain, long index) {
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
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), FLAGS);
    }

    private static long checkpointCount(DungeonInstance instance, NodeData node) {
        var stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        return stage == null ? 0 : stage.checkpoints().stream().filter(c -> node.nodeId().equals(c.nodeId())).count();
    }

    private static CheckpointData checkpoint(DungeonInstance instance, NodeData node, int index) {
        var stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        if (stage == null) return null;
        return stage.checkpoints().stream().filter(c -> node.nodeId().equals(c.nodeId())).skip(index).findFirst().orElse(null);
    }

}
