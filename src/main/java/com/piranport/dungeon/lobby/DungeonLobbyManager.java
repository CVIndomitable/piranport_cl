package com.piranport.dungeon.lobby;

import com.piranport.dungeon.network.LobbyUpdatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages dungeon lobbies. Each lectern GlobalPos can have one active lobby.
 * The first player to interact becomes the flagship (lobby leader).
 */
public final class DungeonLobbyManager {
    public static final DungeonLobbyManager INSTANCE = new DungeonLobbyManager();

    private final Map<GlobalPos, Lobby> lobbies = new HashMap<>();

    private DungeonLobbyManager() {}

    public static class Lobby {
        private final GlobalPos lecternPos;
        private UUID flagshipUuid;
        private String flagshipName;
        private final List<UUID> memberUuids = new ArrayList<>();
        private final List<String> memberNames = new ArrayList<>();
        private String selectedStageId;

        public Lobby(GlobalPos pos, ServerPlayer flagship) {
            this.lecternPos = pos;
            this.flagshipUuid = flagship.getUUID();
            this.flagshipName = flagship.getGameProfile().getName();
            this.memberUuids.add(flagship.getUUID());
            this.memberNames.add(flagshipName);
        }

        public GlobalPos getLecternPos() { return lecternPos; }
        public UUID getFlagshipUuid() { return flagshipUuid; }
        public String getFlagshipName() { return flagshipName; }
        public List<UUID> getMemberUuids() { return List.copyOf(memberUuids); }
        public List<String> getMemberNames() { return List.copyOf(memberNames); }
        public String getSelectedStageId() { return selectedStageId; }
        public void setSelectedStageId(String stageId) { this.selectedStageId = stageId; }

        public boolean isFlagship(UUID uuid) {
            return flagshipUuid.equals(uuid);
        }

        /** Maximum number of players per lobby. */
        public static final int MAX_LOBBY_SIZE = 6;

        public void addMember(ServerPlayer player) {
            UUID uuid = player.getUUID();
            if (!memberUuids.contains(uuid) && memberUuids.size() < MAX_LOBBY_SIZE) {
                memberUuids.add(uuid);
                memberNames.add(player.getGameProfile().getName());
            }
        }

        public void removeMember(UUID uuid) {
            int idx = memberUuids.indexOf(uuid);
            if (idx >= 0) {
                memberUuids.remove(idx);
                memberNames.remove(idx);
            }
            // If flagship left and there are others, promote first member
            if (flagshipUuid.equals(uuid) && !memberUuids.isEmpty()) {
                flagshipUuid = memberUuids.get(0);
                flagshipName = memberNames.get(0);
            }
        }

        public boolean isEmpty() {
            return memberUuids.isEmpty();
        }

        public int size() {
            return memberUuids.size();
        }
    }

    public Lobby joinLobby(GlobalPos lecternPos, ServerPlayer player) {
        Lobby lobby = lobbies.get(lecternPos);
        if (lobby == null) {
            lobby = new Lobby(lecternPos, player);
            lobbies.put(lecternPos, lobby);
        } else {
            lobby.addMember(player);
        }
        return lobby;
    }

    public void leaveLobby(GlobalPos lecternPos, UUID playerUuid) {
        Lobby lobby = lobbies.get(lecternPos);
        if (lobby != null) {
            lobby.removeMember(playerUuid);
            if (lobby.isEmpty()) {
                lobbies.remove(lecternPos);
            }
        }
    }

    public Lobby getLobby(GlobalPos lecternPos) {
        return lobbies.get(lecternPos);
    }

    public void removeLobby(GlobalPos lecternPos) {
        lobbies.remove(lecternPos);
    }

    /** Send current lobby state to all online members. */
    public void broadcastLobbyUpdate(MinecraftServer server, GlobalPos lecternPos) {
        Lobby lobby = lobbies.get(lecternPos);
        if (lobby == null) return;
        LobbyUpdatePayload payload = new LobbyUpdatePayload(
                lobby.getMemberNames(), lobby.getFlagshipName(),
                lobby.getSelectedStageId() != null ? lobby.getSelectedStageId() : "");
        for (UUID uuid : lobby.getMemberUuids()) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public void clearAll() {
        lobbies.clear();
    }
}
