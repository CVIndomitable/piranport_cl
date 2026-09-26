package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import com.piranport.config.ConfigToolPermissions;
import com.piranport.menu.DebugTerminalMenu;
import com.piranport.terminal.TerminalParameterCsv;
import com.piranport.terminal.TerminalParameterCatalog;
import com.piranport.terminal.TerminalParametersSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 终端命令：刷新、重置、CSV 导入与导出；导入文件仅在服务器固定目录查找。 */
public record TerminalParameterActionPayload(String action, String filename) implements CustomPacketPayload {
    public static final Type<TerminalParameterActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "terminal_parameter_action"));
    public static final StreamCodec<ByteBuf, TerminalParameterActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32), TerminalParameterActionPayload::action,
            ByteBufCodecs.stringUtf8(128), TerminalParameterActionPayload::filename,
            TerminalParameterActionPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TerminalParameterActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!ConfigToolPermissions.canUse(player) || !(player.containerMenu instanceof DebugTerminalMenu menu)
                    || !menu.stillValid(player)) {
                PacketDistributor.sendToPlayer(player, SyncTerminalParametersPayload.denied());
                return;
            }
            TerminalParametersSavedData data = TerminalParametersSavedData.get(player.serverLevel());
            String result;
            try {
                result = switch (payload.action()) {
                    case "refresh" -> "已刷新";
                    case "save" -> "已保存";
                    case "reset" -> {
                        if (TerminalParameterCatalog.find(payload.filename()) == null)
                            throw new IllegalArgumentException("未知参数");
                        data.remove(payload.filename());
                        if (payload.filename().startsWith("core.")) reapplyCore(player);
                        yield "已重置参数";
                    }
                    case "reset_all" -> {
                        data.clearAll();
                        reapplyCore(player);
                        yield "已重置全部参数";
                    }
                    case "export" -> "已导出: " + TerminalParameterCsv.export(player.serverLevel(), data);
                    case "import" -> {
                        int count = TerminalParameterCsv.importFile(player.serverLevel(), data, payload.filename());
                        reapplyCore(player);
                        yield "已导入 " + count + " 项";
                    }
                    default -> throw new IllegalArgumentException("未知操作");
                };
            } catch (Exception e) {
                result = "操作失败: " + e.getMessage();
                PiranPort.LOGGER.warn("Terminal action {} failed for {}: {}",
                        payload.action(), player.getName().getString(), e.getMessage());
            }
            if (("reset_all".equals(payload.action()) || "reset".equals(payload.action())
                    || "import".equals(payload.action())) && !result.startsWith("操作失败")) {
                SyncTerminalParametersPayload.broadcast(player, data, result);
            } else {
                PacketDistributor.sendToPlayer(player, SyncTerminalParametersPayload.from(data, result));
            }
        });
    }

    private static void reapplyCore(ServerPlayer player) {
        for (ServerPlayer online : player.getServer().getPlayerList().getPlayers()) {
            TransformationManager.onTerminalCoreOverrideChanged(online);
        }
    }
}
