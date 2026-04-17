package com.piranport.block.entity;

import com.piranport.npc.ai.FleetGroupManager;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.UUID;

/**
 * Block entity for one-shot enemy spawner.
 * <p>
 * NBT fields (set in structure template via data command block):
 * <ul>
 *   <li>{@code entityType} — registry id, e.g. "piranport:deep_ocean_destroyer"</li>
 *   <li>{@code count} — number to spawn (1-4)</li>
 *   <li>{@code clusterUUID} — shared fleet group id (string, auto-generated if absent)</li>
 *   <li>{@code patrolRadius} — patrol wander radius</li>
 * </ul>
 * <p>
 * On first server tick, spawns enemies then self-destructs.
 */
public class AbyssalSpawnerBlockEntity extends BlockEntity {

    private String entityTypeId = "piranport:deep_ocean_destroyer";
    private int count = 1;
    private UUID clusterUUID = null;
    private float patrolRadius = 16.0f;
    private boolean hasSpawned = false;

    public AbyssalSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.ABYSSAL_SPAWNER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   AbyssalSpawnerBlockEntity be) {
        if (be.hasSpawned) return;
        be.hasSpawned = true;

        if (!(level instanceof ServerLevel serverLevel)) return;

        UUID cluster = be.clusterUUID != null ? be.clusterUUID : UUID.randomUUID();

        if (!be.entityTypeId.contains(":")) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }
        Optional<EntityType<?>> optType = BuiltInRegistries.ENTITY_TYPE
                .getOptional(ResourceLocation.parse(be.entityTypeId));
        if (optType.isEmpty()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }

        EntityType<?> type = optType.get();

        for (int i = 0; i < be.count; i++) {
            Entity entity = type.create(serverLevel);
            if (entity == null) {
                com.piranport.PiranPort.LOGGER.warn("AbyssalSpawner: failed to create entity '{}' at {}", be.entityTypeId, pos);
                continue;
            }

            double offsetX = (level.random.nextDouble() - 0.5) * 4.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 4.0;
            entity.setPos(pos.getX() + 0.5 + offsetX, pos.getY() + 0.5, pos.getZ() + 0.5 + offsetZ);

            if (entity instanceof AbstractDeepOceanEntity abyssal) {
                abyssal.setFleetGroupId(cluster);
                FleetGroupManager mgr = FleetGroupManager.get(serverLevel);
                if (mgr.getGroup(cluster) == null) {
                    mgr.createGroup(cluster);
                }
                mgr.addMember(cluster, abyssal.getUUID());
            }

            if (entity instanceof Mob mob) {
                mob.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos),
                        MobSpawnType.STRUCTURE, null);
                mob.setPersistenceRequired();
            }

            serverLevel.addFreshEntity(entity);
        }

        // Self-destruct
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("entityType", entityTypeId);
        tag.putInt("count", count);
        if (clusterUUID != null) {
            tag.putUUID("clusterUUID", clusterUUID);
        }
        tag.putFloat("patrolRadius", patrolRadius);
        tag.putBoolean("hasSpawned", hasSpawned);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("entityType")) {
            entityTypeId = tag.getString("entityType");
        }
        count = tag.getInt("count");
        if (count < 1) count = 1;
        if (count > 4) count = 4;
        if (tag.hasUUID("clusterUUID")) {
            clusterUUID = tag.getUUID("clusterUUID");
        }
        patrolRadius = tag.getFloat("patrolRadius");
        if (patrolRadius <= 0) patrolRadius = 16.0f;
        hasSpawned = tag.getBoolean("hasSpawned");
    }
}
