package com.piranport.artillery.config.override;

import com.piranport.PiranPort;
import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.ArtilleryConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 配置CSV导出器
 *
 * <p>导出火炮和弹药配置到CSV文件，包含中文显示名称。
 * <p>导出位置: {@code <服务器目录>/config/piranport/exports/}
 */
public class ConfigCSVExporter {

    private static final String[] CANNON_HEADERS = {
            "cannon_name", "display_name", "caliber", "barrels", "damage", "reloadTime",
            "durability", "scopeZoom", "initialSpeed", "dragCoeff", "gravity",
            "explosionPower", "dispersion"
    };

    private static final String[] PROJECTILE_HEADERS = {
            "config_key", "display_name", "value", "type", "description"
    };

    // 弹药配置键列表（按字母顺序）
    private static final String[] PROJECTILE_KEYS = {
            "HE_ARMOR_PENETRATION",
            "HE_DAMAGE_FALLOFF",
            "AP_DAMAGE_MULTIPLIER",
            "AP_ARMOR_IGNORE",
            "UNDERWATER_EXPLOSION_MULTIPLIER",
            "UNDERWATER_EXPLODE"
    };

    private ConfigCSVExporter() {
        // 工具类，禁止实例化
    }

    /**
     * 导出火炮配置到CSV
     *
     * @param level 服务端世界
     * @param outputPath 输出文件路径
     * @throws IOException 文件写入失败
     */
    public static void exportCannonsToCSV(ServerLevel level, Path outputPath) throws IOException {
        ArtilleryConfigOverrideSavedData overrides = ArtilleryConfigOverrideSavedData.get(level);

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            // 写入表头
            writer.write(String.join(",", CANNON_HEADERS));
            writer.newLine();

            // 获取所有火炮名称（排序）
            Set<String> cannonNames = getAllCannonNames();

            // 逐行写入火炮数据
            for (String name : cannonNames) {
                ArtilleryCannonData original = ArtilleryConfig.get(name);
                ArtilleryCannonData withOverrides = ConfigOverrideManager.getCannonData(name, level);

                // 获取中文显示名称
                String displayName = getDisplayName("item.piranport." + name);

                writer.write(formatCannonRow(name, displayName, withOverrides));
                writer.newLine();
            }
        }

