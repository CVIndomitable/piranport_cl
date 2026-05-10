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

    static {
        // ==================== 再装填设施 ====================
        BUILDER.push("equipment");
        BUILDER.push("reload_facility");

        RELOAD_FACILITY_LAND_TIME = BUILDER
            .comment("Reload time on land in ticks (20 ticks = 1 second) (陆地装填时间，20 tick = 1秒)")
            .defineInRange("land_reload_ticks", 100, 1, 6000);

        RELOAD_FACILITY_WATER_TIME = BUILDER
            .comment("Reload time on water in ticks (20 ticks = 1 second) (水上装填时间，20 tick = 1秒)")
            .defineInRange("water_reload_ticks", 60, 1, 6000);

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
