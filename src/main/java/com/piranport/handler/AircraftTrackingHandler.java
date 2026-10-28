package com.piranport.handler;

import com.piranport.PiranPort;
import com.piranport.entity.AircraftEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 实体追踪事件 → 侦察机远距离手工同步。
 *
 * <p>原版 {@code ChunkMap} 用「玩家视距 + 玩家区块追踪范围」判定实体是否同步给某个玩家。
 * 侦察机允许飞出该范围，所以用这两个事件精确获知 owner 何时收到/不再收到原版实体包，
 * 不再自己复刻 ChunkMap 的距离公式，避免版本升级后判据漂移。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class AircraftTrackingHandler {

    private AircraftTrackingHandler() {}

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof AircraftEntity craft) {
            craft.updateReconTracking(event.getEntity(), true);
        }
    }

    @SubscribeEvent
    public static void onStopTracking(PlayerEvent.StopTracking event) {
        if (event.getTarget() instanceof AircraftEntity craft) {
            craft.updateReconTracking(event.getEntity(), false);
        }
    }
}
