package com.piranport.handler;

import com.piranport.aviation.AircraftIndex;
import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import com.piranport.combat.TorpedoGuidanceManager;
import com.piranport.entity.AircraftEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * 战机召回工具方法
 *
 * <p>用于在玩家登出或需要清理飞机时统一处理飞机实体和相关状态。
 */
public class PlayerAircraftHelper {

    /**
     * 召回并清理玩家的所有飞机
     *
     * <p>P0修复: 玩家登出时直接清理飞机实体，不走返航流程，避免资源泄漏
     *
     * @param player 玩家实体
     */
    public static void recallAircraftForPlayer(Player player) {
        if (player.level().isClientSide()) return;
        UUID ownerUUID = player.getUUID();

        // P0修复: 玩家登出时直接清理，不依赖 recallAndRemove() 的在线检查
        for (AircraftEntity aircraft : AircraftIndex.snapshot(ownerUUID)) {
            // 如果是侦察机，先清理侦察状态和强制加载的区块
            if (aircraft.getFlightState() == AircraftEntity.FlightState.RECON_ACTIVE) {
                aircraft.cleanupReconState(player);
            }
            // 直接移除实体，不走返航流程
            aircraft.discard();
        }

        // 清理索引和相关管理器状态
        AircraftIndex.removePlayerAircraft(ownerUUID);
        FireControlManager.clearTargets(ownerUUID);
        ReconManager.endRecon(ownerUUID);
        TorpedoGuidanceManager.endGuidance(ownerUUID);
    }
}
