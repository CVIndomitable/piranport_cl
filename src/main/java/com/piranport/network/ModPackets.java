package com.piranport.network;

import com.piranport.PiranPort;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModPackets {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PiranPort.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        // Client -> Server packets
        CHANNEL.registerMessage(id(), CycleWeaponPayload.class,
                CycleWeaponPayload::encode,
                CycleWeaponPayload::decode,
                CycleWeaponPayload::handle);

        CHANNEL.registerMessage(id(), FireControlPayload.class,
                FireControlPayload::encode,
                FireControlPayload::decode,
                FireControlPayload::handle);

        CHANNEL.registerMessage(id(), ReconControlPayload.class,
                ReconControlPayload::encode,
                ReconControlPayload::decode,
                ReconControlPayload::handle);

        CHANNEL.registerMessage(id(), ReconExitPayload.class,
                ReconExitPayload::encode,
                ReconExitPayload::decode,
                ReconExitPayload::handle);

        CHANNEL.registerMessage(id(), DebugTogglePayload.class,
                DebugTogglePayload::encode,
                DebugTogglePayload::decode,
                DebugTogglePayload::handle);

        CHANNEL.registerMessage(id(), SnapshotRequestPayload.class,
                SnapshotRequestPayload::encode,
                SnapshotRequestPayload::decode,
                SnapshotRequestPayload::handle);

        CHANNEL.registerMessage(id(), DebugCooldownOverridePayload.class,
                DebugCooldownOverridePayload::encode,
                DebugCooldownOverridePayload::decode,
                DebugCooldownOverridePayload::handle);

        CHANNEL.registerMessage(id(), RecallAllAircraftPayload.class,
                RecallAllAircraftPayload::encode,
                RecallAllAircraftPayload::decode,
                RecallAllAircraftPayload::handle);

        CHANNEL.registerMessage(id(), ManualReloadPayload.class,
                ManualReloadPayload::encode,
                ManualReloadPayload::decode,
                ManualReloadPayload::handle);

        CHANNEL.registerMessage(id(), TorpedoSteerPayload.class,
                TorpedoSteerPayload::encode,
                TorpedoSteerPayload::decode,
                TorpedoSteerPayload::handle);

        CHANNEL.registerMessage(id(), HitDisplayTogglePayload.class,
                HitDisplayTogglePayload::encode,
                HitDisplayTogglePayload::decode,
                HitDisplayTogglePayload::handle);

        // Server -> Client packets
        CHANNEL.registerMessage(id(), FireControlSyncPayload.class,
                FireControlSyncPayload::encode,
                FireControlSyncPayload::decode,
                FireControlSyncPayload::handle);

        CHANNEL.registerMessage(id(), ReconStartPayload.class,
                ReconStartPayload::encode,
                ReconStartPayload::decode,
                ReconStartPayload::handle);

        CHANNEL.registerMessage(id(), ReconEndPayload.class,
                ReconEndPayload::encode,
                ReconEndPayload::decode,
                ReconEndPayload::handle);

        CHANNEL.registerMessage(id(), AswSonarSyncPayload.class,
                AswSonarSyncPayload::encode,
                AswSonarSyncPayload::decode,
                AswSonarSyncPayload::handle);
    }
}
