package com.piranport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 飞机配置类 - Aircraft Configuration
 * 管理所有飞机系统的数值配置
 */
public class ModAircraftConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ==================== 战斗机 ====================
    public static final ModConfigSpec.DoubleValue FIGHTER_DAMAGE;
    public static final ModConfigSpec.IntValue FIGHTER_COOLDOWN;
    public static final ModConfigSpec.DoubleValue FIGHTER_SPEED;
    public static final ModConfigSpec.IntValue FIGHTER_HEALTH;

    // ==================== 火箭机 ====================
    public static final ModConfigSpec.DoubleValue ROCKET_FIGHTER_DAMAGE;
    public static final ModConfigSpec.IntValue ROCKET_FIGHTER_COOLDOWN;
    public static final ModConfigSpec.DoubleValue ROCKET_FIGHTER_SPEED;
    public static final ModConfigSpec.IntValue ROCKET_FIGHTER_HEALTH;

    // ==================== 俯冲轰炸机 ====================
    public static final ModConfigSpec.DoubleValue DIVE_BOMBER_DAMAGE;
    public static final ModConfigSpec.IntValue DIVE_BOMBER_COOLDOWN;
    public static final ModConfigSpec.DoubleValue DIVE_BOMBER_SPEED;
    public static final ModConfigSpec.IntValue DIVE_BOMBER_HEALTH;

    // ==================== 水平轰炸机 ====================
    public static final ModConfigSpec.DoubleValue LEVEL_BOMBER_DAMAGE;
    public static final ModConfigSpec.IntValue LEVEL_BOMBER_COOLDOWN;
    public static final ModConfigSpec.DoubleValue LEVEL_BOMBER_SPEED;
    public static final ModConfigSpec.IntValue LEVEL_BOMBER_HEALTH;

    // ==================== 鱼雷机 ====================
    public static final ModConfigSpec.DoubleValue TORPEDO_BOMBER_DAMAGE;
    public static final ModConfigSpec.IntValue TORPEDO_BOMBER_COOLDOWN;
    public static final ModConfigSpec.DoubleValue TORPEDO_BOMBER_SPEED;
    public static final ModConfigSpec.IntValue TORPEDO_BOMBER_HEALTH;

    // ==================== 反潜机 ====================
    public static final ModConfigSpec.DoubleValue ASW_AIRCRAFT_DAMAGE;
    public static final ModConfigSpec.IntValue ASW_AIRCRAFT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue ASW_AIRCRAFT_SPEED;
    public static final ModConfigSpec.IntValue ASW_AIRCRAFT_HEALTH;

    // ==================== 侦察机 ====================
    public static final ModConfigSpec.DoubleValue RECON_AIRCRAFT_SPEED;
    public static final ModConfigSpec.IntValue RECON_AIRCRAFT_HEALTH;
    public static final ModConfigSpec.IntValue RECON_DURATION;

    static {
        // ==================== 战斗机 ====================
        BUILDER.push("aircraft");
        BUILDER.push("fighter");

        FIGHTER_DAMAGE = BUILDER
            .comment("Damage per attack (每次攻击伤害)")
            .defineInRange("damage", 8.0, 0.1, 1000.0);

        FIGHTER_COOLDOWN = BUILDER
            .comment("Attack cooldown in ticks (20 ticks = 1 second) (攻击冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 40, 1, 6000);

        FIGHTER_SPEED = BUILDER
            .comment("Flight speed (飞行速度)")
            .defineInRange("speed", 0.8, 0.1, 5.0);

        FIGHTER_HEALTH = BUILDER
            .comment("Aircraft health points (飞机生命值)")
            .defineInRange("health", 20, 1, 1000);

        BUILDER.pop();

        // ==================== 火箭机 ====================
        BUILDER.push("rocket_fighter");

        ROCKET_FIGHTER_DAMAGE = BUILDER
            .comment("Damage per rocket (每枚火箭伤害)")
            .defineInRange("damage", 12.0, 0.1, 1000.0);

        ROCKET_FIGHTER_COOLDOWN = BUILDER
            .comment("Attack cooldown in ticks (20 ticks = 1 second) (攻击冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 60, 1, 6000);

        ROCKET_FIGHTER_SPEED = BUILDER
            .comment("Flight speed (飞行速度)")
            .defineInRange("speed", 0.8, 0.1, 5.0);

        ROCKET_FIGHTER_HEALTH = BUILDER
            .comment("Aircraft health points (飞机生命值)")
            .defineInRange("health", 20, 1, 1000);

        BUILDER.pop();

        // ==================== 俯冲轰炸机 ====================
        BUILDER.push("dive_bomber");

        DIVE_BOMBER_DAMAGE = BUILDER
            .comment("Damage per bomb (每枚炸弹伤害)")
            .defineInRange("damage", 20.0, 0.1, 1000.0);

        DIVE_BOMBER_COOLDOWN = BUILDER
            .comment("Attack cooldown in ticks (20 ticks = 1 second) (攻击冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 100, 1, 6000);

        DIVE_BOMBER_SPEED = BUILDER
            .comment("Flight speed (飞行速度)")
            .defineInRange("speed", 0.7, 0.1, 5.0);

        DIVE_BOMBER_HEALTH = BUILDER
            .comment("Aircraft health points (飞机生命值)")
            .defineInRange("health", 25, 1, 1000);

        BUILDER.pop();

        // ==================== 水平轰炸机 ====================
        BUILDER.push("level_bomber");

        LEVEL_BOMBER_DAMAGE = BUILDER
            .comment("Damage per bomb (每枚炸弹伤害)")
            .defineInRange("damage", 25.0, 0.1, 1000.0);

        LEVEL_BOMBER_COOLDOWN = BUILDER
            .comment("Attack cooldown in ticks (20 ticks = 1 second) (攻击冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 120, 1, 6000);

        LEVEL_BOMBER_SPEED = BUILDER
            .comment("Flight speed (飞行速度)")
            .defineInRange("speed", 0.6, 0.1, 5.0);

        LEVEL_BOMBER_HEALTH = BUILDER
            .comment("Aircraft health points (飞机生命值)")
            .defineInRange("health", 30, 1, 1000);

        BUILDER.pop();

        // ==================== 鱼雷机 ====================
        BUILDER.push("torpedo_bomber");

        TORPEDO_BOMBER_DAMAGE = BUILDER
            .comment("Damage per torpedo (每枚鱼雷伤害)")
            .defineInRange("damage", 30.0, 0.1, 1000.0);

        TORPEDO_BOMBER_COOLDOWN = BUILDER
            .comment("Attack cooldown in ticks (20 ticks = 1 second) (攻击冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 100, 1, 6000);

        TORPEDO_BOMBER_SPEED = BUILDER
            .comment("Flight speed (飞行速度)")
            .defineInRange("speed", 0.7, 0.1, 5.0);

        TORPEDO_BOMBER_HEALTH = BUILDER
            .comment("Aircraft health points (飞机生命值)")
            .defineInRange("health", 25, 1, 1000);

        BUILDER.pop();

        // ==================== 反潜机 ====================
        BUILDER.push("asw_aircraft");

        ASW_AIRCRAFT_DAMAGE = BUILDER
            .comment("Damage per depth charge (每枚深弹伤害)")
            .defineInRange("damage", 15.0, 0.1, 1000.0);

        ASW_AIRCRAFT_COOLDOWN = BUILDER
            .comment("Attack cooldown in ticks (20 ticks = 1 second) (攻击冷却时间，20 tick = 1秒)")
            .defineInRange("cooldown_ticks", 80, 1, 6000);

        ASW_AIRCRAFT_SPEED = BUILDER
            .comment("Flight speed (飞行速度)")
            .defineInRange("speed", 0.7, 0.1, 5.0);

        ASW_AIRCRAFT_HEALTH = BUILDER
            .comment("Aircraft health points (飞机生命值)")
            .defineInRange("health", 20, 1, 1000);

        BUILDER.pop();

        // ==================== 侦察机 ====================
        BUILDER.push("recon_aircraft");

        RECON_AIRCRAFT_SPEED = BUILDER
            .comment("Flight speed (飞行速度)")
            .defineInRange("speed", 1.0, 0.1, 5.0);

        RECON_AIRCRAFT_HEALTH = BUILDER
            .comment("Aircraft health points (飞机生命值)")
            .defineInRange("health", 15, 1, 1000);

        RECON_DURATION = BUILDER
            .comment("Reconnaissance duration in ticks (20 ticks = 1 second) (侦察持续时间，20 tick = 1秒)")
            .defineInRange("duration_ticks", 600, 20, 12000);

        BUILDER.pop();
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
