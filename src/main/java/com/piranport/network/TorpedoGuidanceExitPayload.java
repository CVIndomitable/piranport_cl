package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.TorpedoGuidanceManager;
import com.piranport.entity.TorpedoEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** C2S: player presses V to drop the torpedo wire. Torpedo continues ballistically. */
public record TorpedoGuidanceExitPayload() implements CustomPacketPayload {

    public static final Type<TorpedoGuidanceExitPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "torpedo_guidance_exit"));

    public static final StreamCodec<ByteBuf, TorpedoGuidanceExitPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {},
            buf -> new TorpedoGuidanceExitPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TorpedoGuidanceExitPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            UUID torpedoUUID = TorpedoGuidanceManager.getGuidedTorpedo(sp.getUUID());
            if (torpedoUUID != null && sp.level() instanceof ServerLevel sl) {
                Entity entity = sl.getEntity(torpedoUUID);
                if (entity instanceof TorpedoEntity torpedo) {
                    torpedo.cutWire();
                }
            }
            TorpedoGuidanceManager.endGuidance(sp);
        });
    }
}
