package com.piranport.dungeon.instance;

import com.piranport.PiranPort;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.EnemySetData;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.TerrainType;
import com.piranport.npc.ai.FleetGroup;
import com.piranport.npc.ai.FleetGroupManager;
import com.piranport.npc.ai.FleetGroup.FormationType;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Generates a flat ocean battlefield for a dungeon node and spawns enemies.
 */
public final class NodeBattleField {
    private NodeBattleField() {}

    /**
     * Generates the battlefield terrain at the node's position
     * within the instance region, based on the node's terrain type.
     *
     * <p>技术指南 05：从平坦海洋基底升级为六种地形变体。</p>
     */
    public static void generateTerrain(ServerLevel dungeonLevel, DungeonInstance instance,
                                        NodeData node) {
        // 生成由持久化管线分帧处理；调用方应轮询 isTerrainReady 后再传送/刷怪。
        TerrainGenerationPipeline.tick(dungeonLevel, instance, node);
    }

    /** 每 tick 推进一个节点的地形写入，供副本入口或服务器 tick 调度。 */
    public static boolean tickTerrain(ServerLevel dungeonLevel, DungeonInstance instance, NodeData node) {
        return TerrainGenerationPipeline.tick(dungeonLevel, instance, node);
    }

    /** 地形已完成基底、特征、POI 和硬边界时才允许刷怪或传送。 */
    public static boolean isTerrainReady(ServerLevel dungeonLevel, DungeonInstance instance, NodeData node) {
        return TerrainGenerationPipeline.isReady(dungeonLevel, instance, node);
    }

    /** 供入口 UI/调度器显示剩余地形队列工作量。 */
    public static long queuedTerrainBlocks(ServerLevel dungeonLevel, DungeonInstance instance, NodeData node) {
        return TerrainGenerationPipeline.queuedBlocks(dungeonLevel, instance, node);
    }

    // ===== 技术指南 05：六种地形特征生成器 =====

    /** T2 岛礁群：散布小岛礁（直径 5-15 格），密度参数化。 */
    private static void generateIslandReefs(ServerLevel level, int startX, int startZ, int flags) {
        var rng = level.getRandom();
        int count = 3 + rng.nextInt(3); // 每 100×100 区域 3-5 座
        int areaSize = DungeonConstants.NODE_AREA_SIZE;
        for (int i = 0; i < count; i++) {
            int cx = startX + rng.nextInt(areaSize);
            int cz = startZ + rng.nextInt(areaSize);
            int radius = 3 + rng.nextInt(4); // 3-6 格半径
            placeCircleIsland(level, cx, cz, radius, 1 + rng.nextInt(3), flags);
        }
    }

    /** T3 残骸带：沉船结构横贯战场。 */
    private static void generateWreckageBand(ServerLevel level, int startX, int startZ, int flags) {
        var rng = level.getRandom();
        int areaSize = DungeonConstants.NODE_AREA_SIZE;
        // 1-2 条残骸带
        int bands = 1 + rng.nextInt(2);
        for (int b = 0; b < bands; b++) {
            int bandZ = startZ + areaSize / 4 + b * areaSize / 3;
            int bandX = startX + areaSize / 4 + rng.nextInt(areaSize / 2);
            int length = 15 + rng.nextInt(20); // 带宽 15-35
            for (int i = 0; i < length; i++) {
                int wx = bandX + i - length / 2;
                int wz = bandZ + rng.nextInt(5) - 2;
                // 沉船结构：木板上铺木板+箱子
                level.setBlock(new BlockPos(wx, DungeonConstants.SEA_LEVEL - 1, wz),
                        Blocks.DARK_OAK_PLANKS.defaultBlockState(), flags);
                level.setBlock(new BlockPos(wx, DungeonConstants.SEA_LEVEL, wz),
                        Blocks.OAK_PLANKS.defaultBlockState(), flags);
                if (rng.nextFloat() < 0.2f) {
                    level.setBlock(new BlockPos(wx, DungeonConstants.SEA_LEVEL, wz),
                            Blocks.CHEST.defaultBlockState(), flags);
                }
            }
        }
    }

