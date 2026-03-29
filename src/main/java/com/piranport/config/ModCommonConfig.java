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
     * When true, right-clicking the ship core while not transformed opens the ship core GUI.
     * Default false — GUI is hidden; right-click only fires weapons or transforms.
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

    public static final ModConfigSpec SPEC = BUILDER.build();
}
