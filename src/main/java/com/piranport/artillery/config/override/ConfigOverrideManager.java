package com.piranport.artillery.config.override;

import com.piranport.PiranPort;
import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.ArtilleryConfig;
import com.piranport.artillery.config.MuzzlePos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
                .flatMap(ConfigOverrideManager::finiteFloatOverride)
                .orElse(original.damage());

        int reloadTime = overrides.getCannonOverride(name, "reloadTime")
                .map(v -> v instanceof Number n ? n.intValue() : null)
                .orElse(original.reloadTime());

        int durability = overrides.getCannonOverride(name, "durability")
                .map(v -> v instanceof Number n ? n.intValue() : null)
                .orElse(original.durability());

        float scopeZoom = overrides.getCannonOverride(name, "scopeZoom")
                .flatMap(ConfigOverrideManager::finiteFloatOverride)
                .orElse(original.scopeZoom());

        float initialSpeed = overrides.getCannonOverride(name, "initialSpeed")
                .flatMap(ConfigOverrideManager::finiteFloatOverride)
                .orElse(original.initialSpeed());

        float dragCoeff = overrides.getCannonOverride(name, "dragCoeff")
                .flatMap(ConfigOverrideManager::finiteFloatOverride)
                .map(ConfigOverrideManager::normalizeDragCoeff)
                .orElse(original.dragCoeff());

        float gravity = overrides.getCannonOverride(name, "gravity")
                .flatMap(ConfigOverrideManager::finiteFloatOverride)
                .orElse(original.gravity());

        float explosionPower = overrides.getCannonOverride(name, "explosionPower")
                .flatMap(ConfigOverrideManager::finiteFloatOverride)
                .orElse(original.explosionPower());

        float dispersion = overrides.getCannonOverride(name, "dispersion")
                .flatMap(ConfigOverrideManager::finiteFloatOverride)
                .map(ConfigOverrideManager::normalizeDispersion)
                .orElse(original.dispersion());

        int fireCooldown = overrides.getCannonOverride(name, "fireCooldown")
                .map(v -> v instanceof Number n ? n.intValue() : null)
                .orElse(original.fireCooldown());

        int salvoCount = overrides.getCannonOverride(name, "salvoCount")
                .map(v -> v instanceof Number n ? n.intValue() : null)
                .orElse(original.salvoCount());

        float salvoInterval = overrides.getCannonOverride(name, "salvoInterval")
                .flatMap(ConfigOverrideManager::finiteFloatOverride)
                .orElse(original.salvoInterval());

        float projectileWeight = overrides.getCannonOverride(name, "projectileWeight")
                .flatMap(ConfigOverrideManager::finiteFloatOverride)
                .orElse(original.projectileWeight());

        float verticalSpread = overrides.getCannonOverride(name, "verticalSpread")
                .flatMap(ConfigOverrideManager::finiteFloatOverride)
                .map(ConfigOverrideManager::normalizeDispersion)
                .orElse(original.verticalSpread());

        float horizontalSpread = overrides.getCannonOverride(name, "horizontalSpread")
                .flatMap(ConfigOverrideManager::finiteFloatOverride)
                .map(ConfigOverrideManager::normalizeDispersion)
                .orElse(original.horizontalSpread());

        float maxElevation = overrides.getCannonOverride(name, "maxElevation")
                .flatMap(ConfigOverrideManager::finiteFloatOverride)
                .orElse(original.maxElevation());

        float minElevation = overrides.getCannonOverride(name, "minElevation")
                .flatMap(ConfigOverrideManager::finiteFloatOverride)
                .orElse(original.minElevation());

        float turretSpeed = overrides.getCannonOverride(name, "turretSpeed")
                .flatMap(ConfigOverrideManager::finiteFloatOverride)
                .orElse(original.turretSpeed());

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
                fireCooldown,
                salvoCount,
                salvoInterval,
                projectileWeight,
                verticalSpread,
                horizontalSpread,
                maxElevation,
                minElevation,
                turretSpeed,
                overrides.getCannonOverride(name, "loadingMode")
                        .map(v -> v == null ? null : v.toString())
                        .orElse(original.loadingMode())
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
                .map(ConfigOverrideManager::normalizeDragCoeff)
                .orElse(original.dragCoeff());

        float gravity = ClientConfigCache.getCannonOverride(name, "gravity")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.gravity());

        float explosionPower = ClientConfigCache.getCannonOverride(name, "explosionPower")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.explosionPower());

        float dispersion = ClientConfigCache.getCannonOverride(name, "dispersion")
                .flatMap(s -> parseFloatSafe(s))
                .map(ConfigOverrideManager::normalizeDispersion)
                .orElse(original.dispersion());

        int fireCooldown = ClientConfigCache.getCannonOverride(name, "fireCooldown")
                .flatMap(s -> parseIntSafe(s))
                .orElse(original.fireCooldown());

        int salvoCount = ClientConfigCache.getCannonOverride(name, "salvoCount")
                .flatMap(s -> parseIntSafe(s))
                .orElse(original.salvoCount());

        float salvoInterval = ClientConfigCache.getCannonOverride(name, "salvoInterval")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.salvoInterval());

        float projectileWeight = ClientConfigCache.getCannonOverride(name, "projectileWeight")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.projectileWeight());

        float verticalSpread = ClientConfigCache.getCannonOverride(name, "verticalSpread")
                .flatMap(s -> parseFloatSafe(s))
                .map(ConfigOverrideManager::normalizeDispersion)
                .orElse(original.verticalSpread());

        float horizontalSpread = ClientConfigCache.getCannonOverride(name, "horizontalSpread")
                .flatMap(s -> parseFloatSafe(s))
                .map(ConfigOverrideManager::normalizeDispersion)
                .orElse(original.horizontalSpread());

        float maxElevation = ClientConfigCache.getCannonOverride(name, "maxElevation")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.maxElevation());

        float minElevation = ClientConfigCache.getCannonOverride(name, "minElevation")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.minElevation());

        float turretSpeed = ClientConfigCache.getCannonOverride(name, "turretSpeed")
                .flatMap(s -> parseFloatSafe(s))
                .orElse(original.turretSpeed());

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
                fireCooldown,
                salvoCount,
                salvoInterval,
                projectileWeight,
                verticalSpread,
                horizontalSpread,
                maxElevation,
                minElevation,
                turretSpeed,
                ClientConfigCache.getCannonOverride(name, "loadingMode")
                        .orElse(original.loadingMode())
        );
    }

    /** 安全解析 float，失败时返回 empty */
    private static java.util.Optional<Float> parseFloatSafe(String s) {
        try {
            float value = Float.parseFloat(s);
            return Float.isFinite(value) ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
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

    private static Optional<Float> finiteFloatOverride(Object value) {
        if (value instanceof Number number) {
            float parsed = number.floatValue();
            return Float.isFinite(parsed) ? Optional.of(parsed) : Optional.empty();
        }
        return Optional.empty();
    }

    private static float normalizeDispersion(float dispersion) {
        if (!Float.isFinite(dispersion)) {
            return 0.01f;
        }
        return dispersion <= 0.01f ? 0.01f : dispersion;
    }

    private static float normalizeDragCoeff(float dragCoeff) {
        if (!Float.isFinite(dragCoeff)) {
            return 0.0001f;
        }
        return dragCoeff <= 0.0001f ? 0.0001f : dragCoeff;
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
                        double x = Double.parseDouble(coords[0].trim());
                        double y = Double.parseDouble(coords[1].trim());
                        double z = Double.parseDouble(coords[2].trim());
                        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                            throw new IllegalArgumentException("Non-finite muzzle position: " + part);
                        }
                        return new MuzzlePos(x, y, z);
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
                .flatMap(v -> finiteDoubleOverride(v))
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
                .flatMap(v -> booleanOverride(v))
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
                .flatMap(v -> intOverride(v))
                .orElse(defaultValue);
    }

    // ==================== 弹药覆盖安全读取 ====================

    private static Optional<Double> finiteDoubleOverride(Object value) {
        if (value instanceof Number number) {
            double d = number.doubleValue();
            if (Double.isFinite(d)) {
                return Optional.of(d);
            }
            PiranPort.LOGGER.warn("Ignoring non-finite projectile override: {}", value);
        } else {
            PiranPort.LOGGER.warn("Ignoring invalid projectile override type {}: expected Number",
                    value.getClass().getName());
        }
        return Optional.empty();
    }

    private static Optional<Boolean> booleanOverride(Object value) {
        if (value instanceof Boolean b) {
            return Optional.of(b);
        }
        PiranPort.LOGGER.warn("Ignoring invalid projectile override type {}: expected Boolean",
                value.getClass().getName());
        return Optional.empty();
    }

    private static Optional<Integer> intOverride(Object value) {
        if (value instanceof Integer i) {
            return Optional.of(i);
        }
        PiranPort.LOGGER.warn("Ignoring invalid projectile override type {}: expected Integer",
                value.getClass().getName());
        return Optional.empty();
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
        if (!isKnownCannonField(field)) {
            throw new IllegalArgumentException("Unknown cannon config field: " + field);
        }

        if ("muzzles".equals(field)) {
            if (value instanceof String s && parseMuzzlesString(s).isPresent()) {
                return s;
            }
            throw new IllegalArgumentException("Invalid muzzle override");
        }

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
                yield clampFinite(f, 0.1f, 1000f, field);
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
                yield clampFinite(f, 1.0f, 20.0f, field);
            }
            case "initialSpeed" -> {
                float f = num.floatValue();
                yield clampFinite(f, 0.1f, 50f, field);
            }
            case "dragCoeff" -> {
                float f = num.floatValue();
                yield clampFinite(f, 0.0001f, 50.0f, field);
            }
            case "gravity" -> {
                float f = num.floatValue();
                yield clampFinite(f, 0.1f, 100f, field);
            }
            case "explosionPower" -> {
                float f = num.floatValue();
                yield clampFinite(f, 0.0f, 20f, field);
            }
            case "dispersion" -> {
                float f = num.floatValue();
                yield clampFinite(f, 0.01f, 10f, field);
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
                yield clampFinite(f, 0f, 100f, field);
            }
            case "projectileWeight" -> {
                float f = num.floatValue();
                yield clampFinite(f, 1f, 100000f, field);
            }
            case "verticalSpread", "horizontalSpread" -> {
                float f = num.floatValue();
                yield clampFinite(f, 0.01f, 30f, field);
            }
            case "maxElevation" -> {
                float f = num.floatValue();
                yield clampFinite(f, -89f, 89f, field);
            }
            case "minElevation" -> {
                float f = num.floatValue();
                yield clampFinite(f, -89f, 89f, field);
            }
            case "turretSpeed" -> {
                float f = num.floatValue();
                yield clampFinite(f, 0.1f, 180f, field);
            }
            default -> {
                if (!Double.isFinite(num.doubleValue())) {
                    throw new IllegalArgumentException("Non-finite config value for " + field);
                }
                yield value;
            }
        };
    }

    /**
     * 验证弹药配置值。加载存档和导入数据必须共用同一套白名单与类型检查。
     */
    public static Object validateProjectileValue(String key, Object value) {
        if (!isKnownProjectileKey(key)) {
            throw new IllegalArgumentException("Unknown projectile config key: " + key);
        }
        if (value == null) {
            throw new IllegalArgumentException("Null projectile config value for " + key);
        }

        if (DOUBLE_PROJECTILE_KEYS.contains(key)) {
            if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) {
                throw new IllegalArgumentException("Expected finite number for projectile config: " + key);
            }
            return switch (key) {
                case "HE_ARMOR_PENETRATION", "AP_ARMOR_IGNORE" ->
                        clampFinite(number.doubleValue(), 0.0, 1.0, key);
                case "AP_DAMAGE_MULTIPLIER" ->
                        clampFinite(number.doubleValue(), 0.1, 10.0, key);
                case "UNDERWATER_EXPLOSION_MULTIPLIER" ->
                        clampFinite(number.doubleValue(), 0.0, 2.0, key);
                default -> number.doubleValue();
            };
        }

        if (BOOLEAN_PROJECTILE_KEYS.contains(key)) {
            if (!(value instanceof Boolean b)) {
                throw new IllegalArgumentException("Expected boolean for projectile config: " + key);
            }
            return b;
        }

        if (INTEGER_PROJECTILE_KEYS.contains(key)) {
            if (!(value instanceof Integer i)) {
                throw new IllegalArgumentException("Expected integer for projectile config: " + key);
            }
            return i;
        }

        if (STRING_PROJECTILE_KEYS.contains(key)) {
            if (!(value instanceof String str)) {
                throw new IllegalArgumentException("Expected string for projectile config: " + key);
            }
            if (str.length() > MAX_PROJECTILE_STRING_LENGTH) {
                throw new IllegalArgumentException("Projectile config string is too long: " + key);
            }
            return str;
        }

        // Every whitelisted key must declare its accepted type.
        throw new IllegalArgumentException("Unsupported projectile config key: " + key);
    }

    /** Cannon override fields accepted by the admin tool, CSV importer, and saved data. */
    private static final Set<String> CANNON_FIELDS = Set.of(
            "caliber", "barrels", "damage", "reloadTime", "durability", "scopeZoom",
            "muzzles", "initialSpeed", "dragCoeff", "gravity", "explosionPower",
            "dispersion", "fireCooldown", "salvoCount", "salvoInterval",
            "projectileWeight", "verticalSpread", "horizontalSpread",
            "maxElevation", "minElevation", "turretSpeed"
    );

    public static boolean isKnownCannonField(String field) {
        return CANNON_FIELDS.contains(field);
    }

    public static boolean isKnownProjectileKey(String key) {
        return DOUBLE_PROJECTILE_KEYS.contains(key)
                || BOOLEAN_PROJECTILE_KEYS.contains(key)
                || INTEGER_PROJECTILE_KEYS.contains(key)
                || STRING_PROJECTILE_KEYS.contains(key);
    }

    private static final Set<String> DOUBLE_PROJECTILE_KEYS = Set.of(
            "HE_ARMOR_PENETRATION",
            "AP_DAMAGE_MULTIPLIER",
            "AP_ARMOR_IGNORE",
            "UNDERWATER_EXPLOSION_MULTIPLIER"
    );

    private static final Set<String> BOOLEAN_PROJECTILE_KEYS = Set.of(
            "HE_DAMAGE_FALLOFF",
            "UNDERWATER_EXPLODE"
    );

    private static final Set<String> INTEGER_PROJECTILE_KEYS = Set.of();

    private static final Set<String> STRING_PROJECTILE_KEYS = Set.of();

    private static final int MAX_PROJECTILE_STRING_LENGTH = 256;

    private static float clampFinite(float value, float min, float max, String field) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Non-finite config value for " + field);
        }
        return Math.max(min, Math.min(max, value));
    }

    private static double clampFinite(double value, double min, double max, String key) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Non-finite projectile config value for " + key);
        }
        return Math.max(min, Math.min(max, value));
    }
}
