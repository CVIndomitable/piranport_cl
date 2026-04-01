package com.piranport.dungeon.saved;

import com.piranport.dungeon.DungeonConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stores speed-run leaderboard entries per stage.
 * Persisted as piranport_leaderboard.dat.
 */
public class DungeonLeaderboard extends SavedData {
    private static final String DATA_NAME = "piranport_leaderboard";

    public record LeaderboardEntry(UUID playerUuid, String playerName, long timeMillis, long timestamp) {}

    private final Map<String, List<LeaderboardEntry>> entries = new HashMap<>();

    public DungeonLeaderboard() {}

    /**
     * Submits a new leaderboard entry. Returns the rank (1-based), or -1 if not in top N.
     */
    public int submit(String stageId, UUID playerUuid, String playerName, long timeMillis) {
        List<LeaderboardEntry> list = entries.computeIfAbsent(stageId, k -> new ArrayList<>());
        LeaderboardEntry entry = new LeaderboardEntry(
                playerUuid, playerName, timeMillis, System.currentTimeMillis());
        list.add(entry);
        list.sort(Comparator.comparingLong(LeaderboardEntry::timeMillis));

        // Trim to max entries
        while (list.size() > DungeonConstants.MAX_LEADERBOARD_ENTRIES) {
            list.remove(list.size() - 1);
        }

        setDirty();

        int rank = list.indexOf(entry);
        return rank >= 0 ? rank + 1 : -1;
    }

    public List<LeaderboardEntry> getEntries(String stageId) {
        return List.copyOf(entries.getOrDefault(stageId, List.of()));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (var entry : entries.entrySet()) {
            ListTag list = new ListTag();
            for (LeaderboardEntry le : entry.getValue()) {
                CompoundTag et = new CompoundTag();
                et.putUUID("Player", le.playerUuid());
                et.putString("Name", le.playerName());
                et.putLong("Time", le.timeMillis());
                et.putLong("Timestamp", le.timestamp());
                list.add(et);
            }
            tag.put(entry.getKey(), list);
        }
        return tag;
    }

    public static DungeonLeaderboard load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonLeaderboard lb = new DungeonLeaderboard();
        for (String key : tag.getAllKeys()) {
            ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
            List<LeaderboardEntry> entries = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                CompoundTag et = list.getCompound(i);
                entries.add(new LeaderboardEntry(
                        et.getUUID("Player"),
                        et.getString("Name"),
                        et.getLong("Time"),
                        et.getLong("Timestamp")));
            }
            entries.sort(Comparator.comparingLong(LeaderboardEntry::timeMillis));
            lb.entries.put(key, entries);
        }
        return lb;
    }

    public static DungeonLeaderboard get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DungeonLeaderboard::new, DungeonLeaderboard::load, null),
                DATA_NAME);
    }
}
