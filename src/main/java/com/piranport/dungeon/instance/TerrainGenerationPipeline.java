package com.piranport.dungeon.instance;

import com.piranport.PiranPort;
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
import java.util.UUID;


/**
 * 副本战场的可恢复分帧生成器。
 *
 * <p>每次 tick 最多写入 {@value #BLOCKS_PER_TICK} 个方块。基底按列深度写入海水和海床，
 * 特征、POI、边界分阶段处理；避免原先一次调用把 128×128 节点全部同步写完。</p>
 */
public final class TerrainGenerationPipeline {
    public static final int BLOCKS_PER_TICK = 8_192;
    public static final int MAP_SIZE = DungeonConstants.MAP_USABLE_SIZE;
    public static final int MAX_DEPTH = 16;
    private static final int SEA = DungeonConstants.SEA_LEVEL;
    private static final int FLAGS = 2 | 16 | 64;

    private TerrainGenerationPipeline() {}

    public static TerrainGenerationState state(ServerLevel level, DungeonInstance instance, NodeData node) {
        String key = "piranport_terrain_" + instance.getInstanceId().toString().replace('-', '_');
        if (node != null) {
            key += "_node_" + UUID.nameUUIDFromBytes(node.nodeId().getBytes(StandardCharsets.UTF_8))
                    .toString().replace('-', '_');
        }
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TerrainGenerationState::new, TerrainGenerationState::load, null), key);
    }

    /** 开始或继续生成；返回 true 仅表示本节点全部方块已准备好。 */
    public static boolean tick(ServerLevel level, DungeonInstance instance, NodeData node) {
        TerrainWorkBudget budget = TerrainWorkBudget.get(level);
        int granted = budget.reserve(level.getServer().getTickCount(), BLOCKS_PER_TICK);
        int used = advance(level, instance, node, granted);
        budget.release(granted - used);
        // 唯一的对外推进入口：在此统一播报后台建造进度（节流在 TerrainGenerationProgress 内）。
        TerrainGenerationProgress.report(level, instance, node);
        return isReady(level, instance, node);
    }

    /** 调度器提供额度；共享基底和该节点特征都完成后才放行。每个写入占一个额度。 */
    public static int advance(ServerLevel level, DungeonInstance instance, NodeData node, int budget) {
        if (budget <= 0) return 0;
        long seed = instance.getInstanceId().getMostSignificantBits() ^ instance.getInstanceId().getLeastSignificantBits();
        TerrainGenerationState base = state(level, instance, null);
        base.initialize(instance.getUsableMinX(), instance.getUsableMinZ(), seed, 0);
        TerrainGenerationState local = state(level, instance, node);
        BlockPos spawn = instance.getNodeSpawnPos(node.nodeId());
        TerrainType terrain = node.terrainType() == null ? TerrainType.T1_OCEAN : node.terrainType();
        local.initializeNode(spawn.getX(), spawn.getZ(), seed ^ node.nodeId().hashCode() * 31L, terrain.ordinal());
        int remaining = budget;
        while (remaining > 0) {
            TerrainGenerationState active = base.phase() == TerrainGenerationState.Phase.READY ? local : base;
            if (active.phase() == TerrainGenerationState.Phase.READY) break;
            int used = switch (active.phase()) {
                case BASE -> processBase(level, active, remaining);
                case FEATURES -> processFeatures(level, active, remaining);
                case POI -> processPoi(level, instance, node, active, remaining);
                case BOUNDARY -> processBoundary(level, active, remaining);
                case READY -> 0;
            };
            remaining -= used;
        }
        return budget - remaining;
    }

    public static boolean isReady(ServerLevel level, DungeonInstance instance, NodeData node) {
        return state(level, instance, null).phase() == TerrainGenerationState.Phase.READY
                && state(level, instance, node).phase() == TerrainGenerationState.Phase.READY;
    }

    /** 当前持久化队列尚需写入的近似方块数，入口可用于调试或进度提示。 */
    public static long queuedBlocks(ServerLevel level, DungeonInstance instance, NodeData node) {
        TerrainGenerationState base = state(level, instance, null);
        TerrainGenerationState local = state(level, instance, node);
        // 屏障环是整块地图规模和节点规模两套，各自按自己的跨度与高度计费，不能共用一份估算。
        long baseBoundary = boundaryBlockCount(level, MAP_SIZE);
        long localBoundary = boundaryBlockCount(level, DungeonConstants.NODE_AREA_SIZE);
        long shared = switch (base.phase()) {
            case BASE -> Math.max(0, (long) MAP_SIZE * MAP_SIZE * (MAX_DEPTH + 1) - base.cursor()) + baseBoundary;
            case BOUNDARY -> Math.max(0, baseBoundary - base.cursor());
            default -> 0;
        };
        long features = featureCount(node.terrainType() == null ? TerrainType.T1_OCEAN : node.terrainType());
        long poi = 75 + 28 * checkpointCount(instance, node);
        return shared + switch (local.phase()) {
            case BASE, FEATURES -> Math.max(0, features - local.cursor()) + poi + localBoundary;
            case POI -> Math.max(0, poi - local.cursor()) + localBoundary;
            case BOUNDARY -> Math.max(0, localBoundary - local.cursor());
            default -> 0;
        };
    }

    private static int processBase(ServerLevel level, TerrainGenerationState state, int budget) {
        long total = (long) MAP_SIZE * MAP_SIZE * (MAX_DEPTH + 1);
        int used = (int) Math.min(total - state.cursor(), budget);
        for (int i = 0; i < used; i++) {
            long index = state.cursor() + i;
            int layer = (int) (index % (MAX_DEPTH + 1));
            int column = (int) (index / (MAX_DEPTH + 1));
            int x = column % MAP_SIZE;
            int z = column / MAP_SIZE;
            int depth = depthAt(state.seed(), x, z);
            int y = SEA - MAX_DEPTH + layer;
            BlockState block = y <= SEA - depth ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState();
            level.setBlock(new BlockPos(state.startX() + x, y, state.startZ() + z), block, FLAGS);
        }
        state.advance(used);
        if (state.cursor() >= total) state.nextPhase();
        return used;
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

    /**
     * 写入边界的屏障环。
     *
     * <h2>两处独立缺陷（260924 审查 P0-5，对抗验证后确认）</h2>
     * <p><b>其一：坐标基准混淆。</b>{@code state.startX()/startZ()} 对共享基底是实例可用区的
     * <b>最小角</b>（{@link DungeonInstance#getUsableMinX()}），对节点局部状态却是<b>该节点的出生
     * 中心</b>（{@link TerrainGenerationState#initializeNode} 传入 spawn）。旧实现一律按"角"用，
     * 等于以节点中心为角向外扫环。</p>
     *
     * <p><b>其二：跨度对不上对象。</b>环边长写死 {@code MAP_SIZE}（512），但节点只占
     * {@code NODE_AREA_SIZE}（128）、中心间距也只有 128。512 是 128 的 4 倍——这个跨度从来
     * 没有对应的设计对象：节点是 4×4 网格排布的，512 的环一个格子根本放不下。</p>
     *
     * <p>只修第一条不够：环仍会横穿同列/同行邻居的出生点（只是从"跨 4 列"缩成"跨邻居"），
     * 玩家依旧被隐形墙围住。<b>两条必须一起修</b>——按状态类型选取跨度：节点用
     * {@code NODE_AREA_SIZE}，共享基底才用 {@code MAP_SIZE}。</p>
     *
     * <p>另注：玩家越界防护是<b>整实例级</b>的
     * （{@code DungeonEventHandler} 把玩家钳回 {@code getUsableMinX/MaxX} 的可用区），
     * 与节点级屏障环职责不同，二者不冲突。</p>
     *
     * <h2>高度：写满到建筑高度上限并加盖（260929）</h2>
     * <p>旧实现只写 {@code MAX_DEPTH + 7}（y=47..69）层——比海面高 6 格。玩家只要自己搭方块、
     * 用火箭鞘翅或任何升空手段越过 70 层，就等于走出了空气墙：屏障环变成一道可以绕过去的矮栅栏。
     * 现在竖直方向一路写到 {@code level.getMaxBuildHeight()} 的最后一层
     * （主世界即 y=319），并在顶端铺满一整层屏障"加盖"，与围墙顶部咬合，形成封闭笼子。</p>
     *
     * <p>代价是方块数：环体 = 周长 × 高度。共享基底 512×4×273 ≈ 56 万，节点 128×4×273 ≈ 14 万，
     * 另加顶盖（基底 512² ≈ 26 万、节点 128² ≈ 1.6 万）。仍按 8192/帧分帧写入，
     * {@link #queuedBlocks} 的估算同步改为按各自跨度计费，否则入口进度会低估到接近 0。</p>
     */
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
                x += local % 128 - 64; z += band * 40 - 20 + local / 128; y = SEA - 1;
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
