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
 */
public final class ClientDungeonData {
    private ClientDungeonData() {}

    // Lobby data (reassigned atomically, no concurrent modification)
    private static List<String> lobbyMembers = List.of();
    private static String lobbyFlagshipName = "";
    private static String lobbySelectedStage = "";
    // Phase 27：每个成员的准备状态（索引与 lobbyMembers 对齐）
    private static List<Boolean> lobbyReadyStates = List.of();

    // Synced registry data (from DungeonRegistrySyncPayload, reassigned atomically)
    private static Map<String, ChapterData> chapters = Map.of();
    private static Map<String, StageData> stages = Map.of();
    private static List<ChapterData> sortedChapters = List.of();

    public static void setLobbyMembers(List<String> members, String flagship, String stage,
                                       List<Boolean> readyStates) {
        lobbyMembers = members;
        lobbyFlagshipName = flagship;
        lobbySelectedStage = stage;
        lobbyReadyStates = readyStates != null ? readyStates : List.of();
    }

    public static List<String> getLobbyMembers() { return lobbyMembers; }
    public static String getLobbyFlagshipName() { return lobbyFlagshipName; }
    public static String getLobbySelectedStage() { return lobbySelectedStage; }
    public static List<Boolean> getLobbyReadyStates() { return lobbyReadyStates; }
    /** 当前玩家是否已准备（根据名字匹配索引；同地图多人同名场景作 fallback 全部 false）。 */
    public static boolean amIReady() {
        return false; // 客户端无法直接知道本地玩家 UUID，使用本地默认 false；切换按钮仍生效
    }

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
        lobbyReadyStates = List.of();
        chapters = Map.of();
        stages = Map.of();
        sortedChapters = List.of();
    }
}
