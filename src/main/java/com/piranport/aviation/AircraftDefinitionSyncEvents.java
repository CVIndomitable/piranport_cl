package com.piranport.aviation;

import com.piranport.PiranPort;
import com.piranport.network.AircraftDefinitionSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 登录与数据重载后把服务端飞机定义同步给客户端。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public final class AircraftDefinitionSyncEvents {
    private AircraftDefinitionSyncEvents() {}

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) send(player);
    }

    @SubscribeEvent
    public static void datapackSync(OnDatapackSyncEvent event) {
        ServerPlayer single = event.getPlayer();
        if (single != null) {
            send(single);
            return;
        }
        event.getRelevantPlayers().forEach(AircraftDefinitionSyncEvents::send);
    }

    private static void send(ServerPlayer player) {
        if (AircraftDefinitionService.snapshot().isEmpty()) return;
        PacketDistributor.sendToPlayer(player,
                AircraftDefinitionSyncPayload.fromDefinitions(AircraftDefinitionService.all()));
    }
}
