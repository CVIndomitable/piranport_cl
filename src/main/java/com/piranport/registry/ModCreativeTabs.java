package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.component.LoadedAmmo;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
                        // Fuel
                        output.accept(ModItems.FUEL.get());
                    }).build());

    // ===== 武器 — 火炮 / 鱼雷发射器 / 导弹发射器（空武器和满装填武器混合）=====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WEAPONS_TAB =
            CREATIVE_TABS.register("weapons_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.weapons"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.MEDIUM_GUN.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Guns (empty + loaded)
                        output.accept(ModItems.SINGLE_SMALL_GUN.get());
                        addPreloadedWeapon(output, ModItems.SINGLE_SMALL_GUN.get(), "piranport:small_he_shell", 1);
                        output.accept(ModItems.SMALL_GUN.get());
                        addPreloadedWeapon(output, ModItems.SMALL_GUN.get(), "piranport:small_he_shell", 2);
                        output.accept(ModItems.MEDIUM_GUN.get());
                        addPreloadedWeapon(output, ModItems.MEDIUM_GUN.get(), "piranport:medium_he_shell", 2);
                        output.accept(ModItems.LARGE_GUN.get());
                        addPreloadedWeapon(output, ModItems.LARGE_GUN.get(), "piranport:large_he_shell", 3);

                        // Torpedo Launchers (empty + loaded)
                        output.accept(ModItems.TWIN_TORPEDO_LAUNCHER.get());
                        addPreloadedWeapon(output, ModItems.TWIN_TORPEDO_LAUNCHER.get(), "piranport:torpedo_533mm_g7a", 2);
                        addPreloadedWeapon(output, ModItems.TWIN_TORPEDO_LAUNCHER.get(), "piranport:magnetic_torpedo_533mm_g7a", 2);
                        output.accept(ModItems.TRIPLE_TORPEDO_LAUNCHER.get());
                        addPreloadedWeapon(output, ModItems.TRIPLE_TORPEDO_LAUNCHER.get(), "piranport:torpedo_533mm_g7a", 3);
                        addPreloadedWeapon(output, ModItems.TRIPLE_TORPEDO_LAUNCHER.get(), "piranport:acoustic_torpedo_533mm_g7e", 3);
                        output.accept(ModItems.QUAD_TORPEDO_LAUNCHER.get());
                        addPreloadedWeapon(output, ModItems.QUAD_TORPEDO_LAUNCHER.get(), "piranport:torpedo_610mm_type93_mk3", 4);

                        // Depth Charge Launchers (empty + loaded)
                        output.accept(ModItems.DEPTH_CHARGE_LAUNCHER.get());
                        addPreloadedWeapon(output, ModItems.DEPTH_CHARGE_LAUNCHER.get(), "piranport:depth_charge", 1);
                        output.accept(ModItems.DEPTH_CHARGE_LAUNCHER_IMPROVED.get());
                        addPreloadedWeapon(output, ModItems.DEPTH_CHARGE_LAUNCHER_IMPROVED.get(), "piranport:depth_charge", 2);
                        output.accept(ModItems.DEPTH_CHARGE_LAUNCHER_ADVANCED.get());
                        addPreloadedWeapon(output, ModItems.DEPTH_CHARGE_LAUNCHER_ADVANCED.get(), "piranport:depth_charge", 3);

                        // Missile Launchers (empty + loaded)
                        output.accept(ModItems.SY1_LAUNCHER.get());
                        addPreloadedWeapon(output, ModItems.SY1_LAUNCHER.get(), "piranport:sy1_missile", 4);
                        output.accept(ModItems.MK14_HARPOON_LAUNCHER.get());
                        addPreloadedWeapon(output, ModItems.MK14_HARPOON_LAUNCHER.get(), "piranport:harpoon_missile", 4);
                        output.accept(ModItems.TERRIER_LAUNCHER.get());
                        output.accept(ModItems.SHIP_ROCKET_LAUNCHER.get());
                        addPreloadedWeapon(output, ModItems.SHIP_ROCKET_LAUNCHER.get(), "piranport:rocket_ammo", 8);
                        output.accept(ModItems.SEA_DART_LAUNCHER.get());
                        output.accept(ModItems.SEACAT_LAUNCHER.get());
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

    // ===== 强化 — 装甲 / 声纳 / 动力 / 皮肤 =====
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
                    }).build());

    // ===== 农业 — 矿石 / 材料（作物类归入"舰娘食物"标签页）=====
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
                    }).build());

    // ===== 食物 — 种子/作物/食材/调料/中间品/菜品/加工站（合并原"舰娘食物"和"厨房"）=====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FOOD_TAB =
            CREATIVE_TABS.register("food_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.food"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.TOAST_BREAD.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // --- 加工站 ---
                        output.accept(ModItems.STONE_MILL.get());
                        output.accept(ModItems.CUTTING_BOARD.get());
                        output.accept(ModItems.COOKING_POT.get());
                        output.accept(ModItems.YUBARI_WATER_BUCKET.get());
                        // --- 种子 ---
                        output.accept(ModItems.TOMATO_SEEDS.get());
                        output.accept(ModItems.SOYBEAN_SEEDS.get());
                        output.accept(ModItems.CHILI_SEEDS.get());
                        output.accept(ModItems.ONION_SEEDS.get());
                        output.accept(ModItems.RICE_SEEDS.get());
                        output.accept(ModItems.LETTUCE_SEEDS.get());
                        output.accept(ModItems.GARLIC_SEEDS.get());
                        output.accept(ModItems.PINEAPPLE_SEED.get());
                        output.accept(ModItems.LABLAB_BEAN_SEEDS.get());
                        output.accept(ModItems.ORMOSIA_SEEDS.get());
                        output.accept(ModItems.CELERY_SEEDS.get());
                        output.accept(ModItems.RYE_SEEDS.get());
                        output.accept(ModItems.PEACH_SAPLING.get());
                        // --- 桃树材料 ---
                        output.accept(ModItems.PEACH_LOG.get());
                        output.accept(ModItems.PEACH_LEAVES.get());
                        // --- 作物产出 ---
                        output.accept(ModItems.TOMATO.get());
                        output.accept(ModItems.SOYBEAN.get());
                        output.accept(ModItems.CHILI.get());
                        output.accept(ModItems.ONION.get());
                        output.accept(ModItems.RICE.get());
                        output.accept(ModItems.LETTUCE.get());
                        output.accept(ModItems.GARLIC.get());
                        output.accept(ModItems.PINEAPPLE.get());
                        output.accept(ModItems.LABLAB_BEAN.get());
                        output.accept(ModItems.ORMOSIA.get());
                        output.accept(ModItems.CELERY.get());
                        output.accept(ModItems.RYE.get());
                        output.accept(ModItems.PEACH.get());
                        // --- 食材/调料 ---
                        output.accept(ModItems.FLOUR.get());
                        output.accept(ModItems.RICE_FLOUR.get());
                        output.accept(ModItems.WALNUT.get());
                        output.accept(ModItems.WALNUT_POWDER.get());
                        output.accept(ModItems.ALMOND.get());
                        output.accept(ModItems.ALMOND_POWDER.get());
                        output.accept(ModItems.CHILI_POWDER.get());
                        output.accept(ModItems.CURRY_POWDER.get());
                        output.accept(ModItems.BLACK_PEPPER.get());
                        output.accept(ModItems.WHITE_PEPPER.get());
                        output.accept(ModItems.GINGER.get());
                        output.accept(ModItems.BLACK_TEA.get());
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
                        output.accept(ModItems.CATCHUP.get());
                        output.accept(ModItems.BOLOGNESE.get());
                        output.accept(ModItems.LABLAB_SOUP.get());
                        // --- 果汁/果酱 ---
                        output.accept(ModItems.APPLE_JUICE.get());
                        output.accept(ModItems.APPLE_JAM.get());
                        output.accept(ModItems.WATERMELON_JUICE.get());
                        output.accept(ModItems.WATERMELON_JAM.get());
                        output.accept(ModItems.PINEAPPLE_JUICE.get());
                        output.accept(ModItems.PINEAPPLE_JAM.get());
                        output.accept(ModItems.CHORUS_FRUIT_JAM.get());
                        // --- 菜品：面包/主食 ---
                        output.accept(ModItems.TOAST_BREAD.get());
                        output.accept(ModItems.RYE_BREAD.get());
                        output.accept(ModItems.NEW_RYE_BREAD.get());
                        output.accept(ModItems.COOKED_RICE.get());
                        output.accept(ModItems.PASTA.get());
                        // --- 菜品：饭/面 ---
                        output.accept(ModItems.ASSORTED_CHAR_SIU_FRIED_RICE.get());
                        output.accept(ModItems.NAVAL_CURRY.get());
                        output.accept(ModItems.SOBA_NOODLE.get());
                        output.accept(ModItems.TEMPURA_SOBA_NOODLE.get());
                        output.accept(ModItems.VENICE_CUTTLEFISH_NOODLES.get());
                        output.accept(ModItems.BOLOGNESE_LINGUINE_RECIPE.get());
                        // --- 菜品：中华 ---
                        output.accept(ModItems.MAPO_TOFU.get());
                        output.accept(ModItems.BEET_BLOSSOM.get());
                        output.accept(ModItems.SALTY_BEAN_CURD.get());
                        output.accept(ModItems.SALTED_EGG_TOFU.get());
                        output.accept(ModItems.DELUXE_BAOZI.get());
                        output.accept(ModItems.DONGPO_PORK.get());
                        output.accept(ModItems.TANGYUAN.get());
                        output.accept(ModItems.LATIAO.get());
                        output.accept(ModItems.YOKAN.get());
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
                        output.accept(ModItems.PLATED_ROYAL_NAVAL_SALTED_BEEF.get());
                        output.accept(ModItems.YORKSHIRE_PUDDING.get());
                        output.accept(ModItems.APPLE_PIE.get());
                        output.accept(ModItems.TARTE_TATIN.get());
                        output.accept(ModItems.STARGAZY_PIE.get());
                        // --- 菜品：美式 ---
                        output.accept(ModItems.AMERICAN_BURGER.get());
                        output.accept(ModItems.DOUBLE_SHELL_AMERICAN_BURGER.get());
                        output.accept(ModItems.TRIPLE_SHELL_AMERICAN_BURGER.get());
                        output.accept(ModItems.EGGS_BENEDICT.get());
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
                        output.accept(ModItems.PEA_SOUP_WITH_RYE_BREAD.get());
                        output.accept(ModItems.KVASS.get());
                        // --- 菜品：地中海/意大利 ---
                        output.accept(ModItems.BOUILLABAISSE.get());
                        output.accept(ModItems.SALAMI_PIZZA.get());
                        output.accept(ModItems.SALAMI_PIZZA_PIECES.get());
                        output.accept(ModItems.MUSSOLINIS_OO.get());
                        // --- 菜品：其他 ---
                        output.accept(ModItems.MACARON.get());
                        output.accept(ModItems.NAVAL_BAKED_BEANS.get());
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
                        output.accept(ModItems.GUIDEBOOK.get());
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
                    }).build());

    // ===== 设施 — 装填设施 / 合成台 / 蓝图 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FACILITY_TAB =
            CREATIVE_TABS.register("facility_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.facility"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.WEAPON_WORKBENCH.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.RELOAD_FACILITY.get());
                        output.accept(ModItems.AMMO_WORKBENCH.get());
                        output.accept(ModItems.WEAPON_WORKBENCH.get());
                        output.accept(ModItems.DUNGEON_LECTERN.get());
                        output.accept(ModItems.FLOATING_TARGET.get());
                        output.accept(ModItems.MEDIUM_GUN_BLUEPRINT.get());
                        output.accept(ModItems.LARGE_GUN_BLUEPRINT.get());
                        output.accept(ModItems.CREATIVE_BLUEPRINT.get());
                    }).build());

    // ===== 装饰 — 家具/花卉/模型 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DECORATION_TAB =
            CREATIVE_TABS.register("decoration_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.decoration"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.CONFIDENTIAL_CARGO.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CONFIDENTIAL_CARGO.get());
                        output.accept(ModItems.ABYSS_RED_SPIDER_LILY.get());
                        output.accept(ModItems.ITALIAN_DISH_KIT.get());
                        output.accept(ModItems.B25_MODEL.get());
                    }).build());

    // ===== 遗迹 — 传送门/碎片/国旗/档案 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RUINS_TAB =
            CREATIVE_TABS.register("ruins_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.ruins"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.ABYSSAL_PORTAL_FRAME.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Blocks
                        output.accept(ModItems.ABYSSAL_PORTAL_FRAME.get());
                        output.accept(ModItems.ABYSSAL_SPAWNER.get());
                        // Placeholder items
                        output.accept(ModItems.ABYSSAL_REPORT.get());
                        output.accept(ModItems.PORTAL_ACTIVATION_CORE.get());
                        output.accept(ModItems.EXP_SHELL.get());
                        // Chaos Shards
                        output.accept(ModItems.CHAOS_SHARD_ALPHA.get());
                        output.accept(ModItems.CHAOS_SHARD_BETA.get());
                        output.accept(ModItems.CHAOS_SHARD_GAMMA.get());
                        output.accept(ModItems.CHAOS_SHARD_DELTA.get());
                        output.accept(ModItems.CHAOS_SHARD_EPSILON.get());
                        output.accept(ModItems.CHAOS_SHARD_ZETA.get());
                        output.accept(ModItems.CHAOS_SHARD_ETA.get());
                        output.accept(ModItems.CHAOS_SHARD_THETA.get());
                        output.accept(ModItems.CHAOS_SHARD_IOTA.get());
                        // National Flags
                        output.accept(ModItems.FLAG_J.get());
                        output.accept(ModItems.FLAG_E.get());
                        output.accept(ModItems.FLAG_U.get());
                        output.accept(ModItems.FLAG_G.get());
                        output.accept(ModItems.FLAG_F.get());
                        output.accept(ModItems.FLAG_I.get());
                        output.accept(ModItems.FLAG_C.get());
                    }).build());

    // ===== 刷怪蛋 — 所有刷怪蛋 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SPAWN_EGGS_TAB =
            CREATIVE_TABS.register("spawn_eggs_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.spawn_eggs"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.SHIP_GIRL_SPAWN_EGG.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
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

    private static void addPreloadedWeapon(CreativeModeTab.Output output, Item weapon, String ammoId, int count) {
        ItemStack stack = weapon.getDefaultInstance();
        stack.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(count, ammoId));
        output.accept(stack);
    }
}
