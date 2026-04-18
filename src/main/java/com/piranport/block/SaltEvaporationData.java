package com.piranport.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-dimension persistence of water-evaporation countdowns for {@link SaltEvaporationHandler}.
 * Saved as piranport_salt_evaporation.dat so in-progress evaporations survive restarts.
 */
public class SaltEvaporationData extends SavedData {
    private static final String DATA_NAME = "piranport_salt_evaporation";

    private final Map<BlockPos, Integer> entries = new HashMap<>();

    public SaltEvaporationData() {}

    public Map<BlockPos, Integer> entries() {
        return entries;
    }

    public void put(BlockPos pos, int ticks) {
        entries.put(pos.immutable(), ticks);
        setDirty();
    }

    public boolean containsKey(BlockPos pos) {
        return entries.containsKey(pos);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, Integer> e : entries.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("pos", e.getKey().asLong());
            c.putInt("ticks", e.getValue());
            list.add(c);
        }
        tag.put("entries", list);
        return tag;
    }

    public static SaltEvaporationData load(CompoundTag tag, HolderLookup.Provider registries) {
        SaltEvaporationData data = new SaltEvaporationData();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            data.entries.put(BlockPos.of(c.getLong("pos")), c.getInt("ticks"));
        }
        return data;
    }

    public static SaltEvaporationData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SaltEvaporationData::new, SaltEvaporationData::load, null),
                DATA_NAME);
    }
}
