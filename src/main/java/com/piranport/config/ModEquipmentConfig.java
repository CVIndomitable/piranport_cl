package com.piranport.config;

/** 终端控制的Equipment数值默认值；旧 TOML 不再注册。 */
public final class ModEquipmentConfig {
    private ModEquipmentConfig() {}



    public static final TerminalConfigValue<Double> SONAR_RANGE =
            TerminalConfigValue.number("equipment", "equipment", "sonar_range", 64.0, 1.0, 256.0);



    public static final TerminalConfigValue<Double> RADAR_RANGE =
            TerminalConfigValue.number("equipment", "equipment", "radar_range", 128.0, 1.0, 512.0);



    public static final TerminalConfigValue<Double> FIRE_CONTROL_RANGE =
            TerminalConfigValue.number("equipment", "equipment", "fire_control_range", 96.0, 1.0, 256.0);



    // 火控雷达炮弹追踪（策划决策/火控/05 §4，占位值需实测）。原文写 equipment.toml，
    // 项目已改为终端参数，键名保持 fire_control_radar.levelN_*。
    public static final TerminalConfigValue<Double> FC_RADAR_L1_RANGE =
            TerminalConfigValue.number("equipment", "fire_control_radar", "level1_range", 8.0, 0.0, 128.0);
    public static final TerminalConfigValue<Double> FC_RADAR_L1_TURN =
            TerminalConfigValue.number("equipment", "fire_control_radar", "level1_turn_coefficient", 0.02, 0.0, 1.0);
    public static final TerminalConfigValue<Double> FC_RADAR_L2_RANGE =
            TerminalConfigValue.number("equipment", "fire_control_radar", "level2_range", 12.0, 0.0, 128.0);
    public static final TerminalConfigValue<Double> FC_RADAR_L2_TURN =
            TerminalConfigValue.number("equipment", "fire_control_radar", "level2_turn_coefficient", 0.04, 0.0, 1.0);
    public static final TerminalConfigValue<Double> FC_RADAR_L3_RANGE =
            TerminalConfigValue.number("equipment", "fire_control_radar", "level3_range", 16.0, 0.0, 128.0);
    public static final TerminalConfigValue<Double> FC_RADAR_L3_TURN =
            TerminalConfigValue.number("equipment", "fire_control_radar", "level3_turn_coefficient", 0.06, 0.0, 1.0);

    // 火控可视化预测颜色（策划决策/火控/06 §4），RGB 整数：白 0xFFFFFF，激活绿 0x40FF60。
    public static final TerminalConfigValue<Integer> PREDICTION_LINE_COLOR =
            TerminalConfigValue.integer("equipment", "fire_control_visual", "prediction_line_color", 0xFFFFFF, 0, 0xFFFFFF);
    public static final TerminalConfigValue<Integer> PREDICTION_LINE_ACTIVE_COLOR =
            TerminalConfigValue.integer("equipment", "fire_control_visual", "prediction_line_active_color", 0x40FF60, 0, 0xFFFFFF);

    // 5/6/7 联鱼雷最大偏角（度），暂定 5°，由策划在调试终端试验（2026-09-30 项目所有者）。
    // 2/3/4 管沿用《武器/鱼雷-联装设计》固定表，不走此处。
    public static final TerminalConfigValue<Double> TORPEDO_SPREAD_TUBES5 =
            TerminalConfigValue.number("equipment", "torpedo_spread", "tubes5_max_angle", 5.0, 0.0, 45.0);
    public static final TerminalConfigValue<Double> TORPEDO_SPREAD_TUBES6 =
            TerminalConfigValue.number("equipment", "torpedo_spread", "tubes6_max_angle", 5.0, 0.0, 45.0);
    public static final TerminalConfigValue<Double> TORPEDO_SPREAD_TUBES7 =
            TerminalConfigValue.number("equipment", "torpedo_spread", "tubes7_max_angle", 5.0, 0.0, 45.0);

    // 水平轰炸机投弹时，航弹继承载机水平速度的倍数（保留前抛，恢复改动前的 0.5，可调试终端实时调）。
    // 投弹提前距离随此值自动重算（见 combat/LevelBombLead），所以改这里不会让弹着点重新偏前/偏后。
    public static final TerminalConfigValue<Double> LEVEL_BOMBER_HORIZONTAL_VELOCITY_MULTIPLIER =
            TerminalConfigValue.number("equipment", "level_bomber", "horizontal_velocity_multiplier", 0.5, 0.0, 2.0);

    // ===== 鱼雷水下贴水面巡航（用户口径 2026-10-05：鱼雷应潜航于水面之下一点，不是浮在水面）=====
    // 数值全部走调试终端（equipment.torpedo.*），策划可实时调手感。

    /** 雷体中心相对水面的目标深度（格）。0.5 时整根雷（高 0.75）几乎没入水面之下，
     *  只剩顶面离水面约 0.125 格，视觉上就是"贴着水面潜航"。 */
    public static final TerminalConfigValue<Double> TORPEDO_SURFACE_DEPTH =
            TerminalConfigValue.number("equipment", "torpedo", "surface_depth", 0.5, 0.05, 3.0);

    /** 每 tick 消除的垂直误差比例（0~1）。越大越"贴"目标深度、收敛越快；越小越柔。 */
    public static final TerminalConfigValue<Double> TORPEDO_SURFACE_VERTICAL_ADJUST =
            TerminalConfigValue.number("equipment", "torpedo", "surface_vertical_adjust", 0.3, 0.05, 1.0);

