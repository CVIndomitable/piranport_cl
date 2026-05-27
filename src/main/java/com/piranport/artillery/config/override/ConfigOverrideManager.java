package com.piranport.artillery.config.override;

import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.ArtilleryConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 配置覆盖管理器 - 运行时拦截层
 *
 * <p>在数据读取点应用存档级别的覆盖值，不修改原始配置系统。
 * <p>线程模型: 服务端主线程（服务端），客户端主线程（客户端缓存）
 * <p>使用方式: 替换所有 {@code ArtilleryConfig.get()} 调用为 {@code ConfigOverrideManager.getCannonData()}
 */
public class ConfigOverrideManager {

    private ConfigOverrideManager() {
        // 工具类，禁止实例化
    }

    // ==================== 火炮数据覆盖 ====================

    /**
     * 获取应用覆盖后的火炮数据
     *
     * @param name 火炮注册ID（如 "medium_gun"）
     * @param level 世界实例（null时返回原始值）
     * @return 应用覆盖后的火炮数据
     */
    public static ArtilleryCannonData getCannonData(String name, @Nullable Level level) {
        // 获取原始数据
        ArtilleryCannonData original = ArtilleryConfig.get(name);

        // null时直接返回原始值
        if (level == null) {
            return original;
        }

        // 客户端：从缓存读取（仅用于GUI显示，实际战斗逻辑在服务端）
        if (level.isClientSide()) {
            return applyCannonOverridesFromCache(original, name);
        }

        // 服务端：应用覆盖
        ServerLevel serverLevel = (ServerLevel) level;
        ArtilleryConfigOverrideSavedData overrides = ArtilleryConfigOverrideSavedData.get(serverLevel);

        return applyCannonOverrides(original, name, overrides);
    }

    /**
     * 应用火炮覆盖（重新构造record实例）- 服务端版本
     */
    private static ArtilleryCannonData applyCannonOverrides(
            ArtilleryCannonData original,
            String name,
            ArtilleryConfigOverrideSavedData overrides) {

        // 检查是否有覆盖
        if (!overrides.getOverriddenCannonNames().contains(name)) {
            return original;
        }

        // 逐字段检查覆盖，未覆盖则使用原始值
        float damage = overrides.getCannonOverride(name, "damage")
                .map(v -> ((Number) v).floatValue())
                .orElse(original.damage());

        int reloadTime = overrides.getCannonOverride(name, "reloadTime")
                .map(v -> ((Number) v).intValue())
                .orElse(original.reloadTime());

        float initialSpeed = overrides.getCannonOverride(name, "initialSpeed")
                .map(v -> ((Number) v).floatValue())
                .orElse(original.initialSpeed());

        float dragCoeff = overrides.getCannonOverride(name, "dragCoeff")
                .map(v -> ((Number) v).floatValue())
                .orElse(original.dragCoeff());

        float gravity = overrides.getCannonOverride(name, "gravity")
                .map(v -> ((Number) v).floatValue())
                .orElse(original.gravity());

        float explosionPower = overrides.getCannonOverride(name, "explosionPower")
                .map(v -> ((Number) v).floatValue())
                .orElse(original.explosionPower());

        float dispersion = overrides.getCannonOverride(name, "dispersion")
                .map(v -> ((Number) v).floatValue())
                .orElse(original.dispersion());

        // 重新构造实例（record不可变）
        return new ArtilleryCannonData(
                original.caliber(),
                original.barrels(),
                damage,
                reloadTime,
                original.durability(),
                original.scopeZoom(),
                original.muzzles(),
                initialSpeed,
                dragCoeff,
                gravity,
                explosionPower,
                dispersion
        );
    }

    /**
     * 应用火炮覆盖（客户端缓存版本）- 仅用于GUI显示
     */
    private static ArtilleryCannonData applyCannonOverridesFromCache(
            ArtilleryCannonData original,
            String name) {

        // 尝试从客户端缓存读取
        float damage = ClientConfigCache.getCannonOverride(name, "damage")
                .map(Float::parseFloat)
                .orElse(original.damage());

        int reloadTime = ClientConfigCache.getCannonOverride(name, "reloadTime")
                .map(Integer::parseInt)
                .orElse(original.reloadTime());

        float initialSpeed = ClientConfigCache.getCannonOverride(name, "initialSpeed")
                .map(Float::parseFloat)
                .orElse(original.initialSpeed());

        float dragCoeff = ClientConfigCache.getCannonOverride(name, "dragCoeff")
                .map(Float::parseFloat)
                .orElse(original.dragCoeff());

        float gravity = ClientConfigCache.getCannonOverride(name, "gravity")
                .map(Float::parseFloat)
                .orElse(original.gravity());

        float explosionPower = ClientConfigCache.getCannonOverride(name, "explosionPower")
                .map(Float::parseFloat)
                .orElse(original.explosionPower());

        float dispersion = ClientConfigCache.getCannonOverride(name, "dispersion")
                .map(Float::parseFloat)
                .orElse(original.dispersion());

        // 重新构造实例（record不可变）
        return new ArtilleryCannonData(
                original.caliber(),
                original.barrels(),
                damage,
                reloadTime,
                original.durability(),
                original.scopeZoom(),
                original.muzzles(),
                initialSpeed,
                dragCoeff,
                gravity,
                explosionPower,
                dispersion
        );
    }

