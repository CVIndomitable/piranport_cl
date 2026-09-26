package com.piranport.terminal;

import com.piranport.PiranPort;
import com.piranport.aviation.AircraftStatsService;
import com.piranport.combat.BallisticSolver;
import com.piranport.combat.HitNotifier;
import com.piranport.config.ConfigToolPermissions;
import com.piranport.network.HitDisplayAckPayload;
import com.piranport.network.SyncTerminalParametersPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** 服务端生命周期与新加入玩家的权威参数同步。 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public final class TerminalServerEvents {
    private TerminalServerEvents() { }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        TerminalParameters.setDedicatedServer(!event.getServer().isSingleplayer());
        TerminalParametersSavedData.get(event.getServer().overworld());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        TerminalParameters.clearServer();
        AircraftStatsService.clear();
        BallisticSolver.clearCache();
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TerminalParametersSavedData data = TerminalParametersSavedData.get(player.serverLevel());
        PacketDistributor.sendToPlayer(player, ConfigToolPermissions.canUse(player)
                ? SyncTerminalParametersPayload.from(data, "")
                : SyncTerminalParametersPayload.effective(data));
        PacketDistributor.sendToPlayer(player, new HitDisplayAckPayload(HitNotifier.isEnabled(player.getUUID())));
    }

    @SubscribeEvent
    public static void datapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) return;
        TerminalParametersSavedData data = TerminalParametersSavedData.get(event.getPlayerList().getServer().overworld());
        data.refreshCatalog();
        // 数据包目录的默认值可变化，即使存档修订号不变也要重发完整有效快照。
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player,
                ConfigToolPermissions.canUse(player) ? SyncTerminalParametersPayload.from(data, "")
                        : SyncTerminalParametersPayload.effective(data)));
        AircraftStatsService.clear();
        BallisticSolver.clearCache();
    }
}
