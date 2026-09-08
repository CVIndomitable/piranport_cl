package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.debug.PiranPortDebug;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
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
        context.enqueueWork(() -> com.piranport.debug.PiranPortDebug.runPayload(
                "DebugToggle", null, () -> {
                    if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer sp)) {
                        return;
                    }
                    if (!sp.hasPermissions(2)) {
                        // P0-1: 权限不足时显式反馈，避免假成功
                        PacketDistributor.sendToPlayer(sp,
                                new DebugToggleAckPayload(false, -1L, "NO_PERMISSION"));
                        return;
                    }
                    var result = PiranPortDebug.togglePlayer(sp.getUUID(), sp.getScoreboardName(), payload.enabled());
                    String status = result.status();
                    long sid = result.sessionId();
                    PacketDistributor.sendToPlayer(sp,
                            new DebugToggleAckPayload(payload.enabled(), sid, status));
                }));
    }
}
