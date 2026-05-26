package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.aviation.ClientReconData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** S2C: server tells client to start/end recon camera mode. */
public record ReconStatePayload(boolean isActive, int entityId) implements CustomPacketPayload {

    public static final Type<ReconStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "recon_state"));

    public static final StreamCodec<ByteBuf, ReconStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.BOOL.encode(buf, p.isActive());
                ByteBufCodecs.VAR_INT.encode(buf, p.entityId());
            },
            buf -> new ReconStatePayload(
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ReconStatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (payload.isActive()) {
                ClientReconData.handleReconStart(payload.entityId());
            } else {
                ClientReconData.handleReconEnd();
            }
        });
    }
}
