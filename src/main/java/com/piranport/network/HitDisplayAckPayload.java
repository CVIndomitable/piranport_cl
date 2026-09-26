package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.platform.ClientHooks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 服务端确认个人命中通知开关。 */
public record HitDisplayAckPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<HitDisplayAckPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "hit_display_ack"));
    public static final StreamCodec<ByteBuf, HitDisplayAckPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(HitDisplayAckPayload::new, HitDisplayAckPayload::enabled);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(HitDisplayAckPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHooks.setHitDisplayEnabledClient(payload.enabled()));
    }
}
