package com.piranport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 弹药配置类 - Projectiles Configuration
 * 管理所有弹药系统的数值配置
 */
public class ModProjectilesConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ==================== 鱼雷 ====================
    public static final ModConfigSpec.DoubleValue TORPEDO_DAMAGE;
    public static final ModConfigSpec.DoubleValue TORPEDO_SPEED;
    public static final ModConfigSpec.IntValue TORPEDO_LIFETIME;
    public static final ModConfigSpec.DoubleValue TORPEDO_EXPLOSION_RADIUS;

    // ==================== 线导鱼雷 ====================
    public static final ModConfigSpec.DoubleValue WIRE_GUIDED_TORPEDO_DAMAGE;
    public static final ModConfigSpec.DoubleValue WIRE_GUIDED_TORPEDO_SPEED;
    public static final ModConfigSpec.IntValue WIRE_GUIDED_TORPEDO_LIFETIME;
    public static final ModConfigSpec.DoubleValue WIRE_GUIDED_TORPEDO_EXPLOSION_RADIUS;
    public static final ModConfigSpec.DoubleValue WIRE_GUIDED_TORPEDO_TURN_SPEED;

    // ==================== 深水炸弹 ====================
    public static final ModConfigSpec.DoubleValue DEPTH_CHARGE_DAMAGE;
    public static final ModConfigSpec.DoubleValue DEPTH_CHARGE_EXPLOSION_RADIUS;
    public static final ModConfigSpec.IntValue DEPTH_CHARGE_FUSE_TIME;

    // ==================== 炸弹 ====================
    public static final ModConfigSpec.DoubleValue BOMB_DAMAGE;
    public static final ModConfigSpec.DoubleValue BOMB_EXPLOSION_RADIUS;
    public static final ModConfigSpec.IntValue BOMB_FUSE_TIME;

    // ==================== 火箭弹 ====================
    public static final ModConfigSpec.DoubleValue ROCKET_DAMAGE;
    public static final ModConfigSpec.DoubleValue ROCKET_SPEED;
    public static final ModConfigSpec.DoubleValue ROCKET_EXPLOSION_RADIUS;

    static {
        // ==================== 鱼雷 ====================
        BUILDER.push("projectiles");
        BUILDER.push("torpedo");

        TORPEDO_DAMAGE = BUILDER
            .comment("Torpedo damage (鱼雷伤害)")
            .defineInRange("damage", 30.0, 0.1, 1000.0);

        TORPEDO_SPEED = BUILDER
            .comment("Torpedo speed (鱼雷速度)")
            .defineInRange("speed", 1.5, 0.1, 10.0);

        TORPEDO_LIFETIME = BUILDER
            .comment("Torpedo lifetime in ticks (20 ticks = 1 second) (鱼雷存活时间，20 tick = 1秒)")
            .defineInRange("lifetime_ticks", 200, 20, 6000);

        TORPEDO_EXPLOSION_RADIUS = BUILDER
            .comment("Explosion radius (爆炸半径)")
            .defineInRange("explosion_radius", 3.0, 0.1, 20.0);

        BUILDER.pop();

        // ==================== 线导鱼雷 ====================
        BUILDER.push("wire_guided_torpedo");

        WIRE_GUIDED_TORPEDO_DAMAGE = BUILDER
            .comment("Wire-guided torpedo damage (线导鱼雷伤害)")
            .defineInRange("damage", 35.0, 0.1, 1000.0);

        WIRE_GUIDED_TORPEDO_SPEED = BUILDER
            .comment("Wire-guided torpedo speed (线导鱼雷速度)")
            .defineInRange("speed", 1.2, 0.1, 10.0);

        WIRE_GUIDED_TORPEDO_LIFETIME = BUILDER
            .comment("Wire-guided torpedo lifetime in ticks (20 ticks = 1 second) (线导鱼雷存活时间，20 tick = 1秒)")
            .defineInRange("lifetime_ticks", 300, 20, 6000);

        WIRE_GUIDED_TORPEDO_EXPLOSION_RADIUS = BUILDER
            .comment("Explosion radius (爆炸半径)")
            .defineInRange("explosion_radius", 3.5, 0.1, 20.0);

        WIRE_GUIDED_TORPEDO_TURN_SPEED = BUILDER
            .comment("Turn speed (转向速度)")
            .defineInRange("turn_speed", 2.0, 0.1, 10.0);

        BUILDER.pop();

        // ==================== 深水炸弹 ====================
        BUILDER.push("depth_charge");

        DEPTH_CHARGE_DAMAGE = BUILDER
            .comment("Depth charge damage (深水炸弹伤害)")
            .defineInRange("damage", 25.0, 0.1, 1000.0);

        DEPTH_CHARGE_EXPLOSION_RADIUS = BUILDER
            .comment("Explosion radius (爆炸半径)")
            .defineInRange("explosion_radius", 4.0, 0.1, 20.0);

        DEPTH_CHARGE_FUSE_TIME = BUILDER
            .comment("Fuse time in ticks (20 ticks = 1 second) (引信时间，20 tick = 1秒)")
            .defineInRange("fuse_time_ticks", 40, 1, 600);

        BUILDER.pop();

        // ==================== 炸弹 ====================
        BUILDER.push("bomb");

        BOMB_DAMAGE = BUILDER
            .comment("Bomb damage (炸弹伤害)")
            .defineInRange("damage", 20.0, 0.1, 1000.0);

        BOMB_EXPLOSION_RADIUS = BUILDER
            .comment("Explosion radius (爆炸半径)")
            .defineInRange("explosion_radius", 3.5, 0.1, 20.0);

        BOMB_FUSE_TIME = BUILDER
            .comment("Fuse time in ticks (20 ticks = 1 second) (引信时间，20 tick = 1秒)")
            .defineInRange("fuse_time_ticks", 30, 1, 600);

        BUILDER.pop();

        // ==================== 火箭弹 ====================
        BUILDER.push("rocket");

        ROCKET_DAMAGE = BUILDER
            .comment("Rocket damage (火箭弹伤害)")
            .defineInRange("damage", 15.0, 0.1, 1000.0);

        ROCKET_SPEED = BUILDER
            .comment("Rocket speed (火箭弹速度)")
            .defineInRange("speed", 2.0, 0.1, 10.0);

        ROCKET_EXPLOSION_RADIUS = BUILDER
            .comment("Explosion radius (爆炸半径)")
            .defineInRange("explosion_radius", 2.5, 0.1, 20.0);

        BUILDER.pop();
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
