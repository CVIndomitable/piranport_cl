package com.piranport.artillery.config.override;

import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.ArtilleryConfig;
import com.piranport.artillery.config.MuzzlePos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

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
        int caliber = overrides.getCannonOverride(name, "caliber")
                .map(v -> v instanceof Number n ? n.intValue() : null)
                .orElse(original.caliber());

        int barrels = overrides.getCannonOverride(name, "barrels")
                .map(v -> v instanceof Number n ? n.intValue() : null)
                .orElse(original.barrels());

        float damage = overrides.getCannonOverride(name, "damage")
                .map(v -> v instanceof Number n ? n.floatValue() : null)
                .orElse(original.damage());

        int reloadTime = overrides.getCannonOverride(name, "reloadTime")
                .map(v -> v instanceof Number n ? n.intValue() : null)
                .orElse(original.reloadTime());

        int durability = overrides.getCannonOverride(name, "durability")
                .map(v -> v instanceof Number n ? n.intValue() : null)
                .orElse(original.durability());

        float scopeZoom = overrides.getCannonOverride(name, "scopeZoom")
                .map(v -> v instanceof Number n ? n.floatValue() : null)
                .orElse(original.scopeZoom());

        float initialSpeed = overrides.getCannonOverride(name, "initialSpeed")
                .map(v -> v instanceof Number n ? n.floatValue() : null)
                .orElse(original.initialSpeed());

        float dragCoeff = overrides.getCannonOverride(name, "dragCoeff")
                .map(v -> v instanceof Number n ? n.floatValue() : null)
                .orElse(original.dragCoeff());

        float gravity = overrides.getCannonOverride(name, "gravity")
                .map(v -> v instanceof Number n ? n.floatValue() : null)
                .orElse(original.gravity());

        float explosionPower = overrides.getCannonOverride(name, "explosionPower")
                .map(v -> v instanceof Number n ? n.floatValue() : null)
                .orElse(original.explosionPower());

        float dispersion = overrides.getCannonOverride(name, "dispersion")
                .map(v -> v instanceof Number n ? n.floatValue() : null)
                .orElse(original.dispersion());

        // 新增字段覆盖
        float projectileWeight = overrides.getCannonOverride(name, "projectileWeight")
                .map(v -> v instanceof Number n ? n.floatValue() : null)
                .orElse(original.projectileWeight());

        int fireCooldown = overrides.getCannonOverride(name, "fireCooldown")
                .map(v -> v instanceof Number n ? n.intValue() : null)
                .orElse(original.fireCooldown());

        int salvoCount = overrides.getCannonOverride(name, "salvoCount")
                .map(v -> v instanceof Number n ? n.intValue() : null)
                .orElse(original.salvoCount());

        float salvoInterval = overrides.getCannonOverride(name, "salvoInterval")
                .map(v -> v instanceof Number n ? n.floatValue() : null)
                .orElse(original.salvoInterval());

        float maxElevation = overrides.getCannonOverride(name, "maxElevation")
                .map(v -> v instanceof Number n ? n.floatValue() : null)
                .orElse(original.maxElevation());

        float minElevation = overrides.getCannonOverride(name, "minElevation")
                .map(v -> v instanceof Number n ? n.floatValue() : null)
                .orElse(original.minElevation());

        List<MuzzlePos> muzzles = overrides.getCannonOverride(name, "muzzles")
                .flatMap(ConfigOverrideManager::parseMuzzlesOverride)
                .orElse(original.muzzles());

        // 重新构造实例（record不可变）
        return new ArtilleryCannonData(
                caliber,
                barrels,
                damage,
                reloadTime,
                durability,
                scopeZoom,
                muzzles,
                initialSpeed,
                dragCoeff,
                gravity,
                explosionPower,
                dispersion,
                projectileWeight,
                fireCooldown,
                salvoCount,
                salvoInterval,
                maxElevation,
                minElevation
        );
    }

    /**
     * 应用火炮覆盖（客户端缓存版本）- 仅用于GUI显示
     */
    private static ArtilleryCannonData applyCannonOverridesFromCache(
            ArtilleryCannonData original,
            String name) {

        // 尝试从客户端缓存读取，解析失败时回退到原始值
        int caliber = ClientConfigCache.getCannonOverride(name, "caliber")
                .flatMap(s -> parseIntSafe(s))
                .orElse(original.caliber());

        int barrels = ClientConfigCache.getCannonOverride(name, "barrels")
                .flatMap(s -> parseIntSafe(s))
                .orElse(original.barrels());

        float damage = ClientConfigCache.getCannonOverride(name, "damage")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.damage());

        int reloadTime = ClientConfigCache.getCannonOverride(name, "reloadTime")
                .flatMap(s -> parseIntSafe(s))
                .orElse(original.reloadTime());

        int durability = ClientConfigCache.getCannonOverride(name, "durability")
                .flatMap(s -> parseIntSafe(s))
                .orElse(original.durability());

        float scopeZoom = ClientConfigCache.getCannonOverride(name, "scopeZoom")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.scopeZoom());

        float initialSpeed = ClientConfigCache.getCannonOverride(name, "initialSpeed")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.initialSpeed());

        float dragCoeff = ClientConfigCache.getCannonOverride(name, "dragCoeff")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.dragCoeff());

        float gravity = ClientConfigCache.getCannonOverride(name, "gravity")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.gravity());

        float explosionPower = ClientConfigCache.getCannonOverride(name, "explosionPower")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.explosionPower());

        float dispersion = ClientConfigCache.getCannonOverride(name, "dispersion")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.dispersion());

        // 新增字段
        float projectileWeight = ClientConfigCache.getCannonOverride(name, "projectileWeight")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.projectileWeight());

        int fireCooldown = ClientConfigCache.getCannonOverride(name, "fireCooldown")
                .flatMap(s -> parseIntSafe(s))
                .orElse(original.fireCooldown());

        int salvoCount = ClientConfigCache.getCannonOverride(name, "salvoCount")
                .flatMap(s -> parseIntSafe(s))
                .orElse(original.salvoCount());

        float salvoInterval = ClientConfigCache.getCannonOverride(name, "salvoInterval")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.salvoInterval());

        float maxElevation = ClientConfigCache.getCannonOverride(name, "maxElevation")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.maxElevation());

        float minElevation = ClientConfigCache.getCannonOverride(name, "minElevation")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.minElevation());

        List<MuzzlePos> muzzles = ClientConfigCache.getCannonOverride(name, "muzzles")
                .flatMap(ConfigOverrideManager::parseMuzzlesString)
                .orElse(original.muzzles());

        // 重新构造实例（record不可变）
        return new ArtilleryCannonData(
                caliber,
                barrels,
                damage,
                reloadTime,
                durability,
                scopeZoom,
                muzzles,
                initialSpeed,
                dragCoeff,
                gravity,
                explosionPower,
                dispersion,
                projectileWeight,
                fireCooldown,
                salvoCount,
                salvoInterval,
                maxElevation,
                minElevation
        );
    }

    /** 安全解析 float，失败时返回 empty */
    private static java.util.Optional<Float> parseFloatSafe(String s) {
        try {
            return java.util.Optional.of(Float.parseFloat(s));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }

    /** 安全解析 int，失败时返回 empty */
    private static java.util.Optional<Integer> parseIntSafe(String s) {
        try {
            return java.util.Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }

    private static Optional<List<MuzzlePos>> parseMuzzlesOverride(Object value) {
        if (value instanceof String s) {
            return parseMuzzlesString(s);
        }
        return Optional.empty();
    }

    /**
     * 解析炮口覆写字符串，格式为 x:y:z;x:y:z。
     * 例如双联装可写为 -0.35:0:1.5;0.35:0:1.5。
     */
    private static Optional<List<MuzzlePos>> parseMuzzlesString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            List<MuzzlePos> muzzles = java.util.Arrays.stream(value.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(part -> {
                        String[] coords = part.split(":");
                        if (coords.length != 3) {
                            throw new IllegalArgumentException("Invalid muzzle position: " + part);
                        }
                        return new MuzzlePos(
                                Double.parseDouble(coords[0].trim()),
                                Double.parseDouble(coords[1].trim()),
                                Double.parseDouble(coords[2].trim()));
                    })
                    .toList();
            return muzzles.isEmpty() ? Optional.empty() : Optional.of(muzzles);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
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
            case "caliber" -> {
                int i = num.intValue();
                yield Math.max(1, Math.min(1000, i));
            }
            case "barrels" -> {
                int i = num.intValue();
                yield Math.max(1, Math.min(20, i));
            }
            case "damage" -> {
                float f = num.floatValue();
                yield Math.max(0.1f, Math.min(1000f, f));
            }
            case "reloadTime" -> {
                int i = num.intValue();
                yield Math.max(1, Math.min(6000, i));
            }
            case "durability" -> {
                int i = num.intValue();
                yield Math.max(1, Math.min(100000, i));
            }
            case "scopeZoom" -> {
                float f = num.floatValue();
                yield Math.max(1.0f, Math.min(20.0f, f));
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
            // 新增字段
            case "projectileWeight" -> {
                float f = num.floatValue();
                yield Math.max(0.1f, Math.min(10000f, f));
            }
            case "fireCooldown" -> {
                int i = num.intValue();
                yield Math.max(0, Math.min(6000, i));
            }
            case "salvoCount" -> {
                int i = num.intValue();
                yield Math.max(1, Math.min(20, i));
            }
            case "salvoInterval" -> {
                float f = num.floatValue();
                yield Math.max(0f, Math.min(100f, f));
            }
            case "maxElevation", "minElevation" -> {
                float f = num.floatValue();
                yield Math.max(-90f, Math.min(90f, f));
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
