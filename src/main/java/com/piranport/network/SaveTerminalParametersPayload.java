package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import com.piranport.config.ConfigToolPermissions;
import com.piranport.menu.DebugTerminalMenu;
import com.piranport.terminal.TerminalParametersSavedData;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/** 保存全部草稿；服务端整批验证后原子替换覆盖表。 */
public record SaveTerminalParametersPayload(Map<String, String> edits) implements CustomPacketPayload {
    public static final Type<SaveTerminalParametersPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "save_terminal_parameters"));
    private static final int MAX_EDITS = 4096;
    public static final StreamCodec<ByteBuf, SaveTerminalParametersPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                if (payload.edits().size() > MAX_EDITS) throw new IllegalArgumentException("Too many edits");
                ByteBufCodecs.VAR_INT.encode(buffer, payload.edits().size());
                payload.edits().forEach((key, value) -> {
                    ByteBufCodecs.stringUtf8(128).encode(buffer, key);
                    ByteBufCodecs.stringUtf8(64).encode(buffer, value);
                });
            },
            buffer -> {
                int count = ByteBufCodecs.VAR_INT.decode(buffer);
                if (count < 0 || count > MAX_EDITS) throw new DecoderException("Edit count out of range");
                Map<String, String> edits = new HashMap<>();
                for (int i = 0; i < count; i++) {
                    String key = ByteBufCodecs.stringUtf8(128).decode(buffer);
                    String value = ByteBufCodecs.stringUtf8(64).decode(buffer);
                    if (edits.putIfAbsent(key, value) != null)
                        throw new DecoderException("Duplicate parameter key");
                }
                return new SaveTerminalParametersPayload(edits);
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SaveTerminalParametersPayload payload, IPayloadContext context) {
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
                Map<String, String> proposed = new HashMap<>(data.overrides());
                proposed.putAll(payload.edits());
                changed = data.replace(proposed);
                if (payload.edits().keySet().stream().anyMatch(key -> key.startsWith("core."))) {
                    for (ServerPlayer online : player.getServer().getPlayerList().getPlayers()) {
                        TransformationManager.onTerminalCoreOverrideChanged(online);
                    }
                }
                result = "已保存 " + payload.edits().size() + " 项";
            } catch (RuntimeException e) {
                result = "保存失败: " + e.getMessage();
                PiranPort.LOGGER.warn("Rejected terminal batch from {}: {}",
                        player.getName().getString(), e.getMessage());
            }
            if (changed) SyncTerminalParametersPayload.broadcast(player, data, result);
            else PacketDistributor.sendToPlayer(player, SyncTerminalParametersPayload.from(data, result));
        });
    }
}
