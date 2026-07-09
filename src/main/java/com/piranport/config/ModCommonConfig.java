package com.piranport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModCommonConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ===== Equipment & Resupply (装备与补给) =====

    public static final ModConfigSpec.BooleanValue FIGHTER_AMMO_ENABLED;
    public static final ModConfigSpec.BooleanValue AUTO_RESUPPLY_ENABLED;

    // ===== Inventory (背包) =====

    public static final ModConfigSpec.ConfigValue<String> SHIP_CORE_SLOT_MODE;
    public static final ModConfigSpec.BooleanValue WEAPON_PICKUP_TO_INVENTORY;

    // ===== Movement (移动) =====

    public static final ModConfigSpec.DoubleValue WATER_WALKING_ACCELERATION;
    public static final ModConfigSpec.DoubleValue WATER_WALKING_DECELERATION;
    public static final ModConfigSpec.DoubleValue WATER_SURFACE_BUOYANCY;

    // ===== Combat (战斗) =====

    public static final ModConfigSpec.BooleanValue FRIENDLY_FIRE_ENABLED;
    public static final ModConfigSpec.BooleanValue EXPLOSION_BLOCK_DAMAGE;

    // ===== World Generation (世界生成) =====

    public static final ModConfigSpec.BooleanValue SEASONAL_LEAF_COLOR_ENABLED;

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
        BUILDER.pop();

        BUILDER.push("inventory");
        SHIP_CORE_SLOT_MODE = BUILDER
                .comment(
                        "Ship Core equipment slot mode (舰装核心装备槽位模式).",
                        "Options: 'offhand' or 'helmet'.",
                        "  - offhand: Ship core must be held in offhand (default, current behavior).",
                        "  - helmet: Ship core must be equipped in helmet armor slot.",
                        "Default: 'offhand' (副手模式，当前行为).",
                        "Set to 'helmet' to enable helmet mode (设为'helmet'启用头盔模式).",
                        "Note: 'chest' is deprecated and will be auto-migrated to 'helmet' (注意：'chest'已弃用，将自动迁移到'helmet').")
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

        WATER_SURFACE_BUOYANCY = BUILDER
                .comment(
                        "Upward force when transformed player is underwater (变身玩家水下上浮力).",
                        "Default: 0.5. Higher values = faster surfacing.",
                        "Range: 0.3 (gentle) to 1.0 (very strong).",
                        "默认0.5。数值越高上浮越快。范围0.3（温和）到1.0（非常强）")
                .defineInRange("waterSurfaceBuoyancy", 0.5, 0.3, 1.0);
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
        SEASONAL_LEAF_COLOR_ENABLED = BUILDER
                .comment(
                        "Enable seasonal color changes for Piran Port tree leaves (树叶季节变色).",
                        "Default: false. When enabled, leaf blocks update by random tick and switch season every 90 Minecraft days.",
                        "默认关闭。开启后树叶通过随机刻按每 90 个 Minecraft 日切换春夏秋冬状态。")
                .define("seasonalLeafColorEnabled", false);
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
