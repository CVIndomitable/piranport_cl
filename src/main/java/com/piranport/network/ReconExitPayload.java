package com.piranport.network;

import com.piranport.aviation.ReconManager;
import com.piranport.entity.AircraftEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** C2S: player presses V to exit recon mode. Server transitions aircraft to RETURNING. */
public class ReconExitPayload {

    public ReconExitPayload() {
    }

    public static void encode(ReconExitPayload msg, FriendlyByteBuf buf) {
        // No data to encode
    }

    public static ReconExitPayload decode(FriendlyByteBuf buf) {
        return new ReconExitPayload();
    }

    public static void handle(ReconExitPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getSender() == null) return;
            UUID playerUUID = ctx.get().getSender().getUUID();
            UUID entityUUID = ReconManager.getReconEntity(playerUUID);
            if (entityUUID == null) return;

            // End recon state first to prevent permanent slowness if entity is unloaded
            ReconManager.endRecon(playerUUID);

            if (ctx.get().getSender().level() instanceof ServerLevel sl) {
                Entity entity = sl.getEntity(entityUUID);
                if (entity instanceof AircraftEntity aircraft) {
                    aircraft.setState(AircraftEntity.FlightState.RETURNING);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
