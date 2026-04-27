package com.piranport.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/**
 * Per-dimension persistence of force-loaded chunks by recon aircraft.
 * Saved as piranport_forced_chunks.dat so orphaned chunks can be released on server restart.
 */
public class ForcedChunkData extends SavedData {
    private static final String DATA_NAME = "piranport_forced_chunks";

    private final Set<Long> forcedChunks = new HashSet<>();

    public ForcedChunkData() {}

    public Set<Long> getForcedChunks() {
        return forcedChunks;
    }

    public void addChunk(int x, int z) {
        forcedChunks.add(ChunkPos.asLong(x, z));
        setDirty();
    }

    public void removeChunk(int x, int z) {
        forcedChunks.remove(ChunkPos.asLong(x, z));
        setDirty();
    }

    public void clear() {
        forcedChunks.clear();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (long chunkKey : forcedChunks) {
            CompoundTag c = new CompoundTag();
            c.putLong("chunk", chunkKey);
            list.add(c);
        }
        tag.put("chunks", list);
        return tag;
    }

    public static ForcedChunkData load(CompoundTag tag, HolderLookup.Provider registries) {
        ForcedChunkData data = new ForcedChunkData();
        ListTag list = tag.getList("chunks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            data.forcedChunks.add(c.getLong("chunk"));
        }
        return data;
    }

    public static ForcedChunkData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ForcedChunkData::new, ForcedChunkData::load, null),
                DATA_NAME);
    }
}
