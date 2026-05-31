package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.artillery.config.override.ArtilleryConfigOverrideSavedData;
import com.piranport.artillery.config.override.ConfigOverrideManager;
import com.piranport.config.ConfigToolPermissions;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 更新配置覆盖网络包（客户端 → 服务端）
 *
 * <p>用于GUI中修改单个配置项时发送到服务端。
 */
public record UpdateConfigOverridePayload(
        String category,  // "cannon" 或 "projectile"
        String key,       // 火炮名称或配置键
        String field,     // 字段名（火炮专用）
        String value      // 字符串表示的值
) implements CustomPacketPayload {

    public static final Type<UpdateConfigOverridePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "update_config_override"));

    // P0修复: 限制字符串长度防止内存耗尽
    private static final int MAX_STRING_LENGTH = 256;

    public static final StreamCodec<ByteBuf, UpdateConfigOverridePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_STRING_LENGTH), UpdateConfigOverridePayload::category,
            ByteBufCodecs.stringUtf8(MAX_STRING_LENGTH), UpdateConfigOverridePayload::key,
            ByteBufCodecs.stringUtf8(MAX_STRING_LENGTH), UpdateConfigOverridePayload::field,
            ByteBufCodecs.stringUtf8(MAX_STRING_LENGTH), UpdateConfigOverridePayload::value,
            UpdateConfigOverridePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理网络包（服务端）
     */
    public static void handle(UpdateConfigOverridePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            if (!ConfigToolPermissions.canUse(serverPlayer)) {
                PiranPort.LOGGER.warn("Player {} tried to update config without admin permission",
                        serverPlayer.getName().getString());
                return;
            }

            ServerLevel level = serverPlayer.serverLevel();
            ArtilleryConfigOverrideSavedData data = ArtilleryConfigOverrideSavedData.get(level);

            try {
                if ("cannon".equals(payload.category)) {
                    // 解析并验证值
                    Object parsedValue = parseValue(payload.value, payload.field);
                    Object validatedValue = ConfigOverrideManager.validateValue(payload.field, parsedValue);

                    data.setCannonOverride(payload.key, payload.field, validatedValue);
                    PiranPort.LOGGER.info("Player {} updated cannon {} field {} to {}",
                            serverPlayer.getName().getString(), payload.key, payload.field, validatedValue);

                } else if ("projectile".equals(payload.category)) {
                    // 解析并验证值
                    Object parsedValue = parseProjectileValue(payload.value, payload.key);
                    Object validatedValue = ConfigOverrideManager.validateProjectileValue(payload.key, parsedValue);

                    data.setProjectileOverride(payload.key, validatedValue);
                    PiranPort.LOGGER.info("Player {} updated projectile config {} to {}",
                            serverPlayer.getName().getString(), payload.key, validatedValue);
                }

            } catch (Exception e) {
                PiranPort.LOGGER.error("Failed to parse config value: {}", payload.value, e);
            }
        });
    }

    /**
     * 解析火炮字段值
     */
    private static Object parseValue(String valueStr, String field) {
        return switch (field) {
            case "damage", "initialSpeed", "dragCoeff", "gravity", "explosionPower", "dispersion",
                 "projectileWeight", "salvoInterval", "verticalSpread", "horizontalSpread",
                 "maxElevation", "minElevation", "turretSpeed", "scopeZoom" ->
                    Float.parseFloat(valueStr);
            case "caliber", "barrels", "reloadTime", "durability", "fireCooldown", "salvoCount" ->
                    Integer.parseInt(valueStr);
            default ->
                    valueStr;
        };
    }

    /**
     * 解析弹药配置值
     *
     * <p>支持类型: boolean, double
     * <p>注意: 当前弹药配置不包含int类型，如需扩展请在此添加分支
     */
    private static Object parseProjectileValue(String valueStr, String key) {
        // 根据配置键判断类型
        if (key.equals("HE_DAMAGE_FALLOFF") || key.equals("UNDERWATER_EXPLODE")) {
            return Boolean.parseBoolean(valueStr);
        } else {
            // 默认为double类型（HE_ARMOR_PENETRATION, AP_DAMAGE_MULTIPLIER等）
            return Double.parseDouble(valueStr);
        }
    }
}
