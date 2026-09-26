package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.debug.PiranPortDebug;
import com.piranport.testtools.PiranPortTestTools;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S: toggle the test-mode cooldown override (clamps every cooldown to 5s).
 *
 * <p>注意：此功能已迁移到 {@link PiranPortTestTools}（独立包 com.piranport.testtools），
 * 与 {@link PiranPortDebug} 严格隔离：
 * <ul>
 *   <li>调试模式开启时拒绝开启测试模式（互斥）</li>
 *   <li>测试模式开启时向客户端发送水印包</li>
 *   <li>日志带 {@code [TEST]} 标签便于区分</li>
 * </ul>
 */
public record DebugCooldownOverridePayload(boolean enabled) implements CustomPacketPayload {

    public static final Type<DebugCooldownOverridePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "debug_cooldown_override"));

    public static final StreamCodec<ByteBuf, DebugCooldownOverridePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(DebugCooldownOverridePayload::new, DebugCooldownOverridePayload::enabled);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(DebugCooldownOverridePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PiranPortDebug.runPayload(
                "DebugCooldownOverride", null, () -> {
                    if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer sp)) {
                        return;
                    }
                    if (!com.piranport.config.ConfigToolPermissions.canUse(sp)) {
                        // 权限不足必须有回执，否则客户端乐观翻转的本地状态永久说谎
                        sp.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                "§c[PP] 测试模式需要 OP 权限"), false);
                        return;
                    }
                    var result = PiranPortTestTools.toggleFor(sp.getUUID(), payload.enabled());
                    // 服务端直接发给该玩家：ClientHooks 在专用服务器上是 Noop 桥接，提示送不到人。
                    switch (result.status) {
                        case "DEBUG_ACTIVE" -> sp.displayClientMessage(
                                net.minecraft.network.chat.Component.literal(
                                        "§c[PP] 测试模式与调试互斥：请先关闭调试 (F8) 再开启测试模式"), false);
                        case "NOT_OWNER" -> sp.displayClientMessage(
                                net.minecraft.network.chat.Component.literal(
                                        "§c[PP] 测试模式由其他玩家开启，无法代为关闭"), false);
                        case "ALREADY_ON" -> {}
                        case "ALREADY_OFF" -> {}
                        default -> {}
                    }
                }));
    }
}
