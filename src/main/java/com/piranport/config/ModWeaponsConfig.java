package com.piranport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 武器配置类 - Weapons Configuration
 * 管理所有武器系统的数值配置
 */
public class ModWeaponsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ==================== 单装小口径炮 ====================
    public static final ModConfigSpec.DoubleValue SMALL_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue SMALL_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SMALL_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue SMALL_GUN_INACCURACY;

    // ==================== 双联装小口径炮 ====================
    public static final ModConfigSpec.DoubleValue TWIN_SMALL_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue TWIN_SMALL_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue TWIN_SMALL_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue TWIN_SMALL_GUN_INACCURACY;

    // ==================== 单装中口径炮 ====================
    public static final ModConfigSpec.DoubleValue MEDIUM_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue MEDIUM_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue MEDIUM_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue MEDIUM_GUN_INACCURACY;

    // ==================== 双联装中口径炮 ====================
    public static final ModConfigSpec.DoubleValue TWIN_MEDIUM_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue TWIN_MEDIUM_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue TWIN_MEDIUM_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue TWIN_MEDIUM_GUN_INACCURACY;

    // ==================== 单装大口径炮 ====================
    public static final ModConfigSpec.DoubleValue LARGE_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue LARGE_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue LARGE_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue LARGE_GUN_INACCURACY;

    // ==================== 双联装大口径炮 ====================
    public static final ModConfigSpec.DoubleValue TWIN_LARGE_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue TWIN_LARGE_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue TWIN_LARGE_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue TWIN_LARGE_GUN_INACCURACY;

    // ==================== 三联装大口径炮 ====================
    public static final ModConfigSpec.DoubleValue TRIPLE_LARGE_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue TRIPLE_LARGE_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue TRIPLE_LARGE_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue TRIPLE_LARGE_GUN_INACCURACY;

    // ==================== 四联装大口径炮 ====================
    public static final ModConfigSpec.DoubleValue QUAD_LARGE_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue QUAD_LARGE_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue QUAD_LARGE_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue QUAD_LARGE_GUN_INACCURACY;

    static {
        // ==================== 单装小口径炮 ====================
        BUILDER.push("cannon");
        BUILDER.push("single_small_gun");

        SMALL_GUN_DAMAGE = BUILDER
            .comment("Damage per shell (每发炮弹伤害)")
            .defineInRange("damage", 6.0, 0.1, 1000.0);

        SMALL_GUN_COOLDOWN = BUILDER
            .comment("Cooldown in ticks (20 ticks = 1 second) (冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 30, 1, 6000);

        SMALL_GUN_VELOCITY = BUILDER
            .comment("Projectile velocity (弹丸速度)")
            .defineInRange("velocity", 3.0, 0.1, 10.0);

        SMALL_GUN_INACCURACY = BUILDER
            .comment("Firing inaccuracy (射击精度偏差)")
            .defineInRange("inaccuracy", 1.0, 0.0, 10.0);

        BUILDER.pop();

        // ==================== 双联装小口径炮 ====================
        BUILDER.push("twin_small_gun");

        TWIN_SMALL_GUN_DAMAGE = BUILDER
            .comment("Damage per shell (每发炮弹伤害)")
            .defineInRange("damage", 6.0, 0.1, 1000.0);

        TWIN_SMALL_GUN_COOLDOWN = BUILDER
            .comment("Cooldown in ticks (20 ticks = 1 second) (冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 30, 1, 6000);

        TWIN_SMALL_GUN_VELOCITY = BUILDER
            .comment("Projectile velocity (弹丸速度)")
            .defineInRange("velocity", 3.0, 0.1, 10.0);

        TWIN_SMALL_GUN_INACCURACY = BUILDER
            .comment("Firing inaccuracy (射击精度偏差)")
            .defineInRange("inaccuracy", 1.0, 0.0, 10.0);

        BUILDER.pop();

        // ==================== 单装中口径炮 ====================
        BUILDER.push("single_medium_gun");

        MEDIUM_GUN_DAMAGE = BUILDER
            .comment("Damage per shell (每发炮弹伤害)")
            .defineInRange("damage", 12.0, 0.1, 1000.0);

        MEDIUM_GUN_COOLDOWN = BUILDER
            .comment("Cooldown in ticks (20 ticks = 1 second) (冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 50, 1, 6000);

        MEDIUM_GUN_VELOCITY = BUILDER
            .comment("Projectile velocity (弹丸速度)")
            .defineInRange("velocity", 3.5, 0.1, 10.0);

        MEDIUM_GUN_INACCURACY = BUILDER
            .comment("Firing inaccuracy (射击精度偏差)")
            .defineInRange("inaccuracy", 0.8, 0.0, 10.0);

        BUILDER.pop();

        // ==================== 双联装中口径炮 ====================
        BUILDER.push("twin_medium_gun");

        TWIN_MEDIUM_GUN_DAMAGE = BUILDER
            .comment("Damage per shell (每发炮弹伤害)")
            .defineInRange("damage", 12.0, 0.1, 1000.0);

        TWIN_MEDIUM_GUN_COOLDOWN = BUILDER
            .comment("Cooldown in ticks (20 ticks = 1 second) (冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 50, 1, 6000);

        TWIN_MEDIUM_GUN_VELOCITY = BUILDER
            .comment("Projectile velocity (弹丸速度)")
            .defineInRange("velocity", 3.5, 0.1, 10.0);

        TWIN_MEDIUM_GUN_INACCURACY = BUILDER
            .comment("Firing inaccuracy (射击精度偏差)")
            .defineInRange("inaccuracy", 0.8, 0.0, 10.0);

        BUILDER.pop();

        // ==================== 单装大口径炮 ====================
        BUILDER.push("single_large_gun");

        LARGE_GUN_DAMAGE = BUILDER
            .comment("Damage per shell (每发炮弹伤害)")
            .defineInRange("damage", 15.0, 0.1, 1000.0);

        LARGE_GUN_COOLDOWN = BUILDER
            .comment("Cooldown in ticks (20 ticks = 1 second) (冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 60, 1, 6000);

        LARGE_GUN_VELOCITY = BUILDER
            .comment("Projectile velocity (弹丸速度)")
            .defineInRange("velocity", 4.0, 0.1, 10.0);

        LARGE_GUN_INACCURACY = BUILDER
            .comment("Firing inaccuracy (射击精度偏差)")
            .defineInRange("inaccuracy", 0.5, 0.0, 10.0);

        BUILDER.pop();

        // ==================== 双联装大口径炮 ====================
        BUILDER.push("twin_large_gun");

        TWIN_LARGE_GUN_DAMAGE = BUILDER
            .comment("Damage per shell (每发炮弹伤害)")
            .defineInRange("damage", 20.0, 0.1, 1000.0);

        TWIN_LARGE_GUN_COOLDOWN = BUILDER
            .comment("Cooldown in ticks (20 ticks = 1 second) (冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 80, 1, 6000);

        TWIN_LARGE_GUN_VELOCITY = BUILDER
            .comment("Projectile velocity (弹丸速度)")
            .defineInRange("velocity", 4.0, 0.1, 10.0);

        TWIN_LARGE_GUN_INACCURACY = BUILDER
            .comment("Firing inaccuracy (射击精度偏差)")
            .defineInRange("inaccuracy", 0.5, 0.0, 10.0);

        BUILDER.pop();

        // ==================== 三联装大口径炮 ====================
        BUILDER.push("triple_large_gun");

        TRIPLE_LARGE_GUN_DAMAGE = BUILDER
            .comment("Damage per shell (每发炮弹伤害)")
            .defineInRange("damage", 20.0, 0.1, 1000.0);

        TRIPLE_LARGE_GUN_COOLDOWN = BUILDER
            .comment("Cooldown in ticks (20 ticks = 1 second) (冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 80, 1, 6000);

        TRIPLE_LARGE_GUN_VELOCITY = BUILDER
            .comment("Projectile velocity (弹丸速度)")
            .defineInRange("velocity", 4.0, 0.1, 10.0);

        TRIPLE_LARGE_GUN_INACCURACY = BUILDER
            .comment("Firing inaccuracy (射击精度偏差)")
            .defineInRange("inaccuracy", 0.5, 0.0, 10.0);

        BUILDER.pop();
        BUILDER.pop();

        // ==================== 四联装大口径炮 ====================
        BUILDER.push("quad_large_gun");

        QUAD_LARGE_GUN_DAMAGE = BUILDER
            .comment("Damage per shell (每发炮弹伤害)")
            .defineInRange("damage", 20.0, 0.1, 1000.0);

        QUAD_LARGE_GUN_COOLDOWN = BUILDER
            .comment("Cooldown in ticks (20 ticks = 1 second) (冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 60, 1, 6000);

        QUAD_LARGE_GUN_VELOCITY = BUILDER
            .comment("Projectile velocity (弹丸速度)")
            .defineInRange("velocity", 4.0, 0.1, 10.0);

        QUAD_LARGE_GUN_INACCURACY = BUILDER
            .comment("Firing inaccuracy (射击精度偏差)")
            .defineInRange("inaccuracy", 0.5, 0.0, 10.0);

        BUILDER.pop();
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
