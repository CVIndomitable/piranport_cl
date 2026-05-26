package com.piranport.artillery.config.override;

import com.piranport.PiranPort;
import com.piranport.artillery.config.ArtilleryConfig;
import net.minecraft.server.level.ServerLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置CSV导入器
 *
 * <p>从CSV文件导入火炮和弹药配置，应用到当前存档。
 * <p>CSV格式必须与 {@link ConfigCSVExporter} 导出的格式一致。
 */
public class ConfigCSVImporter {

    private ConfigCSVImporter() {
        // 工具类，禁止实例化
    }

    /**
     * 从CSV导入火炮配置
     *
     * @param level 服务端世界
     * @param csvPath CSV文件路径
     * @return 导入结果（成功数量，失败数量）
     * @throws IOException 文件读取失败
     */
    public static ImportResult importCannonsFromCSV(ServerLevel level, Path csvPath) throws IOException {
        ArtilleryConfigOverrideSavedData overrides = ArtilleryConfigOverrideSavedData.get(level);

        int successCount = 0;
        int failCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("CSV文件为空");
            }

            // 验证表头
            if (!headerLine.startsWith("cannon_name,display_name,caliber,barrels")) {
                throw new IOException("CSV格式错误：表头不匹配");
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    importCannonLine(line, overrides);
                    successCount++;
                } catch (Exception e) {
                    PiranPort.LOGGER.warn("Failed to import cannon at line {}: {}", lineNumber, e.getMessage());
                    failCount++;
                }
            }
        }

        // 标记数据已修改，触发保存
        overrides.setDirty();

        PiranPort.LOGGER.info("Imported cannon config from {}: {} success, {} failed",
                csvPath, successCount, failCount);

        return new ImportResult(successCount, failCount);
    }

    /**
     * 从CSV导入弹药配置
     *
     * @param level 服务端世界
     * @param csvPath CSV文件路径
     * @return 导入结果
     * @throws IOException 文件读取失败
     */
    public static ImportResult importProjectilesFromCSV(ServerLevel level, Path csvPath) throws IOException {
        ArtilleryConfigOverrideSavedData overrides = ArtilleryConfigOverrideSavedData.get(level);

        int successCount = 0;
        int failCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("CSV文件为空");
            }

            // 验证表头
            if (!headerLine.startsWith("config_key,display_name,value,type")) {
                throw new IOException("CSV格式错误：表头不匹配");
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    importProjectileLine(line, overrides);
                    successCount++;
                } catch (Exception e) {
                    PiranPort.LOGGER.warn("Failed to import projectile config at line {}: {}", lineNumber, e.getMessage());
                    failCount++;
                }
            }
        }

        // 标记数据已修改，触发保存
        overrides.setDirty();

        PiranPort.LOGGER.info("Imported projectile config from {}: {} success, {} failed",
                csvPath, successCount, failCount);

        return new ImportResult(successCount, failCount);
    }

    /**
     * 解析并导入单行火炮数据
     */
    private static void importCannonLine(String line, ArtilleryConfigOverrideSavedData overrides) {
        String[] parts = parseCSVLine(line);

        if (parts.length < 13) {
            throw new IllegalArgumentException("字段数量不足（需要13个字段）");
        }

        String cannonName = parts[0].trim();

        // 验证火炮是否存在
        if (ArtilleryConfig.get(cannonName) == null) {
            throw new IllegalArgumentException("未知的火炮: " + cannonName);
        }

        // 解析数值字段（跳过 display_name, caliber, barrels, durability, scopeZoom）
        try {
            float damage = Float.parseFloat(parts[4]);
            int reloadTime = Integer.parseInt(parts[5]);
            float initialSpeed = Float.parseFloat(parts[8]);
            float dragCoeff = Float.parseFloat(parts[9]);
            float explosionPower = Float.parseFloat(parts[11]);
            float dispersion = Float.parseFloat(parts[12]);

            // 应用验证和范围限制
            damage = (float) ConfigOverrideManager.validateValue("damage", damage);
            reloadTime = (int) ConfigOverrideManager.validateValue("reloadTime", reloadTime);
            initialSpeed = (float) ConfigOverrideManager.validateValue("initialSpeed", initialSpeed);
            dragCoeff = (float) ConfigOverrideManager.validateValue("dragCoeff", dragCoeff);
            explosionPower = (float) ConfigOverrideManager.validateValue("explosionPower", explosionPower);
            dispersion = (float) ConfigOverrideManager.validateValue("dispersion", dispersion);

            // 写入覆盖数据
            overrides.setCannonOverride(cannonName, "damage", damage);
            overrides.setCannonOverride(cannonName, "reloadTime", reloadTime);
            overrides.setCannonOverride(cannonName, "initialSpeed", initialSpeed);
            overrides.setCannonOverride(cannonName, "dragCoeff", dragCoeff);
            overrides.setCannonOverride(cannonName, "explosionPower", explosionPower);
            overrides.setCannonOverride(cannonName, "dispersion", dispersion);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("数值格式错误: " + e.getMessage());
        }
    }

    /**
     * 解析并导入单行弹药配置
     */
    private static void importProjectileLine(String line, ArtilleryConfigOverrideSavedData overrides) {
        String[] parts = parseCSVLine(line);

        if (parts.length < 4) {
            throw new IllegalArgumentException("字段数量不足（需要至少4个字段）");
        }

        String configKey = parts[0].trim();
        String valueStr = parts[2].trim();
        String type = parts[3].trim();

        try {
            Object value = switch (type) {
                case "double" -> {
                    double d = Double.parseDouble(valueStr);
                    yield ConfigOverrideManager.validateProjectileValue(configKey, d);
                }
                case "int" -> Integer.parseInt(valueStr);
                case "boolean" -> Boolean.parseBoolean(valueStr);
                case "string" -> valueStr;
                default -> throw new IllegalArgumentException("未知的类型: " + type);
            };

            overrides.setProjectileOverride(configKey, value);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("数值格式错误: " + e.getMessage());
        }
    }

    /**
     * 解析CSV行（处理引号和逗号转义）
     */
    private static String[] parseCSVLine(String line) {
        var fields = new java.util.ArrayList<String>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // 检查是否是转义的引号
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentField.append('"');
                    i++; // 跳过下一个引号
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // 字段分隔符
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        // 添加最后一个字段
        fields.add(currentField.toString());

        return fields.toArray(new String[0]);
    }

    /**
     * 导入结果记录
     */
    public record ImportResult(int successCount, int failCount) {
        public boolean hasFailures() {
            return failCount > 0;
        }

        public int totalCount() {
            return successCount + failCount;
        }
    }
}