    /** 贴深度时单 tick 最大垂直速度（格/tick），限制上浮/下潜的猛度。 */
    public static final TerminalConfigValue<Double> TORPEDO_SURFACE_VERTICAL_MAX_SPEED =
            TerminalConfigValue.number("equipment", "torpedo", "surface_vertical_max_speed", 0.3, 0.05, 1.0);

    /** 水面捕获窗口（格）：雷体中心离目标深度不超过该值时才主动贴面；
     *  超过且在水下时保持既有垂直运动（深水巡航），避免把潜艇/水中发射的深雷硬拽上水面。 */
    public static final TerminalConfigValue<Double> TORPEDO_SURFACE_CAPTURE_RANGE =
            TerminalConfigValue.number("equipment", "torpedo", "surface_capture_range", 1.0, 0.25, 6.0);

    // ===== 鱼雷制导/引信/口径兜底（2026-10-05 下沉）=====
    // WHY：这些原先是 TorpedoEntity 里的写死常量，策划无法在调试终端调手感。
    // 键统一为 global.torpedo.*（与上面的 surface_* 同 target，终端里归到同一目标下）。
    // 默认值全部 = 下沉前的写死值，纯重构、行为逐字节等价。

    /** 声导扫描半径（格）：非潜行目标。默认 25.0。 */
    public static final TerminalConfigValue<Double> TORPEDO_ACOUSTIC_DETECT_RANGE =
            TerminalConfigValue.number("equipment", "torpedo", "acoustic_detect_range", 25.0, 0.0, 256.0);

    /** 声导扫描半径（格）：潜行（Shift）目标，比常规更近。默认 10.0。 */
    public static final TerminalConfigValue<Double> TORPEDO_ACOUSTIC_SNEAK_RANGE =
            TerminalConfigValue.number("equipment", "torpedo", "acoustic_sneak_range", 10.0, 0.0, 256.0);

    /** 声导基础最大转角（度/tick）。近/远距离再乘倍率。默认 3.0。 */
    public static final TerminalConfigValue<Double> TORPEDO_ACOUSTIC_MAX_TURN_DEG =
            TerminalConfigValue.number("equipment", "torpedo", "acoustic_max_turn_deg", 3.0, 0.0, 180.0);

    /** 声导引信起爆前的安全延迟（ticks）。默认 10。 */
    public static final TerminalConfigValue<Integer> TORPEDO_ACOUSTIC_ARM_TICKS =
            TerminalConfigValue.integer("equipment", "torpedo", "acoustic_arm_ticks", 10, 0, 12000);

    /** 声导「近距离」判定（格），以内用近距转角倍率。默认 5.0。 */
    public static final TerminalConfigValue<Double> TORPEDO_ACOUSTIC_CLOSE_RANGE =
            TerminalConfigValue.number("equipment", "torpedo", "acoustic_close_range", 5.0, 0.0, 256.0);

    /** 声导「中距离」判定（格），以内用基础转角、以外用远距倍率。默认 15.0。 */
    public static final TerminalConfigValue<Double> TORPEDO_ACOUSTIC_MID_RANGE =
            TerminalConfigValue.number("equipment", "torpedo", "acoustic_mid_range", 15.0, 0.0, 256.0);

    /** 近距离声导转角倍率。默认 1.5。 */
    public static final TerminalConfigValue<Double> TORPEDO_ACOUSTIC_CLOSE_TURN_MULTIPLIER =
            TerminalConfigValue.number("equipment", "torpedo", "acoustic_close_turn_multiplier", 1.5, 0.0, 10.0);

    /** 远距离声导转角倍率。默认 0.6。 */
    public static final TerminalConfigValue<Double> TORPEDO_ACOUSTIC_FAR_TURN_MULTIPLIER =
            TerminalConfigValue.number("equipment", "torpedo", "acoustic_far_turn_multiplier", 0.6, 0.0, 10.0);

    /** 声导目标扫描节流（ticks），避免每 tick 扫描。默认 5。 */
    public static final TerminalConfigValue<Integer> TORPEDO_ACOUSTIC_SCAN_INTERVAL =
            TerminalConfigValue.integer("equipment", "torpedo", "acoustic_scan_interval", 5, 1, 1200);

    /** 声导最短锁定周期（ticks）：锁定期内不切换目标。默认 60。 */
    public static final TerminalConfigValue<Integer> TORPEDO_LOCK_MIN_DURATION =
            TerminalConfigValue.integer("equipment", "torpedo", "lock_min_duration", 60, 0, 12000);

    /** 声导断锁距离（格）：目标超出即丢失。默认 30.0。 */
    public static final TerminalConfigValue<Double> TORPEDO_LOCK_BREAK_DISTANCE =
            TerminalConfigValue.number("equipment", "torpedo", "lock_break_distance", 30.0, 0.0, 512.0);

    /** 声导目标切换阈值：新目标距离需小于当前目标的此比例才切换。默认 0.7。 */
    public static final TerminalConfigValue<Double> TORPEDO_TARGET_SWITCH_THRESHOLD =
            TerminalConfigValue.number("equipment", "torpedo", "target_switch_threshold", 0.7, 0.0, 1.0);

