package com.piranport.handler;

import com.piranport.aviation.AircraftIndex;
import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import com.piranport.combat.TorpedoGuidanceManager;
import com.piranport.entity.AircraftEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** 战机召回工具方法 */
public class PlayerAircraftHelper {

    public static void recallAircraftForPlayer(Player player) {
        if (player.level().isClientSide()) return;
        UUID ownerUUID = player.getUUID();
        for (AircraftEntity aircraft : AircraftIndex.snapshot(ownerUUID)) {
            aircraft.recallAndRemove();
        }
        FireControlManager.clearTargets(ownerUUID);
        ReconManager.endRecon(ownerUUID);
        TorpedoGuidanceManager.endGuidance(ownerUUID);
    }
}
