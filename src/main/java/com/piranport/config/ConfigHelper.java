package com.piranport.config;

import com.piranport.PiranPort;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 配置工具类 - Configuration Helper
 * 提供配置值验证和安全读取功能
 */
public class ConfigHelper {

    /**
     * 验证配置值是否为正数，不合法时返回默认值并记录警告
     *
     * @param config 配置项
     * @param defaultValue 默认值
     * @param name 配置项名称
     * @return 验证后的值
     */
    public static float validatePositive(ModConfigSpec.DoubleValue config,
                                        float defaultValue, String name) {
        float value = config.get().floatValue();
        if (value <= 0) {
            PiranPort.LOGGER.warn("Invalid config value for {}: {} (must be positive). Using default: {}",
                                 name, value, defaultValue);
            return defaultValue;
        }
        return value;
    }

    /**
     * 验证配置值是否在指定范围内，不合法时返回默认值并记录警告
     *
     * @param config 配置项
     * @param min 最小值
     * @param max 最大值
     * @param defaultValue 默认值
     * @param name 配置项名称
     * @return 验证后的值
     */
    public static int validateRange(ModConfigSpec.IntValue config,
                                   int min, int max, int defaultValue, String name) {
        int value = config.get();
        if (value < min || value > max) {
            PiranPort.LOGGER.warn("Config value for {} out of range: {} (valid: {}-{}). Using default: {}",
                                 name, value, min, max, defaultValue);
            return defaultValue;
        }
        return value;
    }

    /**
     * 验证Double配置值是否在指定范围内，不合法时返回默认值并记录警告
     *
     * @param config 配置项
     * @param min 最小值
     * @param max 最大值
     * @param defaultValue 默认值
     * @param name 配置项名称
     * @return 验证后的值
     */
    public static double validateRange(ModConfigSpec.DoubleValue config,
                                      double min, double max, double defaultValue, String name) {
        double value = config.get();
        if (value < min || value > max) {
            PiranPort.LOGGER.warn("Config value for {} out of range: {} (valid: {}-{}). Using default: {}",
                                 name, value, min, max, defaultValue);
            return defaultValue;
        }
        return value;
    }

    /**
     * 安全获取整数配置值
     *
     * @param config 配置项
     * @param defaultValue 默认值
     * @param name 配置项名称
     * @return 配置值或默认值
     */
    public static int getIntSafe(ModConfigSpec.IntValue config, int defaultValue, String name) {
        try {
            return config.get();
        } catch (Exception e) {
            PiranPort.LOGGER.error("Failed to read config value for {}: {}. Using default: {}",
                                  name, e.getMessage(), defaultValue);
            return defaultValue;
        }
    }

    /**
     * 安全获取Double配置值
     *
     * @param config 配置项
     * @param defaultValue 默认值
     * @param name 配置项名称
     * @return 配置值或默认值
     */
    public static double getDoubleSafe(ModConfigSpec.DoubleValue config, double defaultValue, String name) {
        try {
            return config.get();
        } catch (Exception e) {
            PiranPort.LOGGER.error("Failed to read config value for {}: {}. Using default: {}",
                                  name, e.getMessage(), defaultValue);
            return defaultValue;
        }
    }
}