    /** 氧气鱼雷航速倍率（无航迹、高速）。默认 1.3。 */
    public static final TerminalConfigValue<Double> TORPEDO_OXYGEN_SPEED_MULTIPLIER =
            TerminalConfigValue.number("equipment", "torpedo", "oxygen_speed_multiplier", 1.3, 0.1, 10.0);

    /** 磁性近炸检测/起爆距离（格）。默认 3.0。 */
    public static final TerminalConfigValue<Double> TORPEDO_MAGNETIC_DETONATE_DIST =
            TerminalConfigValue.number("equipment", "torpedo", "magnetic_detonate_dist", 3.0, 0.0, 64.0);

    /** 磁性引信起爆前的安全延迟（ticks）。默认 5。 */
    public static final TerminalConfigValue<Integer> TORPEDO_MAGNETIC_ARM_TICKS =
            TerminalConfigValue.integer("equipment", "torpedo", "magnetic_arm_ticks", 5, 0, 12000);

    // ---- 口径兜底值 ----
    // WHY 两套口径分开：533/530/720 走常规档，610 走重雷档。注意玩家发射路径会由
    // TorpedoFireStrategy 用 TorpedoItem 的型号值（终端键 torpedo.<型号>.*）覆盖伤害/寿命/航速；
    // 这里的兜底值真正生效的是「不指定型号」的路径（NPC/空投雷）与未覆盖的爆炸半径。

    /** 常规口径鱼雷兜底伤害。默认 18.0。 */
    public static final TerminalConfigValue<Double> TORPEDO_DEFAULT_DAMAGE =
            TerminalConfigValue.number("equipment", "torpedo", "default_damage", 18.0, 0.0, 10000.0);

    /** 610mm 重雷兜底伤害。默认 28.0。 */
    public static final TerminalConfigValue<Double> TORPEDO_HEAVY_DAMAGE =
            TerminalConfigValue.number("equipment", "torpedo", "heavy_damage", 28.0, 0.0, 10000.0);

    /** 常规口径鱼雷爆炸半径。默认 2.0。 */
    public static final TerminalConfigValue<Double> TORPEDO_DEFAULT_EXPLOSION_RADIUS =
            TerminalConfigValue.number("equipment", "torpedo", "default_explosion_radius", 2.0, 0.0, 100.0);

    /** 610mm 重雷爆炸半径。默认 2.5。 */
    public static final TerminalConfigValue<Double> TORPEDO_HEAVY_EXPLOSION_RADIUS =
            TerminalConfigValue.number("equipment", "torpedo", "heavy_explosion_radius", 2.5, 0.0, 100.0);

    /** 鱼雷兜底寿命（ticks），未按型号指定航程时使用。默认 1200（= 航程 60 × 20）。 */
    public static final TerminalConfigValue<Integer> TORPEDO_DEFAULT_LIFETIME =
            TerminalConfigValue.integer("equipment", "torpedo", "default_lifetime", 1200, 1, 100000);

    public static final TerminalConfigValue<Double> ARMOR_PLATE_PROTECTION =
            TerminalConfigValue.number("equipment", "equipment", "armor_plate_protection", 3.0, 0.0, 100.0);


    public static final TerminalConfigValue<Integer> BALLISTIC_MAX_ITERATIONS =
            TerminalConfigValue.integer("equipment", "equipment", "ballistic_max_iterations", 100, 5, 200);

    public static final TerminalConfigValue<Integer> BALLISTIC_MAX_STEPS =
            TerminalConfigValue.integer("equipment", "equipment", "ballistic_max_steps", 200, 50, 1000);

    public static final TerminalConfigValue<Double> BALLISTIC_ACCURACY =
            TerminalConfigValue.number("equipment", "equipment", "ballistic_accuracy", 0.01, 0.001, 5.0);

    public static final TerminalConfigValue<Integer> BALLISTIC_CACHE_SIZE =
            TerminalConfigValue.integer("equipment", "equipment", "ballistic_cache_size", 32, 1, 256);

    // ===== 小型船职能分化（策划决策/数值/05）=====
    // 这两个值原先在 ProjectileImpactResolver（实际开火路径）和 ShipTypeMitigationHelper
    // 各写死一份，存在双份漂移风险；现只在此处定义，两处读取同一来源。
    // 键：equipment/small_ship/* → global.small_ship.*

    /** 大口径（caliber>8）AP 命中变身小船的过穿倍率：实际伤害 = 标伤 × 该值。默认 0.05 = 5%。 */
    public static final TerminalConfigValue<Double> SMALL_SHIP_LARGE_AP_OVERPEN =
            TerminalConfigValue.number("equipment", "small_ship", "large_ap_overpen", 0.05, 0.0, 1.0);

    /** 小船单次受击伤害上限 = 目标最大血量 × 该值。默认 0.25 = 1/4。 */
    public static final TerminalConfigValue<Double> SMALL_SHIP_DAMAGE_CAP_RATIO =
            TerminalConfigValue.number("equipment", "small_ship", "damage_cap_ratio", 0.25, 0.0, 1.0);

    // ===== 起火 Debuff 施加规则（策划决策/战斗/04）=====
    // 键：equipment/fire_debuff/* → global.fire_debuff.*

    /** 着火持续时长（tick）。默认 300 = 15 秒。 */
    public static final TerminalConfigValue<Integer> FIRE_DURATION_TICKS =
            TerminalConfigValue.integer("equipment", "fire_debuff", "duration_ticks", 300, 1, 6000);

