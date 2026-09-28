package com.piranport.skin;

import com.piranport.PiranPort;

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

    /** 皮肤贴图上限，对应 assets/piranport/textures/skin/skin_1..skin_24.png。 */
    public static final int MAX_SKIN_ID = 24;

    private static final Map<UUID, Integer> activeSkins = new HashMap<>();

    public static int getActiveSkin(UUID playerUuid) {
        return activeSkins.getOrDefault(playerUuid, 0);
    }

    public static void setActiveSkin(UUID playerUuid, int skinId) {
        if (skinId <= 0) {
            // 0 = 未装备皮肤核心，是默认状态而非错误，静默清除即可。
            activeSkins.remove(playerUuid);
            return;
        }
        if (skinId > MAX_SKIN_ID) {
            // WHY：越界 id 会让渲染层去找一张不存在的贴图。校验放在这里是因为本方法每次
            // 同步只跑一次，而渲染层（SkinOverlayLayer.render）每个玩家模型每帧都跑；
            // 若把警告写在渲染层，未装备皮肤的玩家会按帧刷日志（实测一晚 13548 行）。
            PiranPort.LOGGER.warn("Invalid skin ID: {} for {}, expected 1-{}",
                    skinId, playerUuid, MAX_SKIN_ID);
            activeSkins.remove(playerUuid);
            return;
        }
        activeSkins.put(playerUuid, skinId);
    }

    public static void clear() {
        activeSkins.clear();
    }
}
