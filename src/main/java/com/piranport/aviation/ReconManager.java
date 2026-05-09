package com.piranport.aviation;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side: tracks which player is in recon mode and their pending movement input.
 *  Primarily accessed on server thread; ConcurrentHashMap guards clearAll() during shutdown. */
public class ReconManager {
    // playerUUID → reconEntityUUID
    private static final Map<UUID, UUID> activeRecon = new ConcurrentHashMap<>();
    // playerUUID → [dx, dy, dz]
    private static final Map<UUID, float[]> pendingInput = new ConcurrentHashMap<>();

    public static void startRecon(UUID playerUUID, UUID entityUUID) {
        activeRecon.put(playerUUID, entityUUID);
        com.piranport.PiranPort.LOGGER.info("ReconManager START | player={} entity={}", playerUUID, entityUUID);
    }

    public static void endRecon(UUID playerUUID) {
        UUID removed = activeRecon.remove(playerUUID);
        pendingInput.remove(playerUUID);
        com.piranport.PiranPort.LOGGER.info("ReconManager END | player={} wasActive={}", playerUUID, removed != null);
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
        boolean isActive = activeRecon.containsKey(playerUUID);
        if (isActive) {
            pendingInput.put(playerUUID, new float[]{dx, dy, dz});
        } else {
            com.piranport.PiranPort.LOGGER.warn("ReconManager INPUT_REJECTED | player={} notInRecon", playerUUID);
        }
    }

    /** Called from AircraftEntity.tick() each tick to consume pending input. Returns null if none. */
    public static float[] consumeInput(UUID playerUUID) {
        float[] input = pendingInput.remove(playerUUID);
        if (input != null) {
            com.piranport.PiranPort.LOGGER.debug("ReconManager INPUT_CONSUMED | player={} dx={} dy={} dz={}",
                playerUUID, input[0], input[1], input[2]);
        }
        return input;
    }

    /** Remove all state (call on server stop / world unload). */
    public static void clearAll() {
        activeRecon.clear();
        pendingInput.clear();
    }
}
