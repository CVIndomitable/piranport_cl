package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.debug.PiranPortDebug;
import com.piranport.platform.ClientHooks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: 服务端确认调试开关结果。
 *
 * <p>由 {@link DebugTogglePayload} 处理后发回客户端，避免客户端提示成功但服务端权限不足
 * 时静默失败的假反馈。{@code status} 字段语义：
 * <ul>
 *   <li>{@code OPENED}        — 成功开启（含 sessionId）</li>
 *   <li>{@code CLOSED}        — 成功关闭</li>
 *   <li>{@code ALREADY_OPEN}  — 已是开启状态（幂等）</li>
 *   <li>{@code ALREADY_CLOSED}— 已是关闭状态（幂等）</li>
 *   <li>{@code NO_PERMISSION} — 玩家无 OP 权限</li>
 * </ul>
 */
public record DebugToggleAckPayload(boolean enabled, long sessionId, String status)
        implements CustomPacketPayload {

    public static final Type<DebugToggleAckPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "debug_toggle_ack"));

    public static final StreamCodec<ByteBuf, DebugToggleAckPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, DebugToggleAckPayload::enabled,
                    ByteBufCodecs.VAR_LONG, DebugToggleAckPayload::sessionId,
                    ByteBufCodecs.STRING_UTF8, DebugToggleAckPayload::status,
                    DebugToggleAckPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DebugToggleAckPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // 校正客户端本地状态，避免与服务端偏离
            com.piranport.client.input.DebugInputHandler.setDebugEnabledClient(payload.enabled());
            String msg;
            switch (payload.status()) {
                case "OPENED" ->
                    msg = String.format("[PP] 调试已开启 session=#%d (日志: logs/piranport-debug-%d.log)",
                            payload.sessionId(), payload.sessionId());
                case "CLOSED" ->
                    msg = String.format("[PP] 调试已关闭 session=#%d (日志已归档)", payload.sessionId());
                case "ALREADY_OPEN" ->
                    msg = String.format("[PP] 调试已是开启状态 session=#%d", payload.sessionId());
                case "ALREADY_CLOSED" ->
                    msg = "[PP] 调试已是关闭状态";
                case "NO_PERMISSION" ->
                    msg = "[PP] 调试需要 OP 权限";
                default ->
                    msg = "[PP] 调试状态: " + payload.status();
            }
            ClientHooks.displayClientMessage(Component.literal(msg));
        });
    }
}
