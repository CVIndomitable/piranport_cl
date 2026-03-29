package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.debug.PiranPortDebug;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: toggle server-side debug logging on/off. */
public record DebugTogglePayload(boolean enabled) implements CustomPacketPayload {

    public static final Type<DebugTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "debug_toggle"));

    public static final StreamCodec<ByteBuf, DebugTogglePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(DebugTogglePayload::new, DebugTogglePayload::enabled);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(DebugTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PiranPortDebug.setServerEnabled(payload.enabled()));
    }
}