    /** 起火最大等级（amplifier 上限 = 该值 - 1）。默认 4。 */
    public static final TerminalConfigValue<Integer> FIRE_MAX_LEVEL =
            TerminalConfigValue.integer("equipment", "fire_debuff", "max_level", 4, 1, 10);

    /** 小口径 HE 起火概率。默认 0.05 = 5%。 */
    public static final TerminalConfigValue<Double> FIRE_SMALL_PROB =
            TerminalConfigValue.number("equipment", "fire_debuff", "small_prob", 0.05, 0.0, 1.0);
    /** 中口径 HE 起火概率。默认 0.15 = 15%。 */
    public static final TerminalConfigValue<Double> FIRE_MEDIUM_PROB =
            TerminalConfigValue.number("equipment", "fire_debuff", "medium_prob", 0.15, 0.0, 1.0);
    /** 大口径 HE 起火概率。默认 0.40 = 40%。 */
    public static final TerminalConfigValue<Double> FIRE_LARGE_PROB =
            TerminalConfigValue.number("equipment", "fire_debuff", "large_prob", 0.40, 0.0, 1.0);

    /** 航空炸弹单次随机判定阈值：r < bomb_double_roll → 等级 +2；r < bomb_single_roll → +1；否则失败。 */
    public static final TerminalConfigValue<Double> FIRE_BOMB_DOUBLE_ROLL =
            TerminalConfigValue.number("equipment", "fire_debuff", "bomb_double_roll", 0.4, 0.0, 1.0);
    public static final TerminalConfigValue<Double> FIRE_BOMB_SINGLE_ROLL =
            TerminalConfigValue.number("equipment", "fire_debuff", "bomb_single_roll", 0.8, 0.0, 1.0);

    // ===== 舰载机攻击战术（AircraftCombat）=====
    // WHY：这些原先是 AircraftCombat 各攻击剖面的写死手感常量，策划需在调试终端实时调，
    // 故统一下沉（用户规则：需要调手感的数值一律终端可调）。默认值全部 = 下沉前的写死值，
    // 纯重构、行为逐字节等价。
    // 插入层选择：AircraftCombatService 只是把「profile → AttackStrategy」映射到旧战斗分支的
    // 迁移适配器（AttackStrategy 默认 execute 直接调 AircraftCombat.tickAttacking），战术实现
    // 仍集中在 AircraftCombat 且玩家/自主两条路径共用同一份 tickXxx。因此参数在 AircraftCombat
    // 这一层读取，而不是塞进各 profile 策略（策略里目前没有任何数值，塞进去反而会把共用战术拆散）。
    // 键：global.<剖面>_combat.*；水平轰炸沿用既有 target level_bomber（与既有
    // horizontal_velocity_multiplier 归到同一目标下）。
    //
    // 注意两条语义不同、拒绝合并的机制：
    //   - 俯冲投弹的水平动量 ×0.85（global.dive_bomber_combat.horizontal_momentum）——俯冲角下坠，
    //     只保留少量水平动量；
    //   - 水平轰炸的水平速度倍数（global.level_bomber.horizontal_velocity_multiplier，已在终端）——
    //     平飞前抛。
    // 两者都要可调，但不是同一种东西，不能合成一个参数。

    // ---- 战斗机（tickFighterAttack）----
    /** 追击/退避的距离基准（格，目标眼位到飞机的距离）。默认 11.0。 */
    public static final TerminalConfigValue<Double> FIGHTER_HOVER_DISTANCE =
            TerminalConfigValue.number("aircraft", "fighter_combat", "hover_distance", 11.0, 0.0, 256.0);
    /** 悬停死区半宽（格）：距离在基准 ±该值内则减速悬停。默认 3.0。 */
    public static final TerminalConfigValue<Double> FIGHTER_HOVER_BAND =
            TerminalConfigValue.number("aircraft", "fighter_combat", "hover_band", 3.0, 0.0, 128.0);
    /** 追击速度 = 面板速度 × 该值 × 阶段系数。默认 0.5。 */
    public static final TerminalConfigValue<Double> FIGHTER_PURSUIT_MULTIPLIER =
            TerminalConfigValue.number("aircraft", "fighter_combat", "pursuit_multiplier", 0.5, 0.0, 5.0);
    /** 退避速度 = 面板速度 × 该值 × 阶段系数。默认 0.2。 */
    public static final TerminalConfigValue<Double> FIGHTER_RETREAT_MULTIPLIER =
            TerminalConfigValue.number("aircraft", "fighter_combat", "retreat_multiplier", 0.2, 0.0, 5.0);
    /** 悬停时的每 tick 速度阻尼（0~1）。默认 0.8。 */
    public static final TerminalConfigValue<Double> FIGHTER_HOVER_DAMPING =
            TerminalConfigValue.number("aircraft", "fighter_combat", "hover_damping", 0.8, 0.0, 1.0);
    /** 机枪单发伤害 = 面板伤害 / 该值。默认 8.0。 */
    public static final TerminalConfigValue<Double> FIGHTER_BULLET_DAMAGE_DIVISOR =
            TerminalConfigValue.number("aircraft", "fighter_combat", "bullet_damage_divisor", 8.0, 0.01, 1024.0);
    /** 机枪弹速（格/tick）。默认 2.5。 */
    public static final TerminalConfigValue<Double> FIGHTER_BULLET_SPEED =
            TerminalConfigValue.number("aircraft", "fighter_combat", "bullet_speed", 2.5, 0.0, 100.0);
    /** 机枪开火距离（格）。默认 24.0。 */
    public static final TerminalConfigValue<Double> FIGHTER_BULLET_RANGE =
            TerminalConfigValue.number("aircraft", "fighter_combat", "bullet_range", 24.0, 0.0, 512.0);

