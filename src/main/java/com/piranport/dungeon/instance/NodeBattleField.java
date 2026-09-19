package com.piranport.dungeon.instance;

import com.piranport.PiranPort;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.EnemySetData;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.TerrainType;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        int radius = 30; // spawn enemies ~30 blocks from player spawn

        // ===== 副本/15：编队生成器接入 =====
        FormationGenerator.FormationResult formationResult =
                FormationGenerator.generateWithFleetGroup(enemySet, center, dungeonLevel);
        if (formationResult != null) {
            // 有 formation 配置：按队形生成 + 注册 FleetGroup
            com.piranport.npc.ai.FleetGroup group = formationResult.group();
            List<FormationGenerator.SpawnPlan> plans = formationResult.plans();
            boolean flagshipSpawned = false;
            for (FormationGenerator.SpawnPlan plan : plans) {
                Entity entity = plan.entityType().create(dungeonLevel);
                if (entity != null) {
                    entity.setPos(plan.position().x, DungeonConstants.SPAWN_Y, plan.position().z);
                    entity.addTag("dungeon_instance_" + instance.getInstanceId());
                    entity.addTag("dungeon_node_" + node.nodeId());
                    if (plan.isLeader()) {
                        entity.addTag("dungeon_flagship");
                        flagshipSpawned = true;
                    }
                    dungeonLevel.addFreshEntity(entity);
                    spawned.add(entity);
                    // 注册到编队
                    if (entity instanceof AbstractDeepOceanEntity deep) {
                        com.piranport.npc.ai.FleetGroupManager.get(dungeonLevel)
                                .addMember(group.getGroupId(), deep.getUUID());
                    }
                }
            }
            // 设置旗舰为领舰
            if (!group.getMembers().isEmpty() && group.getLeaderUuid() == null) {
                for (Entity e : spawned) {
                    if (e instanceof AbstractDeepOceanEntity deep && e.getTags().contains("dungeon_flagship")) {
                        group.setLeaderUuid(deep.getUUID());
                        break;
                    }
                }
            }

            if (enemySet.flagship() != null && !flagshipSpawned) {
                PiranPort.LOGGER.warn("Formation flagship configured but not spawned — recovery portal");
                spawnCompletionPortal(dungeonLevel, instance, node.nodeId());
            } else if (spawned.isEmpty()) {
                spawnCompletionPortal(dungeonLevel, instance, node.nodeId());
            }

            if (node.type() == NodeData.NodeType.BOSS && flagshipSpawned) {
                com.piranport.dungeon.BossAntiStuckScheduler.register(instance, node, spawned);
            }
            com.piranport.dungeon.saved.DungeonObjectiveData.get(dungeonLevel)
                    .register(instance.getInstanceId(), node.nodeId(), spawned);
            return spawned;
        }

        // ===== 原有随机散布逻辑（无 formation 时执行） =====
        // Spawn regular enemies
        var rng = dungeonLevel.getRandom();
        for (EnemySetData.SpawnEntry entry : enemySet.spawnList()) {
            for (int i = 0; i < entry.count(); i++) {
                Entity entity = createEntity(dungeonLevel, entry.entity());
                if (entity != null) {
                    double angle = rng.nextDouble() * Math.PI * 2;
                    double dist = radius + rng.nextDouble() * 20;
                    double ex = center.getX() + Math.cos(angle) * dist;
                    double ez = center.getZ() + Math.sin(angle) * dist;
                    entity.setPos(ex, DungeonConstants.SPAWN_Y, ez);
                    entity.addTag("dungeon_instance_" + instance.getInstanceId());
                    entity.addTag("dungeon_node_" + node.nodeId());
                    dungeonLevel.addFreshEntity(entity);
                    spawned.add(entity);
                }
            }
        }

        // Spawn flagship
        boolean flagshipSpawned = false;
        if (enemySet.flagship() != null) {
            for (int i = 0; i < enemySet.flagship().count(); i++) {
                Entity flagship = createEntity(dungeonLevel, enemySet.flagship().entity());
                if (flagship != null) {
                    double ex = center.getX() + radius + 10;
                    double ez = center.getZ();
                    flagship.setPos(ex, DungeonConstants.SPAWN_Y, ez);
                    // Tag the flagship so we can detect its death
                    flagship.addTag("dungeon_flagship");
                    flagship.addTag("dungeon_instance_" + instance.getInstanceId());
                    flagship.addTag("dungeon_node_" + node.nodeId());
                    dungeonLevel.addFreshEntity(flagship);
                    spawned.add(flagship);
                    flagshipSpawned = true;
                }
            }
        }

        // If a flagship was configured but none could be created, the node would
        // never resolve — spawn a completion portal as a recovery mechanism.
        if (enemySet.flagship() != null && !flagshipSpawned) {
            PiranPort.LOGGER.warn("Flagship entity '{}' could not be created — spawning recovery portal",
                    enemySet.flagship().entity());
            spawnCompletionPortal(dungeonLevel, instance, node.nodeId());
        } else if (enemySet.flagship() == null && spawned.isEmpty()) {
            // No flagship configured AND nothing else spawned — open a portal so players can move on.
            PiranPort.LOGGER.warn("Node '{}' produced 0 entities — spawning recovery portal", node.nodeId());
            spawnCompletionPortal(dungeonLevel, instance, node.nodeId());
        }

        // 决策/副本/07：BOSS 节点注册防卡 tick 调度器
        if (node.type() == NodeData.NodeType.BOSS && flagshipSpawned) {
            com.piranport.dungeon.BossAntiStuckScheduler.register(instance, node, spawned);
        }

        com.piranport.dungeon.saved.DungeonObjectiveData.get(dungeonLevel)
                .register(instance.getInstanceId(), node.nodeId(), spawned);
        return spawned;
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

    /**
     * Clears all blocks and entities in an instance's region.
     * Explicitly handles dungeon-owned entities (portals, crates) first so they cannot
     * be inherited by a future instance that reuses this region's index.
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
        // 先收集再删除，避免 ConcurrentModificationException
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

        // Note: we don't clear blocks here to avoid lag.
        // The region will be overwritten by future instances or left as-is.
    }
}
