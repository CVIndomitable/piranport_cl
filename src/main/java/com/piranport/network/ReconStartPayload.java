package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.aviation.ClientReconData;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** S2C: server notifies client to switch camera to recon entity. */
public record ReconStartPayload(int entityId) implements CustomPacketPayload {

    public static final Type<ReconStartPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "recon_start"));

    public static final StreamCodec<ByteBuf, ReconStartPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> ByteBufCodecs.VAR_INT.encode(buf, p.entityId()),
            buf -> new ReconStartPayload(ByteBufCodecs.VAR_INT.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ReconStartPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity entity = mc.level.getEntity(payload.entityId());
            if (entity != null) {
                mc.setCameraEntity(entity);
            }
            ClientReconData.setReconActive(payload.entityId());

            // Display HUD hint
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.piranport.recon_enter"), true);
            }
        });
    }
}
