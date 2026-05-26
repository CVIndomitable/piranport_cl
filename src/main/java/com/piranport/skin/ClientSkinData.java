package com.piranport.skin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端皮肤数据 — 追踪玩家当前使用的皮肤 ID。
 *
 * <p><b>线程模型</b>: 客户端渲染线程（单线程），HashMap 无需同步。
 * <p><b>生命周期</b>: 在 {@link com.piranport.client.ClientGameEvents#onClientDisconnect} 中通过 {@link #clear()} 清理。
 * <p><b>访问限制</b>: 仅限客户端，服务端不可访问。
 */
public class ClientSkinData {

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
