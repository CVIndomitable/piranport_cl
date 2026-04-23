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
                    .withTabsAfter(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.MEDIUM_SHIP_CORE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SMALL_SHIP_CORE.get());
                        output.accept(ModItems.MEDIUM_SHIP_CORE.get());
                        output.accept(ModItems.LARGE_SHIP_CORE.get());
                        output.accept(ModItems.SUBMARINE_CORE.get());
                        // Fuel
                        output.accept(ModItems.FUEL.get());
                        // Guidebook
                        output.accept(ModItems.GUIDEBOOK.get());
                    }).build());

    // ===== 炮雷 — 火炮 / 鱼雷发射器 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CANNON_TORPEDO_TAB =
            CREATIVE_TABS.register("cannon_torpedo_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.cannon_torpedo"))
                    .withTabsAfter(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.MEDIUM_GUN.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Guns
                        output.accept(ModItems.SINGLE_SMALL_GUN.get());
                        output.accept(ModItems.SMALL_GUN.get());
                        output.accept(ModItems.MEDIUM_GUN.get());
                        output.accept(ModItems.LARGE_GUN.get());
                        // Torpedo Launchers
                        output.accept(ModItems.TWIN_TORPEDO_LAUNCHER.get());
                        output.accept(ModItems.TRIPLE_TORPEDO_LAUNCHER.get());
                        output.accept(ModItems.QUAD_TORPEDO_LAUNCHER.get());
                        output.accept(ModItems.DEPTH_CHARGE_LAUNCHER.get());
                        output.accept(ModItems.DEPTH_CHARGE_LAUNCHER_IMPROVED.get());
                    }).build());

    // ===== 航空 — 飞机编队 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AVIATION_TAB =
            CREATIVE_TABS.register("aviation_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.aviation"))
                    .withTabsAfter(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.B25_BOMBER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Torpedo bombers
                        output.accept(ModItems.SWORDFISH_TORPEDO.get());
                        output.accept(ModItems.SWORDFISH_ASW.get());
                        output.accept(ModItems.TBF_TORPEDO.get());
                        output.accept(ModItems.TBF_ASW.get());
                        output.accept(ModItems.TENZAN_TORPEDO.get());
                        output.accept(ModItems.TYPE97_TORPEDO.get());
                        output.accept(ModItems.SKY_PIRATE_TORPEDO.get());
                        // Dive bombers
                        output.accept(ModItems.SBD_DAUNTLESS.get());
                        output.accept(ModItems.FIREFLY_AS_MK5.get());
                        output.accept(ModItems.SUISEI_BOMBER.get());
                        // Level bombers
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
                    }).build());

    // ===== 导弹 — 导弹发射器 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MISSILE_TAB =
            CREATIVE_TABS.register("missile_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.missile"))
                    .withTabsAfter(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.SY1_LAUNCHER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SY1_LAUNCHER.get());
                        output.accept(ModItems.MK14_HARPOON_LAUNCHER.get());
                        output.accept(ModItems.TERRIER_LAUNCHER.get());
                        output.accept(ModItems.SHIP_ROCKET_LAUNCHER.get());
                        output.accept(ModItems.SEA_DART_LAUNCHER.get());
                        output.accept(ModItems.SEACAT_LAUNCHER.get());
                    }).build());

    // ===== 强化 — 装甲 / 声纳 / 动力 / 皮肤 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENHANCEMENT_TAB =
            CREATIVE_TABS.register("enhancement_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.enhancement"))
                    .withTabsAfter(CreativeModeTabs.SPAWN_EGGS)
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
                    }).build());

    // ===== 农业 — 矿石 / 材料（作物类归入"舰娘食物"标签页）=====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AGRICULTURE_TAB =
            CREATIVE_TABS.register("agriculture_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.agriculture"))
                    .withTabsAfter(CreativeModeTabs.SPAWN_EGGS)
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
                        output.accept(ModItems.QUICKLIME.get());
                    }).build());

    // ===== 厨房 — 加工方块 / 食材 / 调料 / 中间品 / 菜品（按地区）/ Buff 食物 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KITCHEN_TAB =
            CREATIVE_TABS.register("kitchen_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.kitchen"))
                    .withTabsAfter(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.TOAST_BREAD.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // --- 食材/调料 ---
                        output.accept(ModItems.FLOUR.get());
                        output.accept(ModItems.BLACK_PEPPER.get());
                        output.accept(ModItems.WHITE_PEPPER.get());
                        output.accept(ModItems.GINGER.get());
                        output.accept(ModItems.BLACK_TEA.get());
                        output.accept(ModItems.EDIBLE_OIL.get());
                        output.accept(ModItems.BUTTER.get());
                        output.accept(ModItems.CREAM.get());
                        output.accept(ModItems.CHEESE.get());
                        output.accept(ModItems.YEAST.get());
                        output.accept(ModItems.SOY_SAUCE.get());
                        output.accept(ModItems.VINEGAR.get());
                        output.accept(ModItems.COOKING_WINE.get());
                        output.accept(ModItems.MISO.get());
                        output.accept(ModItems.BRINE.get());
                        output.accept(ModItems.PIE_CRUST.get());
                        output.accept(ModItems.ROAST_PASTRY_OF_PIE.get());
                        output.accept(ModItems.RAW_PASTA.get());
                        output.accept(ModItems.FERMENTED_FISH.get());
                        output.accept(ModItems.PIZZA_BASE.get());
                        output.accept(ModItems.MILK_ICE_CREAM.get());
                        output.accept(ModItems.EMBRYO_OF_APPLE_PIE.get());
                        output.accept(ModItems.EMBRYO_OF_SALAMI_PIZZA.get());
                        output.accept(ModItems.WOODEN_BOWL.get());
                        output.accept(ModItems.WOODEN_BARREL.get());
                        // --- 中间品 ---
                        output.accept(ModItems.SAUSAGE.get());
                        output.accept(ModItems.SLICED_SAUSAGE.get());
                        output.accept(ModItems.SALAMI.get());
                        output.accept(ModItems.SLICED_SALAMI.get());
                        output.accept(ModItems.BACON.get());
                        output.accept(ModItems.TOAST_BREAD_SLICES.get());
                        output.accept(ModItems.BEER.get());
                        output.accept(ModItems.ROUND_BUN.get());
                        output.accept(ModItems.BAGEL.get());
                        output.accept(ModItems.BEANS_CAN.get());
                        // --- 果汁/果酱 ---
                        output.accept(ModItems.APPLE_JUICE.get());
                        output.accept(ModItems.APPLE_JAM.get());
                        output.accept(ModItems.WATERMELON_JUICE.get());
                        output.accept(ModItems.WATERMELON_JAM.get());
                        output.accept(ModItems.CHORUS_FRUIT_JAM.get());
                        // --- 菜品：面包/主食 ---
                        output.accept(ModItems.TOAST_BREAD.get());
                        output.accept(ModItems.PASTA.get());
                        // --- 菜品：饭/面 ---
                        output.accept(ModItems.ASSORTED_CHAR_SIU_FRIED_RICE.get());
                        output.accept(ModItems.NAVAL_CURRY.get());
                        output.accept(ModItems.SOBA_NOODLE.get());
                        output.accept(ModItems.TEMPURA_SOBA_NOODLE.get());
                        output.accept(ModItems.VENICE_CUTTLEFISH_NOODLES.get());
                        output.accept(ModItems.BOLOGNESE_LINGUINE_RECIPE.get());
                        // --- 菜品：中华 ---
                        output.accept(ModItems.DONGPO_PORK.get());
                        // --- 菜品：日式 ---
                        output.accept(ModItems.MISO_SOUP.get());
                        output.accept(ModItems.FRIED_FISH_MISO_SOUP.get());
                        output.accept(ModItems.CHICKEN_TATSUTA.get());
                        output.accept(ModItems.TEMPURA.get());
                        // --- 菜品：英式 ---
                        output.accept(ModItems.FRIED_FISH_AND_CHIPS.get());
                        output.accept(ModItems.SCONE.get());
                        output.accept(ModItems.BLACK_TEA_SCONE.get());
                        output.accept(ModItems.BLACK_TEA_SANDWICH.get());
                        output.accept(ModItems.EGG_SANDWICH.get());
                        output.accept(ModItems.BACON_SANDWICH.get());
                        output.accept(ModItems.ROYAL_NAVAL_SALTED_BEEF.get());
                        output.accept(ModItems.YORKSHIRE_PUDDING.get());
                        output.accept(ModItems.EGGS_BENEDICT.get());
                        output.accept(ModItems.APPLE_PIE.get());
                        output.accept(ModItems.TARTE_TATIN.get());
                        // --- 菜品：美式 ---
                        output.accept(ModItems.AMERICAN_BURGER.get());
                        output.accept(ModItems.DOUBLE_SHELL_AMERICAN_BURGER.get());
                        output.accept(ModItems.TRIPLE_SHELL_AMERICAN_BURGER.get());
                        output.accept(ModItems.HOTDOG.get());
                        output.accept(ModItems.BARBECUE.get());
                        // --- 菜品：德式 ---
                        output.accept(ModItems.SCHWEINSHAXE.get());
                        output.accept(ModItems.THURINGER_ROSTBRATWURST.get());
                        output.accept(ModItems.THURINGER_ROSTBRATWURST_UND_BIER.get());
                        output.accept(ModItems.WEISSWURST_MIT_DER_BAGEL.get());
                        output.accept(ModItems.BLACK_FOREST_GATEAU.get());
                        // --- 菜品：俄式 ---
                        output.accept(ModItems.BORSCHT.get());
                        output.accept(ModItems.OKROSHKA.get());
                        output.accept(ModItems.RUSSIAN_DUMPLING.get());
                        output.accept(ModItems.KVASS.get());
                        // --- 菜品：地中海/意大利 ---
                        output.accept(ModItems.BOUILLABAISSE.get());
                        output.accept(ModItems.SALAMI_PIZZA.get());
                        output.accept(ModItems.SALAMI_PIZZA_PIECES.get());
                        output.accept(ModItems.MUSSOLINIS_OO.get());
                        // --- 菜品：其他 ---
                        output.accept(ModItems.SURSTROMMING.get());
                        output.accept(ModItems.HE_WEI_DAO.get());
                        output.accept(ModItems.TAPTAP_ICE_CREAM.get());
                        // --- Buff 食物/饮料 ---
                        output.accept(ModItems.TORPEDO_JUICE.get());
                    }).build());

    // ===== 弹药 — 炮弹 / 鱼雷弹药 / 航空弹药 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AMMO_TAB =
            CREATIVE_TABS.register("ammo_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.ammo"))
                    .withTabsAfter(CreativeModeTabs.SPAWN_EGGS)
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
                    .withTabsAfter(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.HENTAI_TROPHY.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.UNICORN_HARP.get());
                        output.accept(ModItems.ELITE_DAMAGE_CONTROL.get());
                        output.accept(ModItems.DAMAGE_CONTROL.get());
                        output.accept(ModItems.QUICK_REPAIR.get());
                        output.accept(ModItems.SMOKE_CANDLE.get());
                        output.accept(ModItems.FLARE_LAUNCHER.get());
                        output.accept(ModItems.KIRIN_HEADBAND.get());
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
                    }).build());

    // ===== 设施 — 装填设施 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FACILITY_TAB =
            CREATIVE_TABS.register("facility_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.facility"))
                    .withTabsAfter(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.RELOAD_FACILITY.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.RELOAD_FACILITY.get());
                        output.accept(ModItems.FLOATING_TARGET.get());
                    }).build());

    // ===== 装饰 — 家具/花卉/模型 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DECORATION_TAB =
            CREATIVE_TABS.register("decoration_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.decoration"))
                    .withTabsAfter(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.CONFIDENTIAL_CARGO.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CONFIDENTIAL_CARGO.get());
                        output.accept(ModItems.ABYSS_RED_SPIDER_LILY.get());
                        output.accept(ModItems.ITALIAN_DISH_KIT.get());
                        output.accept(ModItems.B25_MODEL.get());
                    }).build());

}
