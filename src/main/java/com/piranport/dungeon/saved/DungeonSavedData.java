package com.piranport.dungeon.saved;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stores first-clear records per stage per player.
 * Persisted in world save as piranport_dungeon.dat.
 */
public class DungeonSavedData extends SavedData {
    private static final String DATA_NAME = "piranport_dungeon";

    // stageId → set of player UUIDs who have first-cleared
    private final Map<String, Set<UUID>> firstClearRecords = new HashMap<>();

    public DungeonSavedData() {}

    public boolean hasFirstCleared(String stageId, UUID playerUuid) {
        Set<UUID> cleared = firstClearRecords.get(stageId);
        return cleared != null && cleared.contains(playerUuid);
    }

    public void markFirstCleared(String stageId, UUID playerUuid) {
        firstClearRecords.computeIfAbsent(stageId, k -> new HashSet<>()).add(playerUuid);
        setDirty();
    }

    public Set<String> getFirstClearedStages(UUID playerUuid) {
        Set<String> result = new HashSet<>();
        for (var entry : firstClearRecords.entrySet()) {
            if (entry.getValue().contains(playerUuid)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (var entry : firstClearRecords.entrySet()) {
            ListTag list = new ListTag();
            for (UUID uuid : entry.getValue()) {
                list.add(NbtUtils.createUUID(uuid));
            }
            tag.put(entry.getKey(), list);
        }
        return tag;
    }

    public static DungeonSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonSavedData data = new DungeonSavedData();
        for (String key : tag.getAllKeys()) {
            ListTag list = tag.getList(key, Tag.TAG_INT_ARRAY);
            Set<UUID> uuids = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                uuids.add(NbtUtils.loadUUID(list.get(i)));
            }
            data.firstClearRecords.put(key, uuids);
        }
        return data;
    }

    public static DungeonSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DungeonSavedData::new, DungeonSavedData::load, null),
                DATA_NAME);
    }
}
