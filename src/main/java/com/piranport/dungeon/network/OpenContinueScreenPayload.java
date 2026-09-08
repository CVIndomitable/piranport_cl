package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: 服务端通知玩家打开 ContinueScreen（"继续/从头开始"对话框）。
 *
 * <p>整合版 §3.1：讲台右键 → 校验钥匙 → 无存档直接进入；有存档弹出选择对话框。</p>
 */
public record OpenContinueScreenPayload(BlockPos lecternPos, String stageName, int clearedNodeCount)
        implements CustomPacketPayload {

    private static final int MAX_NAME_LENGTH = 128;

    public static final Type<OpenContinueScreenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "open_continue_screen"));

    public static final StreamCodec<ByteBuf, OpenContinueScreenPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeLong(p.lecternPos().asLong());
                net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.encode(buf, p.stageName());
                buf.writeInt(p.clearedNodeCount());
            },
            buf -> new OpenContinueScreenPayload(
                    BlockPos.of(buf.readLong()),
                    net.minecraft.network.codec.ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH).decode(buf),
                    buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(OpenContinueScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 客户端打开 DungeonContinueScreen（独立 Screen，无 Menu）
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.piranport.dungeon.client.DungeonContinueScreen(
                            payload.lecternPos(),
                            payload.stageName(),
                            payload.clearedNodeCount()));
        });
    }
}