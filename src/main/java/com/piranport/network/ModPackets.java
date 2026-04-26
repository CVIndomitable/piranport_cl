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
        registrar.playToServer(
                DebugCooldownOverridePayload.TYPE,
                DebugCooldownOverridePayload.STREAM_CODEC,
                DebugCooldownOverridePayload::handle
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

        // Hit display (hit/kill/miss chat) toggle
        registrar.playToServer(
                HitDisplayTogglePayload.TYPE,
                HitDisplayTogglePayload.STREAM_CODEC,
                HitDisplayTogglePayload::handle
        );

    }
}
