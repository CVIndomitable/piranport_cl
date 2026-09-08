package com.piranport.dungeon.network;

import com.piranport.dungeon.data.ChapterData;
import com.piranport.dungeon.data.StageData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Client-side storage for dungeon state synced via S2C payloads.
 * Thread safety: All fields are reassigned atomically on the client render thread.
 *
 * <p>整合版 §3.1 联机大厅与队长机制已作废（副本/10）：删除 lobby 相关字段。</p>
 */
public final class ClientDungeonData {
    private ClientDungeonData() {}

    // Synced registry data (from DungeonRegistrySyncPayload, reassigned atomically)
    private static Map<String, ChapterData> chapters = Map.of();
    private static Map<String, StageData> stages = Map.of();
    private static List<ChapterData> sortedChapters = List.of();

    public static void setRegistryData(Map<String, ChapterData> chapterMap,
                                        Map<String, StageData> stageMap) {
        chapters = Map.copyOf(chapterMap);
        stages = Map.copyOf(stageMap);
        List<ChapterData> sorted = new ArrayList<>(chapterMap.values());
        sorted.sort(Comparator.comparingInt(ChapterData::sortOrder));
        sortedChapters = List.copyOf(sorted);
    }

    public static List<ChapterData> getSortedChapters() { return sortedChapters; }
    public static StageData getStage(String stageId) { return stages.get(stageId); }
    public static boolean hasRegistryData() { return !chapters.isEmpty(); }

    public static void clear() {
        chapters = Map.of();
        stages = Map.of();
        sortedChapters = List.of();
    }
}