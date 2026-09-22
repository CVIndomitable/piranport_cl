package com.piranport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 装备配置类 - Equipment Configuration
 * 管理所有装备系统的数值配置
 */
public class ModEquipmentConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ==================== 再装填设施 ====================
    public static final ModConfigSpec.IntValue RELOAD_FACILITY_LAND_TIME;
    public static final ModConfigSpec.IntValue RELOAD_FACILITY_WATER_TIME;

    // ==================== 声呐 ====================
    public static final ModConfigSpec.DoubleValue SONAR_RANGE;
    public static final ModConfigSpec.IntValue SONAR_DURATION;
    public static final ModConfigSpec.IntValue SONAR_COOLDOWN;

    // ==================== 雷达 ====================
    public static final ModConfigSpec.DoubleValue RADAR_RANGE;
    public static final ModConfigSpec.IntValue RADAR_DURATION;
    public static final ModConfigSpec.IntValue RADAR_COOLDOWN;

    // ==================== 火控系统 ====================
    public static final ModConfigSpec.DoubleValue FIRE_CONTROL_RANGE;
    public static final ModConfigSpec.IntValue FIRE_CONTROL_DURATION;
    public static final ModConfigSpec.IntValue FIRE_CONTROL_COOLDOWN;

    // ==================== 装甲板 ====================
    public static final ModConfigSpec.DoubleValue ARMOR_PLATE_PROTECTION;
    public static final ModConfigSpec.DoubleValue ARMOR_PLATE_TOUGHNESS;

    // ==================== Phase 5: 弹道解算 ====================
    /** 二分法最大迭代次数 */
    public static final ModConfigSpec.IntValue BALLISTIC_MAX_ITERATIONS;
    /** 弹道模拟步长（MC tick） */
    public static final ModConfigSpec.IntValue BALLISTIC_MAX_STEPS;
    /** 精度阈值（格） */
    public static final ModConfigSpec.DoubleValue BALLISTIC_ACCURACY;
    /** 最大缓存解数量 */
    public static final ModConfigSpec.IntValue BALLISTIC_CACHE_SIZE;
    /**
     * 瞄准完全生效所需的长按 tick 数。默认 1 表示按下右键当 tick 立即开镜，无延迟。
     * 调大可重现早期"需长按 N tick 才完全开镜"的手感。
     */
    public static final ModConfigSpec.IntValue SCOPE_ACTIVATION_TICKS;

    /** 近防炮参数保留现有射程/射速；负重默认 0，等待策划各型号定值后可直接配置。 */
    public record CIWSConfig(ModConfigSpec.DoubleValue range, ModConfigSpec.IntValue interval,
                             ModConfigSpec.IntValue barrels, ModConfigSpec.IntValue weight) {}

    public static final CIWSConfig CIWS_20MM;
    public static final CIWSConfig CIWS_40MM;
    public static final CIWSConfig CIWS_76MM;

    private static CIWSConfig defineCIWS(String caliber) {
        BUILDER.push(caliber);
        var range = BUILDER.comment("近防炮射程（格）").defineInRange("range", 16.0, 1.0, 128.0);
        var interval = BUILDER.comment("近防炮射击间隔（tick）").defineInRange("interval", 20, 1, 1200);
        var barrels = BUILDER.comment("近防炮联装数").defineInRange("barrels", 1, 1, 16);
        var weight = BUILDER.comment("近防炮负重：具体数值尚待策划定稿，默认保留现状 0")
                .defineInRange("weight", 0, 0, 112);
        BUILDER.pop();
        return new CIWSConfig(range, interval, barrels, weight);
    }

    static {
        BUILDER.push("ciws");
        CIWS_20MM = defineCIWS("20mm");
        CIWS_40MM = defineCIWS("40mm");
        CIWS_76MM = defineCIWS("76mm");
        BUILDER.pop();
        // ==================== 再装填设施 ====================
        BUILDER.push("equipment");
        BUILDER.push("reload_facility");

        RELOAD_FACILITY_LAND_TIME = BUILDER
            .comment("Reload time on land in ticks (20 ticks = 1 second) (陆地装填时间，策划 10s = 200 tick)")
            .defineInRange("land_reload_ticks", 200, 1, 6000);

        RELOAD_FACILITY_WATER_TIME = BUILDER
            .comment("Reload time on water in ticks (20 ticks = 1 second) (水上装填时间，策划 10s = 200 tick)")
            .defineInRange("water_reload_ticks", 200, 1, 6000);

        BUILDER.pop();

        // ==================== 声呐 ====================
        BUILDER.push("sonar");

        SONAR_RANGE = BUILDER
            .comment("Detection range in blocks (探测范围，方块数)")
            .defineInRange("range", 64.0, 1.0, 256.0);

        SONAR_DURATION = BUILDER
            .comment("Effect duration in ticks (20 ticks = 1 second) (效果持续时间，20 tick = 1秒)")
            .defineInRange("duration_ticks", 200, 20, 6000);

        SONAR_COOLDOWN = BUILDER
            .comment("Cooldown in ticks (20 ticks = 1 second) (冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 300, 20, 6000);

        BUILDER.pop();

        // ==================== 雷达 ====================
        BUILDER.push("radar");

        RADAR_RANGE = BUILDER
            .comment("Detection range in blocks (探测范围，方块数)")
            .defineInRange("range", 128.0, 1.0, 512.0);

        RADAR_DURATION = BUILDER
            .comment("Effect duration in ticks (20 ticks = 1 second) (效果持续时间，20 tick = 1秒)")
            .defineInRange("duration_ticks", 200, 20, 6000);

        RADAR_COOLDOWN = BUILDER
            .comment("Cooldown in ticks (20 ticks = 1 second) (冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 400, 20, 6000);

        BUILDER.pop();

        // ==================== 火控系统 ====================
        BUILDER.push("fire_control");

        FIRE_CONTROL_RANGE = BUILDER
            .comment("Targeting range in blocks (瞄准范围，方块数)")
            .defineInRange("range", 96.0, 1.0, 256.0);

        FIRE_CONTROL_DURATION = BUILDER
            .comment("Effect duration in ticks (20 ticks = 1 second) (效果持续时间，20 tick = 1秒)")
            .defineInRange("duration_ticks", 100, 20, 6000);

        FIRE_CONTROL_COOLDOWN = BUILDER
            .comment("Cooldown in ticks (20 ticks = 1 second) (冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 200, 20, 6000);

        BUILDER.pop();

        // ==================== Phase 5: 弹道解算 ====================
        BUILDER.push("ballistic");

        BALLISTIC_MAX_ITERATIONS = BUILDER
            .comment("Maximum bisection iterations (二分法最大迭代次数)")
            .defineInRange("max_iterations", 100, 5, 200);

        BALLISTIC_MAX_STEPS = BUILDER
            .comment("Maximum simulation steps per trajectory (弹道模拟最大步数)")
            .defineInRange("max_steps", 200, 50, 1000);

        BALLISTIC_ACCURACY = BUILDER
            .comment("Accuracy threshold in blocks (弹道解算停止阈值，单位：格)")
            .defineInRange("accuracy", 0.01, 0.001, 5.0);

        BALLISTIC_CACHE_SIZE = BUILDER
            .comment("Ballistic cache size (弹道解算缓存大小)")
            .defineInRange("cache_size", 32, 1, 256);

        SCOPE_ACTIVATION_TICKS = BUILDER
            .comment("Ticks of right-click hold before the scope fully activates. 1 = instant, no delay.",
                     "瞄准完全生效所需的长按 tick 数；1 = 按下即开镜，无延迟")
            .defineInRange("activation_ticks", 1, 1, 40);

        BUILDER.pop();

        // ==================== 装甲板 ====================
        BUILDER.push("armor_plate");

        ARMOR_PLATE_PROTECTION = BUILDER
            .comment("Armor protection value (护甲保护值)")
            .defineInRange("protection", 3.0, 0.0, 100.0);

        ARMOR_PLATE_TOUGHNESS = BUILDER
            .comment("Armor toughness value (护甲韧性值)")
            .defineInRange("toughness", 2.0, 0.0, 100.0);

        BUILDER.pop();
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
