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
 *   <li>{@code TEST_ACTIVE}   — 反向互斥：测试模式运行中，拒绝开启调试</li>
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
            ClientHooks.setDebugEnabledClient(payload.enabled());

            // CLOSED 前缀携带关闭原因（PiranPortDebug 发的 CLOSED:<CloseReason>）：
            // 不剥离前缀会让 switch 走 default 分支，把 "CLOSED:USER" 原样显示给玩家。
            String status = payload.status();
            String closeReason = null;
            int sep = status.indexOf(':');
            if (sep >= 0) {
                closeReason = status.substring(sep + 1);
                status = status.substring(0, sep);
            }

            // sessionId < 0 表示"本次操作没有产生/不涉及会话"（幂等拒绝、权限不足），
            // 此时不能打印 -1，否则会出现 "session=#-1 (日志: logs/piranport-debug--1.log)" 这种假路径。
            boolean hasSession = payload.sessionId() >= 0;
            String sessionNote = hasSession ? String.format(java.util.Locale.ROOT, " session=#%d", payload.sessionId()) : "";

            String msg;
            switch (status) {
                case "OPENED" ->
                    msg = hasSession
                            ? String.format(java.util.Locale.ROOT,
                                    "[PP] 调试已开启 session=#%d (日志: logs/piranport-debug-%d.log)",
                                    payload.sessionId(), payload.sessionId())
                            : "[PP] 调试已开启 (日志写入 logs/)";
                case "CLOSED" ->
                    msg = "[PP] 调试已关闭" + sessionNote
                            + (closeReason == null ? " (日志已归档)" : " (" + closeReasonLabel(closeReason) + ")");
                case "ALREADY_OPEN" ->
                    msg = "[PP] 调试已是开启状态" + sessionNote;
                case "ALREADY_CLOSED" ->
                    msg = "[PP] 调试已是关闭状态";
                case "NO_PERMISSION" ->
                    msg = "[PP] 调试需要 OP 权限";
                case "TEST_ACTIVE" ->
                    msg = "[PP] 测试模式运行中，无法开启调试：请先按 N 关闭测试模式";
                default ->
                    msg = "[PP] 调试状态: " + payload.status();
            }
            // overlay=false：这是需要留痕的状态变更提示，走聊天栏而不是会被覆盖的 actionbar
            ClientHooks.displayClientMessage(Component.literal(msg), false);
        });
    }

    /** CloseReason 枚举名 → 中文说明，避免把枚举名直接抛给玩家。 */
    private static String closeReasonLabel(String reason) {
        return switch (reason) {
            case "USER" -> "玩家手动关闭";
            case "LOGOUT" -> "玩家登出";
            case "TIMEOUT" -> "会话超时";
            case "SERVER_STOP" -> "服务端停止";
            case "REPLACED" -> "会话被替换";
            default -> "原因: " + reason;
        };
    }
}
