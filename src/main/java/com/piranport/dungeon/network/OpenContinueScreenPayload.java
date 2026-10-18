package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: 服务端通知玩家打开 ContinueScreen（"继续/从头开始"对话框）。
 *
 * <p>整合版 §3.1：讲台右键 → 校验钥匙 → 无存档直接进入；有存档弹出选择对话框。
 * 关卡名与已通关数不随包下发：换关后讲台状态会变，屏幕每帧从讲台 BE 读取。</p>
 */
public record OpenContinueScreenPayload(BlockPos lecternPos) implements CustomPacketPayload {

    public static final Type<OpenContinueScreenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "open_continue_screen"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, OpenContinueScreenPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeLong(p.lecternPos().asLong()),
            buf -> new OpenContinueScreenPayload(BlockPos.of(buf.readLong()))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(OpenContinueScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.piranport.platform.ClientHooks.openDungeonContinueScreen(payload.lecternPos()));
    }
}
