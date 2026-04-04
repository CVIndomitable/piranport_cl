package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.aviation.ReconManager;
import com.piranport.entity.AircraftEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** C2S: player presses V to exit recon mode. Server transitions aircraft to RETURNING. */
public record ReconExitPayload() implements CustomPacketPayload {

    public static final Type<ReconExitPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "recon_exit"));

    public static final StreamCodec<ByteBuf, ReconExitPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {},
            buf -> new ReconExitPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ReconExitPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() == null) return;
            UUID playerUUID = ctx.player().getUUID();
            UUID entityUUID = ReconManager.getReconEntity(playerUUID);
            if (entityUUID == null) return;

            // End recon state first to prevent permanent slowness if entity is unloaded
            ReconManager.endRecon(playerUUID);

            if (ctx.player().level() instanceof ServerLevel sl) {
                Entity entity = sl.getEntity(entityUUID);
                if (entity instanceof AircraftEntity aircraft) {
                    aircraft.setState(AircraftEntity.FlightState.RETURNING);
                }
            }
        });
    }
}