    /** T4 岛链湾：岛屿链围出弯曲航道。 */
    private static void generateIslandChain(ServerLevel level, int startX, int startZ, int flags) {
        var rng = level.getRandom();
        int areaSize = DungeonConstants.NODE_AREA_SIZE;
        // 两条岛链围出中间航道
        for (int chain = 0; chain < 2; chain++) {
            int chainX = startX + areaSize / 4 + chain * areaSize / 2;
            int chainZ = startZ + areaSize / 4;
            int chainLen = areaSize / 2;
            for (int i = 0; i < chainLen; i++) {
                int iz = chainZ + i;
                int ix = chainX + (int) (Math.sin(i * 0.3) * 5);
                int radius = 3 + rng.nextInt(3);
                placeCircleIsland(level, ix, iz, radius, 1 + rng.nextInt(2), flags);
            }
        }
    }

    /** T5 要塞环礁：中心环形礁盘 + 要塞结构。 */
    private static void generateFortressReef(ServerLevel level, BlockPos center,
                                              int startX, int startZ, int flags) {
        var rng = level.getRandom();
        int areaSize = DungeonConstants.NODE_AREA_SIZE;
        int midX = startX + areaSize / 2;
        int midZ = startZ + areaSize / 2;
        // 环形礁盘
        for (int angle = 0; angle < 360; angle += 10) {
            double rad = Math.toRadians(angle);
            int rx = midX + (int) (Math.cos(rad) * 20);
            int rz = midZ + (int) (Math.sin(rad) * 20);
            placeCircleIsland(level, rx, rz, 2 + rng.nextInt(2), 1, flags);
        }
        // 中心要塞平台
        int fortSize = 8;
        for (int dx = -fortSize; dx <= fortSize; dx++) {
            for (int dz = -fortSize; dz <= fortSize; dz++) {
                if (dx * dx + dz * dz <= fortSize * fortSize) {
                    BlockPos pos = new BlockPos(midX + dx, DungeonConstants.SEA_LEVEL - 1, midZ + dz);
                    level.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), flags);
                }
            }
        }
        // 要塞中央方块
        level.setBlock(new BlockPos(midX, DungeonConstants.SEA_LEVEL, midZ),
                Blocks.OBSIDIAN.defaultBlockState(), flags);
    }

    /** T6 港口遗迹：岸式结构（复用原版方块模拟码头）。 */
    private static void generatePortRuins(ServerLevel level, BlockPos center,
                                            int startX, int startZ, int flags) {
        var rng = level.getRandom();
        int areaSize = DungeonConstants.NODE_AREA_SIZE;
        // 码头平台（一侧延伸）
        int pierX = startX + areaSize / 2;
        int pierZ = startZ + 2;
        for (int i = 0; i < 15; i++) {
            for (int dx = -2; dx <= 2; dx++) {
                BlockPos pos = new BlockPos(pierX + dx, DungeonConstants.SEA_LEVEL - 1, pierZ + i);
                level.setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), flags);
            }
            // 两侧柱子
            if (i % 4 == 0) {
                for (int dx : new int[]{-3, 3}) {
                    BlockPos pillar = new BlockPos(pierX + dx, DungeonConstants.SEA_LEVEL - 1, pierZ + i);
                    level.setBlock(pillar, Blocks.OAK_LOG.defaultBlockState(), flags);
                }
            }
        }
        // 仓库结构
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos pos = new BlockPos(pierX + dx, DungeonConstants.SEA_LEVEL, pierZ + 16 + dz);
                if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
                    level.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), flags);
                } else {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
                }
            }
        }
    }

    /** 辅助：放置圆形岛礁（水面以上 1 格沙土 + 周围石头）。 */
    private static void placeCircleIsland(ServerLevel level, int cx, int cz, int radius,
                                          int heightAbove, int flags) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius) {
                    BlockPos base = new BlockPos(cx + dx, DungeonConstants.SEA_LEVEL - 1, cz + dz);
                    level.setBlock(base, Blocks.STONE.defaultBlockState(), flags);
                    // 水面以上沙土
                    for (int h = 0; h < heightAbove; h++) {
                        BlockPos above = base.above(h);
                        level.setBlock(above, Blocks.SAND.defaultBlockState(), flags);
                    }
                }
            }
        }
    }

    /** 辅助：出生点平台。 */
    private static void placeSpawnPlatform(ServerLevel level, BlockPos spawn, int flags) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos platPos = spawn.offset(dx, -1, dz);
                level.setBlock(platPos, Blocks.OAK_PLANKS.defaultBlockState(), flags);
                BlockPos abovePos = spawn.offset(dx, 0, dz);
                level.setBlock(abovePos, Blocks.AIR.defaultBlockState(), flags);
            }
        }
    }

    // ===== 敌人生成 =====

    /**
     * Spawns enemies for a battle/boss node. Returns the list of spawned entities.
     * If the configured entity types are missing or no enemies got spawned, immediately
     * spawns a completion portal so players are never stranded on an empty node.
     */
    public static List<Entity> spawnEnemies(ServerLevel dungeonLevel, DungeonInstance instance,
                                             NodeData node) {
        List<Entity> spawned = new ArrayList<>();
        if (node.enemies() == null) {
            spawnCompletionPortal(dungeonLevel, instance, node.nodeId());
            return spawned;
        }

        EnemySetData enemySet = DungeonRegistry.INSTANCE.getEnemySet(node.enemies());
        if (enemySet == null) {
            PiranPort.LOGGER.warn("Enemy set not found: {} — spawning completion portal", node.enemies());
            spawnCompletionPortal(dungeonLevel, instance, node.nodeId());
            return spawned;
        }

        BlockPos center = instance.getNodeSpawnPos(node.nodeId());
        int spawnRadius = 30;

        // 编队生成：收集实体 -> 排序（大前小后） -> 分配队形 -> 生成
        List<SpawnEntry> allEntries = new ArrayList<>();
        for (EnemySetData.SpawnEntry entry : enemySet.spawnList()) {
            allEntries.add(new SpawnEntry(entry.entity(), entry.count(), false));
        }
        if (enemySet.flagship() != null) {
            allEntries.add(new SpawnEntry(enemySet.flagship().entity(), enemySet.flagship().count(), true));
        }

        // 区分潜艇与非潜艇（潜艇单独成队，不参与水面舰队形）
        List<SpawnEntry> surfaceEntries = new ArrayList<>();
        List<SpawnEntry> subEntries = new ArrayList<>();
        for (SpawnEntry e : allEntries) {
            if (e.entityId.contains("submarine")) {
                subEntries.add(e);
            } else {
                surfaceEntries.add(e);
            }
        }

        // 解析队形
        FormationType formation = parseFormation(enemySet.formation());

        // 生成水面舰编队
        if (!surfaceEntries.isEmpty()) {
            List<Entity> surfaceFleet = spawnFleet(dungeonLevel, instance, node, center, spawnRadius, surfaceEntries, formation, true);
            spawned.addAll(surfaceFleet);
        }

        // 生成潜艇编队（潜艇独立行动，不参与队形）
        if (!subEntries.isEmpty()) {
            List<Entity> subFleet = spawnFleet(dungeonLevel, instance, node, center, spawnRadius, subEntries, FormationType.SINGLE_LINE, false);
            spawned.addAll(subFleet);
        }

        // If nothing could be created, spawn a completion portal as a recovery mechanism.
        if (spawned.isEmpty()) {
            PiranPort.LOGGER.warn("Node '{}' produced 0 entities — spawning recovery portal", node.nodeId());
            spawnCompletionPortal(dungeonLevel, instance, node.nodeId());
        }

        // 决策/副本/07：BOSS 节点注册防卡 tick 调度器
        if (node.type() == NodeData.NodeType.BOSS && enemySet.flagship() != null) {
            com.piranport.dungeon.BossAntiStuckScheduler.register(instance, node, spawned);
        }

        com.piranport.dungeon.saved.DungeonObjectiveData.get(dungeonLevel)
                .register(instance.getInstanceId(), node.nodeId(), spawned);
        return spawned;
    }

    /** 按策划规则排序后生成编队 */
    private static List<Entity> spawnFleet(ServerLevel dungeonLevel, DungeonInstance instance, NodeData node,
                                            BlockPos center, int spawnRadius,
                                            List<SpawnEntry> entries, FormationType formation, boolean sortBySize) {
        List<Entity> fleet = new ArrayList<>();

        // 展开为个体列表
        List<EntitySpec> specs = new ArrayList<>();
        for (SpawnEntry e : entries) {
            for (int i = 0; i < e.count(); i++) {
                specs.add(new EntitySpec(e.entityId, e.isFlagship));
            }
        }

        // 排序：大船在前、同舰种高级在前
        if (sortBySize && specs.size() > 1) {
            specs.sort(Comparator.comparingInt(EntitySpec::weight).reversed());
        }

        if (specs.isEmpty()) return fleet;

        // 创建 FleetGroup
        UUID groupId = UUID.randomUUID();
        FleetGroupManager mgr = FleetGroupManager.get(dungeonLevel);
        FleetGroup group = mgr.createGroup(groupId);
        group.setFormation(formation);

        // 逐个生成
        double angleStep = (2.0 * Math.PI) / specs.size();
        double baseAngle = dungeonLevel.getRandom().nextDouble() * Math.PI * 2;
        double baseDist = spawnRadius;

        for (int i = 0; i < specs.size(); i++) {
            EntitySpec spec = specs.get(i);
            Entity entity = createEntity(dungeonLevel, spec.entityId);
            if (entity == null) continue;

            // 初始位置：在编队队形中均匀分布（后续由 FollowLeaderGoal 微调）
            double angle = baseAngle + angleStep * i;
            double dist = baseDist;
            double ex = center.getX() + Math.cos(angle) * dist;
            double ez = center.getZ() + Math.sin(angle) * dist;
            entity.setPos(ex, DungeonConstants.SPAWN_Y, ez);

            if (entity instanceof AbstractDeepOceanEntity abyssal) {
                abyssal.setFleetGroupId(groupId);
                mgr.addMember(groupId, abyssal.getUUID());
                if (i == 0) {
                    group.setLeaderUuid(abyssal.getUUID());
                }
                if (spec.isFlagship) {
                    entity.addTag("dungeon_flagship");
                }
            }

            if (entity instanceof net.minecraft.world.entity.Mob mob) {
                net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(mob, dungeonLevel,
                        dungeonLevel.getCurrentDifficultyAt(blockPos(ex, ez)),
                        net.minecraft.world.entity.MobSpawnType.STRUCTURE, null);
                mob.setPersistenceRequired();
            }

            entity.addTag("dungeon_instance_" + instance.getInstanceId());
            entity.addTag("dungeon_node_" + node.nodeId());
            dungeonLevel.addFreshEntity(entity);
            fleet.add(entity);
        }

        return fleet;
    }

    private static BlockPos blockPos(double x, double z) {
        return new BlockPos((int) Math.round(x), DungeonConstants.SPAWN_Y, (int) Math.round(z));
    }

    private static FormationType parseFormation(String formationStr) {
        if (formationStr == null || formationStr.isEmpty()) return FormationType.SINGLE_LINE;
        return switch (formationStr.toLowerCase()) {
            case "double_line", "复纵阵", "double" -> FormationType.DOUBLE_LINE;
            case "wheel", "轮型阵" -> FormationType.WHEEL;
            case "single_horizontal", "单横阵", "horizontal" -> FormationType.SINGLE_HORIZONTAL;
            default -> FormationType.SINGLE_LINE;
        };
    }

    private static void spawnCompletionPortal(ServerLevel dungeonLevel, DungeonInstance instance,
                                                String nodeId) {
        BlockPos spawn = instance.getNodeSpawnPos(nodeId);
        com.piranport.dungeon.block.PortalStructureHelper.buildPortalStructure(
                dungeonLevel, spawn.below(), instance.getInstanceId(), nodeId);
    }

    /**
     * Returns null when the entity type is not registered. Callers must treat null
     * as "skip" rather than substituting an untagged fallback entity, since that
     * would never trigger node completion handlers.
     */
    public static Entity createEntity(ServerLevel level, String entityId) {
        ResourceLocation rl = ResourceLocation.tryParse(entityId);
        if (rl == null) {
            PiranPort.LOGGER.warn("Invalid entity ID: {}", entityId);
            return null;
        }
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(rl);
        if (type.isEmpty()) {
            PiranPort.LOGGER.warn("Entity type not found: {}, skipping (no fallback)", entityId);
            return null;
        }
        return type.get().create(level);
    }

    /** 舰种权重：数值越大越靠前（大船在前）。 */
    private static int entityWeight(String entityId) {
        return switch (entityId) {
            case "piranport:deep_ocean_flagship" -> 100;
            case "piranport:deep_ocean_battleship" -> 90;
            case "piranport:deep_ocean_carrier" -> 85;
            case "piranport:deep_ocean_battle_cruiser" -> 80;
            case "piranport:deep_ocean_light_carrier" -> 70;
            case "piranport:deep_ocean_heavy_cruiser" -> 65;
            case "piranport:deep_ocean_guided_destroyer",
                 "piranport:deep_ocean_air_destroyer" -> 55;
            case "piranport:deep_ocean_light_cruiser" -> 50;
            case "piranport:deep_ocean_destroyer" -> 40;
            case "piranport:deep_ocean_supply" -> 30;
            case "piranport:deep_ocean_submarine" -> 20;
            default -> 10;
        };
    }

    private record SpawnEntry(String entityId, int count, boolean isFlagship) {}
    private record EntitySpec(String entityId, boolean isFlagship) {
        int weight() { return entityWeight(entityId); }
    }

    /**
     * Clears all blocks and entities in an instance's region.
     */
    public static void cleanupRegion(ServerLevel dungeonLevel, DungeonInstance instance) {
        int originX = instance.getRegionOriginX();
        int originZ = instance.getRegionOriginZ();
        int size = DungeonConstants.REGION_SIZE;

        net.minecraft.world.phys.AABB regionBox = new net.minecraft.world.phys.AABB(
                originX, -64, originZ, originX + size, 320, originZ + size);

        // P1修复: 清理所有副本专属实体类型，防止实体泄漏
        dungeonLevel.getEntitiesOfClass(
                com.piranport.dungeon.entity.DungeonPortalEntity.class, regionBox)
                .forEach(net.minecraft.world.entity.Entity::discard);
        dungeonLevel.getEntitiesOfClass(
                com.piranport.dungeon.entity.LootShipEntity.class, regionBox)
                .forEach(net.minecraft.world.entity.Entity::discard);

        // 清理带副本标签的脚本生成实体
        String instanceTag = "dungeon_instance_" + instance.getInstanceId();
        java.util.List<net.minecraft.world.entity.Entity> tagged = new java.util.ArrayList<>();
        dungeonLevel.getEntities().get(regionBox, entity -> {
            if (entity.getTags().contains(instanceTag)) {
                tagged.add(entity);
            }
        });
        tagged.forEach(net.minecraft.world.entity.Entity::discard);

        // Then sweep everything else.
        java.util.List<net.minecraft.world.entity.Entity> remaining = new java.util.ArrayList<>();
        dungeonLevel.getEntities().get(regionBox, entity -> {
            if (!(entity instanceof net.minecraft.server.level.ServerPlayer)) {
                remaining.add(entity);
            }
        });
        remaining.forEach(net.minecraft.world.entity.Entity::discard);
    }
}
