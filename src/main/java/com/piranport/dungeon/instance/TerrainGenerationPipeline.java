package com.piranport.dungeon.instance;

import com.piranport.PiranPort;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.data.CheckpointData;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.TerrainType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;


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
        // 地形属于整个 512×512 实例地图，而非 128×128 节点；同一实例的后续节点调用复用这份状态。
        String key = "piranport_terrain_" + instance.getInstanceId().toString().replace('-', '_');
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TerrainGenerationState::new, TerrainGenerationState::load, null), key);
    }

    /** 开始或继续生成；返回 true 仅表示本节点全部方块已准备好。 */
    public static boolean tick(ServerLevel level, DungeonInstance instance, NodeData node) {
        TerrainGenerationState state = state(level, instance, node);
        if (!state.initialized()) {
            BlockPos spawn = instance.getNodeSpawnPos(node.nodeId());
            TerrainType terrain = node.terrainType() == null ? TerrainType.T1_OCEAN : node.terrainType();
            long seed = instance.getInstanceId().getMostSignificantBits() ^ instance.getInstanceId().getLeastSignificantBits()
                    ^ node.nodeId().hashCode() * 31L;
            state.initialize(instance.getUsableMinX(), instance.getUsableMinZ(), seed, terrain.ordinal());
        }
        int budget = BLOCKS_PER_TICK;
        while (budget > 0 && state.phase() != TerrainGenerationState.Phase.READY) {
            int used = switch (state.phase()) {
                case BASE -> processBase(level, state, budget);
                case FEATURES -> processFeatures(level, state, budget);
                case POI -> processPoi(level, instance, node, state, budget);
                case BOUNDARY -> processBoundary(level, state, budget);
                case READY -> 0;
            };
            if (used <= 0) break;
            budget -= used;
        }
        return state.phase() == TerrainGenerationState.Phase.READY;
    }

    public static boolean isReady(ServerLevel level, DungeonInstance instance, NodeData node) {
        return state(level, instance, node).phase() == TerrainGenerationState.Phase.READY;
    }

    /** 当前持久化队列尚需写入的近似方块数，入口可用于调试或进度提示。 */
    public static long queuedBlocks(ServerLevel level, DungeonInstance instance, NodeData node) {
        TerrainGenerationState state = state(level, instance, node);
        return switch (state.phase()) {
            case BASE -> (long) MAP_SIZE * MAP_SIZE * (MAX_DEPTH + 1) - state.cursor();
            case FEATURES -> Math.max(0, featureCount(TerrainType.values()[state.terrainOrdinal()]) - state.cursor());
            case POI -> Math.max(0, 26 + checkpointCount(instance, node) - state.cursor());
            case BOUNDARY -> Math.max(0, (long) MAP_SIZE * 4 * (MAX_DEPTH + 7) - state.cursor());
            case READY -> 0;
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
            return 1;
        }
        int used = (int) Math.min(total - state.cursor(), budget);
        for (int i = 0; i < used; i++) placeFeature(level, state, terrain, state.cursor() + i);
        state.advance(used);
        if (state.cursor() >= total) state.nextPhase();
        return used;
    }

    private static int processPoi(ServerLevel level, DungeonInstance instance, NodeData node,
                                  TerrainGenerationState state, int budget) {
        long total = 26L + checkpointCount(instance, node);
        int used = (int) Math.min(total - state.cursor(), budget);
        for (int i = 0; i < used; i++) {
            long index = state.cursor() + i;
            BlockPos spawn = instance.getNodeSpawnPos(node.nodeId());
            if (index < 25) {
                int dx = (int) (index % 5) - 2;
                int dz = (int) (index / 5) - 2;
                level.setBlock(spawn.offset(dx, -1, dz), Blocks.OAK_PLANKS.defaultBlockState(), FLAGS);
                level.setBlock(spawn.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), FLAGS);
            } else {
                CheckpointData checkpoint = checkpoint(instance, node, (int) index - 26);
                if (checkpoint != null) {
                    Block block = BuiltInRegistries.BLOCK.getOptional(
                            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "dungeon_checkpoint"))
                            .orElse(Blocks.LODESTONE);
                    BlockPos pos = new BlockPos(spawn.getX() + checkpoint.posX(),
                            checkpoint.posY() > 0 ? checkpoint.posY() : SEA,
                            spawn.getZ() + checkpoint.posZ());
                    level.setBlock(pos, block.defaultBlockState(), FLAGS);
                }
            }
        }
        state.advance(used);
        if (state.cursor() >= total) state.nextPhase();
        return used;
    }

    private static int processBoundary(ServerLevel level, TerrainGenerationState state, int budget) {
        int height = MAX_DEPTH + 7;
        long total = (long) MAP_SIZE * 4 * height;
        int used = (int) Math.min(total - state.cursor(), budget);
        for (int i = 0; i < used; i++) {
            long index = state.cursor() + i;
            int side = (int) (index / (MAP_SIZE * height));
            int rem = (int) (index % (MAP_SIZE * height));
            int offset = rem / height;
            int y = SEA - MAX_DEPTH + rem % height;
            int x = state.startX() + (side < 2 ? offset : side == 2 ? 0 : MAP_SIZE - 1);
            int z = state.startZ() + (side < 2 ? side == 0 ? 0 : MAP_SIZE - 1 : offset);
            level.setBlock(new BlockPos(x, y, z), Blocks.BARRIER.defaultBlockState(), FLAGS);
        }
        state.advance(used);
        if (state.cursor() >= total) state.markReady();
        return used;
    }

    private static int depthAt(long seed, int x, int z) {
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
        int cx = state.startX() + MAP_SIZE / 2;
        int cz = state.startZ() + MAP_SIZE / 2;
        Block block = Blocks.STONE;
        int x = cx, z = cz, y = SEA - 1;
        switch (terrain) {
            case T2_ISLAND_REEFS -> {
                int island = (int) (index / 169), local = (int) (index % 169);
                int radius = 3 + (int) Math.floorMod(state.seed() + island, 4);
                int ox = (int) Math.floorMod(state.seed() / 7 + island * 37L, MAP_SIZE - 24) - MAP_SIZE / 2 + 12;
                int oz = (int) Math.floorMod(state.seed() / 11 + island * 53L, MAP_SIZE - 24) - MAP_SIZE / 2 + 12;
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
