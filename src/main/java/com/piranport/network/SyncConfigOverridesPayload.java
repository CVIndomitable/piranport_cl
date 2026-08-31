package com.piranport.network;

import com.piranport.PiranPort;
import io.netty.handler.codec.DecoderException;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/**
 * 同步配置覆盖网络包（服务端 → 客户端）
 *
 * <p>GUI打开时发送，将当前存档的所有覆盖值同步到客户端显示。
 */
public record SyncConfigOverridesPayload(
        Map<String, Map<String, String>> cannonOverrides,  // cannonName → (field → value)
        Map<String, String> projectileOverrides            // configKey → value
) implements CustomPacketPayload {

    public static final Type<SyncConfigOverridesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "sync_config_overrides"));

    private static final int MAX_STRING_LENGTH = 256;
    private static final int MAX_CANNONS = 200;
    private static final int MAX_FIELDS_PER_CANNON = 50;
    private static final int MAX_PROJECTILES = 100;

    // 自定义StreamCodec用于Map序列化
    public static final StreamCodec<ByteBuf, SyncConfigOverridesPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(ByteBuf buffer, SyncConfigOverridesPayload payload) {
            // 编码火炮覆盖
            ByteBufCodecs.VAR_INT.encode(buffer, payload.cannonOverrides.size());
            for (Map.Entry<String, Map<String, String>> entry : payload.cannonOverrides.entrySet()) {
                ByteBufCodecs.STRING_UTF8.encode(buffer, entry.getKey());
                Map<String, String> fields = entry.getValue();
                ByteBufCodecs.VAR_INT.encode(buffer, fields.size());
                for (Map.Entry<String, String> fieldEntry : fields.entrySet()) {
                    ByteBufCodecs.STRING_UTF8.encode(buffer, fieldEntry.getKey());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, fieldEntry.getValue());
                }
            }

            // 编码弹药覆盖
            ByteBufCodecs.VAR_INT.encode(buffer, payload.projectileOverrides.size());
            for (Map.Entry<String, String> entry : payload.projectileOverrides.entrySet()) {
                ByteBufCodecs.STRING_UTF8.encode(buffer, entry.getKey());
                ByteBufCodecs.STRING_UTF8.encode(buffer, entry.getValue());
            }
        }

        @Override
        public SyncConfigOverridesPayload decode(ByteBuf buffer) {
            // 解码火炮覆盖
            int cannonCount = ByteBufCodecs.VAR_INT.decode(buffer);
            if (cannonCount < 0 || cannonCount > MAX_CANNONS) {
                throw new DecoderException("cannon count out of range: " + cannonCount);
            }
            Map<String, Map<String, String>> cannonOverrides = new HashMap<>();
            for (int i = 0; i < cannonCount; i++) {
                String cannonName = ByteBufCodecs.stringUtf8(MAX_STRING_LENGTH).decode(buffer);
                int fieldCount = ByteBufCodecs.VAR_INT.decode(buffer);
                if (fieldCount < 0 || fieldCount > MAX_FIELDS_PER_CANNON) {
                    throw new DecoderException("cannon field count out of range: " + fieldCount);
                }
                Map<String, String> fields = new HashMap<>();
                for (int j = 0; j < fieldCount; j++) {
                    String fieldName = ByteBufCodecs.stringUtf8(MAX_STRING_LENGTH).decode(buffer);
                    String value = ByteBufCodecs.stringUtf8(MAX_STRING_LENGTH).decode(buffer);
                    fields.put(fieldName, value);
                }
                cannonOverrides.put(cannonName, fields);
            }

            // 解码弹药覆盖
            int projectileCount = ByteBufCodecs.VAR_INT.decode(buffer);
            if (projectileCount < 0 || projectileCount > MAX_PROJECTILES) {
                throw new DecoderException("projectile override count out of range: " + projectileCount);
            }
            Map<String, String> projectileOverrides = new HashMap<>();
            for (int i = 0; i < projectileCount; i++) {
                String key = ByteBufCodecs.stringUtf8(MAX_STRING_LENGTH).decode(buffer);
                String value = ByteBufCodecs.stringUtf8(MAX_STRING_LENGTH).decode(buffer);
                projectileOverrides.put(key, value);
            }

            return new SyncConfigOverridesPayload(cannonOverrides, projectileOverrides);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理网络包（客户端）
     */
    public static void handle(SyncConfigOverridesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 客户端接收到同步数据
            // 数据会被存储在客户端的临时缓存中，供GUI显示使用
            // 实际的配置应用仍然在服务端进行
            PiranPort.LOGGER.debug("Received config overrides: {} cannons, {} projectiles",
                    payload.cannonOverrides.size(), payload.projectileOverrides.size());

            // 存储到客户端缓存
            com.piranport.artillery.config.override.ClientConfigCache.updateCache(
                    payload.cannonOverrides,
                    payload.projectileOverrides
            );
        });
    }
}
