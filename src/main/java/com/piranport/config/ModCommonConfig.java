package com.piranport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModCommonConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ===== Equipment & Resupply (装备与补给) =====

    public static final ModConfigSpec.BooleanValue FIGHTER_AMMO_ENABLED =
            BUILDER
                    .comment(
                            "Enable ammo consumption for fighter aircraft (战斗机).",
                            "Default: false (fighters have unlimited bullets, only fuel is consumed).",
                            "Set to true to enable finite bullet count per sortie. (战斗机子弹消耗，默认关闭)")
                    .define("equipment.fighterAmmoEnabled", false);

    public static final ModConfigSpec.BooleanValue AUTO_RESUPPLY_ENABLED =
            BUILDER
                    .comment(
                            "Enable automatic ammo resupply for all weapons (自动装填模式).",
                            "Default: false (manual reload — right-click ammo onto weapon slot to load, 手动装填).",
                            "Set to true to auto-consume ammo from inventory on each shot. (自动从背包消耗弹药，默认关闭)")
                    .define("equipment.autoResupplyEnabled", false);

    public static final ModConfigSpec.BooleanValue FLAMMABLE_EFFECT_ENABLED =
            BUILDER
                    .comment(
                            "When true, loading aviation fuel onto an aircraft applies the Flammable (易燃易爆) debuff.",
                            "Default: false.",
                            "Set to true to enable the fire-hazard penalty for fueled aircraft. (飞机装燃料触发易燃易爆，默认关闭)")
                    .define("equipment.flammableEffectEnabled", false);

    // ===== GUI & Inventory (界面与背包) =====

    public static final ModConfigSpec.BooleanValue SHIP_CORE_GUI_ENABLED =
            BUILDER
                    .comment(
                            "Enable the Ship Core GUI (舰装核心界面).",
                            "Default: false (right-click does not open GUI; only fires/transforms).",
                            "Set to true to allow opening the ship core inventory screen. (舰装核心GUI开关，默认关闭)")
                    .define("gui.shipCoreGuiEnabled", false);

    public static final ModConfigSpec.BooleanValue WEAPON_PICKUP_TO_INVENTORY =
            BUILDER
                    .comment(
                            "Send picked-up weapon items to main inventory instead of hotbar (武器拾取入背包).",
                            "Default: false (vanilla behavior — hotbar first).",
                            "Set to true to redirect guns/torpedoes/aircraft/armor plates to slots 9-35 on pickup. (武器拾取自动进入背包而非快捷栏，默认关闭)")
                    .define("gui.weaponPickupToInventory", false);

    // ===== Movement (移动) =====

    public static final ModConfigSpec.DoubleValue WATER_WALKING_ACCELERATION =
            BUILDER
                    .comment(
                            "Horizontal acceleration boost when walking on water surface (水面行走水平加速度补偿).",
                            "Default: 0.03. Higher values = faster acceleration, lower = more sliding.",
                            "Range: 0.0 (disabled) to 0.05 (very responsive).",
                            "默认0.03。数值越高加速越快，越低越滑。范围0.0（禁用）到0.05（非常灵敏）")
                    .defineInRange("movement.waterWalkingAcceleration", 0.03, 0.0, 0.05);

    // ===== Combat (战斗) =====

    public static final ModConfigSpec.BooleanValue FRIENDLY_FIRE_ENABLED =
            BUILDER
                    .comment(
                            "Enable friendly fire between players (友军伤害开关).",
                            "Default: true (player projectiles can hit other players, 默认开启友伤).",
                            "Set to false to prevent player-fired projectiles from hitting other players. (关闭后玩家抛射物不会命中其他玩家)")
                    .define("combat.friendlyFireEnabled", true);

    public static final ModConfigSpec.BooleanValue EXPLOSION_BLOCK_DAMAGE =
            BUILDER
                    .comment(
                            "Allow cannon/torpedo explosions to destroy blocks (炮弹/鱼雷爆炸破坏方块).",
                            "Default: true (explosions break blocks like TNT, 爆炸会破坏地形).",
                            "Set to false to disable block destruction from projectile explosions. (关闭后爆炸不破坏方块)")
                    .define("combat.explosionBlockDamage", true);

    // ===== World Generation (世界生成) =====

    public static final ModConfigSpec.BooleanValue SALT_GENERATION_ENABLED =
            BUILDER
                    .comment(
                            "Enable natural salt block generation in river biomes (河床盐矿生成).",
                            "Default: false (salt blocks do not generate naturally, 盐矿不自然生成).",
                            "Set to true to enable salt disk features in rivers. (开启后河流底部会生成盐块)")
                    .define("worldgen.saltGenerationEnabled", false);

    // ===== Ruins & World Structures (遗迹与世界结构) =====


    // ===== Game Mode (游戏模式) =====

    public static final ModConfigSpec.BooleanValue KANSEN_MODE =
            BUILDER
                    .comment(
                            "Enable Kansen Mode (舰R模式).",
                            "Default: true. When enabled, forces no-GUI mode (overrides shipCoreGuiEnabled).",
                            "舰R模式，默认开启。开启时强制使用无GUI模式。")
                    .define("gameMode.kansenMode", true);

    public static final ModConfigSpec.BooleanValue SLAV_PRISON_MODE =
            BUILDER
                    .comment(
                            "Enable Slav Prison Mode (斯拉夫大牢模式).",
                            "Default: false. When enabled, also activates flammableEffectEnabled.",
                            "斯拉夫大牢模式，默认关闭。开启时同时启用易燃易爆效果。")
                    .define("gameMode.slavPrisonMode", false);

    public static final ModConfigSpec.BooleanValue GIVE_GUIDEBOOK_ON_FIRST_JOIN =
            BUILDER
                    .comment(
                            "Give players a Guidebook when they first join the world (首次进入世界赠送教程书).",
                            "Default: true.",
                            "开启后，玩家首次加入时自动获得一本航行手册。")
                    .define("gameMode.giveGuidebookOnFirstJoin", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    /**
     * Helper: returns true only if the Ship Core GUI is truly enabled.
     * Kansen Mode overrides GUI to disabled.
     */
    public static boolean isShipCoreGuiEnabled() {
        return SHIP_CORE_GUI_ENABLED.get() && !KANSEN_MODE.get();
    }

    /**
     * Helper: returns true if the flammable effect should be active.
     * Slav Prison Mode forces this on.
     */
    public static boolean isFlammableEffectActive() {
        return FLAMMABLE_EFFECT_ENABLED.get() || SLAV_PRISON_MODE.get();
    }
}
