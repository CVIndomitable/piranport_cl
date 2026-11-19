package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.platform.ClientHooks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 服务端通知演奏者和观察者开始一次演出；不传治疗数值。 */
public record HarpPerformancePayload(int playerId, InteractionHand hand) implements CustomPacketPayload {
    public static final Type<HarpPerformancePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "harp_performance"));
    public static final StreamCodec<ByteBuf, HarpPerformancePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HarpPerformancePayload::playerId,
            ByteBufCodecs.BOOL, p -> p.hand == InteractionHand.OFF_HAND,
            (id, offHand) -> new HarpPerformancePayload(id, offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND));

    @Override
    public Type<HarpPerformancePayload> type() { return TYPE; }

    public static void handle(HarpPerformancePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientHooks.playUnicornHarp(payload.playerId, payload.hand));
    }
}
