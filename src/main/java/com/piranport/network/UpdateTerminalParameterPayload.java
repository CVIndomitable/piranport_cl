package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import com.piranport.config.ConfigToolPermissions;
import com.piranport.menu.DebugTerminalMenu;
import com.piranport.terminal.TerminalParametersSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 单项显式确认；只接受目录白名单，结果一律回权威快照。 */
public record UpdateTerminalParameterPayload(String key, String value) implements CustomPacketPayload {
    public static final Type<UpdateTerminalParameterPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "update_terminal_parameter"));
    public static final StreamCodec<ByteBuf, UpdateTerminalParameterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128), UpdateTerminalParameterPayload::key,
            ByteBufCodecs.stringUtf8(64), UpdateTerminalParameterPayload::value,
            UpdateTerminalParameterPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(UpdateTerminalParameterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!ConfigToolPermissions.canUse(player) || !(player.containerMenu instanceof DebugTerminalMenu menu)
                    || !menu.stillValid(player)) {
                PacketDistributor.sendToPlayer(player, SyncTerminalParametersPayload.denied());
                return;
            }
            TerminalParametersSavedData data = TerminalParametersSavedData.get(player.serverLevel());
            String result;
            boolean changed = false;
            try {
                changed = data.put(payload.key(), payload.value());
                if (payload.key().startsWith("core.")) {
                    for (ServerPlayer online : player.getServer().getPlayerList().getPlayers()) {
                        TransformationManager.onTerminalCoreOverrideChanged(online);
                    }
                }
                result = "已保存";
            } catch (RuntimeException e) {
                result = "保存失败: " + e.getMessage();
                PiranPort.LOGGER.warn("Rejected terminal parameter {} from {}: {}",
                        payload.key(), player.getName().getString(), e.getMessage());
            }
            if (changed) SyncTerminalParametersPayload.broadcast(player, data, result);
            else PacketDistributor.sendToPlayer(player, SyncTerminalParametersPayload.from(data, result));
        });
    }
}
