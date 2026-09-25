package com.piranport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModCommonConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ===== Equipment & Resupply (装备与补给) =====


    // ===== Inventory (背包) =====


    // ===== Movement (移动) =====

    public static final ModConfigSpec.DoubleValue WATER_WALKING_ACCELERATION;
    public static final ModConfigSpec.DoubleValue WATER_WALKING_DECELERATION;
    public static final ModConfigSpec.DoubleValue WATER_SURFACE_BUOYANCY;

    // ===== Combat (战斗) =====

    public static final ModConfigSpec.BooleanValue EXPLOSION_BLOCK_DAMAGE;

    // ===== World Generation (世界生成) =====


    // ===== Game Mode (游戏模式) =====

    public static final ModConfigSpec.BooleanValue GIVE_GUIDEBOOK_ON_FIRST_JOIN;

    static {
        BUILDER.push("equipment");
        BUILDER.pop();

        BUILDER.push("inventory");
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
