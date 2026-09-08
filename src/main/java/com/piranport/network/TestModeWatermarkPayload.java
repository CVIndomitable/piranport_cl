package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.testtools.PiranPortTestTools;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: 测试模式水印提示。
 *
 * <p>服务端在玩家开启 / 关闭测试模式时发送，客户端渲染红色 {@code [PP TEST MODE]} 水印。
 * 这是与调试系统隔离的独立测试工具，避免污染调试取证的纯净度。
 */
public record TestModeWatermarkPayload(boolean enabled, long sessionId) implements CustomPacketPayload {

    public static final Type<TestModeWatermarkPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "test_mode_watermark"));

    public static final StreamCodec<ByteBuf, TestModeWatermarkPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, TestModeWatermarkPayload::enabled,
                    ByteBufCodecs.VAR_LONG, TestModeWatermarkPayload::sessionId,
                    TestModeWatermarkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TestModeWatermarkPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            String msg = payload.enabled()
                    ? String.format("§c[PP TEST MODE] §r测试模式已开启 session=#%d", payload.sessionId())
                    : String.format("[PP] 测试模式已关闭 session=#%d", payload.sessionId());
            com.piranport.platform.ClientHooks.displayClientMessage(Component.literal(msg));
            // 同步到客户端本地状态，便于 HUD 渲染判断
            com.piranport.client.input.DebugInputHandler.setTestModeClient(payload.enabled());
        });
    }
}
