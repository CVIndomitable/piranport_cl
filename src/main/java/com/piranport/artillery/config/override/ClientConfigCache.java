package com.piranport.artillery.config.override;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 客户端配置覆盖缓存
 *
 * <p>存储从服务端同步的配置覆盖值，供GUI显示使用。
 * <p>线程模型: 客户端主线程
 */
@OnlyIn(Dist.CLIENT)
public class ClientConfigCache {

    // 火炮覆盖: cannonName → (field → value)
    private static final Map<String, Map<String, String>> cannonOverrides = new HashMap<>();

    // 弹药配置覆盖: configKey → value
    private static final Map<String, String> projectileOverrides = new HashMap<>();

    private ClientConfigCache() {
        // 工具类，禁止实例化
    }

    /**
     * 更新缓存（从网络包接收）
     */
    public static void updateCache(
            Map<String, Map<String, String>> cannons,
            Map<String, String> projectiles) {
        cannonOverrides.clear();
        cannonOverrides.putAll(cannons);
        projectileOverrides.clear();
        projectileOverrides.putAll(projectiles);
    }

    /**
     * 清空缓存（断开连接时）
     */
    public static void clearCache() {
        cannonOverrides.clear();
        projectileOverrides.clear();
    }

    /**
     * 获取火炮字段覆盖值
     */
    public static Optional<String> getCannonOverride(String cannonName, String field) {
        Map<String, String> fields = cannonOverrides.get(cannonName);
        if (fields == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(fields.get(field));
    }

    /**
     * 获取弹药配置覆盖值
     */
    public static Optional<String> getProjectileOverride(String configKey) {
        return Optional.ofNullable(projectileOverrides.get(configKey));
    }

    /**
     * 获取所有火炮覆盖（用于GUI显示）
     */
    public static Map<String, Map<String, String>> getAllCannonOverrides() {
        return new HashMap<>(cannonOverrides);
    }

    /**
     * 设置单门火炮的覆盖值并保留其余缓存
     */
    public static void setCannonOverrides(String cannonName, Map<String, String> fields) {
        cannonOverrides.put(cannonName, new HashMap<>(fields));
    }

    /**
     * 获取所有弹药配置覆盖（用于GUI显示）
     */
    public static Map<String, String> getAllProjectileOverrides() {
        return new HashMap<>(projectileOverrides);
    }
}
