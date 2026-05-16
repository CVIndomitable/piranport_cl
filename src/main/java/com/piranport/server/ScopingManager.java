package com.piranport.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端瞄准状态跟踪。
 * 当客户端进入瞄准镜模式时，通过 ScopeEnterPayload 告知服务端，
 * 服务端据此组织 ArtilleryItem.use() 中立即开火，等待 ScopeFirePayload。
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
