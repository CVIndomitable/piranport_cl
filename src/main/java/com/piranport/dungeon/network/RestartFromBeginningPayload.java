package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S: Player chose "从头开始" — 跳转到副本起点（stage.startNode 节点）。
 *
 * <p>整合版 §3.1：怪物/宝箱不刷新（已击杀不复活、已开启不重置）——目的是允许玩家寻找遗漏的宝箱。
 * 传送玩家到副本起点对应的 node spawn 位置。</p>
 */
public record RestartFromBeginningPayload(BlockPos lecternPos)
        implements CustomPacketPayload {

    public static final Type<RestartFromBeginningPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "restart_from_beginning"));

    public static final StreamCodec<ByteBuf, RestartFromBeginningPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeLong(p.lecternPos().asLong()),
            buf -> new RestartFromBeginningPayload(BlockPos.of(buf.readLong()))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(RestartFromBeginningPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                com.piranport.dungeon.event.DungeonEntryService.enter(
                        player, payload.lecternPos(), false, null);
            }
        });
    }
}
