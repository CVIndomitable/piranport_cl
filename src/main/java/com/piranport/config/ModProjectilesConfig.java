package com.piranport.config;

/** 终端控制的Projectiles数值默认值；旧 TOML 不再注册。 */
public final class ModProjectilesConfig {
    private ModProjectilesConfig() {}


    public static final TerminalConfigValue<Double> UNDERWATER_EXPLOSION_MULTIPLIER =
            TerminalConfigValue.number("projectiles", "projectiles", "underwater_explosion_multiplier", 0.5, 0.0, 2.0);


    public static final TerminalConfigValue<Boolean> UNDERWATER_EXPLODE =
            TerminalConfigValue.bool("projectiles", "projectiles", "underwater_explode", false);

    public static final TerminalConfigValue<Double> AP_DAMAGE_MULTIPLIER =
            TerminalConfigValue.number("projectiles", "projectiles", "ap_damage_multiplier", 1.3, 0.1, 5.0);

    public static final TerminalConfigValue<Double> AP_ARMOR_IGNORE =
            TerminalConfigValue.number("projectiles", "projectiles", "ap_armor_ignore", 0.5, 0.0, 1.0);

    public static final TerminalConfigValue<Integer> TRIPLE_TORPEDO_LAUNCHER_COOLDOWN =
            TerminalConfigValue.integer("projectiles", "projectiles", "triple_torpedo_launcher_cooldown", 100, 1, 6000);

    public static final TerminalConfigValue<Integer> QUAD_TORPEDO_LAUNCHER_COOLDOWN =
            TerminalConfigValue.integer("projectiles", "projectiles", "quad_torpedo_launcher_cooldown", 120, 1, 6000);

    public static final TerminalConfigValue<Integer> QUINTUPLE_TORPEDO_LAUNCHER_COOLDOWN =
            TerminalConfigValue.integer("projectiles", "projectiles", "quintuple_torpedo_launcher_cooldown", 140, 1, 6000);
}
