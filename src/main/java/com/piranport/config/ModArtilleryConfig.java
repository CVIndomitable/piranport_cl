package com.piranport.config;

/** 终端控制的Artillery数值默认值；旧 TOML 不再注册。 */
public final class ModArtilleryConfig {
    private ModArtilleryConfig() {}

    public static final TerminalConfigValue<Integer> ARTILLERY_MAX_PROJECTILES =
            TerminalConfigValue.integer("artillery", "artillery", "artillery_max_projectiles", 200, 1, 1000);

    public static final TerminalConfigValue<Double> ARTILLERY_UNDERWATER_DESTROY_TIME =
            TerminalConfigValue.number("artillery", "artillery", "artillery_underwater_destroy_time", 3.0, 0.5, 30.0);


    public static final TerminalConfigValue<Double> BALLISTIC_NO_SOLUTION_THRESHOLD =
            TerminalConfigValue.number("artillery", "artillery", "ballistic_no_solution_threshold", 5.0, 0.5, 50.0);


    public static final TerminalConfigValue<Boolean> PERF_CACHE_SOLUTIONS =
            TerminalConfigValue.bool("artillery", "artillery", "perf_cache_solutions", true);

    public static final TerminalConfigValue<Integer> PERF_SHRAPNEL_LIMIT =
            TerminalConfigValue.integer("artillery", "artillery", "perf_shrapnel_limit", 64, 0, 256);

    public static final TerminalConfigValue<Integer> PERF_VT_CHECK_INTERVAL =
            TerminalConfigValue.integer("artillery", "artillery", "perf_vt_check_interval", 5, 1, 40);

    public static final TerminalConfigValue<Double> VT_DETECT_RANGE =
            TerminalConfigValue.number("artillery", "artillery", "vt_detect_range", 3.0, 0.5, 20.0);

    public static final TerminalConfigValue<Double> VT_CONE_HALF_ANGLE =
            TerminalConfigValue.number("artillery", "artillery", "vt_cone_half_angle", 30.0, 1.0, 90.0);

    public static final TerminalConfigValue<Double> VT_BLOCK_RANGE =
            TerminalConfigValue.number("artillery", "artillery", "vt_block_range", 3.0, 0.5, 20.0);

    public static final TerminalConfigValue<Integer> VT_ARM_TICKS =
            TerminalConfigValue.integer("artillery", "artillery", "vt_arm_ticks", 5, 0, 100);

    public static final TerminalConfigValue<Integer> PELLET_MAX_LIFETIME =
            TerminalConfigValue.integer("artillery", "artillery", "pellet_max_lifetime", 200, 20, 600);

    // ===== 三式弹（对空弹幕）参数（策划决策/武器）=====
    // 键：artillery/type3/* → global.type3.*。原先写死在 CannonFireService，现供策划调手感。
    // 注意：pellet_count 增大会同时占用 PERF_SHRAPNEL_LIMIT 的性能预算，需一并调整。

    /** 单次三式弹生成的弹丸数。默认 64。 */
    public static final TerminalConfigValue<Integer> TYPE3_PELLET_COUNT =
            TerminalConfigValue.integer("artillery", "type3", "pellet_count", 64, 1, 256);

    /** 弹幕基准半径（格，作为 shootFromRotation 的 pitch/yaw 偏移系数，沿 sqrt 分布展开）。默认 7.0。 */
    public static final TerminalConfigValue<Double> TYPE3_PELLET_RADIUS =
            TerminalConfigValue.number("artillery", "type3", "pellet_radius", 7.0, 0.0, 128.0);

    /** 每颗弹丸伤害倍率（最终 = 请求伤害 × 弹种倍率 × 该值）。默认 0.25。 */
    public static final TerminalConfigValue<Double> TYPE3_DAMAGE_MULTIPLIER =
            TerminalConfigValue.number("artillery", "type3", "damage_multiplier", 0.25, 0.0, 10.0);

    /** 弹丸散布（shootFromRotation 的 inaccuracy 参数，越大越散）。默认 0.5。 */
    public static final TerminalConfigValue<Double> TYPE3_PELLET_INACCURACY =
            TerminalConfigValue.number("artillery", "type3", "pellet_inaccuracy", 0.5, 0.0, 10.0);
}
