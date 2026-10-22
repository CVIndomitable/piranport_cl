package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.platform.ClientHooks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C：节点通关时右上角滑下的进度式横幅（《副本/19》2026-10-22 补充）。
 *
 * <p>取代原来的全屏结算页 {@code DungeonResultPayload}：不弹窗、不需要玩家确认。
 * 只带横幅需要的三样东西——关卡名、节点名、是否首通；奖励与击杀统计已经直接进包，
 * 不再占用屏幕。</p>
 */
public record NodeClearedToastPayload(String stageName, String nodeName, boolean firstClear)
        implements CustomPacketPayload {

    public NodeClearedToastPayload {
        if (stageName == null) stageName = "";
        if (nodeName == null) nodeName = "";
    }

    public static final Type<NodeClearedToastPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "node_cleared_toast"));

    public static final StreamCodec<ByteBuf, NodeClearedToastPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, p.stageName());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.nodeName());
                ByteBufCodecs.BOOL.encode(buf, p.firstClear());
            },
            buf -> new NodeClearedToastPayload(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(NodeClearedToastPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHooks.showNodeClearedToast(
                payload.stageName(), payload.nodeName(), payload.firstClear()));
    }
}
