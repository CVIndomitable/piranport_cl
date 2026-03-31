package com.piranport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModCommonConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /**
     * When true, fighters (战斗机) consume ammo (remainingAmmo counter decrements
     * each shot, aircraft returns when depleted).
     * Default false — fighters have unlimited bullets and only consume fuel.
     */
    public static final ModConfigSpec.BooleanValue FIGHTER_AMMO_ENABLED =
            BUILDER
                    .comment(
                            "Enable ammo consumption for fighter aircraft (战斗机).",
                            "Default: false (fighters have unlimited bullets, only fuel is consumed).",
                            "Set to true to enable finite bullet count per sortie. (战斗机子弹消耗，默认关闭)")
                    .define("fighterAmmoEnabled", false);

    /**
     * When true (auto mode): cannons/torpedoes scan the player's inventory for ammo on each shot;
     * aircraft fuel is automatically consumed from the ammo slots on transformation.
     * When false (manual mode): all weapons require manual loading —
     * right-click ammo onto the weapon item in the inventory to load one round at a time.
     * Default false — manual reload mode.
     */
    public static final ModConfigSpec.BooleanValue AUTO_RESUPPLY_ENABLED =
            BUILDER
                    .comment(
                            "Enable automatic ammo resupply for all weapons (自动装填模式).",
                            "Default: false (manual reload — right-click ammo onto weapon slot to load, 手动装填).",
                            "Set to true to auto-consume ammo from inventory on each shot. (自动从背包消耗弹药，默认关闭)")
                    .define("autoResupplyEnabled", false);

    /**
     * When true, loading aviation fuel onto an aircraft applies the Flammable (易燃易爆) debuff.
     * Default false.
     */
    public static final ModConfigSpec.BooleanValue FLAMMABLE_EFFECT_ENABLED =
            BUILDER
                    .comment(
                            "When true, loading aviation fuel onto an aircraft applies the Flammable (易燃易爆) debuff.",
                            "Default: false.",
                            "Set to true to enable the fire-hazard penalty for fueled aircraft. (飞机装燃料触发易燃易爆，默认关闭)")
                    .define("flammableEffectEnabled", false);

    public static final ModConfigSpec.BooleanValue SHIP_CORE_GUI_ENABLED =
            BUILDER
                    .comment(
                            "Enable the Ship Core GUI (舰装核心界面).",
                            "Default: false (right-click does not open GUI; only fires/transforms).",
                            "Set to true to allow opening the ship core inventory screen. (舰装核心GUI开关，默认关闭)")
                    .define("shipCoreGuiEnabled", false);

    /**
     * When true, picking up a weapon item (gun/torpedo/aircraft/armor plate) sends it directly
     * to main inventory slots (9–35) instead of the hotbar.
     * Falls back to vanilla behavior if main inventory is full.
     */
    public static final ModConfigSpec.BooleanValue WEAPON_PICKUP_TO_INVENTORY =
            BUILDER
                    .comment(
                            "Send picked-up weapon items to main inventory instead of hotbar (武器拾取入背包).",
                            "Default: false (vanilla behavior — hotbar first).",
                            "Set to true to redirect guns/torpedoes/aircraft/armor plates to slots 9-35 on pickup. (武器拾取自动进入背包而非快捷栏，默认关闭)")
                    .define("weaponPickupToInventory", false);

    /**
     * When true (hotbar mode): only items in the hotbar (slots 0–8) count toward load and take effect.
     * When false (full inventory mode): all items across the entire inventory count toward load.
     * Only applies when shipCoreGuiEnabled=false (inventory mode).
     */
    public static final ModConfigSpec.BooleanValue HOTBAR_ONLY_LOAD =
            BUILDER
                    .comment(
                            "Hotbar-only load mode (快捷物品栏负重模式).",
                            "Default: false (all inventory items contribute to load, 全背包负重).",
                            "Set to true to only count items in hotbar slots 0-8 for load and effect. (仅快捷物品栏的装备计算负重和生效)")
                    .define("hotbarOnlyLoad", false);

    /**
     * When true, salt blocks generate naturally in river biomes.
     * Default false — salt is only obtainable via smelting water buckets or crafting.
     */
    public static final ModConfigSpec.BooleanValue SALT_GENERATION_ENABLED =
            BUILDER
                    .comment(
                            "Enable natural salt block generation in river biomes (河床盐矿生成).",
                            "Default: false (salt blocks do not generate naturally, 盐矿不自然生成).",
                            "Set to true to enable salt disk features in rivers. (开启后河流底部会生成盐块)")
                    .define("saltGenerationEnabled", false);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
