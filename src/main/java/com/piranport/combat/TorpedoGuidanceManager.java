package com.piranport.combat;

import com.piranport.entity.TorpedoEntity;
import com.piranport.network.TorpedoGuidanceEndPayload;
import com.piranport.network.TorpedoGuidanceStartPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side: tracks which player is guiding which torpedo and their pending direction input. */
public class TorpedoGuidanceManager {
    private static final Map<UUID, UUID> activeGuidance = new ConcurrentHashMap<>();
    private static final Map<UUID, float[]> pendingInput = new ConcurrentHashMap<>();

    public static void startGuidance(ServerPlayer player, TorpedoEntity torpedo) {
        UUID playerUUID = player.getUUID();
        UUID prev = activeGuidance.put(playerUUID, torpedo.getUUID());
        if (prev != null && !prev.equals(torpedo.getUUID())) {
            pendingInput.remove(playerUUID);
            PacketDistributor.sendToPlayer(player, new com.piranport.network.TorpedoGuidanceEndPayload());
        }
        PacketDistributor.sendToPlayer(player, new TorpedoGuidanceStartPayload(torpedo.getId()));
    }

    public static void endGuidance(UUID playerUUID) {
        UUID removed = activeGuidance.remove(playerUUID);
        pendingInput.remove(playerUUID);
        if (removed == null) return;
    }

    /** End guidance and notify the client to restore its camera. */
    public static void endGuidance(ServerPlayer player) {
        if (activeGuidance.remove(player.getUUID()) == null) {
            pendingInput.remove(player.getUUID());
            return;
        }
        pendingInput.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, new TorpedoGuidanceEndPayload());
    }

    public static boolean isGuiding(UUID playerUUID) {
        return activeGuidance.containsKey(playerUUID);
    }

    @Nullable
    public static UUID getGuidedTorpedo(UUID playerUUID) {
        return activeGuidance.get(playerUUID);
    }

    public static void handleInput(UUID playerUUID, float dx, float dy, float dz) {
        if (activeGuidance.containsKey(playerUUID)) {
            pendingInput.put(playerUUID, new float[]{dx, dy, dz});
        }
    }

    /** Consume the latest direction input. Returns null if none (torpedo drifts). */
    @Nullable
    public static float[] consumeInput(UUID playerUUID) {
        return pendingInput.remove(playerUUID);
    }

    /** Called when the guided torpedo is destroyed/wire-cut so we can tell the client to exit. */
    public static void onTorpedoGone(ServerPlayer player) {
        endGuidance(player);
    }

    public static void clearAll() {
        activeGuidance.clear();
        pendingInput.clear();
    }
}
