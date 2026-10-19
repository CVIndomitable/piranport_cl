package com.piranport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModCommonConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ===== Equipment & Resupply (装备与补给) =====


    // ===== Inventory (背包) =====


    // ===== Movement (移动) =====

    public static final TerminalConfigValue<Double> WATER_WALKING_ACCELERATION =
            TerminalConfigValue.number("movement", "movement", "water_walking_acceleration", 0.03, 0.0, 0.05);
    public static final TerminalConfigValue<Double> WATER_WALKING_DECELERATION =
            TerminalConfigValue.number("movement", "movement", "water_walking_deceleration", 0.85, 0.5, 0.95);
    public static final TerminalConfigValue<Double> WATER_SURFACE_BUOYANCY =
            TerminalConfigValue.number("movement", "movement", "water_surface_buoyancy", 0.5, 0.3, 1.0);

    // ===== Combat (战斗) =====

    /** 护航节点：深海敌人在此距离（格）内有玩家时仇恨转给玩家。默认 12 为待策划确认的临时值。 */
    public static final TerminalConfigValue<Double> ESCORT_PLAYER_AGGRO_RANGE =
            TerminalConfigValue.number("escort", "escort", "player_aggro_range", 12.0, 0.0, 64.0);

    /** 护航节点：路径波次刷在运输船前方这么远（格，沿航线）。默认 48 为待策划确认的临时值。 */
    public static final TerminalConfigValue<Double> ESCORT_WAVE_AHEAD_DISTANCE =
            TerminalConfigValue.number("escort", "escort", "wave_ahead_distance", 48.0, 0.0, 256.0);

    /** 护航节点：侧翼波次离航线的横向距离（格）。默认 60 为待策划确认的临时值。 */
    public static final TerminalConfigValue<Double> ESCORT_WAVE_FLANK_OFFSET =
            TerminalConfigValue.number("escort", "escort", "wave_flank_offset", 60.0, 0.0, 256.0);

    // ===== Weather (天气，数值为临时默认，待策划定稿；规则见《副本/27》) =====

    /** 大雾：怪物响应距离乘数。临时默认 0.6。 */
    public static final TerminalConfigValue<Double> WEATHER_FOG_FOLLOW_RANGE_MULT =
            TerminalConfigValue.number("weather", "weather", "fog_follow_range_mult", 0.6, 0.0, 4.0);

    /** 烈日：怪物响应距离乘数。临时默认 1.4。 */
    public static final TerminalConfigValue<Double> WEATHER_HOT_SUN_FOLLOW_RANGE_MULT =
            TerminalConfigValue.number("weather", "weather", "hot_sun_follow_range_mult", 1.4, 0.0, 4.0);

    /** 雷雨：单次闪电对玩家的伤害。临时默认 4。 */
    public static final TerminalConfigValue<Double> WEATHER_STORM_LIGHTNING_DAMAGE =
            TerminalConfigValue.number("weather", "weather", "storm_lightning_damage", 4.0, 0.0, 40.0);

    /** 雷雨：每隔多少 tick 落一轮闪电。临时默认 200。 */
    public static final TerminalConfigValue<Integer> WEATHER_STORM_LIGHTNING_PERIOD_TICKS =
            TerminalConfigValue.integer("weather", "weather", "storm_lightning_period_ticks", 200, 1, 72000);

    /** 雷雨：每轮闪电对每名在场玩家的命中概率为 1/N。临时默认 3。 */
    public static final TerminalConfigValue<Integer> WEATHER_STORM_LIGHTNING_ONE_IN =
            TerminalConfigValue.integer("weather", "weather", "storm_lightning_one_in", 3, 1, 100);

    public static final ModConfigSpec.BooleanValue EXPLOSION_BLOCK_DAMAGE;

    // ===== World Generation (世界生成) =====


    // ===== Game Mode (游戏模式) =====

    public static final ModConfigSpec.BooleanValue GIVE_GUIDEBOOK_ON_FIRST_JOIN;

    static {
        BUILDER.push("equipment");
        BUILDER.pop();

        BUILDER.push("inventory");
        BUILDER.pop();

        BUILDER.push("combat");
        EXPLOSION_BLOCK_DAMAGE = BUILDER
                .comment(
                        "Allow cannon/torpedo explosions to destroy blocks (炮弹/鱼雷爆炸破坏方块).",
                        "Default: true (explosions break blocks like TNT, 爆炸会破坏地形).",
                        "Set to false to disable block destruction from projectile explosions. (关闭后爆炸不破坏方块)")
                .define("explosionBlockDamage", true);
        BUILDER.pop();

        BUILDER.push("worldgen");
        BUILDER.pop();

        BUILDER.push("gameMode");
        GIVE_GUIDEBOOK_ON_FIRST_JOIN = BUILDER
                .comment(
                        "Give players a Guidebook when they first join the world (首次进入世界赠送教程书).",
                        "Default: true.",
                        "开启后，玩家首次加入时自动获得一本航行手册。")
                .define("giveGuidebookOnFirstJoin", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
