package com.piranport.network;

import com.piranport.PiranPort;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = PiranPort.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModPackets {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
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
    }
}
