package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.platform.ClientHooks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** S2C: server tells client to start/end torpedo guidance camera. */
public record TorpedoGuidanceStatePayload(boolean isActive, int entityId) implements CustomPacketPayload {

    public static final Type<TorpedoGuidanceStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "torpedo_guidance_state"));

    public static final StreamCodec<ByteBuf, TorpedoGuidanceStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.BOOL.encode(buf, p.isActive());
                ByteBufCodecs.VAR_INT.encode(buf, p.entityId());
            },
            buf -> new TorpedoGuidanceStatePayload(
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TorpedoGuidanceStatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientHooks.handleTorpedoGuidanceState(payload.isActive(), payload.entityId());
        });
    }
}
