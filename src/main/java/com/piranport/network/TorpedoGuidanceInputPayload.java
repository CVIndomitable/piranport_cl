package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.TorpedoGuidanceManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: player sends a unit direction vector (from look angle) for torpedo guidance. */
public record TorpedoGuidanceInputPayload(float dx, float dy, float dz) implements CustomPacketPayload {

    public static final Type<TorpedoGuidanceInputPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "torpedo_guidance_input"));

    public static final StreamCodec<ByteBuf, TorpedoGuidanceInputPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.FLOAT.encode(buf, p.dx());
                ByteBufCodecs.FLOAT.encode(buf, p.dy());
                ByteBufCodecs.FLOAT.encode(buf, p.dz());
            },
            buf -> new TorpedoGuidanceInputPayload(
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TorpedoGuidanceInputPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() == null) return;
            if (Float.isNaN(payload.dx()) || Float.isNaN(payload.dy()) || Float.isNaN(payload.dz())) return;
            float dx = Mth.clamp(payload.dx(), -1.0f, 1.0f);
            float dy = Mth.clamp(payload.dy(), -1.0f, 1.0f);
            float dz = Mth.clamp(payload.dz(), -1.0f, 1.0f);
            // Normalize vector to prevent client from sending oversized input
            double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (len > 1.0) {
                dx /= len;
                dy /= len;
                dz /= len;
            }
            TorpedoGuidanceManager.handleInput(ctx.player().getUUID(), dx, dy, dz);
        });
    }
}