    // ==================== 弹药配置覆盖 ====================

    /**
     * 获取应用覆盖后的double类型弹药配置
     *
     * @param key 配置键（如 "HE_ARMOR_PENETRATION"）
     * @param defaultValue 原始默认值
     * @param level 世界实例
     * @return 应用覆盖后的值
     */
    public static double getProjectileConfigDouble(String key, double defaultValue, @Nullable Level level) {
        if (level == null || level.isClientSide()) {
            return defaultValue;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        ArtilleryConfigOverrideSavedData overrides = ArtilleryConfigOverrideSavedData.get(serverLevel);

        return overrides.getProjectileOverride(key)
                .map(v -> ((Number) v).doubleValue())
                .orElse(defaultValue);
    }

    /**
     * 获取应用覆盖后的boolean类型弹药配置
     */
    public static boolean getProjectileConfigBoolean(String key, boolean defaultValue, @Nullable Level level) {
        if (level == null || level.isClientSide()) {
            return defaultValue;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        ArtilleryConfigOverrideSavedData overrides = ArtilleryConfigOverrideSavedData.get(serverLevel);

        return overrides.getProjectileOverride(key)
                .map(v -> (Boolean) v)
                .orElse(defaultValue);
    }

    /**
     * 获取应用覆盖后的int类型弹药配置
     */
    public static int getProjectileConfigInt(String key, int defaultValue, @Nullable Level level) {
        if (level == null || level.isClientSide()) {
            return defaultValue;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        ArtilleryConfigOverrideSavedData overrides = ArtilleryConfigOverrideSavedData.get(serverLevel);

        return overrides.getProjectileOverride(key)
                .map(v -> ((Number) v).intValue())
                .orElse(defaultValue);
    }

    // ==================== 数值验证 ====================

    /**
     * 验证并限制数值范围（防止非法值导致崩溃）
     *
     * @param field 字段名
     * @param value 原始值
     * @return 验证后的值
     */
    public static Object validateValue(String field, Object value) {
        if (!(value instanceof Number num)) {
            return value;
        }

        return switch (field) {
            case "damage" -> {
                float f = num.floatValue();
                yield Math.max(0.1f, Math.min(1000f, f));
            }
            case "reloadTime" -> {
                int i = num.intValue();
                yield Math.max(1, Math.min(6000, i));
            }
            case "initialSpeed" -> {
                float f = num.floatValue();
                yield Math.max(0.1f, Math.min(50f, f));
            }
            case "dragCoeff" -> {
                float f = num.floatValue();
                yield Math.max(0.0f, Math.min(1.0f, f));
            }
            case "gravity" -> {
                float f = num.floatValue();
                yield Math.max(0.1f, Math.min(100f, f));
            }
            case "explosionPower" -> {
                float f = num.floatValue();
                yield Math.max(0.0f, Math.min(20f, f));
            }
            case "dispersion" -> {
                float f = num.floatValue();
                yield Math.max(0.0f, Math.min(10f, f));
            }
            default -> value;
        };
    }

    /**
     * 验证弹药配置值
     */
    public static Object validateProjectileValue(String key, Object value) {
        if (!(value instanceof Number num)) {
            return value;
        }

        return switch (key) {
            case "HE_ARMOR_PENETRATION", "AP_ARMOR_IGNORE" -> {
                double d = num.doubleValue();
                yield Math.max(0.0, Math.min(1.0, d));
            }
            case "AP_DAMAGE_MULTIPLIER" -> {
                double d = num.doubleValue();
                yield Math.max(0.1, Math.min(10.0, d));
            }
            case "UNDERWATER_EXPLOSION_MULTIPLIER" -> {
                double d = num.doubleValue();
                yield Math.max(0.0, Math.min(2.0, d));
            }
            default -> value;
        };
    }
}