        PiranPort.LOGGER.info("Exported cannon config to: {}", outputPath);
    }

    /**
     * 导出弹药配置到CSV
     */
    public static void exportProjectilesToCSV(ServerLevel level, Path outputPath) throws IOException {
        ArtilleryConfigOverrideSavedData overrides = ArtilleryConfigOverrideSavedData.get(level);

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            // 写入表头
            writer.write(String.join(",", PROJECTILE_HEADERS));
            writer.newLine();

            // 逐行写入弹药配置
            for (String key : PROJECTILE_KEYS) {
                String displayName = getDisplayName("config.piranport." + key.toLowerCase());
                Object value = overrides.getProjectileOverride(key).orElse(getDefaultProjectileValue(key));
                String type = getValueType(value);
                String description = getProjectileDescription(key);

                writer.write(formatProjectileRow(key, displayName, value, type, description));
                writer.newLine();
            }
        }

        PiranPort.LOGGER.info("Exported projectile config to: {}", outputPath);
    }

    /**
     * 格式化火炮数据行
     */
    private static String formatCannonRow(String name, String displayName, ArtilleryCannonData data) {
        return String.format("%s,%s,%d,%d,%.1f,%d,%d,%.1f,%.2f,%.4f,%.1f,%.1f,%.2f",
                name,
                escapeCSV(displayName),
                data.caliber(),
                data.barrels(),
                data.damage(),
                data.reloadTime(),
                data.durability(),
                data.scopeZoom(),
                data.initialSpeed(),
                data.dragCoeff(),
                data.gravity(),
                data.explosionPower(),
                data.dispersion()
        );
    }

    /**
     * 格式化弹药配置行
     */
    private static String formatProjectileRow(String key, String displayName, Object value, String type, String description) {
        return String.format("%s,%s,%s,%s,%s",
                key,
                escapeCSV(displayName),
                value.toString(),
                type,
                escapeCSV(description)
        );
    }

    /**
     * 转义CSV字段（处理逗号和引号）
     */
    private static String escapeCSV(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * 获取所有火炮名称（从JSON配置目录扫描）
     */
    private static Set<String> getAllCannonNames() {
        // 使用TreeSet自动排序
        Set<String> names = new TreeSet<>();

        // 已知的火炮列表（从ModItems推断）
        names.add("single_small_gun");
        names.add("small_gun");
        names.add("medium_gun");
        names.add("large_gun");
        names.add("french_quad_380mm_gun");
        names.add("seven_barrel_gun");
        names.add("salvo_test_gun");

        return names;
    }

    /**
     * 获取显示名称（从翻译键）
     * 注意：服务端无法直接访问翻译，这里返回翻译键作为占位
     */
    private static String getDisplayName(String translationKey) {
        // 服务端环境下，Component.translatable 无法获取实际翻译
        // 返回简化的中文名称映射
        return switch (translationKey) {
            case "item.piranport.single_small_gun" -> "单装小型火炮";
            case "item.piranport.small_gun" -> "小型火炮";
            case "item.piranport.medium_gun" -> "中型火炮";
            case "item.piranport.large_gun" -> "大型火炮";
            case "item.piranport.french_quad_380mm_gun" -> "法国四联380毫米炮";
            case "item.piranport.seven_barrel_gun" -> "七联装主炮群";
            case "item.piranport.salvo_test_gun" -> "齐射测试";
            case "config.piranport.he_armor_penetration" -> "HE弹护甲穿透";
            case "config.piranport.he_damage_falloff" -> "HE弹距离衰减";
            case "config.piranport.ap_damage_multiplier" -> "AP弹伤害倍率";
            case "config.piranport.ap_armor_ignore" -> "AP弹护甲忽略";
            case "config.piranport.underwater_explosion_multiplier" -> "水中爆炸倍率";
            case "config.piranport.underwater_explode" -> "水中到期爆炸";
            default -> translationKey;
        };
    }

    /**
     * 获取弹药配置的默认值（从ModProjectilesConfig）
     */
    private static Object getDefaultProjectileValue(String key) {
        return switch (key) {
            case "HE_ARMOR_PENETRATION" -> 0.3;
            case "HE_DAMAGE_FALLOFF" -> true;
            case "AP_DAMAGE_MULTIPLIER" -> 1.3;
            case "AP_ARMOR_IGNORE" -> 0.5;
            case "UNDERWATER_EXPLOSION_MULTIPLIER" -> 0.5;
            case "UNDERWATER_EXPLODE" -> false;
            default -> "unknown";
        };
    }

    /**
     * 获取值的类型字符串
     */
    private static String getValueType(Object value) {
        if (value instanceof Double || value instanceof Float) {
            return "double";
        } else if (value instanceof Integer) {
            return "int";
        } else if (value instanceof Boolean) {
            return "boolean";
        } else {
            return "string";
        }
    }

    /**
     * 获取弹药配置的描述
     */
    private static String getProjectileDescription(String key) {
        return switch (key) {
            case "HE_ARMOR_PENETRATION" -> "HE弹护甲穿透比例";
            case "HE_DAMAGE_FALLOFF" -> "HE弹距离衰减开关";
            case "AP_DAMAGE_MULTIPLIER" -> "AP弹伤害倍率";
            case "AP_ARMOR_IGNORE" -> "AP弹护甲忽略比例";
            case "UNDERWATER_EXPLOSION_MULTIPLIER" -> "水中爆炸半径倍率";
            case "UNDERWATER_EXPLODE" -> "水中到期是否爆炸";
            default -> "";
        };
    }

    /**
     * 生成带时间戳的文件名
     */
    public static String generateTimestampedFilename(String prefix, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return prefix + "_" + timestamp + "." + extension;
    }
}
