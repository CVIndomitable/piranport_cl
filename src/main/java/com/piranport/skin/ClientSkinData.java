package com.piranport.skin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientSkinData {
    private static final Map<UUID, Integer> activeSkins = new ConcurrentHashMap<>();

    public static int getActiveSkin(UUID playerUuid) {
        return activeSkins.getOrDefault(playerUuid, 0);
    }

    public static void setActiveSkin(UUID playerUuid, int skinId) {
        if (skinId <= 0) {
            activeSkins.remove(playerUuid);
        } else {
            activeSkins.put(playerUuid, skinId);
        }
    }

    public static void clear() {
        activeSkins.clear();
    }
}
