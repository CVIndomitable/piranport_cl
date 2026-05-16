package com.piranport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 火炮性能配置 — Artillery Performance Configuration
 *
 * 三个配置段，匹配火炮重构计划 Phase 11 定义的 TOML 结构：
 *   [artillery]    — 全局火炮/炮弹限制
 *   [ballistics]   — 弹道解算参数（仅本模组新增参数，
 *                     max_iterations/accuracy/max_steps/cache_size
 *                     定义在 ModEquipmentConfig 中）
 *   [performance]  — 性能相关参数（缓存、三式弹、VT 弹）
 */
public class ModArtilleryConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ==================== Artillery (全局炮弹) ====================
    /** 全局最大同时存在的炮弹实体数（ShellEntity + CannonProjectileEntity 合计）。超出上限时拒绝发射。 */
    public static final ModConfigSpec.IntValue ARTILLERY_MAX_PROJECTILES;
    /** 水中弹销毁时间（秒），到期后静默销毁或爆炸取决于 ModProjectilesConfig.UNDERWATER_EXPLODE。 */
    public static final ModConfigSpec.DoubleValue ARTILLERY_UNDERWATER_DESTROY_TIME;

    // ==================== Ballistics (弹道解算) ====================
    /** 弹道模拟步长（MC tick 为单位，0.05 = 每 tick 模拟 20 步）。值越小精度越高但计算量越大。 */
    public static final ModConfigSpec.DoubleValue BALLISTIC_SIMULATION_DT;
    /** 误差大于此值时认为打不到目标，回退到最大射程角度。 */
    public static final ModConfigSpec.DoubleValue BALLISTIC_NO_SOLUTION_THRESHOLD;

    // ==================== Performance (性能) ====================
    /** 是否启用异步弹道计算（占位，暂未实现）。 */
    public static final ModConfigSpec.BooleanValue PERF_ASYNC_BALLISTICS;
    /** 是否启用弹道解算缓存。关闭后每次解算都重新计算。 */
    public static final ModConfigSpec.BooleanValue PERF_CACHE_SOLUTIONS;
    /** 最大同时存在的三式弹霰弹数。设为 0 禁用手持三式弹。 */
    public static final ModConfigSpec.IntValue PERF_SHRAPNEL_LIMIT;
    /** VT 弹近炸引信锥形扫描间隔（tick）。数值越大性能越好但反应越迟钝。 */
    public static final ModConfigSpec.IntValue PERF_VT_CHECK_INTERVAL;
    /** VT 锥形检测范围（格）。 */
    public static final ModConfigSpec.DoubleValue VT_DETECT_RANGE;
    /** VT 锥形半角（度），目标方向与弹头速度方向的夹角在此范围内才触发。 */
    public static final ModConfigSpec.DoubleValue VT_CONE_HALF_ANGLE;
    /** VT 方块接近检测前方射线长度（格）。 */
    public static final ModConfigSpec.DoubleValue VT_BLOCK_RANGE;
    /** VT 引信解锁前宽限期（tick），防止出膛即炸。 */
    public static final ModConfigSpec.IntValue VT_ARM_TICKS;
    /** 三式弹霰弹最大存活时间（tick）。 */
    public static final ModConfigSpec.IntValue PELLET_MAX_LIFETIME;

    static {
        // ==================== Artillery ====================
        BUILDER.push("artillery");

        ARTILLERY_MAX_PROJECTILES = BUILDER
            .comment(
                "Max simultaneous shell projectiles (全局最大同时存在的炮弹实体数).",
                "ShellEntity + CannonProjectileEntity total. Exceeding this discards new shots.",
                "Range: 1-1000, Default: 200. (默认200，超限时拒绝发射)")
            .defineInRange("max_projectiles", 200, 1, 1000);

        ARTILLERY_UNDERWATER_DESTROY_TIME = BUILDER
            .comment(
                "Underwater shell destroy time in seconds (水中弹销毁时间，秒).",
                "After this many seconds underwater the shell is removed or explodes.",
                "Range: 0.5-30.0, Default: 3.0 (3秒)")
            .defineInRange("underwater_destroy_time_seconds", 3.0, 0.5, 30.0);

        BUILDER.pop();

        // ==================== Ballistics ====================
        BUILDER.push("ballistics");

        BALLISTIC_SIMULATION_DT = BUILDER
            .comment(
                "[DEPRECATED] Ballistic simulation timestep in ticks (弹道模拟步长，tick为单位).",
                "This parameter is no longer used. Simulation now uses fixed 1.0 tick timestep",
                "to match CannonProjectileEntity physics exactly.",
                "该参数已废弃。模拟现在使用固定的 1.0 tick 步长以匹配炮弹实体物理。",
                "Range: 0.01-1.0, Default: 0.05 (已废弃)")
            .defineInRange("simulation_timestep", 0.05, 0.01, 1.0);

        BALLISTIC_NO_SOLUTION_THRESHOLD = BUILDER
            .comment(
                "Error threshold for 'no solution' fallback (无解判定阈值，格).",
                "When the best solution error exceeds this, the solver falls back to max-range angle.",
                "Range: 0.5-50.0, Default: 5.0 (误差超过5格认为打不到)")
            .defineInRange("no_solution_threshold", 5.0, 0.5, 50.0);

        BUILDER.pop();

        // ==================== Performance ====================
        BUILDER.push("performance");

        PERF_ASYNC_BALLISTICS = BUILDER
            .comment(
                "Enable async ballistic computation (异步弹道计算，暂未实现).",
                "Placeholder for future async implementation.",
                "Default: true (占位，默认开启)")
            .define("async_ballistics", true);

        PERF_CACHE_SOLUTIONS = BUILDER
            .comment(
                "Cache ballistic solver results (弹道解算缓存开关).",
                "Set to false to disable caching — each shot recomputes from scratch.",
                "Default: true (缓存解算结果)")
            .define("cache_solutions", true);

        PERF_SHRAPNEL_LIMIT = BUILDER
            .comment(
                "Max simultaneous Type 3 (Sanshiki) pellets (三式弹最大霰弹数).",
                "0 = disable Type 3 shell handheld firing entirely. Higher = more lag.",
                "Range: 0-256, Default: 64 (上限64枚)")
            .defineInRange("shrapnel_limit", 64, 0, 256);

        PERF_VT_CHECK_INTERVAL = BUILDER
            .comment(
                "VT fuze cone scan interval in ticks (VT弹近炸扫描间隔，tick).",
                "Higher = better performance but slower reaction.",
                "Range: 1-40, Default: 5 (每5 tick扫描一次)")
            .defineInRange("vt_check_interval", 5, 1, 40);

        VT_DETECT_RANGE = BUILDER
            .comment(
                "VT fuze detection range in blocks (VT锥形检测范围，格).",
                "Entities beyond this range from the shell won't trigger the fuze.",
                "Range: 0.5-20.0, Default: 3.0")
            .defineInRange("vt_detect_range", 3.0, 0.5, 20.0);

        VT_CONE_HALF_ANGLE = BUILDER
            .comment(
                "VT fuze cone half-angle in degrees (VT锥形半角，度).",
                "Target direction must be within this angle of the shell velocity to trigger.",
                "Range: 1.0-90.0, Default: 30.0")
            .defineInRange("vt_cone_half_angle", 30.0, 1.0, 90.0);

        VT_BLOCK_RANGE = BUILDER
            .comment(
                "VT fuze block-proximity ray length in blocks (VT方块接近射线长度).",
                "Short ray cast ahead of the shell to trigger on terrain.",
                "Range: 0.5-20.0, Default: 3.0")
            .defineInRange("vt_block_range", 3.0, 0.5, 20.0);

        VT_ARM_TICKS = BUILDER
            .comment(
                "VT fuze arming delay in ticks (VT引信解锁延迟，tick).",
                "Prevents detonation immediately after firing.",
                "Range: 0-100, Default: 5")
            .defineInRange("vt_arm_ticks", 5, 0, 100);

        PELLET_MAX_LIFETIME = BUILDER
            .comment(
                "Sanshiki pellet max lifetime in ticks (三式弹霰弹最大存活时间，tick).",
                "20 ticks = 1 second. Pellets are discarded after this.",
                "Range: 20-600, Default: 200 (10秒)")
            .defineInRange("pellet_max_lifetime", 200, 20, 600);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
