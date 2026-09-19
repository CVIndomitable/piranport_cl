package com.piranport.block;

import com.piranport.dungeon.block.DungeonLecternBlockEntity;
import com.piranport.dungeon.event.DungeonEntryService;
import com.piranport.dungeon.event.DungeonEventHandler;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** 旧深海门也走讲台钥匙入口，不能绕过实例分配直接写入副本坐标。 */
public final class AbyssalPortalTeleporter {
    private AbyssalPortalTeleporter() {}

    public static boolean teleport(ServerPlayer player) {
        if (player.isOnPortalCooldown()) return false;
        player.setPortalCooldown(100);
        if (DungeonEventHandler.isInDungeon(player)) {
            var instance = DungeonInstanceManager.get(player.serverLevel()).getInstanceForPlayer(player);
            if (instance == null) return false;
            DungeonEventHandler.teleportToLectern(player, instance);
            return true;
        }
        BlockPos center = player.blockPosition();
        BlockPos closest = null;
        double distance = 64.0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -8, -8), center.offset(8, 8, 8))) {
            double candidate = player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
            if (candidate > distance || !player.serverLevel().hasChunkAt(pos)) continue;
            if (player.level().getBlockEntity(pos) instanceof DungeonLecternBlockEntity lectern && lectern.hasKey()) {
                closest = pos.immutable();
                distance = candidate;
            }
        }
        if (closest == null) return false;
        DungeonEntryService.enter(player, closest, true, null);
        return true;
    }
}
