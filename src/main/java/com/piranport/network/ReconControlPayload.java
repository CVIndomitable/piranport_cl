package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.aviation.ReconManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
            // NaN check must come before clamp (clamp converts NaN to min value)
            if (Float.isNaN(payload.dx()) || Float.isNaN(payload.dy()) || Float.isNaN(payload.dz())) return;
            float dx = Mth.clamp(payload.dx(), -1.0f, 1.0f);
            float dy = Mth.clamp(payload.dy(), -1.0f, 1.0f);
            float dz = Mth.clamp(payload.dz(), -1.0f, 1.0f);
            ReconManager.handleControl(ctx.player().getUUID(), dx, dy, dz);
        });
    }
}
