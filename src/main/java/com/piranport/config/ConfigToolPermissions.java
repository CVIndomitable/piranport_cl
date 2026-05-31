package com.piranport.config;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Permission gate for world-level config mutation tools.
 */
public final class ConfigToolPermissions {
    private static final int CONFIG_ADMIN_PERMISSION_LEVEL = 2;

    private ConfigToolPermissions() {
    }

    public static boolean canUse(Player player) {
        if (player.hasPermissions(CONFIG_ADMIN_PERMISSION_LEVEL)) {
            return true;
        }

        return player instanceof ServerPlayer serverPlayer
                && isSingleplayerOwner(serverPlayer);
    }

    private static boolean isSingleplayerOwner(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return server != null
                && server.isSingleplayerOwner(player.getGameProfile());
    }
}
