package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.item.BottleFoodItem;
import com.piranport.item.ModFoodItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * 阶段 2：简单 Item 批量占位注册。
 *
 * 原则：
 *   - 不引入任何自定义 Item 子类（AircraftItem / CannonItem / ShipCoreItem / EngineItem 等均未启用）。
 *   - 依赖 DataComponents 的字段全部砍掉，等阶段 7 NBT 重写时复活。
 *   - 食物 / 饮料（ModFoodItem / BottleFoodItem）延到阶段 3。
 *   - 飞机（AircraftItem 18+ 个变体）延到阶段 5。
 *   - Footballer 套装只按普通 Item 占位，不挂 ArmorMaterial。
 *
 * 命名顺序尽量对齐 1.0 ModItems 的分节，方便后续阶段回填玩法。
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, PiranPort.MOD_ID);

    // ===== Tab Icon =====
    public static final RegistryObject<Item> TAB_ICON = simple("tab_icon");

    // ===== Materials =====
    public static final RegistryObject<Item> RAW_ALUMINUM    = simple("raw_aluminum");
    public static final RegistryObject<Item> ALUMINUM_INGOT  = simple("aluminum_ingot");
    public static final RegistryObject<Item> SALT            = simple("salt");

    // ===== Shells (HE / AP / VT / Type3) =====
    public static final RegistryObject<Item> SMALL_HE_SHELL    = simple("small_he_shell");
    public static final RegistryObject<Item> MEDIUM_HE_SHELL   = simple("medium_he_shell");
    public static final RegistryObject<Item> LARGE_HE_SHELL    = simple("large_he_shell");
    public static final RegistryObject<Item> SMALL_AP_SHELL    = simple("small_ap_shell");
    public static final RegistryObject<Item> MEDIUM_AP_SHELL   = simple("medium_ap_shell");
    public static final RegistryObject<Item> LARGE_AP_SHELL    = simple("large_ap_shell");
    public static final RegistryObject<Item> SMALL_VT_SHELL    = simple("small_vt_shell");
    public static final RegistryObject<Item> SMALL_TYPE3_SHELL = simple("small_type3_shell");
    public static final RegistryObject<Item> MEDIUM_TYPE3_SHELL= simple("medium_type3_shell");
    public static final RegistryObject<Item> LARGE_TYPE3_SHELL = simple("large_type3_shell");

    // ===== Guns (占位，阶段 7 换 CannonItem) =====
    public static final RegistryObject<Item> SINGLE_SMALL_GUN = single("single_small_gun");
    public static final RegistryObject<Item> SMALL_GUN        = single("small_gun");
    public static final RegistryObject<Item> MEDIUM_GUN       = single("medium_gun");
    public static final RegistryObject<Item> LARGE_GUN        = single("large_gun");

    // ===== Torpedo Ammo (legacy generic, 占位) =====
    public static final RegistryObject<Item> TORPEDO_533MM             = stack16("torpedo_533mm");
    public static final RegistryObject<Item> TORPEDO_610MM             = stack16("torpedo_610mm");
    public static final RegistryObject<Item> MAGNETIC_TORPEDO_533MM    = stack16("magnetic_torpedo_533mm");
    public static final RegistryObject<Item> WIRE_GUIDED_TORPEDO_533MM = stack16("wire_guided_torpedo_533mm");
    public static final RegistryObject<Item> ACOUSTIC_TORPEDO_533MM    = stack16("acoustic_torpedo_533mm");

    // ===== Torpedo Ammo (named variants, 占位) =====
    public static final RegistryObject<Item> TORPEDO_533MM_G7A          = stack16("torpedo_533mm_g7a");
    public static final RegistryObject<Item> MAGNETIC_TORPEDO_533MM_G7A = stack16("magnetic_torpedo_533mm_g7a");
    public static final RegistryObject<Item> TORPEDO_533MM_MK17         = stack16("torpedo_533mm_mk17");
    public static final RegistryObject<Item> TORPEDO_610MM_TYPE91       = stack16("torpedo_610mm_type91");
    public static final RegistryObject<Item> TORPEDO_610MM_TYPE93_MK1   = stack16("torpedo_610mm_type93_mk1");
    public static final RegistryObject<Item> TORPEDO_610MM_TYPE93_MK3   = stack16("torpedo_610mm_type93_mk3");
    public static final RegistryObject<Item> TORPEDO_720MM_TYPE0        = stack16("torpedo_720mm_type0");
    public static final RegistryObject<Item> TORPEDO_533MM_MK14         = stack16("torpedo_533mm_mk14");
    public static final RegistryObject<Item> TORPEDO_533MM_MK16         = stack16("torpedo_533mm_mk16");
    public static final RegistryObject<Item> MAGNETIC_TORPEDO_533MM_G7E = stack16("magnetic_torpedo_533mm_g7e");
    public static final RegistryObject<Item> ACOUSTIC_TORPEDO_533MM_G7E = stack16("acoustic_torpedo_533mm_g7e");
    public static final RegistryObject<Item> WIRE_GUIDED_TORPEDO_533MM_G7E = stack16("wire_guided_torpedo_533mm_g7e");
    public static final RegistryObject<Item> ACOUSTIC_TORPEDO_533MM_MK27 = stack16("acoustic_torpedo_533mm_mk27");
    public static final RegistryObject<Item> TORPEDO_530MM_TYPE95       = stack16("torpedo_530mm_type95");
    public static final RegistryObject<Item> TORPEDO_610MM_TYPE95_MK2   = stack16("torpedo_610mm_type95_mk2");

    // ===== Armor Plates (占位，阶段 7 换 ArmorPlateItem) =====
    public static final RegistryObject<Item> SMALL_ARMOR_PLATE  = single("small_armor_plate");
    public static final RegistryObject<Item> MEDIUM_ARMOR_PLATE = single("medium_armor_plate");
    public static final RegistryObject<Item> LARGE_ARMOR_PLATE  = single("large_armor_plate");

    // ===== Food Ingredients (阶段 11a) =====
    public static final RegistryObject<Item> FLOUR           = simple("flour");
    public static final RegistryObject<Item> EDIBLE_OIL      = simple("edible_oil");
    public static final RegistryObject<Item> BUTTER          = simple("butter");
    public static final RegistryObject<Item> CREAM           = simple("cream");
    public static final RegistryObject<Item> CHEESE          = simple("cheese");
    public static final RegistryObject<Item> YEAST           = simple("yeast");
    public static final RegistryObject<Item> SOY_SAUCE       = simple("soy_sauce");
    public static final RegistryObject<Item> VINEGAR         = simple("vinegar");
    public static final RegistryObject<Item> COOKING_WINE    = simple("cooking_wine");
    public static final RegistryObject<Item> MISO            = simple("miso");
    public static final RegistryObject<Item> BRINE           = simple("brine");
    public static final RegistryObject<Item> PIE_CRUST       = simple("pie_crust");
    public static final RegistryObject<Item> RAW_PASTA       = simple("raw_pasta");
    public static final RegistryObject<Item> FERMENTED_FISH  = simple("fermented_fish");
    public static final RegistryObject<Item> PIZZA_BASE      = simple("pizza_base");
    public static final RegistryObject<Item> QUICKLIME       = simple("quicklime");

    // ===== Intermediate Products =====
    public static final RegistryObject<Item> SAUSAGE            = simple("sausage");
    public static final RegistryObject<Item> SLICED_SAUSAGE     = simple("sliced_sausage");
    public static final RegistryObject<Item> BACON              = simple("bacon");
    public static final RegistryObject<Item> TOAST_BREAD_SLICES = simple("toast_bread_slices");
    public static final RegistryObject<Item> BEER               = simple("beer");
    public static final RegistryObject<Item> ROUND_BUN          = simple("round_bun");

    // ===== Ship Cores (占位，阶段 7 换 ShipCoreItem) =====
    public static final RegistryObject<Item> SMALL_SHIP_CORE  = single("small_ship_core");
    public static final RegistryObject<Item> MEDIUM_SHIP_CORE = single("medium_ship_core");
    public static final RegistryObject<Item> LARGE_SHIP_CORE  = single("large_ship_core");
    public static final RegistryObject<Item> SUBMARINE_CORE   = single("submarine_core");

    // ===== Skin Cores (占位) =====
    public static final RegistryObject<Item> SKIN_CORE_1 = single("skin_core_1");
    public static final RegistryObject<Item> SKIN_CORE_2 = single("skin_core_2");
    public static final RegistryObject<Item> SKIN_CORE_3 = single("skin_core_3");

    // ===== Aviation Ammo =====
    public static final RegistryObject<Item> AVIATION_FUEL     = simple("aviation_fuel");
    public static final RegistryObject<Item> AERIAL_BOMB       = simple("aerial_bomb");
    public static final RegistryObject<Item> AERIAL_TORPEDO    = simple("aerial_torpedo");
    public static final RegistryObject<Item> DEPTH_CHARGE      = simple("depth_charge");
    public static final RegistryObject<Item> PROJECTILE_BULLET = simple("projectile_bullet");

    // ===== Sonar / Engines / Torpedo Reload (占位) =====
    public static final RegistryObject<Item> STANDARD_SONAR      = single("standard_sonar");
    public static final RegistryObject<Item> STANDARD_ENGINE     = single("standard_engine");
    public static final RegistryObject<Item> IMPROVED_ENGINE     = single("improved_engine");
    public static final RegistryObject<Item> ADVANCED_ENGINE     = single("advanced_engine");
    public static final RegistryObject<Item> HIGH_PRESSURE_BOILER= single("high_pressure_boiler");
    public static final RegistryObject<Item> DIESEL_ENGINE       = single("diesel_engine");
    public static final RegistryObject<Item> TORPEDO_RELOAD      = single("torpedo_reload");

    // ===== Torpedo Launchers (占位，带耐久) =====
    public static final RegistryObject<Item> TWIN_TORPEDO_LAUNCHER   = durable("twin_torpedo_launcher", 64);
    public static final RegistryObject<Item> TRIPLE_TORPEDO_LAUNCHER = durable("triple_torpedo_launcher", 48);
    public static final RegistryObject<Item> QUAD_TORPEDO_LAUNCHER   = durable("quad_torpedo_launcher", 32);

    // ===== Depth Charge Launchers (占位) =====
    public static final RegistryObject<Item> DEPTH_CHARGE_LAUNCHER          = durable("depth_charge_launcher", 64);
    public static final RegistryObject<Item> DEPTH_CHARGE_LAUNCHER_IMPROVED = durable("depth_charge_launcher_improved", 48);

    // ===== Missile Ammo (占位) =====
    public static final RegistryObject<Item> SY1_MISSILE      = stack16("sy1_missile");
    public static final RegistryObject<Item> HARPOON_MISSILE  = stack16("harpoon_missile");
    public static final RegistryObject<Item> TERRIER_MISSILE  = stack16("terrier_missile");
    public static final RegistryObject<Item> ANTI_AIR_MISSILE = stack16("anti_air_missile");
    public static final RegistryObject<Item> ROCKET_AMMO      = stack16("rocket_ammo");

    // ===== Missile Launchers (占位) =====
    public static final RegistryObject<Item> SY1_LAUNCHER          = single("sy1_launcher");
    public static final RegistryObject<Item> MK14_HARPOON_LAUNCHER = single("mk14_harpoon_launcher");
    public static final RegistryObject<Item> TERRIER_LAUNCHER      = single("terrier_launcher");
    public static final RegistryObject<Item> SHIP_ROCKET_LAUNCHER  = single("ship_rocket_launcher");
    public static final RegistryObject<Item> SEA_DART_LAUNCHER     = single("sea_dart_launcher");
    public static final RegistryObject<Item> SEACAT_LAUNCHER       = single("seacat_launcher");

    // ===== Fuel =====
    public static final RegistryObject<Item> FUEL = simple("fuel");

    // ===== Tools =====
    public static final RegistryObject<Item> UNICORN_HARP = single("unicorn_harp");

    // ===== Survival Items / 道具 =====
    public static final RegistryObject<Item> ELITE_DAMAGE_CONTROL = single("elite_damage_control");
    public static final RegistryObject<Item> DAMAGE_CONTROL       = single("damage_control");
    public static final RegistryObject<Item> QUICK_REPAIR         = single("quick_repair");
    public static final RegistryObject<Item> SMOKE_CANDLE         = durable("smoke_candle", 16);
    public static final RegistryObject<Item> FLARE_LAUNCHER       = durable("flare_launcher", 4096);
    public static final RegistryObject<Item> KIRIN_HEADBAND       = single("kirin_headband");
    public static final RegistryObject<Item> MYSTERIOUS_WEAPON    = durable("mysterious_weapon", 128);
    public static final RegistryObject<Item> RICHELIEU_COMMAND_SWORD = single("richelieu_command_sword");
    public static final RegistryObject<Item> TAIHOU_UMBRELLA      = durable("taihou_umbrella", 1520);
    public static final RegistryObject<Item> EUGEN_SHIELD         = durable("eugen_shield", 1200);

    // ===== Props Tab Icon =====
    public static final RegistryObject<Item> HENTAI_TROPHY = simple("hentai_trophy");

    // ===== Football Superstar Set (占位 Item，阶段 8 换真正的 ArmorItem) =====
    public static final RegistryObject<Item> SPIDER_GLOVES   = single("spider_gloves");
    public static final RegistryObject<Item> BLUE_JERSEY     = single("blue_jersey");
    public static final RegistryObject<Item> RED_BLACK_SOCKS = single("red_black_socks");
    public static final RegistryObject<Item> MIRACLE_BOOTS   = single("miracle_boots");

    // ===== Named Weapons (占位) =====
    public static final RegistryObject<Item> HATSUYUKI_MAIN_GUN = single("hatsuyuki_main_gun");
    public static final RegistryObject<Item> GUNGNIR           = durable("gungnir", 512);

    // ===== Floating Target / Guidebook (占位) =====
    public static final RegistryObject<Item> FLOATING_TARGET = stack16("floating_target");
    public static final RegistryObject<Item> GUIDEBOOK       = single("guidebook");

    // ===== Food Ingredients - Phase 28 补充 =====
    public static final RegistryObject<Item> SALAMI         = simple("salami");
    public static final RegistryObject<Item> SLICED_SALAMI  = simple("sliced_salami");
    public static final RegistryObject<Item> WOODEN_BOWL    = simple("wooden_bowl");
    public static final RegistryObject<Item> WOODEN_BARREL  = simple("wooden_barrel");

    // ===== Intermediate Foods (plain Item with food) =====
    public static final RegistryObject<Item> HOTDOG = ITEMS.register("hotdog",
            () -> new Item(new Item.Properties().food(fp(4, 5f)
                    .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0), 1.0f)
                    .build())));
    public static final RegistryObject<Item> BEANS_CAN = ITEMS.register("beans_can",
            () -> new Item(new Item.Properties().food(fp(4, 5f).build())));
    public static final RegistryObject<Item> BAGEL = ITEMS.register("bagel",
            () -> new Item(new Item.Properties().food(fp(5, 6.3f).build())));
    public static final RegistryObject<Item> ROAST_PASTRY_OF_PIE = ITEMS.register("roast_pastry_of_pie",
            () -> new Item(new Item.Properties().food(fp(3, 3.8f).build())));

    // ===== Cooking Pot Foods (ModFoodItem - 阶段 7 NBT 重写后回填 placeable) =====
    public static final RegistryObject<ModFoodItem> TOAST_BREAD = food("toast_bread", 15, 18.8f);
    public static final RegistryObject<ModFoodItem> NAVAL_CURRY = ITEMS.register("naval_curry",
            () -> new ModFoodItem(new Item.Properties().food(fp(5, 6.3f)
                    .effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 4800, 0), 1.0f)
                    .build())));
    public static final RegistryObject<ModFoodItem> FRIED_FISH_AND_CHIPS = ITEMS.register("fried_fish_and_chips",
            () -> new ModFoodItem(new Item.Properties().food(fp(5, 6.3f)
                    .effect(() -> new MobEffectInstance(MobEffects.JUMP, 3600, 1), 1.0f)
                    .build())));
    public static final RegistryObject<ModFoodItem> SCONE = food("scone", 3, 3.8f);
    public static final RegistryObject<ModFoodItem> APPLE_PIE = food("apple_pie", 5, 6f);
    public static final RegistryObject<ModFoodItem> ASSORTED_CHAR_SIU_FRIED_RICE = food("assorted_char_siu_fried_rice", 5, 6f);
    public static final RegistryObject<ModFoodItem> SURSTROMMING = ITEMS.register("surstromming",
            () -> new ModFoodItem(new Item.Properties().food(fp(4, 5f)
                    .effect(() -> new MobEffectInstance(MobEffects.WITHER, 40, 1), 1.0f)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 280, 3), 1.0f)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 4800, 1), 1.0f)
                    .build())));
    public static final RegistryObject<ModFoodItem> AMERICAN_BURGER = ITEMS.register("american_burger",
            () -> new ModFoodItem(new Item.Properties().food(fp(8, 10f)
                    .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 1), 1.0f)
                    .build())));
    public static final RegistryObject<ModFoodItem> PASTA = food("pasta", 4, 5f);
    public static final RegistryObject<ModFoodItem> MISO_SOUP = food("miso_soup", 6, 7.5f);
    public static final RegistryObject<ModFoodItem> BARBECUE = food("barbecue", 5, 6f);
    public static final RegistryObject<ModFoodItem> BLACK_FOREST_GATEAU = food("black_forest_gateau", 5, 6f);
    public static final RegistryObject<ModFoodItem> BLACK_TEA_SANDWICH = food("black_tea_sandwich", 5, 6f);
    public static final RegistryObject<ModFoodItem> BLACK_TEA_SCONE = food("black_tea_scone", 5, 6f);
    public static final RegistryObject<ModFoodItem> BORSCHT = food("borscht", 5, 6f);
    public static final RegistryObject<ModFoodItem> BOUILLABAISSE = food("bouillabaisse", 5, 6f);
    public static final RegistryObject<ModFoodItem> DONGPO_PORK = food("dongpo_pork", 5, 6f);
    public static final RegistryObject<ModFoodItem> DOUBLE_SHELL_AMERICAN_BURGER = food("double_shell_american_burger", 5, 6f);
    public static final RegistryObject<ModFoodItem> EGGS_BENEDICT = food("eggs_benedict", 5, 6f);
    public static final RegistryObject<ModFoodItem> FRIED_FISH_MISO_SOUP = food("fried_fish_miso_soup", 5, 6f);
    public static final RegistryObject<ModFoodItem> MUSSOLINIS_OO = food("mussolinis_oo", 5, 6f);
    public static final RegistryObject<ModFoodItem> SCHWEINSHAXE = food("schweinshaxe", 5, 6f);
    public static final RegistryObject<ModFoodItem> SALAMI_PIZZA = food("salami_pizza", 5, 6f);
    public static final RegistryObject<ModFoodItem> OKROSHKA = food("okroshka", 5, 6f);
    public static final RegistryObject<ModFoodItem> ROYAL_NAVAL_SALTED_BEEF = food("royal_naval_salted_beef", 5, 6f);
    public static final RegistryObject<ModFoodItem> RUSSIAN_DUMPLING = food("russian_dumpling", 5, 6f);
    public static final RegistryObject<ModFoodItem> SOBA_NOODLE = food("soba_noodle", 5, 6f);
    public static final RegistryObject<ModFoodItem> TARTE_TATIN = food("tarte_tatin", 5, 6f);
    public static final RegistryObject<ModFoodItem> TEMPURA_SOBA_NOODLE = food("tempura_soba_noodle", 5, 6f);
    public static final RegistryObject<ModFoodItem> THURINGER_ROSTBRATWURST_UND_BIER = food("thuringer_rostbratwurst_und_bier", 5, 6f);
    public static final RegistryObject<ModFoodItem> THURINGER_ROSTBRATWURST = food("thuringer_rostbratwurst", 5, 6f);
    public static final RegistryObject<ModFoodItem> TRIPLE_SHELL_AMERICAN_BURGER = food("triple_shell_american_burger", 5, 6f);
    public static final RegistryObject<ModFoodItem> VENICE_CUTTLEFISH_NOODLES = food("venice_cuttlefish_noodles", 5, 6f);
    public static final RegistryObject<ModFoodItem> WEISSWURST_MIT_DER_BAGEL = food("weisswurst_mit_der_bagel", 5, 6f);
    public static final RegistryObject<ModFoodItem> YORKSHIRE_PUDDING = food("yorkshire_pudding", 5, 6f);

    /** 龙田烧 — 装填加速 I × 180s */
    public static final RegistryObject<ModFoodItem> CHICKEN_TATSUTA = ITEMS.register("chicken_tatsuta",
            () -> new ModFoodItem(new Item.Properties().food(fp(6, 7.5f)
                    .effect(() -> new MobEffectInstance(ModMobEffects.RELOAD_BOOST.get(), 3600, 0), 1.0f)
                    .build())));
    /** 鱼雷果汁 — 饥饿 II + 装填加速 II + 抗火 I */
    public static final RegistryObject<BottleFoodItem> TORPEDO_JUICE = ITEMS.register("torpedo_juice",
            () -> new BottleFoodItem(new Item.Properties().food(fp(3, 3.8f)
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 3600, 1), 1.0f)
                    .effect(() -> new MobEffectInstance(ModMobEffects.RELOAD_BOOST.get(), 6000, 1), 1.0f)
                    .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), 1.0f)
                    .build())));
    /** 炸鱼天妇罗 — 高速规避 I × 180s */
    public static final RegistryObject<ModFoodItem> TEMPURA = ITEMS.register("tempura",
            () -> new ModFoodItem(new Item.Properties().food(fp(6, 7.5f)
                    .effect(() -> new MobEffectInstance(ModMobEffects.EVASION.get(), 3600, 0), 1.0f)
                    .build())));
    /** 格瓦斯 — 缓慢 I + 缓降 I + 高速规避 II × 120s */
    public static final RegistryObject<BottleFoodItem> KVASS = ITEMS.register("kvass",
            () -> new BottleFoodItem(new Item.Properties().food(fp(4, 5.0f)
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2400, 0), 1.0f)
                    .effect(() -> new MobEffectInstance(MobEffects.SLOW_FALLING, 2400, 0), 1.0f)
                    .effect(() -> new MobEffectInstance(ModMobEffects.EVASION.get(), 2400, 1), 1.0f)
                    .build())));
    /** Taptap冰激凌 — 幸运 I 5min + 抗火 I 2min */
    public static final RegistryObject<ModFoodItem> TAPTAP_ICE_CREAM = ITEMS.register("taptap_ice_cream",
            () -> new ModFoodItem(new Item.Properties().food(fp(1, 1f)
                    .effect(() -> new MobEffectInstance(MobEffects.LUCK, 6000, 0), 1.0f)
                    .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2400, 0), 1.0f)
                    .build())));
    /** 合味道 — 生命提升 II × 20min */
    public static final RegistryObject<ModFoodItem> HE_WEI_DAO = ITEMS.register("he_wei_dao",
            () -> new ModFoodItem(new Item.Properties().food(fp(12, 16f)
                    .effect(() -> new MobEffectInstance(MobEffects.HEALTH_BOOST, 24000, 1), 1.0f)
                    .build())));
    public static final RegistryObject<ModFoodItem> EGG_SANDWICH = food("egg_sandwich", 5, 6.3f);
    public static final RegistryObject<ModFoodItem> BACON_SANDWICH = food("bacon_sandwich", 6, 7.5f);
    /** 切片萨拉米披萨 — 力量 I × 2min */
    public static final RegistryObject<ModFoodItem> SALAMI_PIZZA_PIECES = ITEMS.register("salami_pizza_pieces",
            () -> new ModFoodItem(new Item.Properties().food(fp(3, 3.8f)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2400, 0), 1.0f)
                    .build())));
    /** 番茄肉酱意面 — 速度 I × 3min */
    public static final RegistryObject<ModFoodItem> BOLOGNESE_LINGUINE_RECIPE = ITEMS.register("bolognese_linguine_recipe",
            () -> new ModFoodItem(new Item.Properties().food(fp(8, 10f)
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 0), 1.0f)
                    .build())));

    // ===== Bottled Juice / Jam (Phase 28) — 食用后返还玻璃瓶 =====
    public static final RegistryObject<BottleFoodItem> APPLE_JUICE = ITEMS.register("apple_juice",
            () -> new BottleFoodItem(new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    public static final RegistryObject<BottleFoodItem> APPLE_JAM = ITEMS.register("apple_jam",
            () -> new BottleFoodItem(new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    public static final RegistryObject<BottleFoodItem> WATERMELON_JUICE = ITEMS.register("watermelon_juice",
            () -> new BottleFoodItem(new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    public static final RegistryObject<BottleFoodItem> WATERMELON_JAM = ITEMS.register("watermelon_jam",
            () -> new BottleFoodItem(new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    public static final RegistryObject<BottleFoodItem> CHORUS_FRUIT_JAM = ITEMS.register("chorus_fruit_jam",
            () -> new BottleFoodItem(new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));

    // ===== Aircraft Items (Phase 5 placeholders) =====
    public static final RegistryObject<Item> F4F_WILDCAT    = single("f4f_wildcat");
    public static final RegistryObject<Item> SBD_DAUNTLESS  = single("sbd_dauntless");
    public static final RegistryObject<Item> TBF_TORPEDO    = single("tbf_torpedo");
    public static final RegistryObject<Item> B25_BOMBER     = single("b25_bomber");
    public static final RegistryObject<Item> TBF_ASW        = single("tbf_asw");

    // ---- helpers ----

    private static FoodProperties.Builder fp(int nutrition, float saturation) {
        return new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationMod(saturation / Math.max(1, nutrition * 2f))
                .alwaysEat();
    }

    private static RegistryObject<ModFoodItem> food(String name, int nutrition, float saturation) {
        return ITEMS.register(name,
                () -> new ModFoodItem(new Item.Properties().food(fp(nutrition, saturation).build())));
    }

    private static RegistryObject<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> single(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(1)));
    }

    private static RegistryObject<Item> stack16(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(16)));
    }

    private static RegistryObject<Item> durable(String name, int durability) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(1).durability(durability)));
    }

    static RegistryObject<Item> register(String name, Supplier<Item> factory) {
        return ITEMS.register(name, factory);
    }

    private ModItems() {}
}
