package com.piranport.dungeon.network;

import java.util.List;

/**
 * Client-side storage for dungeon state synced via S2C payloads.
 */
public final class ClientDungeonData {
    private ClientDungeonData() {}

    // Lobby data
    private static List<String> lobbyMembers = List.of();
    private static String lobbyFlagshipName = "";
    private static String lobbySelectedStage = "";

    public static void setLobbyMembers(List<String> members, String flagship, String stage) {
        lobbyMembers = members;
        lobbyFlagshipName = flagship;
        lobbySelectedStage = stage;
    }

    public static List<String> getLobbyMembers() { return lobbyMembers; }
    public static String getLobbyFlagshipName() { return lobbyFlagshipName; }
    public static String getLobbySelectedStage() { return lobbySelectedStage; }

    public static void clear() {
        lobbyMembers = List.of();
        lobbyFlagshipName = "";
        lobbySelectedStage = "";
    }
}
