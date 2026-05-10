package com.piranport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 舰装配置类 - Ships Configuration
 * 管理所有舰装系统的数值配置
 */
public class ModShipsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ==================== 驱逐舰装 ====================
    public static final ModConfigSpec.DoubleValue DESTROYER_ARMOR;
    public static final ModConfigSpec.DoubleValue DESTROYER_SPEED_BONUS;
    public static final ModConfigSpec.DoubleValue DESTROYER_KNOCKBACK_RESISTANCE;

    // ==================== 轻巡舰装 ====================
    public static final ModConfigSpec.DoubleValue LIGHT_CRUISER_ARMOR;
    public static final ModConfigSpec.DoubleValue LIGHT_CRUISER_SPEED_BONUS;
    public static final ModConfigSpec.DoubleValue LIGHT_CRUISER_KNOCKBACK_RESISTANCE;

    // ==================== 重巡舰装 ====================
    public static final ModConfigSpec.DoubleValue HEAVY_CRUISER_ARMOR;
    public static final ModConfigSpec.DoubleValue HEAVY_CRUISER_SPEED_BONUS;
    public static final ModConfigSpec.DoubleValue HEAVY_CRUISER_KNOCKBACK_RESISTANCE;

    // ==================== 战列舰装 ====================
    public static final ModConfigSpec.DoubleValue BATTLESHIP_ARMOR;
    public static final ModConfigSpec.DoubleValue BATTLESHIP_SPEED_BONUS;
    public static final ModConfigSpec.DoubleValue BATTLESHIP_KNOCKBACK_RESISTANCE;

    // ==================== 航母舰装 ====================
    public static final ModConfigSpec.DoubleValue CARRIER_ARMOR;
    public static final ModConfigSpec.DoubleValue CARRIER_SPEED_BONUS;
    public static final ModConfigSpec.DoubleValue CARRIER_KNOCKBACK_RESISTANCE;

    // ==================== 潜艇舰装 ====================
    public static final ModConfigSpec.DoubleValue SUBMARINE_ARMOR;
    public static final ModConfigSpec.DoubleValue SUBMARINE_SPEED_BONUS;
    public static final ModConfigSpec.DoubleValue SUBMARINE_KNOCKBACK_RESISTANCE;
    public static final ModConfigSpec.IntValue SUBMARINE_DIVE_DURATION;
    public static final ModConfigSpec.IntValue SUBMARINE_SURFACE_COOLDOWN;

    static {
        // ==================== 驱逐舰装 ====================
        BUILDER.push("ships");
        BUILDER.push("destroyer");

        DESTROYER_ARMOR = BUILDER
            .comment("Armor points (护甲值)")
            .defineInRange("armor", 4.0, 0.0, 100.0);

        DESTROYER_SPEED_BONUS = BUILDER
            .comment("Movement speed bonus (移动速度加成)")
            .defineInRange("speed_bonus", 0.1, 0.0, 1.0);

        DESTROYER_KNOCKBACK_RESISTANCE = BUILDER
            .comment("Knockback resistance (击退抗性)")
            .defineInRange("knockback_resistance", 0.2, 0.0, 1.0);

        BUILDER.pop();

        // ==================== 轻巡舰装 ====================
        BUILDER.push("light_cruiser");

        LIGHT_CRUISER_ARMOR = BUILDER
            .comment("Armor points (护甲值)")
            .defineInRange("armor", 6.0, 0.0, 100.0);

        LIGHT_CRUISER_SPEED_BONUS = BUILDER
            .comment("Movement speed bonus (移动速度加成)")
            .defineInRange("speed_bonus", 0.05, 0.0, 1.0);

        LIGHT_CRUISER_KNOCKBACK_RESISTANCE = BUILDER
            .comment("Knockback resistance (击退抗性)")
            .defineInRange("knockback_resistance", 0.3, 0.0, 1.0);

        BUILDER.pop();

        // ==================== 重巡舰装 ====================
        BUILDER.push("heavy_cruiser");

        HEAVY_CRUISER_ARMOR = BUILDER
            .comment("Armor points (护甲值)")
            .defineInRange("armor", 8.0, 0.0, 100.0);

        HEAVY_CRUISER_SPEED_BONUS = BUILDER
            .comment("Movement speed bonus (移动速度加成)")
            .defineInRange("speed_bonus", 0.0, 0.0, 1.0);

        HEAVY_CRUISER_KNOCKBACK_RESISTANCE = BUILDER
            .comment("Knockback resistance (击退抗性)")
            .defineInRange("knockback_resistance", 0.5, 0.0, 1.0);

        BUILDER.pop();

        // ==================== 战列舰装 ====================
        BUILDER.push("battleship");

        BATTLESHIP_ARMOR = BUILDER
            .comment("Armor points (护甲值)")
            .defineInRange("armor", 12.0, 0.0, 100.0);

        BATTLESHIP_SPEED_BONUS = BUILDER
            .comment("Movement speed bonus (移动速度加成)")
            .defineInRange("speed_bonus", -0.05, -1.0, 1.0);

        BATTLESHIP_KNOCKBACK_RESISTANCE = BUILDER
            .comment("Knockback resistance (击退抗性)")
            .defineInRange("knockback_resistance", 0.8, 0.0, 1.0);

        BUILDER.pop();

        // ==================== 航母舰装 ====================
        BUILDER.push("carrier");

        CARRIER_ARMOR = BUILDER
            .comment("Armor points (护甲值)")
            .defineInRange("armor", 5.0, 0.0, 100.0);

        CARRIER_SPEED_BONUS = BUILDER
            .comment("Movement speed bonus (移动速度加成)")
            .defineInRange("speed_bonus", 0.0, 0.0, 1.0);

        CARRIER_KNOCKBACK_RESISTANCE = BUILDER
            .comment("Knockback resistance (击退抗性)")
            .defineInRange("knockback_resistance", 0.4, 0.0, 1.0);

        BUILDER.pop();

        // ==================== 潜艇舰装 ====================
        BUILDER.push("submarine");

        SUBMARINE_ARMOR = BUILDER
            .comment("Armor points (护甲值)")
            .defineInRange("armor", 3.0, 0.0, 100.0);

        SUBMARINE_SPEED_BONUS = BUILDER
            .comment("Movement speed bonus (移动速度加成)")
            .defineInRange("speed_bonus", 0.0, 0.0, 1.0);

        SUBMARINE_KNOCKBACK_RESISTANCE = BUILDER
            .comment("Knockback resistance (击退抗性)")
            .defineInRange("knockback_resistance", 0.1, 0.0, 1.0);

        SUBMARINE_DIVE_DURATION = BUILDER
            .comment("Dive duration in ticks (20 ticks = 1 second) (潜航持续时间，20 tick = 1秒)")
            .defineInRange("dive_duration_ticks", 600, 20, 12000);

        SUBMARINE_SURFACE_COOLDOWN = BUILDER
            .comment("Surface cooldown in ticks (20 ticks = 1 second) (上浮冷却时间，20 tick = 1秒)")
            .defineInRange("surface_cooldown_ticks", 200, 20, 6000);

        BUILDER.pop();
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
