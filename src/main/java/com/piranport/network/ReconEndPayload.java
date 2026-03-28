package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.aviation.ClientReconData;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** S2C: server notifies client to restore camera to the player. */
public record ReconEndPayload() implements CustomPacketPayload {

    public static final Type<ReconEndPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "recon_end"));

    public static final StreamCodec<ByteBuf, ReconEndPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {},
            buf -> new ReconEndPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ReconEndPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.setCameraEntity(mc.player);
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.piranport.recon_exit"), true);
            }
            ClientReconData.clearRecon();
        });
    }
}
