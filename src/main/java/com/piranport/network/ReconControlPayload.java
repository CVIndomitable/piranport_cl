package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.aviation.ReconManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: player sends WASD movement direction for recon aircraft control. */
public record ReconControlPayload(float dx, float dy, float dz) implements CustomPacketPayload {

    public static final Type<ReconControlPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "recon_control"));

    public static final StreamCodec<ByteBuf, ReconControlPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.FLOAT.encode(buf, p.dx());
                ByteBufCodecs.FLOAT.encode(buf, p.dy());
                ByteBufCodecs.FLOAT.encode(buf, p.dz());
            },
            buf -> new ReconControlPayload(
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ReconControlPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() == null) return;
            ReconManager.handleControl(ctx.player().getUUID(), payload.dx(), payload.dy(), payload.dz());
        });
    }
}
