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
