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

    /** 空投下落阶段的水平速度衰减系数。默认沿用 0.98。 */
    public static final TerminalConfigValue<Double> TORPEDO_AIR_DROP_HORIZONTAL_DECAY =
            TerminalConfigValue.number("equipment", "torpedo", "air_drop_horizontal_decay", 0.98, 0.0, 1.0);

    /** 空投下落阶段每 tick 的垂直加速度。默认沿用 0.08。 */
    public static final TerminalConfigValue<Double> TORPEDO_AIR_DROP_VERTICAL_ACCEL =
            TerminalConfigValue.number("equipment", "torpedo", "air_drop_vertical_accel", 0.08, 0.0, 0.5);

    /** 非空投空中回退的水平速度衰减系数。默认沿用 0.70。 */
    public static final TerminalConfigValue<Double> TORPEDO_AIR_FALL_HORIZONTAL_DECAY =
            TerminalConfigValue.number("equipment", "torpedo", "air_fall_horizontal_decay", 0.70, 0.0, 1.0);

    /** 非空投空中回退每 tick 的垂直加速度。默认沿用 0.25。 */
    public static final TerminalConfigValue<Double> TORPEDO_AIR_FALL_VERTICAL_ACCEL =
            TerminalConfigValue.number("equipment", "torpedo", "air_fall_vertical_accel", 0.25, 0.0, 0.5);

    /** 线导模式垂直输入的归一化死区。 */
    public static final TerminalConfigValue<Double> TORPEDO_WIRE_VERTICAL_DEADZONE =
            TerminalConfigValue.number("equipment", "torpedo", "wire_vertical_deadzone", 0.05, 0.0, 0.99);

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

    // ===== 导弹发射器共享默认值（2026-10-05）=====
    // WHY 单独一组 global.missile_launcher.*：每个型号的数值已按 missile_launcher.<注册路径>.*
    // 单独进终端，这组「全型号共用」的值让策划一次调完整族，不必逐个型号改。
    // 读取侧（MissileLauncherItem）三层回退：单型号键 > 本组共享键 > 物品注册基准；本组未设置时
    // 回退到每个发射器各自的注册值，因此默认行为与下沉前逐字节一致。
    // 注意 group=missile_launcher，经 DebugTerminalScreen.categoryFor 落到「导弹」独立分类。
    // 注意 fire_cooldown 命中终端的 tick→秒 显示启发式（×0.05），与各型号同键一致，勿改名。
    /** 共享伤害覆盖（未设置时用各发射器注册值）。 */
    public static final TerminalConfigValue<Double> MISSILE_LAUNCHER_DAMAGE =
            TerminalConfigValue.number("missile_launcher", "missile_launcher", "damage", 24.0, 0.0, 10000.0);
    /** 共享穿甲覆盖（未设置时用各发射器注册值）。 */
    public static final TerminalConfigValue<Double> MISSILE_LAUNCHER_ARMOR_PEN =
            TerminalConfigValue.number("missile_launcher", "missile_launcher", "armor_pen", 0.0, 0.0, 100.0);
    /** 共享爆炸威力覆盖（未设置时用各发射器注册值）。 */
    public static final TerminalConfigValue<Double> MISSILE_LAUNCHER_EXPLOSION_POWER =
            TerminalConfigValue.number("missile_launcher", "missile_launcher", "explosion_power", 2.0, 0.0, 100.0);
    /** 共享连装数覆盖（未设置时用各发射器注册值）。 */
    public static final TerminalConfigValue<Integer> MISSILE_LAUNCHER_BURST_COUNT =
            TerminalConfigValue.integer("missile_launcher", "missile_launcher", "burst_count", 2, 0, 64);
    /** 共享发射冷却覆盖（tick，未设置时用各发射器注册值）。 */
    public static final TerminalConfigValue<Integer> MISSILE_LAUNCHER_FIRE_COOLDOWN =
            TerminalConfigValue.integer("missile_launcher", "missile_launcher", "fire_cooldown", 1200, 0, 12000);

    // ===== 随从（舰娘）养成与战斗（2026-10-05 下沉）=====
    // WHY：这些原先是 ShipGirlProgressionManager / DungeonFollowerManager / ShipGirlEntity /
    // ShipGirlCombatGoal 里的写死平衡值，策划需要反复调手感（用户规则：手感数值一律终端可调）。
    // 默认值全部 = 下沉前的写死值，纯重构、行为等价。键：global.<target>.*，group=ship_girl
    // 使其归入终端「NPC舰娘」分类。

    // ---- 养成曲线（target=follower_progression）----
    /** 每名玩家携带位上限。默认 2。养成本与副本携带位共用此值，避免两处漂移。 */
    public static final TerminalConfigValue<Integer> FOLLOW_PROGRESSION_MAX_FOLLOWERS =
            TerminalConfigValue.integer("ship_girl", "follower_progression", "max_followers", 2, 0, 64);
    /** 舰娘等级上限。默认 50。 */
    public static final TerminalConfigValue<Integer> FOLLOW_PROGRESSION_MAX_LEVEL =
            TerminalConfigValue.integer("ship_girl", "follower_progression", "max_level", 50, 1, 1000);
    /** 每级所需经验基数。默认 100。 */
    public static final TerminalConfigValue<Integer> FOLLOW_PROGRESSION_BASE_XP =
            TerminalConfigValue.integer("ship_girl", "follower_progression", "base_xp_per_level", 100, 1, 1000000);
    /** 经验缩放因子（每级所需经验 = 基数 × 该值^(level-1)）。默认 1.15。 */
    public static final TerminalConfigValue<Double> FOLLOW_PROGRESSION_XP_SCALE =
            TerminalConfigValue.number("ship_girl", "follower_progression", "xp_scale", 1.15, 0.5, 5.0);

    // ---- 随从战斗（target=follower_combat）----
    /** 随从 DPS 上限系数：归一化攻击 = 基础攻击 × 难度 × 该值。默认 0.7，随从的总强度闸。 */
    public static final TerminalConfigValue<Double> FOLLOW_COMBAT_DPS_CAP =
            TerminalConfigValue.number("ship_girl", "follower_combat", "dps_cap", 0.7, 0.0, 10.0);
    /** 舰娘随从每次炮击后的开火冷却（tick，终端按秒显示）。默认 80（4 秒）。 */
    public static final TerminalConfigValue<Integer> FOLLOW_COMBAT_FIRE_COOLDOWN =
            TerminalConfigValue.integer("ship_girl", "follower_combat", "fire_cooldown", 80, 1, 6000);
    /** 炮击抛弧抬升系数：垂直瞄准补偿 = 水平距离 × 该值。默认 0.05。 */
    public static final TerminalConfigValue<Double> FOLLOW_COMBAT_SHELL_ARC_FACTOR =
            TerminalConfigValue.number("ship_girl", "follower_combat", "shell_arc_factor", 0.05, 0.0, 10.0);

    // ---- 随从重伤撤退（target=follower_retreat）----
    /** 大破阈值：HP 低于最大值的该比例时随从撤退。默认 0.20。 */
    public static final TerminalConfigValue<Double> FOLLOW_RETREAT_HEAVY_DAMAGE_THRESHOLD =
            TerminalConfigValue.number("ship_girl", "follower_retreat", "heavy_damage_threshold", 0.20, 0.0, 1.0);
    /** 大破撤退间隔（tick，终端按秒显示），防止同一随从被反复触发。默认 100（5 秒）。 */
    public static final TerminalConfigValue<Integer> FOLLOW_RETREAT_COOLDOWN =
            TerminalConfigValue.integer("ship_girl", "follower_retreat", "retreat_cooldown", 100, 0, 12000);

    // ===== 战斗 Buff 数值（CombatEffectRules，2026-10-05 下沉）=====
    // WHY：规避/经验/装填/燃烧原先在 CombatEffectRules 里写死，4 处消费者（EvasionHandler、
    // TransformationManager、ServerGameEvents、BurningEffect）都读该类，改这里即全面生效。
    // 规避按策划口径「每档位一组」拆成 3 个独立参数，不压成单个步长值。
    // 经验/装填保留「基数 ± 步进 × 档位」的原算法，只把基数与步进参数化，保证逐位等价。
    // 燃烧间隔按 4 档位各给一个整数参数（整数无精度问题）。
    // 键：global.combat_effect.*。

    /** 规避 I 档闪避概率。默认 0.10 = 10%。 */
    public static final TerminalConfigValue<Double> COMBAT_EFFECT_EVASION_L1 =
            TerminalConfigValue.number("equipment", "combat_effect", "evasion_level1_chance", 0.10, 0.0, 1.0);
    /** 规避 II 档闪避概率。默认 0.20 = 20%。 */
    public static final TerminalConfigValue<Double> COMBAT_EFFECT_EVASION_L2 =
            TerminalConfigValue.number("equipment", "combat_effect", "evasion_level2_chance", 0.20, 0.0, 1.0);
    /** 规避 III 档闪避概率（封顶）。默认 0.30 = 30%。 */
    public static final TerminalConfigValue<Double> COMBAT_EFFECT_EVASION_L3 =
            TerminalConfigValue.number("equipment", "combat_effect", "evasion_level3_chance", 0.30, 0.0, 1.0);

    /** 经验加成基数：倍率 = 基数 + 档位 × 步进。默认 1.2。 */
    public static final TerminalConfigValue<Double> COMBAT_EFFECT_EXPERIENCE_BASE =
            TerminalConfigValue.number("equipment", "combat_effect", "experience_multiplier_base", 1.2, 0.0, 100.0);
    /** 经验加成每档步进。默认 0.2。 */
    public static final TerminalConfigValue<Double> COMBAT_EFFECT_EXPERIENCE_STEP =
            TerminalConfigValue.number("equipment", "combat_effect", "experience_multiplier_step", 0.2, 0.0, 100.0);

    /** 装填倍率基数：倍率 = 基数 − 档位 × 步进。默认 0.9。 */
    public static final TerminalConfigValue<Double> COMBAT_EFFECT_RELOAD_BASE =
            TerminalConfigValue.number("equipment", "combat_effect", "reload_multiplier_base", 0.9, 0.0, 1.0);
    /** 装填倍率每档步进。默认 0.1。 */
    public static final TerminalConfigValue<Double> COMBAT_EFFECT_RELOAD_STEP =
            TerminalConfigValue.number("equipment", "combat_effect", "reload_multiplier_step", 0.1, 0.0, 1.0);

    /** 燃烧 I 档伤害间隔（tick）。默认 60（3 秒）。 */
    public static final TerminalConfigValue<Integer> COMBAT_EFFECT_BURNING_INTERVAL_L1 =
            TerminalConfigValue.integer("equipment", "combat_effect", "burning_interval_level1", 60, 1, 1200);
    /** 燃烧 II 档伤害间隔（tick）。默认 30。 */
    public static final TerminalConfigValue<Integer> COMBAT_EFFECT_BURNING_INTERVAL_L2 =
            TerminalConfigValue.integer("equipment", "combat_effect", "burning_interval_level2", 30, 1, 1200);
    /** 燃烧 III 档伤害间隔（tick）。默认 20。 */
    public static final TerminalConfigValue<Integer> COMBAT_EFFECT_BURNING_INTERVAL_L3 =
            TerminalConfigValue.integer("equipment", "combat_effect", "burning_interval_level3", 20, 1, 1200);
    /** 燃烧 IV 档伤害间隔（tick，封顶）。默认 15。 */
    public static final TerminalConfigValue<Integer> COMBAT_EFFECT_BURNING_INTERVAL_L4 =
            TerminalConfigValue.integer("equipment", "combat_effect", "burning_interval_level4", 15, 1, 1200);

    // ===== 损管 / 治疗道具（2026-10-05 下沉）=====
    // 键：global.<target>.*。

    /** 损管冷却（tick，终端按秒显示）。默认 2400（120 秒）。
     *  <p>灭火器是另一个独立消耗品（触发效果不同），不共用本参数，见 {@link #FIRE_EXTINGUISHER_USE_COOLDOWN}。</p> */
    public static final TerminalConfigValue<Integer> DAMAGE_CONTROL_USE_COOLDOWN =
            TerminalConfigValue.integer("equipment", "damage_control", "use_cooldown", 2400, 0, 120000);

    /** 灭火器冷却（tick，终端按秒显示）。默认 2400（120 秒）。与损管拆开各自独立。 */
    public static final TerminalConfigValue<Integer> FIRE_EXTINGUISHER_USE_COOLDOWN =
            TerminalConfigValue.integer("equipment", "fire_extinguisher", "use_cooldown", 2400, 0, 120000);

    // ===== 防御锥（2026-10-05 下沉）=====

    /** 大凤的伞与欧根的舰盾共用的格挡半角（度）。默认 75，即 150 度对称锥。
     *  <p>单位是「度」而非 tick/速度，故 property 名不带 {@code _cooldown} / {@code speed} 等终端启发式后缀。</p> */
    public static final TerminalConfigValue<Double> DEFENSE_CONE_HALF_ANGLE_DEG =
            TerminalConfigValue.number("equipment", "defense_cone", "half_angle_deg", 75.0, 0.0, 180.0);

    /** 维修套件作用距离（格）。默认 5.0。 */
    public static final TerminalConfigValue<Double> REPAIR_KIT_RANGE =
            TerminalConfigValue.number("equipment", "repair_kit", "range", 5.0, 0.0, 64.0);
    /** 维修套件生命恢复持续（tick，持有时每 tick 刷新）。默认 60（3 秒）。 */
    public static final TerminalConfigValue<Integer> REPAIR_KIT_REGEN_DURATION =
            TerminalConfigValue.integer("equipment", "repair_kit", "regen_duration", 60, 1, 12000);
    /** 维修套件生命恢复等级（amplifier，0 = I 级，1 = II 级）。默认 1 = II 级。 */
    public static final TerminalConfigValue<Integer> REPAIR_KIT_REGEN_AMPLIFIER =
            TerminalConfigValue.integer("equipment", "repair_kit", "regen_amplifier", 1, 0, 9);

    /** 独角兽竖琴作用半径（格）。默认 16.0。 */
    public static final TerminalConfigValue<Double> UNICORN_HARP_RANGE =
            TerminalConfigValue.number("equipment", "unicorn_harp", "range", 16.0, 0.0, 64.0);
    /** 独角兽竖琴生命恢复持续（tick）。默认 200（10 秒）。 */
    public static final TerminalConfigValue<Integer> UNICORN_HARP_REGEN_DURATION =
            TerminalConfigValue.integer("equipment", "unicorn_harp", "regen_duration", 200, 1, 12000);
    /** 独角兽竖琴生命恢复等级（amplifier，0 = I 级）。默认 0。 */
    public static final TerminalConfigValue<Integer> UNICORN_HARP_REGEN_AMPLIFIER =
            TerminalConfigValue.integer("equipment", "unicorn_harp", "regen_amplifier", 0, 0, 9);
    /** 独角兽竖琴冷却（tick，终端按秒显示），防连点。默认 20（1 秒）。 */
    public static final TerminalConfigValue<Integer> UNICORN_HARP_USE_COOLDOWN =
            TerminalConfigValue.integer("equipment", "unicorn_harp", "use_cooldown", 20, 0, 12000);

    // ===== 村民交易经济（VillagerTradeHandler，2026-10-05 下沉）=====
    // 键：global.villager_trade.*，group=economy（终端归入「系统参数」分类）。
    // WHY：补货次数与价格倍率是策划调经济手感的核心旋钮，不应写死在交易表里。

    /** 绿宝石交易补货前最大次数。默认 12。 */
    public static final TerminalConfigValue<Integer> VILLAGER_EMERALD_USES =
            TerminalConfigValue.integer("economy", "villager_trade", "emerald_uses", 12, 1, 999);
    /** 回收（卖给村民换绿宝石）交易补货前最大次数。默认 16。 */
    public static final TerminalConfigValue<Integer> VILLAGER_RECYCLE_USES =
            TerminalConfigValue.integer("economy", "villager_trade", "recycle_uses", 16, 1, 999);
    /** 战利品交易补货前最大次数，设计上应低于绿宝石交易。默认 3。 */
    public static final TerminalConfigValue<Integer> VILLAGER_TROPHY_USES =
            TerminalConfigValue.integer("economy", "villager_trade", "trophy_uses", 3, 1, 999);
    /** 交易价格浮动倍率（传给 MerchantOffer）。默认 0.05。 */
    public static final TerminalConfigValue<Double> VILLAGER_PRICE_MULTIPLIER =
            TerminalConfigValue.number("economy", "villager_trade", "price_multiplier", 0.05, 0.0, 1.0);

    // ===== 深海敌人 AI（2026-10-05 下沉）=====
    // WHY：这些原先是 LowTierDestroyer / DeepOceanProjectile / DeepOceanSupply / AircraftLaunchGoal
    // 里的写死手感常量，策划需要反复调（用户规则：手感数值一律终端可调）。默认值全部 = 下沉前
    // 写死值，纯重构、行为等价。键：global.<target>.*，group=deep_ocean 使其归入终端「深海舰」分类。
    // 命名注意：*_ticks 为整数 tick 时长（终端原样显示 tick）；orbit_angular_speed 单位为弧度/tick；
    // surface_speed 单位为格/tick；speed/inaccuracy 复用既有的 global.shell.*（见 ModProjectilesConfig）。

    // ---- 低阶驱逐舰（target=low_tier_destroyer）----
    /** 侦测/开火距离（格）。默认 30.0。 */
    public static final TerminalConfigValue<Double> LOW_TIER_DESTROYER_DETECTION_RANGE =
            TerminalConfigValue.number("deep_ocean", "low_tier_destroyer_ai", "detection_range", 30.0, 1.0, 256.0);
    /** 环绕目标的偏好距离（格）。默认 15.0。 */
    public static final TerminalConfigValue<Double> LOW_TIER_DESTROYER_ORBIT_DISTANCE =
            TerminalConfigValue.number("deep_ocean", "low_tier_destroyer_ai", "orbit_distance", 15.0, 0.0, 256.0);
    /** 环绕角速度（弧度/tick）。默认 0.02。 */
    public static final TerminalConfigValue<Double> LOW_TIER_DESTROYER_ORBIT_ANGULAR_SPEED =
            TerminalConfigValue.number("deep_ocean", "low_tier_destroyer_ai", "orbit_angular_speed", 0.02, 0.0, 3.141592653589793);
    /** 水面水平移动速度（格/tick）。默认 0.12。 */
    public static final TerminalConfigValue<Double> LOW_TIER_DESTROYER_SURFACE_SPEED =
            TerminalConfigValue.number("deep_ocean", "low_tier_destroyer_ai", "surface_speed", 0.12, 0.0, 10.0);
    /** 开火间隔（tick）。默认 100（0.2 发/秒）。 */
    public static final TerminalConfigValue<Integer> LOW_TIER_DESTROYER_FIRE_INTERVAL_TICKS =
            TerminalConfigValue.integer("deep_ocean", "low_tier_destroyer_ai", "fire_interval_ticks", 100, 1, 12000);
    /** 炮弹伤害（小口径 HE）。默认 4.0。 */
    public static final TerminalConfigValue<Double> LOW_TIER_DESTROYER_SHELL_DAMAGE =
            TerminalConfigValue.number("deep_ocean", "low_tier_destroyer_ai", "shell_damage", 4.0, 0.0, 10000.0);
    /** 炮弹爆炸威力。默认 1.5。 */
    public static final TerminalConfigValue<Double> LOW_TIER_DESTROYER_EXPLOSION_POWER =
            TerminalConfigValue.number("deep_ocean", "low_tier_destroyer_ai", "explosion_power", 1.5, 0.0, 100.0);
    /**
     * 追踪弹触发阈值下限（发）：开火计数（shotsFired）达到 [min, max] 内随机值时打出一发追踪弹。
     * 默认 2。换算见 LowTierDestroyerEntity#rollTrackingShotThreshold：原写死 2 + nextInt(4) 实际取值
     * 2..5，故默认 (min,max)=(2,5) 时内部用 min + nextInt(max-min+1) 与改前逐字节等价。
     */
    public static final TerminalConfigValue<Integer> LOW_TIER_DESTROYER_TRACKING_SHOT_MIN =
            TerminalConfigValue.integer("deep_ocean", "low_tier_destroyer_ai", "tracking_shot_min", 2, 1, 12000);
    /** 追踪弹触发阈值上限（发）。默认 5。 */
    public static final TerminalConfigValue<Integer> LOW_TIER_DESTROYER_TRACKING_SHOT_MAX =
            TerminalConfigValue.integer("deep_ocean", "low_tier_destroyer_ai", "tracking_shot_max", 5, 1, 12000);
    /** 共享警戒半径（格）：把当前目标同步给此范围内尚未锁敌的同型驱逐舰。默认 40.0。 */
    public static final TerminalConfigValue<Double> LOW_TIER_DESTROYER_ALERT_RANGE =
            TerminalConfigValue.number("deep_ocean", "low_tier_destroyer_ai", "alert_range", 40.0, 0.0, 512.0);

    // ---- 深海炮弹（target=deep_ocean_projectile，DeepOceanProjectileEntity）----
    /** 兜底伤害（仅未由发射方指定时生效，如实体注册构造/NBT 缺省回填）。默认 5.0。 */
    public static final TerminalConfigValue<Double> DEEP_OCEAN_PROJECTILE_DAMAGE =
            TerminalConfigValue.number("deep_ocean", "deep_ocean_projectile", "damage", 5.0, 0.0, 10000.0);
    /** 兜底爆炸威力。默认 1.5。 */
    public static final TerminalConfigValue<Double> DEEP_OCEAN_PROJECTILE_EXPLOSION_POWER =
            TerminalConfigValue.number("deep_ocean", "deep_ocean_projectile", "explosion_power", 1.5, 0.0, 100.0);
    /** 近炸引信检测/起爆距离（格）。默认 3.0。 */
    public static final TerminalConfigValue<Double> DEEP_OCEAN_PROJECTILE_PROXIMITY_RANGE =
            TerminalConfigValue.number("deep_ocean", "deep_ocean_projectile", "proximity_range", 3.0, 0.0, 128.0);
    /** 近炸引信起爆前安全延迟（tick）。默认 5。 */
    public static final TerminalConfigValue<Integer> DEEP_OCEAN_PROJECTILE_ARM_TICKS =
            TerminalConfigValue.integer("deep_ocean", "deep_ocean_projectile", "arm_ticks", 5, 0, 12000);
    /** 抛物线弹重力（格/tick²）；DIRECT 弹不走重力。默认 0.05。 */
    public static final TerminalConfigValue<Double> DEEP_OCEAN_PROJECTILE_GRAVITY =
            TerminalConfigValue.number("deep_ocean", "deep_ocean_projectile", "gravity", 0.05, 0.0, 10.0);
    /** 最大存活时长（tick），到期自毁防泄漏。默认 200（10 秒）。 */
    public static final TerminalConfigValue<Integer> DEEP_OCEAN_PROJECTILE_LIFETIME_TICKS =
            TerminalConfigValue.integer("deep_ocean", "deep_ocean_projectile", "lifetime_ticks", 200, 1, 120000);

    // ---- 深海补给舰（target=deep_ocean_supply）----
    /** 支援附近友军的间隔（tick）。默认 80。 */
    public static final TerminalConfigValue<Integer> DEEP_OCEAN_SUPPLY_INTERVAL_TICKS =
            TerminalConfigValue.integer("deep_ocean", "deep_ocean_supply_ai", "support_interval_ticks", 80, 1, 12000);
    /** 支援作用半径（格）。默认 8.0。 */
    public static final TerminalConfigValue<Double> DEEP_OCEAN_SUPPLY_RADIUS =
            TerminalConfigValue.number("deep_ocean", "deep_ocean_supply_ai", "support_radius", 8.0, 0.0, 256.0);
    /** 每次支援的单体治疗量。默认 3.0。 */
    public static final TerminalConfigValue<Double> DEEP_OCEAN_SUPPLY_HEAL_AMOUNT =
            TerminalConfigValue.number("deep_ocean", "deep_ocean_supply_ai", "support_heal_amount", 3.0, 0.0, 10000.0);
    /** 每次支援最多治疗的友军数。默认 2。 */
    public static final TerminalConfigValue<Integer> DEEP_OCEAN_SUPPLY_TARGET_LIMIT =
            TerminalConfigValue.integer("deep_ocean", "deep_ocean_supply_ai", "support_target_limit", 2, 1, 64);

    // ---- NPC 航母放飞（target=npc_carrier，AircraftLaunchGoal）----
    /** 两次放飞之间的间隔（tick）。默认 300（15 秒）。 */
    public static final TerminalConfigValue<Integer> NPC_CARRIER_READY_INTERVAL_TICKS =
            TerminalConfigValue.integer("deep_ocean", "npc_carrier", "ready_interval_ticks", 300, 1, 120000);
    /** 首次放飞前的初始延迟（tick）。默认 100（5 秒）。 */
    public static final TerminalConfigValue<Integer> NPC_CARRIER_INITIAL_DELAY_TICKS =
            TerminalConfigValue.integer("deep_ocean", "npc_carrier", "initial_delay_ticks", 100, 0, 120000);

    // ===== 副本节奏（2026-10-05 下沉）=====
    // WHY：副本难度缩放与脚本时序原先是 DungeonScaling / DungeonConstants / BossAntiStuckArea
    // 里的写死平衡值，策划需要反复调（用户规则）。默认值全部 = 下沉前写死值，纯重构、行为等价。
    // 键：global.<target>.*，group=dungeon（终端「系统参数」分类）。
    // 注意：DungeonScaling 是纯逻辑类且有单测，读取终端参数后在无覆盖时会回落到基准值，
    // 因此单测默认行为与改前逐字节一致。

    // ---- 进本门槛（target=dungeon_capacity）----
    /**
     * 单实例同时在线人数上限 = <b>进本门槛</b>：书台满员判定、进本拦截、悬浮字幕 x/N 都读它。默认 4。
     *
     * <p>WHY 与缩放钳制拆成两个参数：本参数回答「能进来几个人」，是权威门槛（读取点
     * {@code DungeonEntryService}、{@code LecternPattern}）；缩放钳制
     * {@link #DUNGEON_SCALING_PLAYER_CAP} 只回答「怪按几个人增强度」。二者语义不同，
     * 调大进本门槛不会自动提高怪物强度，调大缩放钳制也不会放更多人进本。</p>
     */
    public static final TerminalConfigValue<Integer> DUNGEON_CAPACITY_MAX_PLAYERS =
            TerminalConfigValue.integer("dungeon", "dungeon_capacity", "max_players", 4, 1, 64);

    // ---- 难度缩放（target=dungeon_scaling）----
    /**
     * 缩放公式的<b>人数钳制上限</b>：只钳制 DungeonScaling 里 n 的取值。默认 4。
     *
     * <p>WHY 与进本门槛分开：本参数只影响「怪按几个人增强度」；「能进来几个人」由进本门槛
     * {@link #DUNGEON_CAPACITY_MAX_PLAYERS} 决定。钳制 ≤ 门槛时才有意义；两者不等时，
     * 超出钳制的人数能进副本但不增强度（例如门槛 8 / 钳制 4 → 8 人能进，强度按 4 人算）。</p>
     */
    public static final TerminalConfigValue<Integer> DUNGEON_SCALING_PLAYER_CAP =
            TerminalConfigValue.integer("dungeon", "dungeon_scaling", "scaling_player_cap", 4, 1, 64);
    /** 血量倍率 = 1 + 该值 × (人数 − 1)。默认 0.5。 */
    public static final TerminalConfigValue<Double> DUNGEON_HEALTH_SCALE_PER_PLAYER =
            TerminalConfigValue.number("dungeon", "dungeon_scaling", "health_scale_per_player", 0.5, 0.0, 100.0);
    /** 波数 = ceil(基础波数 × (1 + 该值 × (人数 − 1)))。默认 0.2。 */
    public static final TerminalConfigValue<Double> DUNGEON_WAVE_SCALE_PER_PLAYER =
            TerminalConfigValue.number("dungeon", "dungeon_scaling", "wave_scale_per_player", 0.2, 0.0, 100.0);
    /** 迷路的运输舰替换概率（第二章起每波）。默认 0.10。 */
    public static final TerminalConfigValue<Double> DUNGEON_LOST_TRANSPORT_CHANCE =
            TerminalConfigValue.number("dungeon", "dungeon_scaling", "lost_transport_chance", 0.10, 0.0, 1.0);

    // ---- 副本时序（target=dungeon_timing）----
    /** 战利品箱船自动消失时长（tick）。默认 6000（300 秒）。 */
    public static final TerminalConfigValue<Integer> DUNGEON_LOOT_SHIP_DESPAWN_TICKS =
            TerminalConfigValue.integer("dungeon", "dungeon_timing", "loot_ship_despawn_ticks", 6000, 1, 1000000);
    /** 回城卷轴使用冷却（tick，终端按秒显示）。默认 60（3 秒）。 */
    public static final TerminalConfigValue<Integer> DUNGEON_TOWN_SCROLL_COOLDOWN =
            TerminalConfigValue.integer("dungeon", "dungeon_timing", "town_scroll_cooldown", 60, 0, 12000);
    /** Boss 演出开场阶段超时（tick）：Boss 就位后经过该延时进入战斗。默认 60（3 秒）。 */
    public static final TerminalConfigValue<Integer> DUNGEON_BOSS_INTRO_PHASE_TIMEOUT_TICKS =
            TerminalConfigValue.integer("dungeon", "dungeon_timing", "boss_intro_phase_timeout_ticks", 60, 0, 120000);
    /** Boss 战安全超时（tick），自战斗开始计时。默认 12000（10 分钟）。 */
    public static final TerminalConfigValue<Integer> DUNGEON_BOSS_INTRO_BATTLE_TIMEOUT_TICKS =
            TerminalConfigValue.integer("dungeon", "dungeon_timing", "boss_intro_battle_timeout_ticks", 12000, 0, 10000000);
    /** Boss 击败后到退场演出的延时（tick）。默认 60（3 秒）。 */
    public static final TerminalConfigValue<Integer> DUNGEON_BOSS_INTRO_DEFEAT_DELAY_TICKS =
            TerminalConfigValue.integer("dungeon", "dungeon_timing", "boss_intro_defeat_delay_ticks", 60, 0, 120000);
    /** 火炮登场关卡「拾取阶段」超时（tick）。默认 6000（5 分钟）。 */
    public static final TerminalConfigValue<Integer> DUNGEON_ARTILLERY_INTRO_LOOTING_TIMEOUT_TICKS =
            TerminalConfigValue.integer("dungeon", "dungeon_timing", "artillery_intro_looting_timeout_ticks", 6000, 1, 10000000);
    /** 火炮登场关卡：玩家离补给箱超过该距离（格）即提前进入战斗。默认 20.0。 */
    public static final TerminalConfigValue<Double> DUNGEON_ARTILLERY_INTRO_LEAVE_DISTANCE =
            TerminalConfigValue.number("dungeon", "dungeon_timing", "artillery_intro_leave_distance", 20.0, 0.0, 512.0);

    // ---- Boss 防卡死区（target=boss_anti_stuck）----
    /** 环境破坏执行间隔（tick）。默认 100（5 秒）。最小值 2，因为内部还会做 /2 取子周期。 */
    public static final TerminalConfigValue<Integer> BOSS_ANTI_STUCK_TICK_INTERVAL =
            TerminalConfigValue.integer("dungeon", "boss_anti_stuck", "tick_interval", 100, 2, 120000);
    /** 环境破坏影响半径（格）。默认 24。 */
    public static final TerminalConfigValue<Integer> BOSS_ANTI_STUCK_RADIUS =
            TerminalConfigValue.integer("dungeon", "boss_anti_stuck", "radius", 24, 1, 256);
}
