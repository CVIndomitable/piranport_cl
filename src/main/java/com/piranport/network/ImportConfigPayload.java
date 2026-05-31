package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.artillery.config.override.ConfigCSVImporter;
import com.piranport.config.ConfigToolPermissions;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
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
 * 导入配置CSV网络包（客户端 → 服务端）
 *
 * <p>触发服务端从CSV文件导入火炮和弹药配置。
 */
public record ImportConfigPayload(String cannonFilename, String projectileFilename) implements CustomPacketPayload {

    public static final Type<ImportConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "import_config"));

    public static final StreamCodec<ByteBuf, ImportConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ImportConfigPayload::cannonFilename,
            ByteBufCodecs.STRING_UTF8,
            ImportConfigPayload::projectileFilename,
            ImportConfigPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 验证文件名是否安全（防止路径遍历攻击）
     */
    private static boolean isValidFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        // P0修复: 防止路径遍历漏洞
        return !filename.contains("..")
            && !filename.contains("/")
            && !filename.contains("\\")
            && filename.length() < 256;
    }

    /**
     * 处理网络包（服务端）
     */
    public static void handle(ImportConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            if (!ConfigToolPermissions.canUse(serverPlayer)) {
                PiranPort.LOGGER.warn("Player {} tried to import config without admin permission",
                        serverPlayer.getName().getString());
                return;
            }

            // P0修复: 验证文件名安全性
            if (!isValidFilename(payload.cannonFilename) && !payload.cannonFilename.isEmpty()) {
                PiranPort.LOGGER.warn("Player {} tried to import config with invalid cannon filename: {}",
                        serverPlayer.getName().getString(), payload.cannonFilename);
                serverPlayer.sendSystemMessage(
                        Component.literal("Invalid cannon filename").withStyle(ChatFormatting.RED)
                );
                return;
            }
            if (!isValidFilename(payload.projectileFilename) && !payload.projectileFilename.isEmpty()) {
                PiranPort.LOGGER.warn("Player {} tried to import config with invalid projectile filename: {}",
                        serverPlayer.getName().getString(), payload.projectileFilename);
                serverPlayer.sendSystemMessage(
                        Component.literal("Invalid projectile filename").withStyle(ChatFormatting.RED)
                );
                return;
            }

            ServerLevel level = serverPlayer.serverLevel();

            try {
                // 导入目录
                Path importDir = level.getServer().getServerDirectory()
                        .resolve("config/piranport/exports");

                if (!Files.exists(importDir)) {
                    serverPlayer.sendSystemMessage(
                            Component.translatable("message.piranport.import_dir_not_found",
                                    importDir.toString()).withStyle(ChatFormatting.RED)
                    );
                    return;
                }

                int totalSuccess = 0;
                int totalFail = 0;

                // 导入火炮配置
                if (!payload.cannonFilename.isEmpty()) {
                    Path cannonFile = importDir.resolve(payload.cannonFilename);
                    if (Files.exists(cannonFile)) {
                        ConfigCSVImporter.ImportResult result = ConfigCSVImporter.importCannonsFromCSV(level, cannonFile);
                        totalSuccess += result.successCount();
                        totalFail += result.failCount();

                        serverPlayer.sendSystemMessage(
                                Component.translatable("message.piranport.cannon_imported",
                                        result.successCount(), result.failCount()).withStyle(ChatFormatting.GREEN)
                        );
                    } else {
                        serverPlayer.sendSystemMessage(
                                Component.translatable("message.piranport.file_not_found",
                                        payload.cannonFilename).withStyle(ChatFormatting.RED)
                        );
                    }
                }

                // 导入弹药配置
                if (!payload.projectileFilename.isEmpty()) {
                    Path projectileFile = importDir.resolve(payload.projectileFilename);
                    if (Files.exists(projectileFile)) {
                        ConfigCSVImporter.ImportResult result = ConfigCSVImporter.importProjectilesFromCSV(level, projectileFile);
                        totalSuccess += result.successCount();
                        totalFail += result.failCount();

                        serverPlayer.sendSystemMessage(
                                Component.translatable("message.piranport.projectile_imported",
                                        result.successCount(), result.failCount()).withStyle(ChatFormatting.GREEN)
                        );
                    } else {
                        serverPlayer.sendSystemMessage(
                                Component.translatable("message.piranport.file_not_found",
                                        payload.projectileFilename).withStyle(ChatFormatting.RED)
                        );
                    }
                }

                // 发送总结消息
                if (totalSuccess > 0 || totalFail > 0) {
                    serverPlayer.sendSystemMessage(
                            Component.translatable("message.piranport.config_imported_summary",
                                    totalSuccess, totalFail).withStyle(ChatFormatting.AQUA)
                    );
                }

                PiranPort.LOGGER.info("Player {} imported config: {} success, {} failed",
                        serverPlayer.getName().getString(), totalSuccess, totalFail);

            } catch (IOException e) {
                // 发送失败消息
                serverPlayer.sendSystemMessage(
                        Component.translatable("message.piranport.config_import_failed",
                                e.getMessage()).withStyle(ChatFormatting.RED)
                );

                PiranPort.LOGGER.error("Failed to import config for player {}",
                        serverPlayer.getName().getString(), e);
            }
        });
    }
}
