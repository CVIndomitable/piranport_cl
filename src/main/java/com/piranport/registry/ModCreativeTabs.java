package com.piranport.registry;

import com.piranport.PiranPort;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PiranPort.MOD_ID);

    // ===== 核心 — 舰装核心 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CORE_TAB =
            CREATIVE_TABS.register("core_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.core"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.MEDIUM_SHIP_CORE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SMALL_SHIP_CORE.get());
                        output.accept(ModItems.MEDIUM_SHIP_CORE.get());
                        output.accept(ModItems.LARGE_SHIP_CORE.get());
                        output.accept(ModItems.SUBMARINE_CORE.get());
                    }).build());

    // ===== 炮雷 — 火炮 / 鱼雷发射器 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CANNON_TORPEDO_TAB =
            CREATIVE_TABS.register("cannon_torpedo_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.cannon_torpedo"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.MEDIUM_GUN.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Guns
                        output.accept(ModItems.SMALL_GUN.get());
                        output.accept(ModItems.MEDIUM_GUN.get());
                        output.accept(ModItems.LARGE_GUN.get());
                        // Torpedo Launchers
                        output.accept(ModItems.TWIN_TORPEDO_LAUNCHER.get());
                        output.accept(ModItems.TRIPLE_TORPEDO_LAUNCHER.get());
                        output.accept(ModItems.QUAD_TORPEDO_LAUNCHER.get());
                        output.accept(ModItems.DEPTH_CHARGE_LAUNCHER.get());
                        output.accept(ModItems.DEPTH_CHARGE_LAUNCHER_IMPROVED.get());
                        output.accept(ModItems.DEPTH_CHARGE_LAUNCHER_ADVANCED.get());
                    }).build());

    // ===== 航空 — 飞机编队 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AVIATION_TAB =
            CREATIVE_TABS.register("aviation_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.aviation"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.FIGHTER_SQUADRON.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Generic squadrons
                        output.accept(ModItems.FIGHTER_SQUADRON.get());
                        output.accept(ModItems.DIVE_BOMBER_SQUADRON.get());

                        output.accept(ModItems.XTB2D.get());
                        output.accept(ModItems.RECON_SQUADRON.get());
                        // Torpedo bombers
                        output.accept(ModItems.SWORDFISH_TORPEDO.get());
                        output.accept(ModItems.SWORDFISH_ASW.get());
                        output.accept(ModItems.TBF_TORPEDO.get());
                        output.accept(ModItems.TBF_ASW.get());
                        output.accept(ModItems.TENZAN_TORPEDO.get());
                        output.accept(ModItems.TYPE97_TORPEDO.get());
                        output.accept(ModItems.SKY_PIRATE_TORPEDO.get());
                        // Dive bombers
                        output.accept(ModItems.PETREL_BOMBER.get());
                        output.accept(ModItems.TYPE99_DIVE_BOMBER.get());
                        output.accept(ModItems.SBD_DAUNTLESS.get());
                        output.accept(ModItems.FIREFLY_AS_MK5.get());
                        output.accept(ModItems.SUISEI_BOMBER.get());
                        // Level bombers
                        output.accept(ModItems.SEIUN_BOMBER.get());
                        output.accept(ModItems.B25_BOMBER.get());
                        output.accept(ModItems.XA2J_BOMBER.get());
                        // Fighters
                        output.accept(ModItems.F6F_HELLCAT_ROCKET.get());
                        output.accept(ModItems.SEAFIRE.get());
                        output.accept(ModItems.ZERO_MODEL52.get());
                        output.accept(ModItems.F4F_WILDCAT.get());
                        output.accept(ModItems.F4U_CORSAIR_ICE.get());
                        output.accept(ModItems.F4U_CORSAIR.get());
                        output.accept(ModItems.F2H_BANSHEE.get());
                        // Recon
                        output.accept(ModItems.TYPE0_RECON.get());
                        output.accept(ModItems.C1_RECON.get());
                        output.accept(ModItems.SAIUN_RECON.get());
                    }).build());

    // ===== 导弹 — 导弹发射器 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MISSILE_TAB =
            CREATIVE_TABS.register("missile_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.missile"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.SY1_LAUNCHER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SY1_LAUNCHER.get());
                        output.accept(ModItems.MK14_HARPOON_LAUNCHER.get());
                        output.accept(ModItems.TERRIER_LAUNCHER.get());
                        output.accept(ModItems.SHIP_ROCKET_LAUNCHER.get());
                        output.accept(ModItems.SEA_DART_LAUNCHER.get());
                        output.accept(ModItems.SEACAT_LAUNCHER.get());
                        // Missile Ammo
                        output.accept(ModItems.SY1_MISSILE.get());
                        output.accept(ModItems.HARPOON_MISSILE.get());
                        output.accept(ModItems.TERRIER_MISSILE.get());
                        output.accept(ModItems.ANTI_AIR_MISSILE.get());
                        output.accept(ModItems.ROCKET_AMMO.get());
                    }).build());

    // ===== 强化 — 装甲 / 声纳 / 动力 / 皮肤 / 燃料 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENHANCEMENT_TAB =
            CREATIVE_TABS.register("enhancement_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.enhancement"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.MEDIUM_ARMOR_PLATE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Armor Plates
                        output.accept(ModItems.SMALL_ARMOR_PLATE.get());
                        output.accept(ModItems.MEDIUM_ARMOR_PLATE.get());
                        output.accept(ModItems.LARGE_ARMOR_PLATE.get());
                        // Sonar
                        output.accept(ModItems.STANDARD_SONAR.get());
                        // Engines
                        output.accept(ModItems.STANDARD_ENGINE.get());
                        output.accept(ModItems.IMPROVED_ENGINE.get());
                        output.accept(ModItems.ADVANCED_ENGINE.get());
                        output.accept(ModItems.HIGH_PRESSURE_BOILER.get());
                        output.accept(ModItems.DIESEL_ENGINE.get());
                        // Torpedo Reload
                        output.accept(ModItems.TORPEDO_RELOAD.get());
                        // Skin Cores
                        output.accept(ModItems.SKIN_CORE_1.get());
                        output.accept(ModItems.SKIN_CORE_2.get());
                        output.accept(ModItems.SKIN_CORE_3.get());
                        // Fuel
                        output.accept(ModItems.FUEL.get());
                        // Reload Facility
                        output.accept(ModItems.RELOAD_FACILITY.get());
                        // Ammo Workbench
                        output.accept(ModItems.AMMO_WORKBENCH.get());
                        // Weapon Workbench
                        output.accept(ModItems.WEAPON_WORKBENCH.get());
                        // Blueprints
                        output.accept(ModItems.MEDIUM_GUN_BLUEPRINT.get());
                        output.accept(ModItems.LARGE_GUN_BLUEPRINT.get());
                    }).build());

    // ===== 农业 — 矿石 / 材料 / 种子 / 作物 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AGRICULTURE_TAB =
            CREATIVE_TABS.register("agriculture_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.agriculture"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.RAW_ALUMINUM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Blocks
                        output.accept(ModItems.BAUXITE_ORE.get());
                        output.accept(ModItems.ALUMINUM_BLOCK.get());
                        output.accept(ModItems.SALT_BLOCK.get());
                        output.accept(ModItems.SALT_CHIP.get());
                        // Materials
                        output.accept(ModItems.RAW_ALUMINUM.get());
                        output.accept(ModItems.ALUMINUM_INGOT.get());
                        output.accept(ModItems.SALT.get());
                        output.accept(ModItems.GYPSUM_CHIP.get());
                        output.accept(ModItems.QUICKLIME.get());
                        // Seeds
                        output.accept(ModItems.TOMATO_SEEDS.get());
                        output.accept(ModItems.SOYBEAN_SEEDS.get());
                        output.accept(ModItems.CHILI_SEEDS.get());
                        output.accept(ModItems.ONION_SEEDS.get());
                        output.accept(ModItems.RICE_SEEDS.get());
                        output.accept(ModItems.LETTUCE_SEEDS.get());
                        output.accept(ModItems.GARLIC_SEEDS.get());
                        output.accept(ModItems.PINEAPPLE_SEED.get());
                        // Crop Produce
                        output.accept(ModItems.TOMATO.get());
                        output.accept(ModItems.SOYBEAN.get());
                        output.accept(ModItems.CHILI.get());
                        output.accept(ModItems.ONION.get());
                        output.accept(ModItems.RICE.get());
                        output.accept(ModItems.LETTUCE.get());
                        output.accept(ModItems.GARLIC.get());
                        output.accept(ModItems.PINEAPPLE.get());
                    }).build());

    // ===== 厨房 — 加工站 / 食材 / 中间品 / 食物 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KITCHEN_TAB =
            CREATIVE_TABS.register("kitchen_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.kitchen"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.COOKING_POT.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Processing Blocks
                        output.accept(ModItems.STONE_MILL.get());
                        output.accept(ModItems.CUTTING_BOARD.get());
                        output.accept(ModItems.COOKING_POT.get());
                        output.accept(ModItems.YUBARI_WATER_BUCKET.get());
                        // Food Ingredients
                        output.accept(ModItems.FLOUR.get());
                        output.accept(ModItems.RICE_FLOUR.get());
                        output.accept(ModItems.CHILI_POWDER.get());
                        output.accept(ModItems.PORK_PASTE.get());
                        output.accept(ModItems.EDIBLE_OIL.get());
                        output.accept(ModItems.BUTTER.get());
                        output.accept(ModItems.CREAM.get());
                        output.accept(ModItems.SOYBEAN_MILK.get());
                        output.accept(ModItems.TOFU.get());
                        output.accept(ModItems.CHEESE.get());
                        output.accept(ModItems.YEAST.get());
                        output.accept(ModItems.SOY_SAUCE.get());
                        output.accept(ModItems.VINEGAR.get());
                        output.accept(ModItems.COOKING_WINE.get());
                        output.accept(ModItems.MISO.get());
                        output.accept(ModItems.BRINE.get());
                        output.accept(ModItems.PIE_CRUST.get());
                        output.accept(ModItems.RAW_PASTA.get());
                        output.accept(ModItems.FERMENTED_FISH.get());
                        output.accept(ModItems.PIZZA_BASE.get());
                        // Intermediate Products
                        output.accept(ModItems.SAUSAGE.get());
                        output.accept(ModItems.SLICED_SAUSAGE.get());
                        output.accept(ModItems.BACON.get());
                        output.accept(ModItems.TOAST_BREAD_SLICES.get());
                        output.accept(ModItems.BEER.get());
                        output.accept(ModItems.ROUND_BUN.get());
                        // Food Items
                        output.accept(ModItems.TOAST_BREAD.get());
                        output.accept(ModItems.NAVAL_BAKED_BEANS.get());
                        output.accept(ModItems.LATIAO.get());
                        output.accept(ModItems.MAPO_TOFU.get());
                        output.accept(ModItems.NAVAL_CURRY.get());
                        output.accept(ModItems.FRIED_FISH_AND_CHIPS.get());
                        output.accept(ModItems.SCONE.get());
                        output.accept(ModItems.SALTED_EGG_TOFU.get());
                        output.accept(ModItems.SURSTROMMING.get());
                        output.accept(ModItems.AMERICAN_BURGER.get());
                        output.accept(ModItems.HOTDOG.get());
                        output.accept(ModItems.PASTA.get());
                        output.accept(ModItems.COOKED_RICE.get());
                        output.accept(ModItems.BEET_BLOSSOM.get());
                        output.accept(ModItems.MISO_SOUP.get());
                        output.accept(ModItems.PINEAPPLE_JUICE.get());
                        // Buff Foods
                        output.accept(ModItems.CHICKEN_TATSUTA.get());
                        output.accept(ModItems.TORPEDO_JUICE.get());
                        output.accept(ModItems.TEMPURA.get());
                        output.accept(ModItems.KVASS.get());
                    }).build());

    // ===== 弹药 — 炮弹 / 鱼雷弹药 / 航空弹药 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AMMO_TAB =
            CREATIVE_TABS.register("ammo_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.ammo"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.MEDIUM_HE_SHELL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // HE Shells
                        output.accept(ModItems.SMALL_HE_SHELL.get());
                        output.accept(ModItems.MEDIUM_HE_SHELL.get());
                        output.accept(ModItems.LARGE_HE_SHELL.get());
                        // AP Shells
                        output.accept(ModItems.SMALL_AP_SHELL.get());
                        output.accept(ModItems.MEDIUM_AP_SHELL.get());
                        output.accept(ModItems.LARGE_AP_SHELL.get());
                        // VT Shell
                        output.accept(ModItems.SMALL_VT_SHELL.get());
                        // Type 3 Shells
                        output.accept(ModItems.SMALL_TYPE3_SHELL.get());
                        output.accept(ModItems.MEDIUM_TYPE3_SHELL.get());
                        output.accept(ModItems.LARGE_TYPE3_SHELL.get());
                        // Torpedo Ammo (legacy)
                        output.accept(ModItems.TORPEDO_533MM.get());
                        output.accept(ModItems.TORPEDO_610MM.get());
                        output.accept(ModItems.MAGNETIC_TORPEDO_533MM.get());
                        output.accept(ModItems.WIRE_GUIDED_TORPEDO_533MM.get());
                        output.accept(ModItems.ACOUSTIC_TORPEDO_533MM.get());
                        // Torpedo Ammo (named)
                        output.accept(ModItems.TORPEDO_533MM_G7A.get());
                        output.accept(ModItems.MAGNETIC_TORPEDO_533MM_G7A.get());
                        output.accept(ModItems.TORPEDO_533MM_MK17.get());
                        output.accept(ModItems.TORPEDO_533MM_MK14.get());
                        output.accept(ModItems.TORPEDO_533MM_MK16.get());
                        output.accept(ModItems.MAGNETIC_TORPEDO_533MM_G7E.get());
                        output.accept(ModItems.ACOUSTIC_TORPEDO_533MM_G7E.get());
                        output.accept(ModItems.WIRE_GUIDED_TORPEDO_533MM_G7E.get());
                        output.accept(ModItems.ACOUSTIC_TORPEDO_533MM_MK27.get());
                        output.accept(ModItems.TORPEDO_530MM_TYPE95.get());
                        output.accept(ModItems.TORPEDO_610MM_TYPE91.get());
                        output.accept(ModItems.TORPEDO_610MM_TYPE93_MK1.get());
                        output.accept(ModItems.TORPEDO_610MM_TYPE93_MK3.get());
                        output.accept(ModItems.TORPEDO_610MM_TYPE95_MK2.get());
                        output.accept(ModItems.TORPEDO_720MM_TYPE0.get());
                        // Aviation Ammo
                        output.accept(ModItems.AVIATION_FUEL.get());
                        output.accept(ModItems.AERIAL_BOMB.get());
                        output.accept(ModItems.AERIAL_TORPEDO.get());
                        output.accept(ModItems.DEPTH_CHARGE.get());
                        output.accept(ModItems.FIGHTER_AMMO.get());
                        // Missile / Rocket Ammo
                        output.accept(ModItems.SY1_MISSILE.get());
                        output.accept(ModItems.HARPOON_MISSILE.get());
                        output.accept(ModItems.TERRIER_MISSILE.get());
                        output.accept(ModItems.ANTI_AIR_MISSILE.get());
                        output.accept(ModItems.ROCKET_AMMO.get());
                    }).build());

    // ===== 道具 — 工具 / 副本道具 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PROPS_TAB =
            CREATIVE_TABS.register("props_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.props"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.HENTAI_TROPHY.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.UNICORN_HARP.get());
                        output.accept(ModItems.FLOATING_TARGET.get());
                        output.accept(ModItems.GUIDEBOOK.get());
                        output.accept(ModItems.DUNGEON_LECTERN.get());
                        output.accept(ModItems.DUNGEON_KEY.get());
                        output.accept(ModItems.TOWN_SCROLL.get());
                        output.accept(ModItems.ELITE_DAMAGE_CONTROL.get());
                        output.accept(ModItems.DAMAGE_CONTROL.get());
                        output.accept(ModItems.QUICK_REPAIR.get());
                        output.accept(ModItems.SMOKE_CANDLE.get());
                        output.accept(ModItems.FLARE_LAUNCHER.get());
                        output.accept(ModItems.KIRIN_HEADBAND.get());
                        output.accept(ModItems.REPAIR_KIT.get());
                        output.accept(ModItems.MYSTERIOUS_WEAPON.get());
                        output.accept(ModItems.RICHELIEU_COMMAND_SWORD.get());
                        output.accept(ModItems.TAIHOU_UMBRELLA.get());
                        output.accept(ModItems.EUGEN_SHIELD.get());
                        // Football Superstar Set
                        output.accept(ModItems.SPIDER_GLOVES.get());
                        output.accept(ModItems.BLUE_JERSEY.get());
                        output.accept(ModItems.RED_BLACK_SOCKS.get());
                        output.accept(ModItems.MIRACLE_BOOTS.get());
                        // Hatsuyuki's Main Gun
                        output.accept(ModItems.HATSUYUKI_MAIN_GUN.get());
                        // Gungnir
                        output.accept(ModItems.GUNGNIR.get());
                        // Deep Ocean Spawn Eggs
                        output.accept(ModItems.DEEP_OCEAN_SUPPLY_SPAWN_EGG.get());
                        output.accept(ModItems.DEEP_OCEAN_DESTROYER_SPAWN_EGG.get());
                        output.accept(ModItems.DEEP_OCEAN_LIGHT_CRUISER_SPAWN_EGG.get());
                        output.accept(ModItems.DEEP_OCEAN_HEAVY_CRUISER_SPAWN_EGG.get());
                        output.accept(ModItems.DEEP_OCEAN_BATTLE_CRUISER_SPAWN_EGG.get());
                        output.accept(ModItems.DEEP_OCEAN_BATTLESHIP_SPAWN_EGG.get());
                        output.accept(ModItems.DEEP_OCEAN_LIGHT_CARRIER_SPAWN_EGG.get());
                        output.accept(ModItems.DEEP_OCEAN_CARRIER_SPAWN_EGG.get());
                        output.accept(ModItems.DEEP_OCEAN_SUBMARINE_SPAWN_EGG.get());
                        output.accept(ModItems.SHIP_GIRL_SPAWN_EGG.get());
                    }).build());
}
