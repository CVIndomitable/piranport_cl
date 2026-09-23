package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.debug.PiranPortDebug;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: request a state snapshot to be written to the debug log. */
public record SnapshotRequestPayload(boolean acknowledged) implements CustomPacketPayload {

    /** 旧构造函数默认 ack=true，保持向后兼容 */
    public SnapshotRequestPayload() { this(true); }

    public static final Type<SnapshotRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "snapshot_request"));

    public static final StreamCodec<ByteBuf, SnapshotRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, SnapshotRequestPayload::acknowledged,
                    SnapshotRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SnapshotRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.piranport.debug.PiranPortDebug.runPayload(
                "SnapshotRequest",
                context.player(),
                () -> {
                    if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer sp)) {
                        return;
                    }
                    // 服务端直接发给该玩家：ClientHooks 在专用服务器上是 Noop 桥接，消息送不到人。
                    if (!sp.hasPermissions(2)) {
                        sp.displayClientMessage(Component.literal("[PP] 调试需要 OP 权限"), false);
                        return;
                    }
                    long cooldownMs = PiranPortDebug.snapshot(sp);
                    if (payload.acknowledged()) {
                        if (cooldownMs > 0) {
                            sp.displayClientMessage(Component.literal(
                                    String.format(java.util.Locale.ROOT,
                                            "[PP] 快照冷却中，剩余 %d ms", cooldownMs)), false);
                        } else {
                            sp.displayClientMessage(Component.literal(
                                    "[PP] Snapshot written (会话日志)"), false);
                        }
                    }
                }));
    }
}