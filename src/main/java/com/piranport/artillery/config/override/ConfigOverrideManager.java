package com.piranport.artillery.config.override;

import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.ArtilleryConfig;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 配置覆盖管理器 - 运行时拦截层
 *
 * <p>在数据读取点应用终端有效值，不修改原始配置系统。
 * <p>线程模型：服务端主线程保存参数，客户端通过终端快照读取。
 * <p>使用方式：替换所有 {@code ArtilleryConfig.get()} 调用为 {@code ConfigOverrideManager.getCannonData()}
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
     * @param level 世界实例（保留此参数以兼容现有调用方）
     * @return 应用覆盖后的火炮数据
     */
    public static ArtilleryCannonData getCannonData(String name, @Nullable Level level) {
        // 获取原始数据
        ArtilleryCannonData original = ArtilleryConfig.get(name);

        return applyTerminalParameters(original, name);
    }

    /** 终端是唯一运行时覆盖层；目录基准仍直接读取 ArtilleryConfig。 */
    private static ArtilleryCannonData applyTerminalParameters(ArtilleryCannonData c, String name) {
        String key = "cannon." + name + ".";
        return new ArtilleryCannonData(
                c.caliber(), c.barrels(),
                (float) com.piranport.terminal.TerminalParameters.getDouble(key + "damage", c.damage()),
                com.piranport.terminal.TerminalParameters.getInt(key + "reload_time", c.reloadTime()),
                com.piranport.terminal.TerminalParameters.getInt(key + "durability", c.durability()),
                (float) com.piranport.terminal.TerminalParameters.getDouble(key + "scope_zoom", c.scopeZoom()),
                c.muzzles(),
                (float) com.piranport.terminal.TerminalParameters.getDouble(key + "initial_speed", c.initialSpeed()),
                (float) com.piranport.terminal.TerminalParameters.getDouble(key + "drag_coeff", c.dragCoeff()),
                (float) com.piranport.terminal.TerminalParameters.getDouble(key + "gravity", c.gravity()),
                (float) com.piranport.terminal.TerminalParameters.getDouble(key + "explosion_power", c.explosionPower()),
                (float) com.piranport.terminal.TerminalParameters.getDouble(key + "dispersion", c.dispersion()),
                com.piranport.terminal.TerminalParameters.getInt(key + "fire_cooldown", c.fireCooldown()),
                com.piranport.terminal.TerminalParameters.getInt(key + "salvo_count", c.salvoCount()),
                (float) com.piranport.terminal.TerminalParameters.getDouble(key + "salvo_interval", c.salvoInterval()),
                (float) com.piranport.terminal.TerminalParameters.getDouble(key + "projectile_weight", c.projectileWeight()),
                (float) com.piranport.terminal.TerminalParameters.getDouble(key + "vertical_spread", c.verticalSpread()),
                (float) com.piranport.terminal.TerminalParameters.getDouble(key + "horizontal_spread", c.horizontalSpread()),
                (float) com.piranport.terminal.TerminalParameters.getDouble(key + "max_elevation", c.maxElevation()),
                (float) com.piranport.terminal.TerminalParameters.getDouble(key + "min_elevation", c.minElevation()),
                (float) com.piranport.terminal.TerminalParameters.getDouble(key + "turret_speed", c.turretSpeed()),
                c.loadingMode());
    }

    // ==================== 弹药配置覆盖 ====================

    /**
     * 获取应用覆盖后的double类型弹药配置
     *
     * @param key 配置键（如 "AP_ARMOR_IGNORE"）
     * @param defaultValue 原始默认值
     * @param level 世界实例
     * @return 应用覆盖后的值
     */
    public static double getProjectileConfigDouble(String key, double defaultValue, @Nullable Level level) {
        return com.piranport.terminal.TerminalParameters.getDouble(
                "global.projectiles." + key.toLowerCase(java.util.Locale.ROOT), defaultValue);
    }

    /**
     * 获取应用覆盖后的boolean类型弹药配置
     */
    public static boolean getProjectileConfigBoolean(String key, boolean defaultValue, @Nullable Level level) {
        return com.piranport.terminal.TerminalParameters.getBoolean(
                "global.projectiles." + key.toLowerCase(java.util.Locale.ROOT), defaultValue);
    }

    /**
     * 获取应用覆盖后的int类型弹药配置
     */
    public static int getProjectileConfigInt(String key, int defaultValue, @Nullable Level level) {
        return com.piranport.terminal.TerminalParameters.getInt(
                "global.projectiles." + key.toLowerCase(java.util.Locale.ROOT), defaultValue);
    }

}
