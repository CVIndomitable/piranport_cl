package com.piranport.network;

import com.piranport.PiranPort;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = PiranPort.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModPackets {
    public static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
                CycleWeaponPayload.TYPE,
                CycleWeaponPayload.STREAM_CODEC,
                CycleWeaponPayload::handle
        );
        registrar.playToServer(
                OpenFlightGroupPayload.TYPE,
                OpenFlightGroupPayload.STREAM_CODEC,
                OpenFlightGroupPayload::handle
        );
        registrar.playToServer(
                FlightGroupUpdatePayload.TYPE,
                FlightGroupUpdatePayload.STREAM_CODEC,
                FlightGroupUpdatePayload::handle
        );
        // Phase 20: fire control
        registrar.playToServer(
                FireControlPayload.TYPE,
                FireControlPayload.STREAM_CODEC,
                FireControlPayload::handle
        );
        registrar.playToClient(
                FireControlSyncPayload.TYPE,
                FireControlSyncPayload.STREAM_CODEC,
                FireControlSyncPayload::handle
        );
        // Phase 32: recon aircraft
        registrar.playToServer(
                ReconControlPayload.TYPE,
                ReconControlPayload.STREAM_CODEC,
                ReconControlPayload::handle
        );
        registrar.playToServer(
                ReconExitPayload.TYPE,
                ReconExitPayload.STREAM_CODEC,
                ReconExitPayload::handle
        );
        registrar.playToClient(
                ReconStartPayload.TYPE,
                ReconStartPayload.STREAM_CODEC,
                ReconStartPayload::handle
        );
        registrar.playToClient(
                ReconEndPayload.TYPE,
                ReconEndPayload.STREAM_CODEC,
                ReconEndPayload::handle
        );
        // Phase 36: ship config
        registrar.playToServer(
                AutoLaunchTogglePayload.TYPE,
                AutoLaunchTogglePayload.STREAM_CODEC,
                AutoLaunchTogglePayload::handle
        );
        // Debug system
        registrar.playToServer(
                DebugTogglePayload.TYPE,
                DebugTogglePayload.STREAM_CODEC,
                DebugTogglePayload::handle
        );
        registrar.playToServer(
                SnapshotRequestPayload.TYPE,
                SnapshotRequestPayload.STREAM_CODEC,
                SnapshotRequestPayload::handle
        );

        // No-GUI mode: empty-hand recall all aircraft
        registrar.playToServer(
                RecallAllAircraftPayload.TYPE,
                RecallAllAircraftPayload.STREAM_CODEC,
                RecallAllAircraftPayload::handle
        );

        // Manual reload (R key)
        registrar.playToServer(
                ManualReloadPayload.TYPE,
                ManualReloadPayload.STREAM_CODEC,
                ManualReloadPayload::handle
        );

        // Wire-guided torpedo steering (9/0 keys)
        registrar.playToServer(
                TorpedoSteerPayload.TYPE,
                TorpedoSteerPayload.STREAM_CODEC,
                TorpedoSteerPayload::handle
        );

        // ===== ASW Sonar =====
        registrar.playToClient(
                AswSonarSyncPayload.TYPE,
                AswSonarSyncPayload.STREAM_CODEC,
                AswSonarSyncPayload::handle
        );

        // ===== Skin System =====
        registrar.playToClient(
                SkinSyncPayload.TYPE,
                SkinSyncPayload.STREAM_CODEC,
                SkinSyncPayload::handle
        );
        registrar.playToServer(
                SkinRevertPayload.TYPE,
                SkinRevertPayload.STREAM_CODEC,
                SkinRevertPayload::handle
        );

        // ===== Ammo Workbench =====
        registrar.playToServer(
                AmmoWorkbenchCraftPayload.TYPE,
                AmmoWorkbenchCraftPayload.STREAM_CODEC,
                AmmoWorkbenchCraftPayload::handle
        );

        // ===== Dungeon System =====
        // C2S
        registrar.playToServer(
                com.piranport.dungeon.network.JoinLobbyPayload.TYPE,
                com.piranport.dungeon.network.JoinLobbyPayload.STREAM_CODEC,
                com.piranport.dungeon.network.JoinLobbyPayload::handle
        );
        registrar.playToServer(
                com.piranport.dungeon.network.LeaveLobbyPayload.TYPE,
                com.piranport.dungeon.network.LeaveLobbyPayload.STREAM_CODEC,
                com.piranport.dungeon.network.LeaveLobbyPayload::handle
        );
        registrar.playToServer(
                com.piranport.dungeon.network.SelectStagePayload.TYPE,
                com.piranport.dungeon.network.SelectStagePayload.STREAM_CODEC,
                com.piranport.dungeon.network.SelectStagePayload::handle
        );
        registrar.playToServer(
                com.piranport.dungeon.network.SelectNodePayload.TYPE,
                com.piranport.dungeon.network.SelectNodePayload.STREAM_CODEC,
                com.piranport.dungeon.network.SelectNodePayload::handle
        );
        registrar.playToServer(
                com.piranport.dungeon.network.ReviveRequestPayload.TYPE,
                com.piranport.dungeon.network.ReviveRequestPayload.STREAM_CODEC,
                com.piranport.dungeon.network.ReviveRequestPayload::handle
        );
        registrar.playToServer(
                com.piranport.dungeon.network.TownScrollUsePayload.TYPE,
                com.piranport.dungeon.network.TownScrollUsePayload.STREAM_CODEC,
                com.piranport.dungeon.network.TownScrollUsePayload::handle
        );
        // S2C
        registrar.playToClient(
                com.piranport.dungeon.network.LobbyUpdatePayload.TYPE,
                com.piranport.dungeon.network.LobbyUpdatePayload.STREAM_CODEC,
                com.piranport.dungeon.network.LobbyUpdatePayload::handle
        );
        registrar.playToClient(
                com.piranport.dungeon.network.DungeonStatePayload.TYPE,
                com.piranport.dungeon.network.DungeonStatePayload.STREAM_CODEC,
                com.piranport.dungeon.network.DungeonStatePayload::handle
        );
        registrar.playToClient(
                com.piranport.dungeon.network.NodeEnteredPayload.TYPE,
                com.piranport.dungeon.network.NodeEnteredPayload.STREAM_CODEC,
                com.piranport.dungeon.network.NodeEnteredPayload::handle
        );
        registrar.playToClient(
                com.piranport.dungeon.network.DungeonResultPayload.TYPE,
                com.piranport.dungeon.network.DungeonResultPayload.STREAM_CODEC,
                com.piranport.dungeon.network.DungeonResultPayload::handle
        );
        registrar.playToClient(
                com.piranport.dungeon.network.PlayerDiedInDungeonPayload.TYPE,
                com.piranport.dungeon.network.PlayerDiedInDungeonPayload.STREAM_CODEC,
                com.piranport.dungeon.network.PlayerDiedInDungeonPayload::handle
        );
    }
}
