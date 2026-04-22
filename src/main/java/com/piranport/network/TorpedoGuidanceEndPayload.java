package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.ClientTorpedoGuidance;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** S2C: server tells client to restore camera back to the player. */
public record TorpedoGuidanceEndPayload() implements CustomPacketPayload {

    public static final Type<TorpedoGuidanceEndPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "torpedo_guidance_end"));

    public static final StreamCodec<ByteBuf, TorpedoGuidanceEndPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {},
            buf -> new TorpedoGuidanceEndPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TorpedoGuidanceEndPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(ClientTorpedoGuidance::handleEnd);
    }
}
