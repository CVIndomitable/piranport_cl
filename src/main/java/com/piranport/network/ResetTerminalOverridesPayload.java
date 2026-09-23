package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.config.ConfigToolPermissions;
import com.piranport.terminal.TerminalOverridesSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 重置调试终端覆盖网络包（客户端 → 服务端）。
 *
 * <p>支持两种粒度：整个域（鱼雷全部 / 核心全部）或单个键（某一型号）。
 * 终端界面上「重置全部」与每行右侧的清零按钮走同一个包，避免再维护一条通道。
 */
public record ResetTerminalOverridesPayload(
        String category,  // "torpedo" / "core" / "all"
        String key        // 空串表示整个 category；非空表示只重置这一个键
) implements CustomPacketPayload {

    public static final Type<ResetTerminalOverridesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "reset_terminal_overrides"));

    private static final int MAX_KEY_LENGTH = 128;

    public static final StreamCodec<ByteBuf, ResetTerminalOverridesPayload> STREAM_CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.stringUtf8(MAX_KEY_LENGTH), ResetTerminalOverridesPayload::category,
            net.minecraft.network.codec.ByteBufCodecs.stringUtf8(MAX_KEY_LENGTH), ResetTerminalOverridesPayload::key,
            ResetTerminalOverridesPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResetTerminalOverridesPayload payload, IPayloadContext context) {
        com.piranport.debug.PiranPortDebug.runPayload("ResetTerminalOverrides", context.player(), () -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            if (!ConfigToolPermissions.canUse(serverPlayer)) {
                PiranPort.LOGGER.warn("Player {} tried to reset terminal overrides without admin permission",
                        serverPlayer.getName().getString());
                // 与 Update 通道对齐：拒绝必须有回执，否则界面显示"已重置"而存档没动。
                reject(serverPlayer, "无管理员权限");
                return;
            }

            ServerLevel level = serverPlayer.serverLevel();
            TerminalOverridesSavedData data = TerminalOverridesSavedData.get(level);
            boolean touchedCores = false;

            String key = payload.key() == null ? "" : payload.key();

            switch (payload.category()) {
                case "all" -> {
                    touchedCores = true;  // 可能清掉了核心覆盖，下面无条件重放一次
                    data.clearAll();
                }
                case UpdateTerminalOverridePayload.CATEGORY_TORPEDO -> {
                    if (key.isEmpty()) {
                        for (String modelKey : data.getAllTorpedoDeltas().keySet()) {
                            data.removeTorpedoSpeedDelta(modelKey);
                        }
                    } else {
                        data.removeTorpedoSpeedDelta(key);
                    }
                }
                case UpdateTerminalOverridePayload.CATEGORY_CORE -> {
                    touchedCores = true;
                    if (key.isEmpty()) {
                        for (String coreKey : data.getAllCoreDeltas().keySet()) {
                            data.removeCoreSpeedDelta(coreKey);
                        }
                    } else {
                        data.removeCoreSpeedDelta(key);
                    }
                }
                default -> {
                    PiranPort.LOGGER.warn("Unknown reset category: {}", payload.category());
                    reject(serverPlayer, "未知分类: " + payload.category());
                    return;
                }
            }

            // 核心覆盖被移除时属性不会自动回退，必须重放；重放幂等，宁可多调一次。
            if (touchedCores) {
                com.piranport.combat.TransformationManager.onTerminalCoreOverrideChanged(serverPlayer);
            }

            // 立刻回推最新快照，让界面显示与实际存档保持一致（而不是等下次打开）。
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                    serverPlayer, SyncTerminalOverridesPayload.from(data));

            PiranPort.LOGGER.info("Player {} reset terminal overrides [{}/{}]",
                    serverPlayer.getName().getString(), payload.category(), key);
        });
    }

    /** 拒绝回执：界面已乐观清空了输入框，必须告诉玩家这次没生效。 */
    private static void reject(ServerPlayer player, String reason) {
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "§c[PP] 重置未生效：" + reason), false);
    }
}
