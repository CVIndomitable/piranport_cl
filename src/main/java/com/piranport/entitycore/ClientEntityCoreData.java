package com.piranport.entitycore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端实体核心数据。只保存玩家 UUID 到实体核心 ID 的映射。
 */
public class ClientEntityCoreData {
    private static final Map<UUID, Integer> activeEntityCores = new HashMap<>();

    public static int getActiveEntityCore(UUID playerUuid) {
        return activeEntityCores.getOrDefault(playerUuid, 0);
    }

    public static void setActiveEntityCore(UUID playerUuid, int coreId) {
        if (coreId <= 0) {
            activeEntityCores.remove(playerUuid);
        } else {
            activeEntityCores.put(playerUuid, coreId);
        }
    }

    public static void clear() {
        activeEntityCores.clear();
    }
}
