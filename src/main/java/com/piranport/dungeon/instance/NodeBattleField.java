package com.piranport.dungeon.instance;

import com.piranport.PiranPort;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.EnemySetData;
import com.piranport.dungeon.data.NodeData;
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
     * Generates the battlefield terrain (flat water surface) at the node's position
     * within the instance region.
     */
    public static void generateTerrain(ServerLevel dungeonLevel, DungeonInstance instance, String nodeId) {
        BlockPos spawn = instance.getNodeSpawnPos(nodeId);
        int halfSize = DungeonConstants.NODE_AREA_SIZE / 2;
        int seaLevel = DungeonConstants.SEA_LEVEL;

        // Place a flat water surface
        int startX = spawn.getX() - halfSize;
        int startZ = spawn.getZ() - halfSize;

        // Flag 2 = send to clients, 16 = skip neighbor updates, 64 = skip light updates (performance)
        int flags = 2 | 16 | 64;
        for (int x = 0; x < DungeonConstants.NODE_AREA_SIZE; x++) {
            for (int z = 0; z < DungeonConstants.NODE_AREA_SIZE; z++) {
                BlockPos waterPos = new BlockPos(startX + x, seaLevel, startZ + z);
                dungeonLevel.setBlock(waterPos, Blocks.WATER.defaultBlockState(), flags);
                // Solid floor below water
                BlockPos floorPos = new BlockPos(startX + x, seaLevel - 1, startZ + z);
                dungeonLevel.setBlock(floorPos, Blocks.STONE.defaultBlockState(), flags);
            }
        }

        // Place a small platform at spawn for players to stand on
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos platPos = spawn.offset(dx, -1, dz);
                dungeonLevel.setBlock(platPos, Blocks.OAK_PLANKS.defaultBlockState(), flags);
                // Remove water above platform
                BlockPos abovePos = spawn.offset(dx, 0, dz);
                dungeonLevel.setBlock(abovePos, Blocks.AIR.defaultBlockState(), flags);
            }
        }
    }

    /**
     * Spawns enemies for a battle/boss node. Returns the list of spawned entities.
     */
    public static List<Entity> spawnEnemies(ServerLevel dungeonLevel, DungeonInstance instance,
                                             NodeData node) {
        List<Entity> spawned = new ArrayList<>();
        if (node.enemies() == null) return spawned;

        EnemySetData enemySet = DungeonRegistry.INSTANCE.getEnemySet(node.enemies());
        if (enemySet == null) {
            PiranPort.LOGGER.warn("Enemy set not found: {}", node.enemies());
            return spawned;
        }

        BlockPos center = instance.getNodeSpawnPos(node.nodeId());
        int radius = 30; // spawn enemies ~30 blocks from player spawn

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
                    dungeonLevel.addFreshEntity(entity);
                    spawned.add(entity);
                }
            }
        }

        // Spawn flagship
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
                }
            }
        }

        return spawned;
    }

    private static Entity createEntity(ServerLevel level, String entityId) {
        ResourceLocation rl = ResourceLocation.tryParse(entityId);
        if (rl == null) {
            PiranPort.LOGGER.warn("Invalid entity ID: {}", entityId);
            return null;
        }
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(rl);
        if (type.isEmpty()) {
            // Fallback: use zombie as placeholder for missing entity types
            PiranPort.LOGGER.warn("Entity type not found: {}, using zombie placeholder", entityId);
            return EntityType.ZOMBIE.create(level);
        }
        return type.get().create(level);
    }

    /**
     * Clears all blocks and entities in an instance's region.
     */
    public static void cleanupRegion(ServerLevel dungeonLevel, DungeonInstance instance) {
        int originX = instance.getRegionOriginX();
        int originZ = instance.getRegionOriginZ();
        int size = DungeonConstants.REGION_SIZE;

        // Remove entities in the region using AABB spatial query instead of full traversal
        net.minecraft.world.phys.AABB regionBox = new net.minecraft.world.phys.AABB(
                originX, -64, originZ, originX + size, 320, originZ + size);
        dungeonLevel.getEntities().get(regionBox, entity -> {
            entity.discard();
        });

        // Note: we don't clear blocks here to avoid lag.
        // The region will be overwritten by future instances or left as-is.
    }
}
