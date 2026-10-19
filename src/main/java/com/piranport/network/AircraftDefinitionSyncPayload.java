package com.piranport.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.piranport.PiranPort;
import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.AircraftDefinitionService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * S2C: 同步飞机定义。
 *
 * <p>客户端的资源管理器只暴露 {@code assets/}，看不到 {@code data/piranport/aircraft/*.json}，
 * 因此定义由服务端在登录/数据重载后统一下发。客户端只把结果写入
 * {@link AircraftDefinitionService}，用于物品 tooltip、调试终端和只读查询；
 * 实体渲染使用同步过来的 {@code visual_id}，不依赖这份定义。
 */
public record AircraftDefinitionSyncPayload(List<Entry> definitions) implements CustomPacketPayload {
    private static final int MAX_DEFINITIONS = 256;

    public static final Type<AircraftDefinitionSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "aircraft_definitions"));

    public static final StreamCodec<ByteBuf, AircraftDefinitionSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                int size = Math.min(payload.definitions.size(), MAX_DEFINITIONS);
                ByteBufCodecs.VAR_INT.encode(buffer, size);
                for (int i = 0; i < size; i++) {
                    Entry entry = payload.definitions.get(i);
                    ByteBufCodecs.STRING_UTF8.encode(buffer, entry.id());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, entry.json());
                }
            },
            buffer -> {
                int size = ByteBufCodecs.VAR_INT.decode(buffer);
                if (size < 0 || size > MAX_DEFINITIONS) size = 0;
                List<Entry> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    entries.add(new Entry(ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer)));
                }
                return new AircraftDefinitionSyncPayload(List.copyOf(entries));
            });

    public record Entry(String id, String json) {}

    public static AircraftDefinitionSyncPayload fromDefinitions(Collection<AircraftDefinition> definitions) {
        List<Entry> entries = new ArrayList<>(definitions.size());
        for (AircraftDefinition definition : definitions) {
            String json = AircraftDefinition.JsonDefinition.CODEC
                    .encodeStart(JsonOps.INSTANCE, AircraftDefinition.JsonDefinition.from(definition))
                    .getOrThrow(message -> new IllegalStateException(
                            "failed to encode aircraft definition " + definition.id() + ": " + message))
                    .toString();
            entries.add(new Entry(definition.id().toString(), json));
        }
        return new AircraftDefinitionSyncPayload(List.copyOf(entries));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AircraftDefinitionSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Map<ResourceLocation, AircraftDefinition> definitions = new HashMap<>();
            for (Entry entry : payload.definitions()) {
                ResourceLocation id = ResourceLocation.tryParse(entry.id());
                if (id == null) continue;
                try {
                    JsonElement json = JsonParser.parseString(entry.json());
                    AircraftDefinition.JsonDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                            .result()
                            .ifPresent(definition -> definitions.put(id, definition.withId(id)));
                } catch (RuntimeException exception) {
                    PiranPort.LOGGER.error("Failed to decode synced aircraft definition {}", id,
                            exception);
                }
            }
            AircraftDefinitionService.replaceAll(definitions);
        });
    }
}
