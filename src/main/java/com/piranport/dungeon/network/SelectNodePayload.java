package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S 节点选择请求；服务端从附近讲台读取钥匙并校验路线后再改变状态。 */
public record SelectNodePayload(BlockPos lecternPos, int keySlot, String nodeId)
        implements CustomPacketPayload {

    private static final int MAX_ID_LENGTH = 128;

    public static final Type<SelectNodePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "select_node"));

    public static final StreamCodec<ByteBuf, SelectNodePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeLong(p.lecternPos().asLong());
                ByteBufCodecs.VAR_INT.encode(buf, p.keySlot());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.nodeId());
            },
            buf -> new SelectNodePayload(
                    BlockPos.of(buf.readLong()),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.stringUtf8(MAX_ID_LENGTH).decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SelectNodePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && payload.nodeId().length() <= MAX_ID_LENGTH) {
                // 保留线路格式，但钥匙权威源统一为讲台，不接受客户端背包中的副本状态。
                com.piranport.dungeon.event.DungeonEntryService.enter(
                        player, payload.lecternPos(), false, payload.nodeId());
            }
        });
    }
}