    // ---- 火箭机（tickRocketFighterMissileRun）----
    /** 一轮火箭攻击的最长持续（tick），超时后视为已投弹。默认 200。 */
    public static final TerminalConfigValue<Integer> ROCKET_FIGHTER_RUN_TICKS =
            TerminalConfigValue.integer("aircraft", "rocket_fighter_combat", "run_ticks", 200, 0, 120000);
    /** 攻击高度 = 目标 Y + 该值（格）。默认 8.0。 */
    public static final TerminalConfigValue<Double> ROCKET_FIGHTER_ATTACK_ALTITUDE_OFFSET =
            TerminalConfigValue.number("aircraft", "rocket_fighter_combat", "attack_altitude_offset", 8.0, -64.0, 256.0);
    /** 投弹窗口：目标水平距离下限（格）。默认 12.0。 */
    public static final TerminalConfigValue<Double> ROCKET_FIGHTER_WINDOW_HORIZONTAL_MIN =
            TerminalConfigValue.number("aircraft", "rocket_fighter_combat", "window_horizontal_min", 12.0, 0.0, 512.0);
    /** 投弹窗口：目标水平距离上限（格）。默认 22.0。 */
    public static final TerminalConfigValue<Double> ROCKET_FIGHTER_WINDOW_HORIZONTAL_MAX =
            TerminalConfigValue.number("aircraft", "rocket_fighter_combat", "window_horizontal_max", 22.0, 0.0, 512.0);
    /** 投弹窗口：相对攻击高度的偏低容差（格，负值）。默认 -2.0。 */
    public static final TerminalConfigValue<Double> ROCKET_FIGHTER_WINDOW_ALTITUDE_MIN =
            TerminalConfigValue.number("aircraft", "rocket_fighter_combat", "window_altitude_min", -2.0, -256.0, 256.0);
    /** 投弹窗口：相对攻击高度的偏高容差（格）。默认 8.0。 */
    public static final TerminalConfigValue<Double> ROCKET_FIGHTER_WINDOW_ALTITUDE_MAX =
            TerminalConfigValue.number("aircraft", "rocket_fighter_combat", "window_altitude_max", 8.0, -256.0, 256.0);
    /** 火箭弹伤 = 面板伤害 × 该值。默认 1.2。 */
    public static final TerminalConfigValue<Double> ROCKET_FIGHTER_DAMAGE_MULTIPLIER =
            TerminalConfigValue.number("aircraft", "rocket_fighter_combat", "damage_multiplier", 1.2, 0.0, 100.0);
    /** 火箭弹爆炸威力。默认 2.0。 */
    public static final TerminalConfigValue<Double> ROCKET_FIGHTER_EXPLOSION_POWER =
            TerminalConfigValue.number("aircraft", "rocket_fighter_combat", "explosion_power", 2.0, 0.0, 100.0);
    /** 火箭弹扇形散布的相邻弹夹角（弧度）。默认 0.07。 */
    public static final TerminalConfigValue<Double> ROCKET_FIGHTER_SPREAD_STEP =
            TerminalConfigValue.number("aircraft", "rocket_fighter_combat", "spread_step", 0.07, 0.0, 3.141592653589793);
    /** 接敌速度 = 面板速度 × 该值 × 阶段系数。默认 0.5。 */
    public static final TerminalConfigValue<Double> ROCKET_FIGHTER_PURSUIT_MULTIPLIER =
            TerminalConfigValue.number("aircraft", "rocket_fighter_combat", "pursuit_multiplier", 0.5, 0.0, 5.0);

