package com.piranport.skin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientSkinData {
    // 仅客户端：从渲染线程访问，无需并发控制
    private static final Map<UUID, Integer> activeSkins = new HashMap<>();

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
