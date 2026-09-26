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
