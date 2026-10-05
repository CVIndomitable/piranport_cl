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

    public record CIWSConfig(TerminalConfigValue<Double> range, TerminalConfigValue<Integer> interval,
                             TerminalConfigValue<Integer> barrels, TerminalConfigValue<Integer> weight) {}

    private static CIWSConfig ciws(String caliber) {
        String target = "ciws_" + caliber;
        return new CIWSConfig(
                TerminalConfigValue.number("equipment", target, "range", 16.0, 1.0, 128.0),
                TerminalConfigValue.integer("equipment", target, "interval", 20, 1, 1200),
                TerminalConfigValue.integer("equipment", target, "barrels", 1, 1, 16),
                TerminalConfigValue.integer("equipment", target, "weight", 0, 0, 112));
    }

    public static final CIWSConfig CIWS_20MM = ciws("20mm");
    public static final CIWSConfig CIWS_40MM = ciws("40mm");
    public static final CIWSConfig CIWS_76MM = ciws("76mm");
}