    // ---- 俯冲轰炸（tickDiveBomberAttack）----
    /** 爬升高度 = 目标 Y + 该值（格）。默认 18.0。 */
    public static final TerminalConfigValue<Double> DIVE_BOMBER_CLIMB_ALTITUDE_OFFSET =
            TerminalConfigValue.number("aircraft", "dive_bomber_combat", "climb_altitude_offset", 18.0, -64.0, 256.0);
    /** 爬升速度 = 面板速度 × 该值 × 阶段系数。默认 0.4。 */
    public static final TerminalConfigValue<Double> DIVE_BOMBER_CLIMB_SPEED_MULTIPLIER =
            TerminalConfigValue.number("aircraft", "dive_bomber_combat", "climb_speed_multiplier", 0.4, 0.0, 5.0);
    /** 俯冲瞄准点估计速度 = 面板速度 × 该值 × 阶段系数。默认 0.6。 */
    public static final TerminalConfigValue<Double> DIVE_BOMBER_DIVE_SPEED_MULTIPLIER =
            TerminalConfigValue.number("aircraft", "dive_bomber_combat", "dive_speed_multiplier", 0.6, 0.0, 5.0);
    /** 俯冲段速度 = 面板速度 × 该值 × 阶段系数。默认 0.7。 */
    public static final TerminalConfigValue<Double> DIVE_BOMBER_DIVE_APPROACH_MULTIPLIER =
            TerminalConfigValue.number("aircraft", "dive_bomber_combat", "dive_approach_multiplier", 0.7, 0.0, 5.0);
    /** 判定「已爬升到投弹高度」的高度容差（格）。默认 1.0。 */
    public static final TerminalConfigValue<Double> DIVE_BOMBER_CLIMB_ARRIVAL_TOLERANCE =
            TerminalConfigValue.number("aircraft", "dive_bomber_combat", "climb_arrival_tolerance", 1.0, 0.0, 64.0);
    /** 爬升超时下限（tick），低于此不吃超时保护。默认 80。 */
    public static final TerminalConfigValue<Integer> DIVE_BOMBER_CLIMB_TIMEOUT_MIN_TICKS =
            TerminalConfigValue.integer("aircraft", "dive_bomber_combat", "climb_timeout_min_ticks", 80, 0, 120000);
    /** 投弹距离：瞄准点水平距离小于该值时投弹（格）。默认 2.0。 */
    public static final TerminalConfigValue<Double> DIVE_BOMBER_RELEASE_DISTANCE =
            TerminalConfigValue.number("aircraft", "dive_bomber_combat", "release_distance", 2.0, 0.0, 128.0);
    /** 航弹爆炸威力。默认 4.0。 */
    public static final TerminalConfigValue<Double> DIVE_BOMBER_EXPLOSION_POWER =
            TerminalConfigValue.number("aircraft", "dive_bomber_combat", "explosion_power", 4.0, 0.0, 100.0);
    /** 投弹时航弹继承的载机水平动量倍数。默认 0.85。 */
    public static final TerminalConfigValue<Double> DIVE_BOMBER_HORIZONTAL_MOMENTUM =
            TerminalConfigValue.number("aircraft", "dive_bomber_combat", "horizontal_momentum", 0.85, 0.0, 2.0);
    /** 投弹时航弹垂直速度上限（越负下坠越猛），取 min(该值, 载机垂直速度)。默认 -0.20。 */
    public static final TerminalConfigValue<Double> DIVE_BOMBER_MIN_VERTICAL_VELOCITY =
            TerminalConfigValue.number("aircraft", "dive_bomber_combat", "min_vertical_velocity", -0.2, -10.0, 10.0);
    /** 俯冲投弹的随机散布幅度（格）：坐标偏移 = (rand-0.5) × 该值。默认 0.5。 */
    public static final TerminalConfigValue<Double> DIVE_BOMBER_BOMB_SPREAD =
            TerminalConfigValue.number("aircraft", "dive_bomber_combat", "bomb_spread", 0.5, 0.0, 16.0);

    // ---- 鱼雷机（tickTorpedoBomberAttack）----
    /** 投雷高度 = 目标 Y + 该值（格）。默认 12.0。 */
    public static final TerminalConfigValue<Double> TORPEDO_BOMBER_APPROACH_ALTITUDE =
            TerminalConfigValue.number("aircraft", "torpedo_bomber_combat", "approach_altitude", 12.0, -64.0, 256.0);
    /** 下降段速度 = 面板速度 × 该值 × 阶段系数。默认 0.4。 */
    public static final TerminalConfigValue<Double> TORPEDO_BOMBER_DESCEND_SPEED_MULTIPLIER =
            TerminalConfigValue.number("aircraft", "torpedo_bomber_combat", "descend_speed_multiplier", 0.4, 0.0, 5.0);
    /** 下降段速度硬上限（格/tick）。默认 1.0。 */
    public static final TerminalConfigValue<Double> TORPEDO_BOMBER_DESCEND_MAX_SPEED =
            TerminalConfigValue.number("aircraft", "torpedo_bomber_combat", "descend_max_speed", 1.0, 0.0, 100.0);
    /** 高于投雷高度的判定容差（格），超过则继续下降。默认 2.0。 */
    public static final TerminalConfigValue<Double> TORPEDO_BOMBER_ALTITUDE_TOLERANCE =
            TerminalConfigValue.number("aircraft", "torpedo_bomber_combat", "altitude_tolerance", 2.0, 0.0, 64.0);
    /** 投雷距离：目标水平距离小于该值时投雷（格）。默认 6.0。 */
    public static final TerminalConfigValue<Double> TORPEDO_BOMBER_RELEASE_DISTANCE =
            TerminalConfigValue.number("aircraft", "torpedo_bomber_combat", "release_distance", 6.0, 0.0, 128.0);
    /** 多枚鱼雷的横向偏移间隔（格）。默认 1.2。 */
    public static final TerminalConfigValue<Double> TORPEDO_BOMBER_SPREAD_OFFSET =
            TerminalConfigValue.number("aircraft", "torpedo_bomber_combat", "spread_offset", 1.2, 0.0, 16.0);
    /** 鱼雷投放点相对载机的高度下沉（格）。默认 1.0。 */
    public static final TerminalConfigValue<Double> TORPEDO_BOMBER_DROP_HEIGHT_OFFSET =
            TerminalConfigValue.number("aircraft", "torpedo_bomber_combat", "drop_height_offset", 1.0, 0.0, 16.0);
    /** 空投鱼雷初速（格/tick）。默认 0.8。
     *  WHY 与 NPC 齐射的 torpedo_salvo.speed 分开：那是 NPC 水面齐射，这是空投雷，语义不同不可合并。 */
    public static final TerminalConfigValue<Double> TORPEDO_BOMBER_TORPEDO_SPEED =
            TerminalConfigValue.number("aircraft", "torpedo_bomber_combat", "torpedo_speed", 0.8, 0.0, 10.0);
    /** 空投鱼雷初速的垂直分量（格/tick，入水角度）。默认 -0.1。 */
    public static final TerminalConfigValue<Double> TORPEDO_BOMBER_TORPEDO_VERTICAL_SPEED =
            TerminalConfigValue.number("aircraft", "torpedo_bomber_combat", "torpedo_vertical_speed", -0.1, -10.0, 10.0);
    /** 平飞接敌速度 = 面板速度 × 该值 × 阶段系数。默认 0.5。 */
    public static final TerminalConfigValue<Double> TORPEDO_BOMBER_APPROACH_SPEED_MULTIPLIER =
            TerminalConfigValue.number("aircraft", "torpedo_bomber_combat", "approach_speed_multiplier", 0.5, 0.0, 5.0);
    /** 平飞时对高度差的每 tick 修正比例（0~1）。默认 0.1。 */
    public static final TerminalConfigValue<Double> TORPEDO_BOMBER_ALTITUDE_ADJUST =
            TerminalConfigValue.number("aircraft", "torpedo_bomber_combat", "altitude_adjust", 0.1, 0.0, 1.0);

