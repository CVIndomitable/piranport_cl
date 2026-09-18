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
 * C2S: Player chose "继续" — 跳转到该玩家的最新记录点位置。
 *
 * <p>整合版 §3.1/§3.2：玩家个人最新记录点用于"继续"按钮，传送玩家到对应坐标。</p>
 */
public record ContinueFromCheckpointPayload(BlockPos lecternPos)
        implements CustomPacketPayload {

    public static final Type<ContinueFromCheckpointPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "continue_from_checkpoint"));

    public static final StreamCodec<ByteBuf, ContinueFromCheckpointPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeLong(p.lecternPos().asLong()),
            buf -> new ContinueFromCheckpointPayload(BlockPos.of(buf.readLong()))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ContinueFromCheckpointPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                com.piranport.dungeon.event.DungeonEntryService.enter(
                        player, payload.lecternPos(), true, null);
            }
        });
    }
}
