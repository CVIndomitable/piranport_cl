package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.HitNotifier;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: toggle hit/kill/miss chat notifications for this player. */
public record HitDisplayTogglePayload(boolean enabled) implements CustomPacketPayload {

    public static final Type<HitDisplayTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "hit_display_toggle"));

    public static final StreamCodec<ByteBuf, HitDisplayTogglePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(HitDisplayTogglePayload::new, HitDisplayTogglePayload::enabled);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(HitDisplayTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                HitNotifier.setEnabled(sp.getUUID(), payload.enabled());
            }
        });
    }
}