    // ---- 水平轰炸（tickLevelBomberAttack，target 沿用既有 level_bomber）----
    /** 投弹高度 = 目标 Y + 该值（格）。默认 32.0。 */
    public static final TerminalConfigValue<Double> LEVEL_BOMBER_ALTITUDE_OFFSET =
            TerminalConfigValue.number("equipment", "level_bomber", "altitude_offset", 32.0, -64.0, 512.0);
    /** 投弹初速的垂直分量（格/tick，负值下坠）。默认 -0.3。 */
    public static final TerminalConfigValue<Double> LEVEL_BOMBER_VERTICAL_VELOCITY =
            TerminalConfigValue.number("equipment", "level_bomber", "vertical_velocity", -0.3, -10.0, 10.0);
    /** 投弹提前判据的最小窗口（格），防止倍数为 0 时判据永不成立。默认 0.5。 */
    public static final TerminalConfigValue<Double> LEVEL_BOMBER_MIN_RELEASE_DISTANCE =
            TerminalConfigValue.number("equipment", "level_bomber", "min_release_distance", 0.5, 0.0, 128.0);
    /** 爬升到投弹高度的速度 = 面板速度 × 该值 × 阶段系数。默认 0.4。 */
    public static final TerminalConfigValue<Double> LEVEL_BOMBER_CLIMB_SPEED_MULTIPLIER =
            TerminalConfigValue.number("equipment", "level_bomber", "climb_speed_multiplier", 0.4, 0.0, 5.0);
    /** 投弹航线速度 = 面板速度 × 该值 × 阶段系数。默认 0.6。 */
    public static final TerminalConfigValue<Double> LEVEL_BOMBER_RUN_SPEED_MULTIPLIER =
            TerminalConfigValue.number("equipment", "level_bomber", "run_speed_multiplier", 0.6, 0.0, 5.0);
    /** 判定「已爬升到投弹高度」的高度容差（格）。默认 2.0。 */
    public static final TerminalConfigValue<Double> LEVEL_BOMBER_ALTITUDE_TOLERANCE =
            TerminalConfigValue.number("equipment", "level_bomber", "altitude_tolerance", 2.0, 0.0, 64.0);
    /** 投弹的随机散布幅度（格）：坐标偏移 = (rand-0.5) × 该值。默认 1.0。 */
    public static final TerminalConfigValue<Double> LEVEL_BOMBER_BOMB_SPREAD =
            TerminalConfigValue.number("equipment", "level_bomber", "bomb_spread", 1.0, 0.0, 16.0);
    /** 航弹爆炸威力。默认 4.0。 */
    public static final TerminalConfigValue<Double> LEVEL_BOMBER_EXPLOSION_POWER =
            TerminalConfigValue.number("equipment", "level_bomber", "explosion_power", 4.0, 0.0, 100.0);
    /** 航弹投放点相对载机的下沉高度（格），同时也是提前量算的起始高度差。默认 0.5。 */
    public static final TerminalConfigValue<Double> LEVEL_BOMBER_DROP_Y_OFFSET =
            TerminalConfigValue.number("equipment", "level_bomber", "drop_y_offset", 0.5, 0.0, 16.0);
    /** 投弹航线超时（tick）：超时强制投弹，避免倍数为 0 时永远不投。默认 200。 */
    public static final TerminalConfigValue<Integer> LEVEL_BOMBER_RUN_TIMEOUT_TICKS =
            TerminalConfigValue.integer("equipment", "level_bomber", "run_timeout_ticks", 200, 0, 120000);
    /** 投弹后继续平飞再返航的 tick 数。默认 220。 */
    public static final TerminalConfigValue<Integer> LEVEL_BOMBER_RETURN_TICKS =
            TerminalConfigValue.integer("equipment", "level_bomber", "return_ticks", 220, 0, 120000);

    // ===== 舰载机飞行常量（AircraftEntity）=====
    // WHY：这些原先是 AircraftEntity 的写死飞行常量，属于策划调手感的数值（用户规则）。
    // 它们当前是「全局一份」——所有机型共用同一个静态 final 值，不是每机型可配；因此按实际语义
    // 用单一全局 target aircraft_flight，而不是复制成每机型一份（那会改变默认解析与目录规模）。
    // 速度系数（launch/cruise/attack/...）早已终端化并在 AircraftEntity 读取，这里补几何与节奏。
    // 键：global.aircraft_flight.*。默认值全部 = 下沉前的写死值。

