package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.ClientTorpedoGuidance;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** S2C: server tells client to switch camera to a wire-guided torpedo. */
public record TorpedoGuidanceStartPayload(int entityId) implements CustomPacketPayload {

    public static final Type<TorpedoGuidanceStartPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "torpedo_guidance_start"));

    public static final StreamCodec<ByteBuf, TorpedoGuidanceStartPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> ByteBufCodecs.VAR_INT.encode(buf, p.entityId()),
            buf -> new TorpedoGuidanceStartPayload(ByteBufCodecs.VAR_INT.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TorpedoGuidanceStartPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientTorpedoGuidance.handleStart(payload.entityId()));
    }
}
