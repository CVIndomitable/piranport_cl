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
    }
}
