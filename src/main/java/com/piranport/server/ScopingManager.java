package com.piranport.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端瞄准状态跟踪 — 当客户端进入瞄准镜模式时，服务端据此协调射击逻辑。
 *
 * <p><b>线程模型</b>: 服务端主线程，ConcurrentHashMap 保护 shutdown 时的并发访问。
 * <p><b>生命周期</b>: 玩家退出瞄准模式时在 {@link #setScoping(Player, boolean)} 中清理，
 * 玩家登出时通过 {@link #handleDisconnect(ServerPlayer)} 清理。
 * <p><b>网络同步</b>: 通过 {@link com.piranport.network.ScopeEnterPayload} 从客户端同步状态变更。
 */
public final class ScopingManager {

    private static final Map<UUID, Boolean> scopingPlayers = new ConcurrentHashMap<>();

    private ScopingManager() {}

    public static boolean isScoping(Player player) {
        return scopingPlayers.getOrDefault(player.getUUID(), false);
    }

    public static void setScoping(Player player, boolean scoping) {
        if (scoping) {
            scopingPlayers.put(player.getUUID(), true);
        } else {
            scopingPlayers.remove(player.getUUID());
        }
    }

    /** 断开连接清理 */
    public static void handleDisconnect(ServerPlayer player) {
        scopingPlayers.remove(player.getUUID());
    }

    /** 服务端关闭时清理全部 */
    public static void clearAll() {
        scopingPlayers.clear();
    }
}
