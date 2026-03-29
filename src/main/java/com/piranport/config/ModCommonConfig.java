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
     * When true, aircraft automatically resupply ammo and fuel from the owner's
     * ship core ammo slots when depleted, instead of returning to carrier.
     * Default false — aircraft return when ammo/fuel is exhausted.
     */
    public static final ModConfigSpec.BooleanValue AUTO_RESUPPLY_ENABLED =
            BUILDER
                    .comment(
                            "Enable automatic ammo and fuel resupply for aircraft.",
                            "Default: false (aircraft return to carrier when ammo/fuel is exhausted).",
                            "Set to true to automatically draw from the ship core's ammo slots mid-sortie. (飞机自动补给弹药和燃油，默认关闭)")
                    .define("autoResupplyEnabled", false);

    /**
     * When true, right-clicking the ship core while not transformed opens the ship core GUI.
     * Default false — GUI is hidden; right-click only fires weapons or transforms.
     */
    public static final ModConfigSpec.BooleanValue SHIP_CORE_GUI_ENABLED =
            BUILDER
                    .comment(
                            "Enable the Ship Core GUI (舰装核心界面).",
                            "Default: false (right-click does not open GUI; only fires/transforms).",
                            "Set to true to allow opening the ship core inventory screen. (舰装核心GUI开关，默认关闭)")
                    .define("shipCoreGuiEnabled", false);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
