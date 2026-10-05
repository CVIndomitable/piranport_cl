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

    // ===== 导弹（MissileEntity / MissileType）=====
    // WHY：速度曲线原先写死在 MissileType 枚举里，策划无法在调试终端调。
    // 键：global.missile.<公共>.*、global.missile_<型号>.*。默认值 = 下沉前写死值。
    // 速度属性名用 speed_*（而非 initial_speed/max_speed），避免终端把 initial_speed
    // 按「格/秒」×20 显示、max_speed 却按「格/tick」显示的半截换算，三条单位不一致。

    /** 导弹最大存活时间（ticks）。默认 600（30 秒）。 */
    public static final TerminalConfigValue<Integer> MISSILE_LIFETIME_TICKS =
            TerminalConfigValue.integer("projectiles", "missile", "lifetime_ticks", 600, 1, 120000);

    /** 自动目标搜索半径（格）。默认 32.0。 */
    public static final TerminalConfigValue<Double> MISSILE_SEARCH_RANGE =
            TerminalConfigValue.number("projectiles", "missile", "search_range", 32.0, 1.0, 512.0);

    /** 制导导弹每 tick 最大转向角（度）。默认 12.0。 */
    public static final TerminalConfigValue<Double> MISSILE_MAX_TURN_DEG =
            TerminalConfigValue.number("projectiles", "missile", "max_turn_deg", 12.0, 0.0, 180.0);

    /** 目标搜索节流（ticks）：无目标时每隔多少 tick 重新搜索。默认 5。 */
    public static final TerminalConfigValue<Integer> MISSILE_SEARCH_INTERVAL =
            TerminalConfigValue.integer("projectiles", "missile", "search_interval", 5, 1, 1200);

    // 反舰导弹 ANTI_SHIP：初速 0.04、每 tick 加速 0.04、极速 2.4。
    public static final TerminalConfigValue<Double> MISSILE_ANTI_SHIP_SPEED_INITIAL =
            TerminalConfigValue.number("projectiles", "missile_anti_ship", "speed_initial", 0.04, 0.0, 100.0);
    public static final TerminalConfigValue<Double> MISSILE_ANTI_SHIP_SPEED_INCREMENT =
            TerminalConfigValue.number("projectiles", "missile_anti_ship", "speed_increment", 0.04, 0.0, 100.0);
    public static final TerminalConfigValue<Double> MISSILE_ANTI_SHIP_SPEED_MAX =
            TerminalConfigValue.number("projectiles", "missile_anti_ship", "speed_max", 2.4, 0.0, 100.0);

    // 防空导弹 ANTI_AIR：初速 0.05、每 tick 加速 0.05、极速 3.0。
    public static final TerminalConfigValue<Double> MISSILE_ANTI_AIR_SPEED_INITIAL =
            TerminalConfigValue.number("projectiles", "missile_anti_air", "speed_initial", 0.05, 0.0, 100.0);
    public static final TerminalConfigValue<Double> MISSILE_ANTI_AIR_SPEED_INCREMENT =
            TerminalConfigValue.number("projectiles", "missile_anti_air", "speed_increment", 0.05, 0.0, 100.0);
    public static final TerminalConfigValue<Double> MISSILE_ANTI_AIR_SPEED_MAX =
            TerminalConfigValue.number("projectiles", "missile_anti_air", "speed_max", 3.0, 0.0, 100.0);

    // 火箭弹 ROCKET：初速 0.04、每 tick 加速 0.04、极速 2.0。
    public static final TerminalConfigValue<Double> MISSILE_ROCKET_SPEED_INITIAL =
            TerminalConfigValue.number("projectiles", "missile_rocket", "speed_initial", 0.04, 0.0, 100.0);
    public static final TerminalConfigValue<Double> MISSILE_ROCKET_SPEED_INCREMENT =
            TerminalConfigValue.number("projectiles", "missile_rocket", "speed_increment", 0.04, 0.0, 100.0);
    public static final TerminalConfigValue<Double> MISSILE_ROCKET_SPEED_MAX =
            TerminalConfigValue.number("projectiles", "missile_rocket", "speed_max", 2.0, 0.0, 100.0);

    // ===== NPC 鱼雷齐射（TorpedoAttackGoal / ShipGirlCombatGoal 共用）=====
    // WHY：两处逐字复制的常量抽成唯一来源 NpcCombatTuning，并可按终端调。
    // 键：global.torpedo_salvo.*。默认值 = 下沉前两处相同的写死值。

    /** 齐射触发距离（格）。默认 20.0。 */
    public static final TerminalConfigValue<Double> NPC_TORPEDO_SALVO_RANGE =
            TerminalConfigValue.number("projectiles", "torpedo_salvo", "range", 20.0, 0.0, 256.0);

    /** 齐射冷却（ticks）。默认 200（10 秒）。 */
    public static final TerminalConfigValue<Integer> NPC_TORPEDO_SALVO_COOLDOWN =
            TerminalConfigValue.integer("projectiles", "torpedo_salvo", "salvo_cooldown", 200, 0, 12000);

    /** 每次齐射的鱼雷枚数。默认 3。 */
    public static final TerminalConfigValue<Integer> NPC_TORPEDO_SALVO_COUNT =
            TerminalConfigValue.integer("projectiles", "torpedo_salvo", "torpedoes_per_salvo", 3, 1, 32);

    /** 每枚鱼雷的扇面偏角（度）。默认 8.0。 */
    public static final TerminalConfigValue<Double> NPC_TORPEDO_SALVO_SPREAD =
            TerminalConfigValue.number("projectiles", "torpedo_salvo", "spread_angle", 8.0, 0.0, 180.0);

    /** 齐射鱼雷初速（格/tick）。默认 0.8。 */
    public static final TerminalConfigValue<Double> NPC_TORPEDO_SALVO_SPEED =
            TerminalConfigValue.number("projectiles", "torpedo_salvo", "speed", 0.8, 0.01, 10.0);

    /** 舰娘随从每次炮击后触发雷击的判定分母（1/N 概率）。默认 200。
     *  WHY 与 salvo_cooldown 分开：舰娘版是「每轮炮击 1/N 概率放雷」而非固定冷却，语义不同不可合并。 */
    public static final TerminalConfigValue<Integer> NPC_TORPEDO_TRIGGER_ROLL_BOUND =
            TerminalConfigValue.integer("projectiles", "torpedo_salvo", "trigger_roll_bound", 200, 1, 100000);

    // ===== NPC 炮弹（CannonAttackGoal / ShipGirlCombatGoal 共用）=====
    // 键：global.shell.*。默认值 = 两处写死的 1.5 / 2.0。

    /** 炮弹初速（格/tick，终端按格/秒显示）。默认 1.5。 */
    public static final TerminalConfigValue<Double> NPC_SHELL_SPEED =
            TerminalConfigValue.number("projectiles", "shell", "speed", 1.5, 0.01, 10.0);

    /** 炮弹散布（度）。默认 2.0。 */
    public static final TerminalConfigValue<Double> NPC_SHELL_INACCURACY =
            TerminalConfigValue.number("projectiles", "shell", "inaccuracy", 2.0, 0.0, 180.0);

    // ===== NPC 追踪弹导引（TrackingCalculator，直接决定命中率）=====
    // 键：global.tracking.*。默认值 = 写死的 3.0 / 0.08。

    /** 比例导引常数 N（无量纲），越大越激进。默认 3.0。 */
    public static final TerminalConfigValue<Double> NPC_TRACKING_NAV_CONSTANT =
            TerminalConfigValue.number("projectiles", "tracking", "nav_constant", 3.0, 0.0, 100.0);

    /** 每 tick 最大转向角（弧度），防 90° 急转。默认 0.08（约 4.6°/tick）。 */
    public static final TerminalConfigValue<Double> NPC_TRACKING_MAX_TURN_RATE =
            TerminalConfigValue.number("projectiles", "tracking", "max_turn_rate", 0.08, 0.0, 3.141592653589793);

    // ===== 深水炸弹（DepthChargeEntity / DepthChargeFireStrategy / 兼容层 共用）=====
    // WHY：伤害 14、爆炸威力 3.0 原先在实体字段、发射策略、舰娘兼容层（甚至空投反潜）多处各写一份，
    // 现统一从这里读；其余手感常量也从实体/策略的写死值下沉。键：global.depth_charge.*。

    /** 深弹基础伤害（发射策略会再叠加玩家强化加成）。默认 14.0。 */
    public static final TerminalConfigValue<Double> DEPTH_CHARGE_DAMAGE =
            TerminalConfigValue.number("projectiles", "depth_charge", "damage", 14.0, 0.0, 10000.0);

    /** 深弹爆炸视觉威力（发射策略会再叠加玩家强化加成）。默认 3.0。 */
    public static final TerminalConfigValue<Double> DEPTH_CHARGE_EXPLOSION_POWER =
            TerminalConfigValue.number("projectiles", "depth_charge", "explosion_power", 3.0, 0.0, 100.0);

    /** 深弹最大存活时间（ticks）。默认 600（30 秒）。 */
    public static final TerminalConfigValue<Integer> DEPTH_CHARGE_LIFETIME_TICKS =
            TerminalConfigValue.integer("projectiles", "depth_charge", "lifetime_ticks", 600, 1, 120000);

    /** 近炸引信起爆前安全延迟（ticks）。默认 5。 */
    public static final TerminalConfigValue<Integer> DEPTH_CHARGE_ARM_TICKS =
            TerminalConfigValue.integer("projectiles", "depth_charge", "arm_ticks", 5, 0, 12000);

    /** 近炸检测距离（格）。默认 8.0。 */
    public static final TerminalConfigValue<Double> DEPTH_CHARGE_DETECT_RANGE =
            TerminalConfigValue.number("projectiles", "depth_charge", "detect_range", 8.0, 0.0, 128.0);

    /** 爆炸后水平伤害半径（格）。默认 8.0。 */
    public static final TerminalConfigValue<Double> DEPTH_CHARGE_BLAST_RADIUS =
            TerminalConfigValue.number("projectiles", "depth_charge", "blast_radius", 8.0, 0.0, 128.0);

    /** 爆炸后垂直伤害容差（格，上下各此值）。默认 4.0。 */
    public static final TerminalConfigValue<Double> DEPTH_CHARGE_BLAST_HEIGHT =
            TerminalConfigValue.number("projectiles", "depth_charge", "blast_height", 4.0, 0.0, 128.0);

    /** 入水后的重力加速度（格/tick²）。默认 0.08。 */
    public static final TerminalConfigValue<Double> DEPTH_CHARGE_GRAVITY =
            TerminalConfigValue.number("projectiles", "depth_charge", "gravity", 0.08, 0.0, 10.0);

    /** 近炸检测间隔（ticks），错峰执行降开销。默认 5。 */
    public static final TerminalConfigValue<Integer> DEPTH_CHARGE_PROXIMITY_INTERVAL =
            TerminalConfigValue.integer("projectiles", "depth_charge", "proximity_check_interval", 5, 1, 1200);

    /** 入水后无目标时的下沉时长（ticks），之后才启动近炸检测。默认 20。 */
    public static final TerminalConfigValue<Integer> DEPTH_CHARGE_WATER_SINK_DURATION =
            TerminalConfigValue.integer("projectiles", "depth_charge", "water_sink_duration", 20, 0, 12000);

    /** 水中水平阻力系数（0~1，越小减速越快）。默认 0.85。 */
    public static final TerminalConfigValue<Double> DEPTH_CHARGE_WATER_DRAG =
            TerminalConfigValue.number("projectiles", "depth_charge", "water_drag", 0.85, 0.0, 1.0);

    /** 水中每 tick 额外下沉加速度（格/tick²）。默认 0.04。 */
    public static final TerminalConfigValue<Double> DEPTH_CHARGE_WATER_SINK_ACCEL =
            TerminalConfigValue.number("projectiles", "depth_charge", "water_sink_accel", 0.04, 0.0, 10.0);

    /** 爆炸伤害边缘衰减比例：中心全额，边缘只保留该比例（同时作为伤害下限）。默认 0.5。 */
    public static final TerminalConfigValue<Double> DEPTH_CHARGE_EDGE_DAMAGE_RATIO =
            TerminalConfigValue.number("projectiles", "depth_charge", "edge_damage_ratio", 0.5, 0.0, 1.0);

    /** 单发直投初速（格/tick）。默认 0.6。 */
    public static final TerminalConfigValue<Double> DEPTH_CHARGE_SPEED_SINGLE =
            TerminalConfigValue.number("projectiles", "depth_charge", "speed_single", 0.6, 0.0, 10.0);

    /** 前后散布：远端初速（格/tick）。默认 0.7。 */
    public static final TerminalConfigValue<Double> DEPTH_CHARGE_SPEED_FAR =
            TerminalConfigValue.number("projectiles", "depth_charge", "speed_far", 0.7, 0.0, 10.0);

    /** 前后散布：近端初速（格/tick）。默认 0.4。 */
    public static final TerminalConfigValue<Double> DEPTH_CHARGE_SPEED_NEAR =
            TerminalConfigValue.number("projectiles", "depth_charge", "speed_near", 0.4, 0.0, 10.0);

    /** 三角散布：中/侧弹初速（格/tick）。默认 0.5。 */
    public static final TerminalConfigValue<Double> DEPTH_CHARGE_SPEED_TRIANGLE =
            TerminalConfigValue.number("projectiles", "depth_charge", "speed_triangle", 0.5, 0.0, 10.0);

    /** 三角散布：左右偏角（度）。默认 20.0。 */
    public static final TerminalConfigValue<Double> DEPTH_CHARGE_SPREAD_DEG =
            TerminalConfigValue.number("projectiles", "depth_charge", "spread_deg", 20.0, 0.0, 180.0);

    // ===== 航空炸弹（AerialBombEntity）=====
    // WHY：重力原先写死为 AerialBombEntity.GRAVITY = 0.06，被三处共用——实体物理
    // （ThrowableProjectile#tick 的 getDefaultGravity）、客户端黄色落点标记
    // （getMarkerGravity → ProjectileLandingMarkerRenderer）、水平轰炸投弹提前量解算
    // （LevelBombLead.releaseDistance）。下沉为终端参数后三处仍读同一来源，
    // 改重力不会让标记指向与实际弹着点、提前量互相漂移。
    // 键：global.aerial_bomb.gravity。

    /** 航弹重力（格/tick²），强于原版投掷物默认的 0.03，模拟自由落体炸弹。默认 0.06。 */
    public static final TerminalConfigValue<Double> AERIAL_BOMB_GRAVITY =
            TerminalConfigValue.number("projectiles", "aerial_bomb", "gravity", 0.06, 0.0, 10.0);
}
