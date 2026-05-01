package com.piranport.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Per-player opt-out state for weapon hit/kill/miss chat notifications. Default: enabled. */
public final class HitNotifier {
    private HitNotifier() {}

    private static final Set<UUID> disabled = ConcurrentHashMap.newKeySet();

    public static boolean isEnabled(UUID playerId) {
        return !disabled.contains(playerId);
    }

    public static void setEnabled(UUID playerId, boolean enabled) {
        if (enabled) disabled.remove(playerId);
        else disabled.add(playerId);
    }

    public static void onPlayerLogout(UUID playerId) {
        disabled.remove(playerId);
    }

    public static void send(Player player, Component message) {
        if (!isEnabled(player.getUUID())) return;
        player.sendSystemMessage(message);
    }
}
