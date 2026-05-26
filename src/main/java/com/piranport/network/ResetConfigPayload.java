package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.artillery.config.override.ArtilleryConfigOverrideSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 重置配置覆盖网络包（客户端 → 服务端）
 *
 * <p>清空当前存档的所有配置覆盖，恢复到原始配置。
 */
public record ResetConfigPayload() implements CustomPacketPayload {

    public static final Type<ResetConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "reset_config"));

    public static final StreamCodec<ByteBuf, ResetConfigPayload> STREAM_CODEC = StreamCodec.unit(new ResetConfigPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理网络包（服务端）
     */
    public static void handle(ResetConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            // 仅创造模式或OP可用
            if (!serverPlayer.isCreative() && !serverPlayer.hasPermissions(2)) {
                PiranPort.LOGGER.warn("Player {} tried to reset config without creative mode or OP permission",
                        serverPlayer.getName().getString());
                return;
            }

            ServerLevel level = serverPlayer.serverLevel();
            ArtilleryConfigOverrideSavedData data = ArtilleryConfigOverrideSavedData.get(level);

            // 清空所有覆盖
            data.clearAllOverrides();

            // 发送成功消息
            serverPlayer.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable("message.piranport.config_reset")
                            .withStyle(net.minecraft.ChatFormatting.GREEN)
            );

            PiranPort.LOGGER.info("Player {} reset all config overrides", serverPlayer.getName().getString());
        });
    }
}
