package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.artillery.config.override.ConfigCSVExporter;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 导出配置CSV网络包（客户端 → 服务端）
 *
 * <p>触发服务端导出火炮和弹药配置到CSV文件。
 */
public record ExportConfigPayload() implements CustomPacketPayload {

    public static final Type<ExportConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "export_config"));

    public static final StreamCodec<ByteBuf, ExportConfigPayload> STREAM_CODEC = StreamCodec.unit(new ExportConfigPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理网络包（服务端）
     */
    public static void handle(ExportConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            // 仅创造模式可用
            if (!serverPlayer.isCreative()) {
                PiranPort.LOGGER.warn("Player {} tried to export config without creative mode", serverPlayer.getName().getString());
                return;
            }

            ServerLevel level = serverPlayer.serverLevel();

            try {
                // 创建导出目录
                Path exportDir = level.getServer().getServerDirectory()
                        .resolve("config/piranport/exports");
                Files.createDirectories(exportDir);

                // 生成带时间戳的文件名
                String cannonFilename = ConfigCSVExporter.generateTimestampedFilename("cannons", "csv");
                String projectileFilename = ConfigCSVExporter.generateTimestampedFilename("projectiles", "csv");

                Path cannonFile = exportDir.resolve(cannonFilename);
                Path projectileFile = exportDir.resolve(projectileFilename);

                // 导出火炮配置
                ConfigCSVExporter.exportCannonsToCSV(level, cannonFile);

                // 导出弹药配置
                ConfigCSVExporter.exportProjectilesToCSV(level, projectileFile);

                // 发送成功消息
                serverPlayer.sendSystemMessage(
                        Component.translatable("message.piranport.config_exported",
                                exportDir.toString()).withStyle(ChatFormatting.GREEN)
                );

                PiranPort.LOGGER.info("Player {} exported config to {}", serverPlayer.getName().getString(), exportDir);

            } catch (IOException e) {
                // 发送失败消息
                serverPlayer.sendSystemMessage(
                        Component.translatable("message.piranport.config_export_failed",
                                e.getMessage()).withStyle(ChatFormatting.RED)
                );

                PiranPort.LOGGER.error("Failed to export config for player {}", serverPlayer.getName().getString(), e);
            }
        });
    }
}
