package com.piranport.dungeon.network;

import com.piranport.dungeon.data.ChapterData;
import com.piranport.dungeon.data.StageData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Client-side storage for dungeon state synced via S2C payloads.
 */
public final class ClientDungeonData {
    private ClientDungeonData() {}

    // Lobby data
    private static List<String> lobbyMembers = List.of();
    private static String lobbyFlagshipName = "";
    private static String lobbySelectedStage = "";

    // Synced registry data (from DungeonRegistrySyncPayload)
    private static Map<String, ChapterData> chapters = Map.of();
    private static Map<String, StageData> stages = Map.of();
    private static List<ChapterData> sortedChapters = List.of();

    public static void setLobbyMembers(List<String> members, String flagship, String stage) {
        lobbyMembers = members;
        lobbyFlagshipName = flagship;
        lobbySelectedStage = stage;
    }

    public static List<String> getLobbyMembers() { return lobbyMembers; }
    public static String getLobbyFlagshipName() { return lobbyFlagshipName; }
    public static String getLobbySelectedStage() { return lobbySelectedStage; }

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
        lobbyMembers = List.of();
        lobbyFlagshipName = "";
        lobbySelectedStage = "";
        chapters = Map.of();
        stages = Map.of();
        sortedChapters = List.of();
    }
}
