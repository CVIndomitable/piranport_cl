package com.piranport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** 武器系统数值配置 */
public class ModWeaponsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue SMALL_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue SMALL_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SMALL_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue SMALL_GUN_INACCURACY;

    public static final ModConfigSpec.DoubleValue TWIN_SMALL_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue TWIN_SMALL_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue TWIN_SMALL_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue TWIN_SMALL_GUN_INACCURACY;

    public static final ModConfigSpec.DoubleValue MEDIUM_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue MEDIUM_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue MEDIUM_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue MEDIUM_GUN_INACCURACY;

    public static final ModConfigSpec.DoubleValue TWIN_MEDIUM_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue TWIN_MEDIUM_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue TWIN_MEDIUM_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue TWIN_MEDIUM_GUN_INACCURACY;

    public static final ModConfigSpec.DoubleValue LARGE_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue LARGE_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue LARGE_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue LARGE_GUN_INACCURACY;

    public static final ModConfigSpec.DoubleValue TWIN_LARGE_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue TWIN_LARGE_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue TWIN_LARGE_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue TWIN_LARGE_GUN_INACCURACY;

    public static final ModConfigSpec.DoubleValue TRIPLE_LARGE_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue TRIPLE_LARGE_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue TRIPLE_LARGE_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue TRIPLE_LARGE_GUN_INACCURACY;

    public static final ModConfigSpec.DoubleValue QUAD_LARGE_GUN_DAMAGE;
    public static final ModConfigSpec.IntValue QUAD_LARGE_GUN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue QUAD_LARGE_GUN_VELOCITY;
    public static final ModConfigSpec.DoubleValue QUAD_LARGE_GUN_INACCURACY;

    private record GunDef(ModConfigSpec.DoubleValue damage, ModConfigSpec.IntValue cooldown,
                          ModConfigSpec.DoubleValue velocity, ModConfigSpec.DoubleValue inaccuracy) {}

    private static GunDef defineGun(String path, double dmg, int cd, double vel, double inacc) {
        BUILDER.push(path);
        var d = BUILDER.comment("每发炮弹伤害").defineInRange("damage", dmg, 0.1, 1000.0);
        var c = BUILDER.comment("冷却时间（20 tick = 1秒）").defineInRange("cooldown_ticks", cd, 1, 6000);
        var v = BUILDER.comment("弹丸速度").defineInRange("velocity", vel, 0.1, 10.0);
        var i = BUILDER.comment("射击精度偏差").defineInRange("inaccuracy", inacc, 0.0, 10.0);
        BUILDER.pop();
        return new GunDef(d, c, v, i);
    }

    static {
        BUILDER.push("cannon");

        var s = defineGun("single_small_gun", 6.0, 30, 3.0, 1.0);
        SMALL_GUN_DAMAGE = s.damage; SMALL_GUN_COOLDOWN = s.cooldown;
        SMALL_GUN_VELOCITY = s.velocity; SMALL_GUN_INACCURACY = s.inaccuracy;

        var ts = defineGun("twin_small_gun", 6.0, 30, 3.0, 1.0);
        TWIN_SMALL_GUN_DAMAGE = ts.damage; TWIN_SMALL_GUN_COOLDOWN = ts.cooldown;
        TWIN_SMALL_GUN_VELOCITY = ts.velocity; TWIN_SMALL_GUN_INACCURACY = ts.inaccuracy;

        var m = defineGun("single_medium_gun", 12.0, 50, 3.5, 0.8);
        MEDIUM_GUN_DAMAGE = m.damage; MEDIUM_GUN_COOLDOWN = m.cooldown;
        MEDIUM_GUN_VELOCITY = m.velocity; MEDIUM_GUN_INACCURACY = m.inaccuracy;

        var tm = defineGun("twin_medium_gun", 12.0, 50, 3.5, 0.8);
        TWIN_MEDIUM_GUN_DAMAGE = tm.damage; TWIN_MEDIUM_GUN_COOLDOWN = tm.cooldown;
        TWIN_MEDIUM_GUN_VELOCITY = tm.velocity; TWIN_MEDIUM_GUN_INACCURACY = tm.inaccuracy;

        var l = defineGun("single_large_gun", 15.0, 60, 4.0, 0.5);
        LARGE_GUN_DAMAGE = l.damage; LARGE_GUN_COOLDOWN = l.cooldown;
        LARGE_GUN_VELOCITY = l.velocity; LARGE_GUN_INACCURACY = l.inaccuracy;

        var tl = defineGun("twin_large_gun", 20.0, 80, 4.0, 0.5);
        TWIN_LARGE_GUN_DAMAGE = tl.damage; TWIN_LARGE_GUN_COOLDOWN = tl.cooldown;
        TWIN_LARGE_GUN_VELOCITY = tl.velocity; TWIN_LARGE_GUN_INACCURACY = tl.inaccuracy;

        var trl = defineGun("triple_large_gun", 20.0, 80, 4.0, 0.5);
        TRIPLE_LARGE_GUN_DAMAGE = trl.damage; TRIPLE_LARGE_GUN_COOLDOWN = trl.cooldown;
        TRIPLE_LARGE_GUN_VELOCITY = trl.velocity; TRIPLE_LARGE_GUN_INACCURACY = trl.inaccuracy;

        var ql = defineGun("quad_large_gun", 20.0, 60, 4.0, 0.5);
        QUAD_LARGE_GUN_DAMAGE = ql.damage; QUAD_LARGE_GUN_COOLDOWN = ql.cooldown;
        QUAD_LARGE_GUN_VELOCITY = ql.velocity; QUAD_LARGE_GUN_INACCURACY = ql.inaccuracy;

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
