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
        long boundary = (long) MAP_SIZE * 4 * (MAX_DEPTH + 7);
        long shared = switch (base.phase()) {
            case BASE -> Math.max(0, (long) MAP_SIZE * MAP_SIZE * (MAX_DEPTH + 1) - base.cursor()) + boundary;
            case BOUNDARY -> Math.max(0, boundary - base.cursor());
            default -> 0;
        };
        long features = featureCount(node.terrainType() == null ? TerrainType.T1_OCEAN : node.terrainType());
        long poi = 75 + 28 * checkpointCount(instance, node);
        return shared + switch (local.phase()) {
            case BASE, FEATURES -> Math.max(0, features - local.cursor()) + poi;
            case POI -> Math.max(0, poi - local.cursor());
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
     * 写入节点边界的屏障环。
     *
     * <p><b>坐标基准必须和 BASE 阶段区分开：</b>{@code state.startX()/startZ()} 对共享基底是
     * 实例可用区的<b>最小角</b>（{@link DungeonInstance#getUsableMinX()}），而对节点局部状态则是
     * <b>该节点的出生中心</b>（{@link TerrainGenerationState#initializeNode} 传入 spawn 坐标）。
     * 旧实现在这里一律按"角"处理，于是每个节点都会以自己的中心为角、向外扫出一个
     * {@code MAP_SIZE}（512）见方的屏障环——而节点中心间距只有 {@code NODE_AREA_SIZE}（128）。
     * 512 的跨度是 128 的 4 倍，导致 <b>任一新节点的屏障环都会横穿并封锁相邻节点的出生点</b>，
     * 玩家被关在隐形墙之间（"进了副本却走不出去/像被卡住"）。</p>
     *
     * <p>修法：按节点面积的一半把中心换算成该节点自己的角。节点专属状态（{@code nodeSpecific}）
     * 才做这层换算；共享基底本来就是角，保持不变。</p>
     */
    private static int processBoundary(ServerLevel level, TerrainGenerationState state, int budget) {
        int height = MAX_DEPTH + 7;
        long total = (long) MAP_SIZE * 4 * height;
        int used = (int) Math.min(total - state.cursor(), budget);
        // 节点局部状态的 startX/startZ 是出生中心，需回退半个节点边长才是该节点区域的角。
        int originX = state.nodeSpecific() ? state.startX() - DungeonConstants.NODE_AREA_SIZE / 2 : state.startX();
        int originZ = state.nodeSpecific() ? state.startZ() - DungeonConstants.NODE_AREA_SIZE / 2 : state.startZ();
        for (int i = 0; i < used; i++) {
            long index = state.cursor() + i;
            int side = (int) (index / (MAP_SIZE * height));
            int rem = (int) (index % (MAP_SIZE * height));
            int offset = rem / height;
            int y = SEA - MAX_DEPTH + rem % height;
            int x = originX + (side < 2 ? offset : side == 2 ? 0 : MAP_SIZE - 1);
            int z = originZ + (side < 2 ? side == 0 ? 0 : MAP_SIZE - 1 : offset);
            level.setBlock(new BlockPos(x, y, z), Blocks.BARRIER.defaultBlockState(), FLAGS);
        }
        state.advance(used);
        if (state.cursor() >= total) state.markReady();
        return used;
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
