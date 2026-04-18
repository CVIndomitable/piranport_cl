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

        return spawned;
    }

    private static void spawnCompletionPortal(ServerLevel dungeonLevel, DungeonInstance instance,
                                                String nodeId) {
        BlockPos spawn = instance.getNodeSpawnPos(nodeId);
        var portal = com.piranport.dungeon.entity.DungeonPortalEntity.create(dungeonLevel,
                instance.getInstanceId(), nodeId,
                spawn.getX() + 0.5, DungeonConstants.SPAWN_Y, spawn.getZ() + 0.5);
        dungeonLevel.addFreshEntity(portal);
    }

    /**
     * Returns null when the entity type is not registered. Callers must treat null
     * as "skip" rather than substituting a placeholder, since untagged placeholder
     * entities would never trigger node completion handlers.
     */
    private static Entity createEntity(ServerLevel level, String entityId) {
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

        // Discard dungeon-owned entities first (portals & crates) so their UUIDs
        // can never be re-resolved by a future instance reusing this region index.
        dungeonLevel.getEntitiesOfClass(
                com.piranport.dungeon.entity.DungeonPortalEntity.class, regionBox)
                .forEach(net.minecraft.world.entity.Entity::discard);
        dungeonLevel.getEntitiesOfClass(
                com.piranport.dungeon.entity.LootShipEntity.class, regionBox)
                .forEach(net.minecraft.world.entity.Entity::discard);

        // Then sweep everything else.
        dungeonLevel.getEntities().get(regionBox, entity -> {
            if (!(entity instanceof net.minecraft.server.level.ServerPlayer)) {
                entity.discard();
            }
        });

        // Note: we don't clear blocks here to avoid lag.
        // The region will be overwritten by future instances or left as-is.
    }
}
