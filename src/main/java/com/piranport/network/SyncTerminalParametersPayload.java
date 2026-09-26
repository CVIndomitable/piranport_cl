package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.terminal.TerminalParameterCatalog;
import com.piranport.terminal.TerminalParameters;
import com.piranport.terminal.TerminalParametersSavedData;
import com.piranport.terminal.TerminalParameterSpec;
import com.piranport.config.ConfigToolPermissions;
import com.piranport.menu.DebugTerminalMenu;
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
import java.util.List;
import java.util.ArrayList;

/** 服务端权威完整快照；发送结果时同时校正客户端乐观显示。 */
public record SyncTerminalParametersPayload(long revision, Map<String, String> values,
                                            Map<String, String> overrides,
                                            List<TerminalParameterSpec> specs, String message)
        implements CustomPacketPayload {
    public static final Type<SyncTerminalParametersPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "sync_terminal_parameters"));
    private static final int MAX_ROWS = 4096;
    private static final int MAX_KEY = 128;
    private static final int MAX_VALUE = 64;
    private static final int MAX_MESSAGE = 256;

    public static final StreamCodec<ByteBuf, SyncTerminalParametersPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                ByteBufCodecs.VAR_LONG.encode(buffer, payload.revision());
                writeMap(buffer, payload.values());
                writeMap(buffer, payload.overrides());
                if (payload.specs().size() > MAX_ROWS) throw new IllegalArgumentException("Too many specs");
                ByteBufCodecs.VAR_INT.encode(buffer, payload.specs().size());
                for (TerminalParameterSpec spec : payload.specs()) {
                    ByteBufCodecs.stringUtf8(MAX_KEY).encode(buffer, spec.key());
                    ByteBufCodecs.stringUtf8(MAX_KEY).encode(buffer, spec.group());
                    ByteBufCodecs.stringUtf8(MAX_KEY).encode(buffer, spec.target());
                    ByteBufCodecs.stringUtf8(MAX_KEY).encode(buffer, spec.property());
                    ByteBufCodecs.VAR_INT.encode(buffer, spec.type().ordinal());
                    ByteBufCodecs.stringUtf8(MAX_VALUE).encode(buffer, spec.baseValue());
                    ByteBufCodecs.DOUBLE.encode(buffer, spec.min());
                    ByteBufCodecs.DOUBLE.encode(buffer, spec.max());
                }
                ByteBufCodecs.stringUtf8(MAX_MESSAGE).encode(buffer, boundedMessage(payload.message()));
            },
            buffer -> new SyncTerminalParametersPayload(
                    ByteBufCodecs.VAR_LONG.decode(buffer), readMap(buffer), readMap(buffer), readSpecs(buffer),
                    ByteBufCodecs.stringUtf8(MAX_MESSAGE).decode(buffer)));

    private static List<TerminalParameterSpec> readSpecs(ByteBuf buffer) {
        int count = ByteBufCodecs.VAR_INT.decode(buffer);
        if (count < 0 || count > MAX_ROWS) throw new DecoderException("Spec count out of range");
        List<TerminalParameterSpec> specs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String key = ByteBufCodecs.stringUtf8(MAX_KEY).decode(buffer);
            String group = ByteBufCodecs.stringUtf8(MAX_KEY).decode(buffer);
            String target = ByteBufCodecs.stringUtf8(MAX_KEY).decode(buffer);
            String property = ByteBufCodecs.stringUtf8(MAX_KEY).decode(buffer);
            int ordinal = ByteBufCodecs.VAR_INT.decode(buffer);
            if (ordinal < 0 || ordinal >= TerminalParameterSpec.ValueType.values().length)
                throw new DecoderException("Unknown parameter type");
            String base = ByteBufCodecs.stringUtf8(MAX_VALUE).decode(buffer);
            double min = ByteBufCodecs.DOUBLE.decode(buffer);
            double max = ByteBufCodecs.DOUBLE.decode(buffer);
            specs.add(new TerminalParameterSpec(key, group, target, property,
                    TerminalParameterSpec.ValueType.values()[ordinal], base, min, max));
        }
        return specs;
    }

    private static void writeMap(ByteBuf buffer, Map<String, String> values) {
        if (values.size() > MAX_ROWS) throw new IllegalArgumentException("Too many parameters");
        ByteBufCodecs.VAR_INT.encode(buffer, values.size());
        for (var entry : values.entrySet()) {
            ByteBufCodecs.stringUtf8(MAX_KEY).encode(buffer, entry.getKey());
            ByteBufCodecs.stringUtf8(MAX_VALUE).encode(buffer, entry.getValue());
        }
    }

    private static Map<String, String> readMap(ByteBuf buffer) {
        int count = ByteBufCodecs.VAR_INT.decode(buffer);
        if (count < 0 || count > MAX_ROWS) throw new DecoderException("Parameter count out of range");
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < count; i++) {
            result.put(ByteBufCodecs.stringUtf8(MAX_KEY).decode(buffer),
                    ByteBufCodecs.stringUtf8(MAX_VALUE).decode(buffer));
        }
        return result;
    }

    public static SyncTerminalParametersPayload from(TerminalParametersSavedData data, String message) {
        Map<String, String> values = new HashMap<>();
        Map<String, String> overrides = data.overrides();
        TerminalParameterCatalog.all().forEach(spec ->
                values.put(spec.key(), overrides.getOrDefault(spec.key(), spec.baseValue())));
        return new SyncTerminalParametersPayload(data.revision(), values, overrides,
                List.copyOf(TerminalParameterCatalog.all()), boundedMessage(message));
    }

    private static String boundedMessage(String message) {
        if (message == null) return "";
        return message.length() <= MAX_MESSAGE ? message : message.substring(0, MAX_MESSAGE - 1) + "…";
    }

    public static SyncTerminalParametersPayload denied() {
        return new SyncTerminalParametersPayload(0L, Map.of(), Map.of(), List.of(), "无权限或终端已关闭");
    }

    public static SyncTerminalParametersPayload effective(TerminalParametersSavedData data) {
        SyncTerminalParametersPayload full = from(data, "");
        return new SyncTerminalParametersPayload(full.revision(), full.values(), full.overrides(), List.of(), "");
    }

    public static void broadcast(ServerPlayer author, TerminalParametersSavedData data, String message) {
        for (ServerPlayer online : author.getServer().getPlayerList().getPlayers()) {
            boolean editing = ConfigToolPermissions.canUse(online)
                    && online.containerMenu instanceof DebugTerminalMenu menu && menu.stillValid(online);
            PacketDistributor.sendToPlayer(online, editing || online == author
                    ? from(data, online == author ? message : "") : effective(data));
        }
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncTerminalParametersPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> TerminalParameters.applyClient(
                payload.values(), payload.overrides(), payload.specs(), payload.revision(), payload.message()));
    }
}
