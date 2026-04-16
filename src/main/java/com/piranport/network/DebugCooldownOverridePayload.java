package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.debug.PiranPortDebug;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: toggle the debug cooldown override (clamps every cooldown to 5s). */
public record DebugCooldownOverridePayload(boolean enabled) implements CustomPacketPayload {

    public static final Type<DebugCooldownOverridePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "debug_cooldown_override"));

    public static final StreamCodec<ByteBuf, DebugCooldownOverridePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(DebugCooldownOverridePayload::new, DebugCooldownOverridePayload::enabled);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(DebugCooldownOverridePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp
                    && sp.hasPermissions(2)) {
                PiranPortDebug.setCooldownOverride(payload.enabled());
            }
        });
    }
}
