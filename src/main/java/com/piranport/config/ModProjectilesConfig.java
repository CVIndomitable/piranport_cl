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

    // ==================== 炮弹 ====================
    /** @deprecated 已废弃，请使用 ModArtilleryConfig.ARTILLERY_UNDERWATER_DESTROY_TIME。保留以兼容旧配置文件。 */
    @Deprecated
    public static final ModConfigSpec.IntValue UNDERWATER_DESTROY_TICKS;
    public static final ModConfigSpec.DoubleValue UNDERWATER_EXPLOSION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue HE_ARMOR_PENETRATION;
    public static final ModConfigSpec.BooleanValue HE_DAMAGE_FALLOFF;
    public static final ModConfigSpec.BooleanValue UNDERWATER_EXPLODE;

    // ==================== AP弹 ====================
    public static final ModConfigSpec.DoubleValue AP_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue AP_ARMOR_IGNORE;

    // ==================== 鱼雷发射管 ====================
    public static final ModConfigSpec.IntValue TWIN_TORPEDO_LAUNCHER_COOLDOWN;
    public static final ModConfigSpec.IntValue TRIPLE_TORPEDO_LAUNCHER_COOLDOWN;
    public static final ModConfigSpec.IntValue QUAD_TORPEDO_LAUNCHER_COOLDOWN;

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

        // ==================== 炮弹（Shell） ====================
        BUILDER.push("shell");

        UNDERWATER_DESTROY_TICKS = BUILDER
            .comment(
                "Ticks before shell is destroyed underwater (水中弹药销毁时间，20 tick = 1秒).",
                "Default: 60 (3 seconds). After this many ticks underwater the shell",
                "is either silently removed or explodes depending on underwater_explode.",
                "默认60 tick（3秒），水中弹药到期后根据 underwater_explode 决定静默销毁或爆炸")
            .defineInRange("underwater_destroy_ticks", 60, 1, 600);

        UNDERWATER_EXPLOSION_MULTIPLIER = BUILDER
            .comment(
                "Explosion radius multiplier when detonating underwater (水中爆炸半径倍率).",
                "Default: 0.5 (half radius). Water dampens the blast.",
                "默认0.5，水中爆炸半径减半")
            .defineInRange("underwater_explosion_multiplier", 0.5, 0.0, 2.0);

        HE_ARMOR_PENETRATION = BUILDER
            .comment(
                "HE shell armor penetration ratio 0-1 (HE弹护甲穿透比例).",
                "Default: 0.3. PER tick reduces target armor by this fraction before dealing",
                "explosion damage. 0 = no penetration, 1 = full armor ignore.",
                "默认0.3，爆炸伤害临时忽略目标30%护甲")
            .defineInRange("he_armor_penetration", 0.3, 0.0, 1.0);

        HE_DAMAGE_FALLOFF = BUILDER
            .comment(
                "Enable distance-based damage falloff for HE explosions (HE弹爆炸伤害距离衰减).",
                "Default: true. Damage = base * (1 - distance/radius).",
                "Set to false for uniform damage across the entire blast radius.",
                "默认开启，伤害随距离线性衰减")
            .define("he_damage_falloff", true);

        UNDERWATER_EXPLODE = BUILDER
            .comment(
                "When true, underwater shell explodes when timer expires (水中弹药到期爆炸).",
                "Default: false (silent removal). true = explode at reduced radius.",
                "默认关闭（静默销毁），开启后到期在水中爆炸")
            .define("underwater_explode", false);

        // ==================== AP弹 ====================
        AP_DAMAGE_MULTIPLIER = BUILDER
            .comment(
                "AP shell damage multiplier relative to base gun damage (AP弹伤害倍率，相对火炮基础伤害).",
                "Default: 1.3 (130% of base). AP shell damage = base * multiplier * speed_ratio.",
                "默认1.3，AP弹伤害 = 基础伤害 × 倍率 × 速度比")
            .defineInRange("ap_damage_multiplier", 1.3, 0.1, 5.0);

        AP_ARMOR_IGNORE = BUILDER
            .comment(
                "AP shell armor ignore ratio 0-1 (AP弹护甲忽略比例).",
                "Default: 0.5 (ignores 50% of target armor).",
                "默认0.5，忽略目标50%护甲")
            .defineInRange("ap_armor_ignore", 0.5, 0.0, 1.0);

        BUILDER.pop();

        // ==================== 鱼雷发射管 ====================
        BUILDER.push("torpedo_launcher");

        TWIN_TORPEDO_LAUNCHER_COOLDOWN = BUILDER
            .comment("Twin torpedo launcher cooldown in ticks (双联鱼雷发射器冷却)")
            .defineInRange("twin_cooldown_ticks", 100, 1, 6000);

        TRIPLE_TORPEDO_LAUNCHER_COOLDOWN = BUILDER
            .comment("Triple torpedo launcher cooldown in ticks (三联鱼雷发射器冷却)")
            .defineInRange("triple_cooldown_ticks", 100, 1, 6000);

        QUAD_TORPEDO_LAUNCHER_COOLDOWN = BUILDER
            .comment("Quad torpedo launcher cooldown in ticks (四联鱼雷发射器冷却)")
            .defineInRange("quad_cooldown_ticks", 120, 1, 6000);

        BUILDER.pop();
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
