package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: 服务端通知客户端踩到 checkpoint，客户端触发反馈（信标光柱 + 标题 + 音效）。
 *
 * <p>整合版 §3.2 解锁反馈：踩到记录点后出现信标光柱、屏幕标题显示"记录点名称"、
 * 播放轻快音效；不添加 HUD。</p>
 */
public record CheckpointReachedPayload(String stageName, String checkpointName)
        implements CustomPacketPayload {

    private static final int MAX_NAME_LENGTH = 128;

    public static final Type<CheckpointReachedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "checkpoint_reached"));

    public static final StreamCodec<ByteBuf, CheckpointReachedPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, p.stageName());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.checkpointName());
            },
            buf -> new CheckpointReachedPayload(
                    ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH).decode(buf),
                    ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH).decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(CheckpointReachedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 客户端反射调用 ClientItemHooks.onCheckpointReached(name)
            // 当前阶段先在客户端展示标题（轻量级反馈）
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§6✦ 记录点 §f" + payload.checkpointName()
                                        + " §7(" + payload.stageName() + ")"),
                        true);
                // 标题显示（整合版 §3.2：屏幕标题显示"记录点名称"）
                mc.gui.setTitle(net.minecraft.network.chat.Component.literal(
                        net.minecraft.ChatFormatting.GOLD + payload.checkpointName()));
                // 轻快音效：使用原版 note block pling 音
                mc.player.playSound(
                        net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(),
                        1.0f, 1.5f);
            }
        });
    }
}