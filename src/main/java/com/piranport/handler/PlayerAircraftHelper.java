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
     * 把玩家所有在飞飞机召回并放回物品栏。
     *
     * <p>与 {@link #recallAircraftForPlayer(Player)} 的区别：后者是登出/死亡/换维度用的
     * 硬清理路径，直接 {@code discard()} 实体、不返还飞机物品；本方法走
     * {@link AircraftEntity#recallAndRemove()}，回收后飞机物品回到原武器槽或背包。
     * 取下核心、燃料耗尽解除变身属于「正常召回」，必须用本方法，否则飞机会连同物品一起消失。
     *
     * @return 实际处理的在飞飞机数量
     */
    public static int recallAircraftToInventory(Player player) {
        if (player.level().isClientSide()) return 0;
        int recalled = 0;
        for (AircraftEntity aircraft : AircraftIndex.snapshot(player.getUUID())) {
            if (aircraft.isRemoved()) continue;
            aircraft.recallAndRemove();
            recalled++;
        }
        if (recalled > 0) {
            // 变身结束后不再保留火控锁定，避免下次变身沿用旧目标。
            FireControlManager.clearTargets(player.getUUID());
        }
        return recalled;
    }

    /**
     * 召回并清理玩家的所有飞机。
     *
     * <p>P0 修复：玩家登出/死亡/换维度时直接清理飞机实体，不走返航流程，避免资源泄漏。
     * 正常解除变身（取下核心、燃料耗尽）请用 {@link #recallAircraftToInventory(Player)}，
     * 否则飞机物品不会返还。
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
