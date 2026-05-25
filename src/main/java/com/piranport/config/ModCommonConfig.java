package com.piranport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModCommonConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ===== Equipment & Resupply (装备与补给) =====

    public static final ModConfigSpec.BooleanValue FIGHTER_AMMO_ENABLED;
    public static final ModConfigSpec.BooleanValue AUTO_RESUPPLY_ENABLED;
    public static final ModConfigSpec.BooleanValue FLAMMABLE_EFFECT_ENABLED;

    // ===== GUI & Inventory (界面与背包) =====

    public static final ModConfigSpec.ConfigValue<String> SHIP_CORE_SLOT_MODE;
    public static final ModConfigSpec.BooleanValue WEAPON_PICKUP_TO_INVENTORY;

    // ===== Movement (移动) =====

    public static final ModConfigSpec.DoubleValue WATER_WALKING_ACCELERATION;
    public static final ModConfigSpec.DoubleValue WATER_WALKING_DECELERATION;

    // ===== Combat (战斗) =====

    public static final ModConfigSpec.BooleanValue FRIENDLY_FIRE_ENABLED;
    public static final ModConfigSpec.BooleanValue EXPLOSION_BLOCK_DAMAGE;

    // ===== World Generation (世界生成) =====

    public static final ModConfigSpec.BooleanValue SALT_GENERATION_ENABLED;

    // ===== Game Mode (游戏模式) =====

    public static final ModConfigSpec.BooleanValue GIVE_GUIDEBOOK_ON_FIRST_JOIN;

    static {
        BUILDER.push("equipment");
        FIGHTER_AMMO_ENABLED = BUILDER
                .comment(
                        "Enable ammo consumption for fighter aircraft (战斗机).",
                        "Default: false (fighters have unlimited bullets, only fuel is consumed).",
                        "Set to true to enable finite bullet count per sortie. (战斗机子弹消耗，默认关闭)")
                .define("fighterAmmoEnabled", false);

        AUTO_RESUPPLY_ENABLED = BUILDER
                .comment(
                        "Enable automatic ammo resupply (自动装填模式).",
                        "Default: false (manual reload for torpedoes/aircraft).",
                        "Set to true to auto-consume ammo from inventory on each shot.",
                        "Note: cannons always auto-resupply since Phase 4.")
                .define("autoResupplyEnabled", false);

        FLAMMABLE_EFFECT_ENABLED = BUILDER
                .comment(
                        "When true, loading aviation fuel onto an aircraft applies the Flammable (易燃易爆) debuff.",
                        "Default: false.",
                        "Set to true to enable the fire-hazard penalty for fueled aircraft. (飞机装燃料触发易燃易爆，默认关闭)")
                .define("flammableEffectEnabled", false);
        BUILDER.pop();

        BUILDER.push("gui");
        SHIP_CORE_SLOT_MODE = BUILDER
                .comment(
                        "Ship Core equipment slot mode (舰装核心装备槽位模式).",
                        "Options: 'offhand' or 'chest'.",
                        "  - offhand: Ship core must be held in offhand (default, current behavior).",
                        "  - chest: Ship core must be equipped in chest armor slot.",
                        "Default: 'offhand' (副手模式，当前行为).",
                        "Set to 'chest' to enable chestplate mode (设为'chest'启用胸甲模式).")
                .define("shipCoreSlotMode", "offhand");

        WEAPON_PICKUP_TO_INVENTORY = BUILDER
                .comment(
                        "Send picked-up weapon items to main inventory instead of hotbar (武器拾取入背包).",
                        "Default: false (vanilla behavior — hotbar first).",
                        "Set to true to redirect guns/torpedoes/aircraft/armor plates to slots 9-35 on pickup. (武器拾取自动进入背包而非快捷栏，默认关闭)")
                .define("weaponPickupToInventory", false);
        BUILDER.pop();

        BUILDER.push("movement");
        WATER_WALKING_ACCELERATION = BUILDER
                .comment(
                        "Horizontal acceleration boost when walking on water surface (水面行走水平加速度补偿).",
                        "Default: 0.03. Higher values = faster acceleration, lower = more sliding.",
                        "Range: 0.0 (disabled) to 0.05 (very responsive).",
                        "默认0.03。数值越高加速越快，越低越滑。范围0.0（禁用）到0.05（非常灵敏）")
                .defineInRange("waterWalkingAcceleration", 0.03, 0.0, 0.05);

        WATER_WALKING_DECELERATION = BUILDER
                .comment(
                        "Deceleration factor when no input on water surface (水面行走无输入时减速系数).",
                        "Default: 0.85. Each tick retains this fraction of velocity (每tick保留此比例的速度).",
                        "Higher values = slower deceleration (more sliding), lower = faster stop.",
                        "Range: 0.5 (quick stop) to 0.95 (long slide).",
                        "默认0.85。数值越高减速越慢（更滑），越低停得越快。范围0.5（快速停止）到0.95（长距离滑行）")
                .defineInRange("waterWalkingDeceleration", 0.85, 0.5, 0.95);
        BUILDER.pop();

        BUILDER.push("combat");
        FRIENDLY_FIRE_ENABLED = BUILDER
                .comment(
                        "Enable friendly fire between players (友军伤害开关).",
                        "Default: true (player projectiles can hit other players, 默认开启友伤).",
                        "Set to false to prevent player-fired projectiles from hitting other players. (关闭后玩家抛射物不会命中其他玩家)")
                .define("friendlyFireEnabled", true);

        EXPLOSION_BLOCK_DAMAGE = BUILDER
                .comment(
                        "Allow cannon/torpedo explosions to destroy blocks (炮弹/鱼雷爆炸破坏方块).",
                        "Default: true (explosions break blocks like TNT, 爆炸会破坏地形).",
                        "Set to false to disable block destruction from projectile explosions. (关闭后爆炸不破坏方块)")
                .define("explosionBlockDamage", true);
        BUILDER.pop();

        BUILDER.push("worldgen");
        SALT_GENERATION_ENABLED = BUILDER
                .comment(
                        "Enable natural salt block generation in river biomes (河床盐矿生成).",
                        "Default: false (salt blocks do not generate naturally, 盐矿不自然生成).",
                        "Set to true to enable salt disk features in rivers. (开启后河流底部会生成盐块)")
                .define("saltGenerationEnabled", false);
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

    /**
     * Helper: returns true if the flammable effect should be active.
     */
    public static boolean isFlammableEffectActive() {
        return FLAMMABLE_EFFECT_ENABLED.get();
    }
}
