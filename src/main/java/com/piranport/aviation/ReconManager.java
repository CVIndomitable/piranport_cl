package com.piranport.aviation;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side: tracks which player is in recon mode and their pending movement input. */
public class ReconManager {
    // playerUUID → reconEntityUUID
    private static final Map<UUID, UUID> activeRecon = new ConcurrentHashMap<>();
    // playerUUID → [dx, dy, dz]
    private static final Map<UUID, float[]> pendingInput = new ConcurrentHashMap<>();

    public static void startRecon(UUID playerUUID, UUID entityUUID) {
        activeRecon.put(playerUUID, entityUUID);
    }

    public static void endRecon(UUID playerUUID) {
        activeRecon.remove(playerUUID);
        pendingInput.remove(playerUUID);
    }

    public static boolean isInRecon(UUID playerUUID) {
        return activeRecon.containsKey(playerUUID);
    }

    @Nullable
    public static UUID getReconEntity(UUID playerUUID) {
        return activeRecon.get(playerUUID);
    }

    /** Called from packet handler when client sends movement input. */
    public static void handleControl(UUID playerUUID, float dx, float dy, float dz) {
        if (activeRecon.containsKey(playerUUID)) {
            pendingInput.put(playerUUID, new float[]{dx, dy, dz});
        }
    }

    /** Called from AircraftEntity.tick() each tick to consume pending input. Returns null if none. */
    public static float[] consumeInput(UUID playerUUID) {
        return pendingInput.remove(playerUUID);
    }
}
