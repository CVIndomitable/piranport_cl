package com.piranport.dungeon.instance;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** 预算归属于副本维度；所有实例和所有入口共享同一个服务器 tick 上限。 */
public final class TerrainWorkBudget extends SavedData {
    private long tick = Long.MIN_VALUE;
    private int remaining;

    public static TerrainWorkBudget get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(TerrainWorkBudget::new,
                (tag, registries) -> new TerrainWorkBudget(), null), "piranport_terrain_budget");
    }

    public int reserve(long currentTick, int requested) {
        if (tick != currentTick) {
            tick = currentTick;
            remaining = TerrainGenerationPipeline.BLOCKS_PER_TICK;
        }
        int reserved = Math.min(remaining, Math.max(0, requested));
        remaining -= reserved;
        return reserved;
    }

    public void release(int unused) {
        remaining = Math.min(TerrainGenerationPipeline.BLOCKS_PER_TICK, remaining + Math.max(0, unused));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return tag; // 仅运行时预算，生成游标在 TerrainGenerationState 中单独持久化。
    }
}
