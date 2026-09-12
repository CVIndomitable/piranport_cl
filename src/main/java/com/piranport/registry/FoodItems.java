package com.piranport.registry;

import com.piranport.component.PlaceableInfo;
import com.piranport.item.BottleFoodItem;
import com.piranport.item.ModFoodItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 食物类物品注册中心。
 *
 * <p>共享 {@link ModItems#ITEMS} 同一个 DeferredRegister，避免重复注册。
 * 通过 {@link ModItems} 的同名静态字段以薄包装方式对外暴露，保持原有调用方不变。</p>
 *
 * <p>包含范围：
 * <ul>
 *   <li>Phase 11a：食物原材料（FLOUR/CHILI_POWDER/MISO/PIE_CRUST 等）</li>
 *   <li>Phase 11b：作物产出与种子（TOMATO/LETTUCE/TOMATO_SEEDS 等）</li>
 *   <li>Phase 16：食物菜品（TOAST_BREAD/BORSCHT/YOKAN 等）</li>
 *   <li>Phase 27：BUFF 食物（CHICKEN_TATSUTA/TORPEDO_JUICE/KVASS 等）</li>
 *   <li>Phase 28：舰娘食物扩展（LABLAB_BEAN/PEACH/CHORUS_TREE/WALNUT/APPLE_JUICE/TAPTAP_ICE_CREAM 等）</li>
 * </ul>
 */
public final class FoodItems {
    private FoodItems() {}

    /** 复用 ModItems 的 DeferredRegister，避免双注册。 */
    private static final DeferredRegister.Items ITEMS = ModItems.ITEMS;

    /** 内部使用：食物营养/饱和度构造器，统一启用 alwaysEdible。 */
    private static FoodProperties.Builder fp(int nutrition, float saturation) {
        return new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturation / (nutrition * 2f))
                .alwaysEdible();
    }

    // ===== Food Ingredients (Phase 11a) =====
    public static final DeferredItem<Item> FLOUR           = ITEMS.registerSimpleItem("flour");
    public static final DeferredItem<Item> RICE_FLOUR      = ITEMS.registerSimpleItem("rice_flour");
    public static final DeferredItem<Item> CHILI_POWDER    = ITEMS.registerSimpleItem("chili_powder");
    public static final DeferredItem<Item> PORK_PASTE      = ITEMS.registerSimpleItem("pork_paste");
    public static final DeferredItem<Item> EDIBLE_OIL      = ITEMS.registerSimpleItem("edible_oil");
    public static final DeferredItem<Item> BUTTER          = ITEMS.registerSimpleItem("butter");
    public static final DeferredItem<Item> CREAM           = ITEMS.registerSimpleItem("cream");
    public static final DeferredItem<Item> SOYBEAN_MILK    = ITEMS.registerSimpleItem("soybean_milk");
    public static final DeferredItem<Item> TOFU            = ITEMS.registerSimpleItem("tofu");
    public static final DeferredItem<Item> CHEESE          = ITEMS.registerSimpleItem("cheese");
    public static final DeferredItem<Item> YEAST           = ITEMS.registerSimpleItem("yeast");
    public static final DeferredItem<Item> SOY_SAUCE       = ITEMS.registerSimpleItem("soy_sauce");
    public static final DeferredItem<Item> VINEGAR         = ITEMS.registerSimpleItem("vinegar");
    public static final DeferredItem<Item> COOKING_WINE    = ITEMS.registerSimpleItem("cooking_wine");
    public static final DeferredItem<Item> MISO            = ITEMS.registerSimpleItem("miso");
    public static final DeferredItem<Item> BRINE           = ITEMS.registerSimpleItem("brine");
    public static final DeferredItem<Item> PIE_CRUST       = ITEMS.registerSimpleItem("pie_crust");
    public static final DeferredItem<Item> RAW_PASTA       = ITEMS.registerSimpleItem("raw_pasta");
    public static final DeferredItem<Item> FERMENTED_FISH  = ITEMS.registerSimpleItem("fermented_fish");
    public static final DeferredItem<Item> PIZZA_BASE      = ITEMS.registerSimpleItem("pizza_base");
    public static final DeferredItem<Item> GYPSUM_CHIP     = ITEMS.registerSimpleItem("gypsum_chip");
    public static final DeferredItem<Item> QUICKLIME       = ITEMS.registerSimpleItem("quicklime");

    // ===== Crop Produce (Phase 11b) =====
    public static final DeferredItem<Item> TOMATO  = ITEMS.registerSimpleItem("tomato");
    public static final DeferredItem<Item> SOYBEAN = ITEMS.registerSimpleItem("soybean");
    public static final DeferredItem<Item> CHILI   = ITEMS.registerSimpleItem("chili");
    public static final DeferredItem<Item> LETTUCE = ITEMS.registerSimpleItem("lettuce");
    public static final DeferredItem<Item> RICE    = ITEMS.registerSimpleItem("rice");
    public static final DeferredItem<Item> ONION   = ITEMS.registerSimpleItem("onion");
    public static final DeferredItem<Item> GARLIC  = ITEMS.registerSimpleItem("garlic");

    // ===== Crop Seeds (Phase 11b) =====
    public static final DeferredItem<ItemNameBlockItem> TOMATO_SEEDS =
            ITEMS.register("tomato_seeds", () -> new ItemNameBlockItem(
                    ModBlocks.TOMATO_CROP.get(), new Item.Properties()));
    public static final DeferredItem<ItemNameBlockItem> SOYBEAN_SEEDS =
            ITEMS.register("soybean_seeds", () -> new ItemNameBlockItem(
                    ModBlocks.SOYBEAN_CROP.get(), new Item.Properties()));
    public static final DeferredItem<ItemNameBlockItem> CHILI_SEEDS =
            ITEMS.register("chili_seeds", () -> new ItemNameBlockItem(
                    ModBlocks.CHILI_CROP.get(), new Item.Properties()));
    public static final DeferredItem<ItemNameBlockItem> ONION_SEEDS =
            ITEMS.register("onion_seeds", () -> new ItemNameBlockItem(
                    ModBlocks.ONION_CROP.get(), new Item.Properties()));
    public static final DeferredItem<ItemNameBlockItem> RICE_SEEDS =
            ITEMS.register("rice_seeds", () -> new ItemNameBlockItem(
                    ModBlocks.RICE_CROP.get(), new Item.Properties()));
    public static final DeferredItem<ItemNameBlockItem> LETTUCE_SEEDS =
            ITEMS.register("lettuce_seeds", () -> new ItemNameBlockItem(
                    ModBlocks.LETTUCE_CROP.get(), new Item.Properties()));
    public static final DeferredItem<ItemNameBlockItem> GARLIC_SEEDS =
            ITEMS.register("garlic_seeds", () -> new ItemNameBlockItem(
                    ModBlocks.GARLIC_CROP.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WILD_GARDEN =
            ITEMS.registerSimpleBlockItem(ModBlocks.WILD_GARDEN);

    // ===== Food Items (Phase 16) =====
    public static final DeferredItem<ModFoodItem> TOAST_BREAD = ITEMS.register("toast_bread",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(15, 18.8f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));

    public static final DeferredItem<ModFoodItem> NAVAL_BAKED_BEANS = ITEMS.register("naval_baked_beans",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(4, 5f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> LATIAO = ITEMS.register("latiao",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(2, 2.5f)
                            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 2), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> MAPO_TOFU = ITEMS.register("mapo_tofu",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(4, 5f)
                            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 1), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));

    public static final DeferredItem<ModFoodItem> NAVAL_CURRY = ITEMS.register("naval_curry",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6.3f)
                            .effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 4800, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));

    public static final DeferredItem<ModFoodItem> FRIED_FISH_AND_CHIPS = ITEMS.register("fried_fish_and_chips",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6.3f)
                            .effect(() -> new MobEffectInstance(MobEffects.JUMP, 3600, 1), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> SCONE = ITEMS.register("scone",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(3, 3.8f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 4))));

    public static final DeferredItem<ModFoodItem> APPLE_PIE = ITEMS.register("apple_pie",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> ASSORTED_CHAR_SIU_FRIED_RICE = ITEMS.register("assorted_char_siu_fried_rice",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> SALTED_EGG_TOFU = ITEMS.register("salted_egg_tofu",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(3, 3.8f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 1))));

    public static final DeferredItem<ModFoodItem> SURSTROMMING = ITEMS.register("surstromming",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(4, 5f)
                            .effect(() -> new MobEffectInstance(MobEffects.WITHER, 40, 1), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 280, 3), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 4800, 1), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> AMERICAN_BURGER = ITEMS.register("american_burger",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(8, 10f)
                            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 1), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<Item> HOTDOG = ITEMS.register("hotdog",
            () -> new Item(new Item.Properties()
                    .food(fp(4, 5f)
                            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0), 1.0f)
                            .build())));

    public static final DeferredItem<ModFoodItem> PASTA = ITEMS.register("pasta",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(4, 5f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> COOKED_RICE = ITEMS.register("cooked_rice",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6.3f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> BEET_BLOSSOM = ITEMS.register("beet_blossom",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(3, 3.8f)
                            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 0), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 1))));

    public static final DeferredItem<ModFoodItem> MISO_SOUP = ITEMS.register("miso_soup",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(6, 7.5f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> BARBECUE = ITEMS.register("barbecue",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> BLACK_FOREST_GATEAU = ITEMS.register("black_forest_gateau",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("cake", 3))));

    public static final DeferredItem<ModFoodItem> BLACK_TEA_SANDWICH = ITEMS.register("black_tea_sandwich",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> BLACK_TEA_SCONE = ITEMS.register("black_tea_scone",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> BORSCHT = ITEMS.register("borscht",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> BOUILLABAISSE = ITEMS.register("bouillabaisse",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> DELUXE_BAOZI = ITEMS.register("deluxe_baozi",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> DONGPO_PORK = ITEMS.register("dongpo_pork",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> DOUBLE_SHELL_AMERICAN_BURGER = ITEMS.register("double_shell_american_burger",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> EGGS_BENEDICT = ITEMS.register("eggs_benedict",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> FRIED_FISH_MISO_SOUP = ITEMS.register("fried_fish_miso_soup",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> MACARON = ITEMS.register("macaron",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> MUSSOLINIS_OO = ITEMS.register("mussolinis_oo",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> NEW_RYE_BREAD = ITEMS.register("new_rye_bread",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));

    public static final DeferredItem<ModFoodItem> SCHWEINSHAXE = ITEMS.register("schweinshaxe",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> SALAMI_PIZZA = ITEMS.register("salami_pizza",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> RYE_BREAD = ITEMS.register("rye_bread",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> OKROSHKA = ITEMS.register("okroshka",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> PEA_SOUP_WITH_RYE_BREAD = ITEMS.register("pea_soup_with_rye_bread",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> ROYAL_NAVAL_SALTED_BEEF = ITEMS.register("royal_naval_salted_beef",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> RUSSIAN_DUMPLING = ITEMS.register("russian_dumpling",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> SOBA_NOODLE = ITEMS.register("soba_noodle",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> TANGYUAN = ITEMS.register("tangyuan",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> TARTE_TATIN = ITEMS.register("tarte_tatin",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> TEMPURA_SOBA_NOODLE = ITEMS.register("tempura_soba_noodle",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> THURINGER_ROSTBRATWURST_UND_BIER = ITEMS.register("thuringer_rostbratwurst_und_bier",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> THURINGER_ROSTBRATWURST = ITEMS.register("thuringer_rostbratwurst",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> TRIPLE_SHELL_AMERICAN_BURGER = ITEMS.register("triple_shell_american_burger",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> VENICE_CUTTLEFISH_NOODLES = ITEMS.register("venice_cuttlefish_noodles",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> WEISSWURST_MIT_DER_BAGEL = ITEMS.register("weisswurst_mit_der_bagel",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> YOKAN = ITEMS.register("yokan",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> YORKSHIRE_PUDDING = ITEMS.register("yorkshire_pudding",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    // ===== Phase 27: Buff foods =====

    /** 龙田烧 — 装填加速 I × 180s; plate × 2 */
    public static final DeferredItem<ModFoodItem> CHICKEN_TATSUTA = ITEMS.register("chicken_tatsuta",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(6, 7.5f)
                            .effect(() -> new MobEffectInstance(ModMobEffects.RELOAD_BOOST, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    /** 鱼雷果汁 — 饥饿 II 180s + 装填加速 II 300s + 抗火 I 300s; 食用后返还玻璃瓶 */
    public static final DeferredItem<BottleFoodItem> TORPEDO_JUICE = ITEMS.register("torpedo_juice",
            () -> new BottleFoodItem(new Item.Properties()
                    .food(fp(3, 3.8f)
                            .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 3600, 1), 1.0f)
                            .effect(() -> new MobEffectInstance(ModMobEffects.RELOAD_BOOST, 6000, 1), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), 1.0f)
                            .build())));

    /** 炸鱼天妇罗 — 高速规避 I × 180s; plate × 2 */
    public static final DeferredItem<ModFoodItem> TEMPURA = ITEMS.register("tempura",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(6, 7.5f)
                            .effect(() -> new MobEffectInstance(ModMobEffects.EVASION, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    /** 格瓦斯 — 缓慢 I + 缓降 I + 高速规避 II × 120s; 食用后返还玻璃瓶 */
    public static final DeferredItem<BottleFoodItem> KVASS = ITEMS.register("kvass",
            () -> new BottleFoodItem(new Item.Properties()
                    .food(fp(4, 5.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2400, 0), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.SLOW_FALLING, 2400, 0), 1.0f)
                            .effect(() -> new MobEffectInstance(ModMobEffects.EVASION, 2400, 1), 1.0f)
                            .build())));

    // ===== Phase 28: Shipgirl Food Expansion — Crops (produce) =====
    public static final DeferredItem<Item> LABLAB_BEAN =
            ITEMS.register("lablab_bean", () -> new Item(new Item.Properties().food(fp(1, 1.5f).build())));
    public static final DeferredItem<Item> ORMOSIA =
            ITEMS.register("ormosia", () -> new Item(new Item.Properties().food(fp(1, 1.5f).build())));
    public static final DeferredItem<Item> CELERY =
            ITEMS.register("celery", () -> new Item(new Item.Properties().food(fp(1, 1.5f).build())));
    public static final DeferredItem<Item> RYE =
            ITEMS.registerSimpleItem("rye");
    public static final DeferredItem<Item> PEACH =
            ITEMS.register("peach", () -> new Item(new Item.Properties().food(fp(1, 1.5f).build())));

    // ===== Phase 28: Seeds =====
    public static final DeferredItem<ItemNameBlockItem> LABLAB_BEAN_SEEDS =
            ITEMS.register("lablab_bean_seeds", () -> new ItemNameBlockItem(
                    ModBlocks.LABLAB_BEAN_CROP.get(), new Item.Properties()));
    public static final DeferredItem<ItemNameBlockItem> ORMOSIA_SEEDS =
            ITEMS.register("ormosia_seeds", () -> new ItemNameBlockItem(
                    ModBlocks.ORMOSIA_CROP.get(), new Item.Properties()));
    public static final DeferredItem<ItemNameBlockItem> CELERY_SEEDS =
            ITEMS.register("celery_seeds", () -> new ItemNameBlockItem(
                    ModBlocks.CELERY_CROP.get(), new Item.Properties()));
    public static final DeferredItem<ItemNameBlockItem> RYE_SEEDS =
            ITEMS.register("rye_seeds", () -> new ItemNameBlockItem(
                    ModBlocks.RYE_CROP.get(), new Item.Properties()));

    // ===== Phase 28: Fruit/Tree block items =====
    public static final DeferredItem<BlockItem> PEACH_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.PEACH_LOG);
    public static final DeferredItem<BlockItem> PEACH_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.PEACH_LEAVES);
    public static final DeferredItem<BlockItem> PEACH_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.PEACH_SAPLING);

    public static final DeferredItem<BlockItem> MAIDENHAIR_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.MAIDENHAIR_LOG);
    public static final DeferredItem<BlockItem> MAIDENHAIR_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.MAIDENHAIR_LEAVES);
    public static final DeferredItem<BlockItem> MAIDENHAIR_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.MAIDENHAIR_SAPLING);

    public static final DeferredItem<BlockItem> SAGO_PALM_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.SAGO_PALM_LOG);
    public static final DeferredItem<BlockItem> SAGO_PALM_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.SAGO_PALM_LEAVES);
    public static final DeferredItem<BlockItem> SAGO_PALM_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.SAGO_PALM_SAPLING);

    public static final DeferredItem<BlockItem> GARDENIA_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.GARDENIA_LOG);
    public static final DeferredItem<BlockItem> GARDENIA_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.GARDENIA_LEAVES);
    public static final DeferredItem<BlockItem> GARDENIA_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.GARDENIA_SAPLING);

    public static final DeferredItem<BlockItem> CHINESE_PLUM_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.CHINESE_PLUM_LOG);
    public static final DeferredItem<BlockItem> CHINESE_PLUM_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.CHINESE_PLUM_LEAVES);
    public static final DeferredItem<BlockItem> CHINESE_PLUM_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.CHINESE_PLUM_SAPLING);

    public static final DeferredItem<BlockItem> MAPPLE_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.MAPPLE_LOG);
    public static final DeferredItem<BlockItem> MAPPLE_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.MAPPLE_LEAVES);
    public static final DeferredItem<BlockItem> MAPPLE_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.MAPPLE_SAPLING);

    public static final DeferredItem<BlockItem> CHORUS_TREE_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.CHORUS_TREE_LOG);
    public static final DeferredItem<BlockItem> CHORUS_TREE_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.CHORUS_TREE_LEAVES);
    public static final DeferredItem<BlockItem> CHORUS_TREE_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.CHORUS_TREE_SAPLING);

    public static final DeferredItem<BlockItem> SLIME_TREE_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.SLIME_TREE_LOG);
    public static final DeferredItem<BlockItem> SLIME_TREE_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.SLIME_TREE_LEAVES);
    public static final DeferredItem<BlockItem> SLIME_TREE_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.SLIME_TREE_SAPLING);

    public static final DeferredItem<BlockItem> LAVA_SLIME_TREE_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.LAVA_SLIME_TREE_LOG);
    public static final DeferredItem<BlockItem> LAVA_SLIME_TREE_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.LAVA_SLIME_TREE_LEAVES);
    public static final DeferredItem<BlockItem> LAVA_SLIME_TREE_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.LAVA_SLIME_TREE_SAPLING);

    // ===== Phase 28: New Ingredients/Condiments =====
    public static final DeferredItem<Item> MILK_ICE_CREAM      = ITEMS.registerSimpleItem("milk_ice_cream");
    public static final DeferredItem<Item> WALNUT              = ITEMS.registerSimpleItem("walnut");
    public static final DeferredItem<Item> WALNUT_POWDER       = ITEMS.registerSimpleItem("walnut_powder");
    public static final DeferredItem<Item> EMBRYO_OF_APPLE_PIE = ITEMS.registerSimpleItem("embryo_of_apple_pie");
    public static final DeferredItem<Item> EMBRYO_OF_SALAMI_PIZZA = ITEMS.registerSimpleItem("embryo_of_salami_pizza");
    public static final DeferredItem<Item> BLACK_PEPPER        = ITEMS.registerSimpleItem("black_pepper");
    public static final DeferredItem<Item> WHITE_PEPPER        = ITEMS.registerSimpleItem("white_pepper");
    public static final DeferredItem<Item> CURRY_POWDER        = ITEMS.registerSimpleItem("curry_powder");
    public static final DeferredItem<Item> GINGER              = ITEMS.registerSimpleItem("ginger");
    public static final DeferredItem<Item> BLACK_TEA           = ITEMS.registerSimpleItem("black_tea");
    public static final DeferredItem<Item> SALAMI              = ITEMS.registerSimpleItem("salami");
    public static final DeferredItem<Item> SLICED_SALAMI       = ITEMS.registerSimpleItem("sliced_salami");
    public static final DeferredItem<Item> ALMOND              = ITEMS.registerSimpleItem("almond");
    public static final DeferredItem<Item> ALMOND_POWDER       = ITEMS.registerSimpleItem("almond_powder");
    public static final DeferredItem<Item> WOODEN_BOWL         = ITEMS.registerSimpleItem("wooden_bowl");
    public static final DeferredItem<Item> WOODEN_BARREL       = ITEMS.registerSimpleItem("wooden_barrel");

    // ===== Phase 28: Intermediate Products =====
    public static final DeferredItem<Item> BEANS_CAN           = ITEMS.register("beans_can",
            () -> new Item(new Item.Properties().food(fp(4, 5f).build())));
    public static final DeferredItem<Item> CATCHUP             = ITEMS.register("catchup",
            () -> new Item(new Item.Properties().food(fp(1, 1.5f).build())));
    public static final DeferredItem<Item> BOLOGNESE           = ITEMS.register("bolognese",
            () -> new Item(new Item.Properties().food(fp(3, 3.8f).build())));
    public static final DeferredItem<Item> BAGEL               = ITEMS.register("bagel",
            () -> new Item(new Item.Properties().food(fp(5, 6.3f).build())));
    public static final DeferredItem<Item> LABLAB_SOUP         = ITEMS.register("lablab_soup",
            () -> new Item(new Item.Properties().stacksTo(16).food(fp(4, 5f).build())));
    public static final DeferredItem<Item> ROAST_PASTRY_OF_PIE = ITEMS.register("roast_pastry_of_pie",
            () -> new Item(new Item.Properties().food(fp(3, 3.8f).build())));

    // ===== Phase 28: Juices and Jams (bottled, returns glass bottle) =====
    public static final DeferredItem<BottleFoodItem> APPLE_JUICE =
            ITEMS.register("apple_juice", () -> new BottleFoodItem(
                    new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    public static final DeferredItem<BottleFoodItem> APPLE_JAM =
            ITEMS.register("apple_jam", () -> new BottleFoodItem(
                    new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    public static final DeferredItem<BottleFoodItem> WATERMELON_JUICE =
            ITEMS.register("watermelon_juice", () -> new BottleFoodItem(
                    new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    public static final DeferredItem<BottleFoodItem> WATERMELON_JAM =
            ITEMS.register("watermelon_jam", () -> new BottleFoodItem(
                    new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    public static final DeferredItem<BottleFoodItem> PINEAPPLE_JAM =
            ITEMS.register("pineapple_jam", () -> new BottleFoodItem(
                    new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    public static final DeferredItem<BottleFoodItem> CHORUS_FRUIT_JAM =
            ITEMS.register("chorus_fruit_jam", () -> new BottleFoodItem(
                    new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));

    // ===== Phase 28: New Dishes =====
    /** Taptap冰激凌 — 幸运 I 5min + 抗火 I 2min */
    public static final DeferredItem<ModFoodItem> TAPTAP_ICE_CREAM = ITEMS.register("taptap_ice_cream",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(1, 1f)
                            .effect(() -> new MobEffectInstance(MobEffects.LUCK, 6000, 0), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2400, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 1))));

    /** 合味道 — 生命提升 II × 20min，流浪商人购买 */
    public static final DeferredItem<ModFoodItem> HE_WEI_DAO = ITEMS.register("he_wei_dao",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(12, 16f)
                            .effect(() -> new MobEffectInstance(MobEffects.HEALTH_BOOST, 24000, 1), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));

    /** 咸豆花 — 力量 I + 水下呼吸 I × 3min，碗装 */
    public static final DeferredItem<ModFoodItem> SALTY_BEAN_CURD = ITEMS.register("salty_bean_curd",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(3, 3.8f)
                            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 0), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 1))));

    /** 盘装皇家海军咸牛肉 — 生命提升 I × 3min，碗右键方块形式获得 */
    public static final DeferredItem<ModFoodItem> PLATED_ROYAL_NAVAL_SALTED_BEEF = ITEMS.register("plated_royal_naval_salted_beef",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(8, 10f)
                            .effect(() -> new MobEffectInstance(MobEffects.HEALTH_BOOST, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 1))));

    /** 仰望星空派 — 凋零 II 2s + 反胃 IV 14s + 急迫 III 40s */
    public static final DeferredItem<ModFoodItem> STARGAZY_PIE = ITEMS.register("stargazy_pie",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(6, 7.5f)
                            .effect(() -> new MobEffectInstance(MobEffects.WITHER, 40, 1), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 280, 3), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 800, 2), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));

    /** 鸡蛋三明治 */
    public static final DeferredItem<ModFoodItem> EGG_SANDWICH = ITEMS.register("egg_sandwich",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6.3f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    /** 培根三明治 */
    public static final DeferredItem<ModFoodItem> BACON_SANDWICH = ITEMS.register("bacon_sandwich",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(6, 7.5f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    /** 切片萨拉米披萨 — 力量 I × 2min */
    public static final DeferredItem<ModFoodItem> SALAMI_PIZZA_PIECES = ITEMS.register("salami_pizza_pieces",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(3, 3.8f)
                            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2400, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 1))));

    /** 番茄肉酱意面 — 速度 I × 3min */
    public static final DeferredItem<ModFoodItem> BOLOGNESE_LINGUINE_RECIPE = ITEMS.register("bolognese_linguine_recipe",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(8, 10f)
                            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
}