    /** 巡航/巡逻高度（格，相对所属者）。默认 18.0。 */
    public static final TerminalConfigValue<Double> AIRCRAFT_CRUISE_ALTITUDE =
            TerminalConfigValue.number("aircraft", "aircraft_flight", "cruise_altitude", 18.0, 0.0, 512.0);
    /** 盘旋半径（格）。默认 8.0。 */
    public static final TerminalConfigValue<Double> AIRCRAFT_ORBIT_RADIUS =
            TerminalConfigValue.number("aircraft", "aircraft_flight", "orbit_radius", 8.0, 0.0, 256.0);
    /** 起飞爬升阶段最长 tick 数。默认 30。 */
    public static final TerminalConfigValue<Integer> AIRCRAFT_LAUNCH_TICKS =
            TerminalConfigValue.integer("aircraft", "aircraft_flight", "launch_ticks", 30, 0, 120000);
    /** 返航到达判定距离（格）。默认 3.0。 */
    public static final TerminalConfigValue<Double> AIRCRAFT_RETURN_ARRIVAL_DISTANCE =
            TerminalConfigValue.number("aircraft", "aircraft_flight", "return_arrival_distance", 3.0, 0.0, 256.0);
    /** 油耗节奏：每该值 tick 消耗 1 点燃料。默认 4。 */
    public static final TerminalConfigValue<Integer> AIRCRAFT_FUEL_BURN_INTERVAL =
            TerminalConfigValue.integer("aircraft", "aircraft_flight", "fuel_burn_interval", 4, 1, 120000);
    /** 转弯圆弧半径（格），越大转弯越缓。默认 2.0。 */
    public static final TerminalConfigValue<Double> AIRCRAFT_TURN_RADIUS =
            TerminalConfigValue.number("aircraft", "aircraft_flight", "turn_radius", 2.0, 0.05, 256.0);
    /** 滞空上限（tick），超过强制返航/回收。默认 12000。 */
    public static final TerminalConfigValue<Integer> AIRCRAFT_MAX_AIRTIME =
            TerminalConfigValue.integer("aircraft", "aircraft_flight", "max_airtime", 12000, 1, 1000000);
    /** 轨道角速率（弧度/tick，再乘面板速度与阶段系数）。默认 0.015。 */
    public static final TerminalConfigValue<Double> AIRCRAFT_ORBIT_ANGULAR_RATE =
            TerminalConfigValue.number("aircraft", "aircraft_flight", "orbit_angular_rate", 0.015, 0.0, 3.141592653589793);
    /** 最低速比例：水平速度不得低于 面板速度 × 该值，防止悬停。默认 0.05。 */
    public static final TerminalConfigValue<Double> AIRCRAFT_MIN_SPEED_MULTIPLIER =
            TerminalConfigValue.number("aircraft", "aircraft_flight", "min_speed_multiplier", 0.05, 0.0, 1.0);
    /** 脱困检测间隔（tick）。默认 60。 */
    public static final TerminalConfigValue<Integer> AIRCRAFT_STUCK_CHECK_INTERVAL =
            TerminalConfigValue.integer("aircraft", "aircraft_flight", "stuck_check_interval", 60, 1, 120000);
    /** 脱困判定：一个检测周期内位移小于该值即视为卡住。默认 0.1。 */
    public static final TerminalConfigValue<Double> AIRCRAFT_STUCK_THRESHOLD =
            TerminalConfigValue.number("aircraft", "aircraft_flight", "stuck_threshold", 0.1, 0.0, 64.0);
    /** 离所属者最小距离上限（格），超过即返航；按模拟距离取更大值。默认 48.0。 */
    public static final TerminalConfigValue<Double> AIRCRAFT_MIN_DISTANCE_FROM_OWNER =
            TerminalConfigValue.number("aircraft", "aircraft_flight", "min_distance_from_owner", 48.0, 0.0, 4096.0);
    /** 侦察机离所属者最小距离上限（格）。默认 200.0。 */
    public static final TerminalConfigValue<Double> AIRCRAFT_MIN_RECON_DISTANCE =
            TerminalConfigValue.number("aircraft", "aircraft_flight", "min_recon_distance", 200.0, 0.0, 8192.0);

    public record CIWSConfig(TerminalConfigValue<Double> range, TerminalConfigValue<Integer> interval,
                             TerminalConfigValue<Integer> barrels, TerminalConfigValue<Integer> weight,
                             TerminalConfigValue<Double> damage) {}

    private static CIWSConfig ciws(String caliber) {
        String target = "ciws_" + caliber;
        return new CIWSConfig(
                TerminalConfigValue.number("equipment", target, "range", 16.0, 1.0, 128.0),
                TerminalConfigValue.integer("equipment", target, "interval", 20, 1, 1200),
                TerminalConfigValue.integer("equipment", target, "barrels", 1, 1, 16),
                TerminalConfigValue.integer("equipment", target, "weight", 0, 0, 112),
                // 单管单发基础伤害；最终 = damage × 口径伤害系数 × 管数。默认 1.5（下沉前写死值）。
                TerminalConfigValue.number("equipment", target, "damage", 1.5, 0.0, 100.0));
    }

    public static final CIWSConfig CIWS_20MM = ciws("20mm");
    public static final CIWSConfig CIWS_40MM = ciws("40mm");
    public static final CIWSConfig CIWS_76MM = ciws("76mm");
}
