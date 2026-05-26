package com.piranport.registry.items;

import com.piranport.component.PlaceableInfo;
import com.piranport.item.BottleFoodItem;
import com.piranport.item.ModFoodItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 食物物品注册工厂 — 食材、半成品、成品菜肴、buff食物。
 */
public class FoodItems {

    private FoodItems() {}

    private static FoodProperties.Builder fp(int nutrition, float saturation) {
        return new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturation / (nutrition * 2f))
                .alwaysEdible();
    }

    // ===== Food Ingredients (Phase 11a) =====
    public static DeferredItem<Item> createFlour(DeferredRegister.Items registry) { return registry.registerSimpleItem("flour"); }
    public static DeferredItem<Item> createRiceFlour(DeferredRegister.Items registry) { return registry.registerSimpleItem("rice_flour"); }
    public static DeferredItem<Item> createChiliPowder(DeferredRegister.Items registry) { return registry.registerSimpleItem("chili_powder"); }
    public static DeferredItem<Item> createPorkPaste(DeferredRegister.Items registry) { return registry.registerSimpleItem("pork_paste"); }
    public static DeferredItem<Item> createEdibleOil(DeferredRegister.Items registry) { return registry.registerSimpleItem("edible_oil"); }
    public static DeferredItem<Item> createButter(DeferredRegister.Items registry) { return registry.registerSimpleItem("butter"); }
    public static DeferredItem<Item> createCream(DeferredRegister.Items registry) { return registry.registerSimpleItem("cream"); }
    public static DeferredItem<Item> createSoybeanMilk(DeferredRegister.Items registry) { return registry.registerSimpleItem("soybean_milk"); }
    public static DeferredItem<Item> createTofu(DeferredRegister.Items registry) { return registry.registerSimpleItem("tofu"); }
    public static DeferredItem<Item> createCheese(DeferredRegister.Items registry) { return registry.registerSimpleItem("cheese"); }
    public static DeferredItem<Item> createYeast(DeferredRegister.Items registry) { return registry.registerSimpleItem("yeast"); }
    public static DeferredItem<Item> createSoySauce(DeferredRegister.Items registry) { return registry.registerSimpleItem("soy_sauce"); }
    public static DeferredItem<Item> createVinegar(DeferredRegister.Items registry) { return registry.registerSimpleItem("vinegar"); }
    public static DeferredItem<Item> createCookingWine(DeferredRegister.Items registry) { return registry.registerSimpleItem("cooking_wine"); }
    public static DeferredItem<Item> createMiso(DeferredRegister.Items registry) { return registry.registerSimpleItem("miso"); }
    public static DeferredItem<Item> createBrine(DeferredRegister.Items registry) { return registry.registerSimpleItem("brine"); }
    public static DeferredItem<Item> createPieCrust(DeferredRegister.Items registry) { return registry.registerSimpleItem("pie_crust"); }
    public static DeferredItem<Item> createRawPasta(DeferredRegister.Items registry) { return registry.registerSimpleItem("raw_pasta"); }
    public static DeferredItem<Item> createFermentedFish(DeferredRegister.Items registry) { return registry.registerSimpleItem("fermented_fish"); }
    public static DeferredItem<Item> createPizzaBase(DeferredRegister.Items registry) { return registry.registerSimpleItem("pizza_base"); }
    public static DeferredItem<Item> createGypsumChip(DeferredRegister.Items registry) { return registry.registerSimpleItem("gypsum_chip"); }
    public static DeferredItem<Item> createQuicklime(DeferredRegister.Items registry) { return registry.registerSimpleItem("quicklime"); }

    // ===== Crop Produce (Phase 11b) =====
    public static DeferredItem<Item> createTomato(DeferredRegister.Items registry) { return registry.registerSimpleItem("tomato"); }
    public static DeferredItem<Item> createSoybean(DeferredRegister.Items registry) { return registry.registerSimpleItem("soybean"); }
    public static DeferredItem<Item> createChili(DeferredRegister.Items registry) { return registry.registerSimpleItem("chili"); }
    public static DeferredItem<Item> createLettuce(DeferredRegister.Items registry) { return registry.registerSimpleItem("lettuce"); }
    public static DeferredItem<Item> createRice(DeferredRegister.Items registry) { return registry.registerSimpleItem("rice"); }
    public static DeferredItem<Item> createOnion(DeferredRegister.Items registry) { return registry.registerSimpleItem("onion"); }
    public static DeferredItem<Item> createGarlic(DeferredRegister.Items registry) { return registry.registerSimpleItem("garlic"); }

    // ===== Intermediate Products (Phase 13/16) =====
    public static DeferredItem<Item> createSausage(DeferredRegister.Items registry) { return registry.registerSimpleItem("sausage"); }
    public static DeferredItem<Item> createSlicedSausage(DeferredRegister.Items registry) { return registry.registerSimpleItem("sliced_sausage"); }
    public static DeferredItem<Item> createBacon(DeferredRegister.Items registry) { return registry.registerSimpleItem("bacon"); }
    public static DeferredItem<Item> createToastBreadSlices(DeferredRegister.Items registry) { return registry.registerSimpleItem("toast_bread_slices"); }
    public static DeferredItem<Item> createBeer(DeferredRegister.Items registry) { return registry.registerSimpleItem("beer"); }
    public static DeferredItem<Item> createRoundBun(DeferredRegister.Items registry) { return registry.registerSimpleItem("round_bun"); }

    // ===== Food Items (Phase 16) =====
    public static DeferredItem<ModFoodItem> createToastBread(DeferredRegister.Items registry) {
        return registry.register("toast_bread",
                () -> new ModFoodItem(new Item.Properties()
                        .food(fp(15, 18.8f).build())
                        .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));
    }

    public static DeferredItem<ModFoodItem> createNavalBakedBeans(DeferredRegister.Items registry) {
        return registry.register("naval_baked_beans",
                () -> new ModFoodItem(new Item.Properties()
                        .food(fp(4, 5f).build())
                        .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
    }

    public static DeferredItem<ModFoodItem> createLatiao(DeferredRegister.Items registry) {
        return registry.register("latiao", () -> new ModFoodItem(new Item.Properties()
                .food(fp(2, 2.5f)
                        .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 2), 1.0f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
    }

    public static DeferredItem<ModFoodItem> createMapoTofu(DeferredRegister.Items registry) {
        return registry.register("mapo_tofu", () -> new ModFoodItem(new Item.Properties()
                .food(fp(4, 5f)
                        .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 1), 1.0f)
                        .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600, 0), 1.0f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));
    }

    public static DeferredItem<ModFoodItem> createNavalCurry(DeferredRegister.Items registry) {
        return registry.register("naval_curry", () -> new ModFoodItem(new Item.Properties()
                .food(fp(5, 6.3f)
                        .effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 4800, 0), 1.0f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));
    }

    public static DeferredItem<ModFoodItem> createFriedFishAndChips(DeferredRegister.Items registry) {
        return registry.register("fried_fish_and_chips", () -> new ModFoodItem(new Item.Properties()
                .food(fp(5, 6.3f)
                        .effect(() -> new MobEffectInstance(MobEffects.JUMP, 3600, 1), 1.0f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
    }

    public static DeferredItem<ModFoodItem> createScone(DeferredRegister.Items registry) {
        return registry.register("scone", () -> new ModFoodItem(new Item.Properties()
                .food(fp(3, 3.8f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 4))));
    }

    public static DeferredItem<ModFoodItem> createApplePie(DeferredRegister.Items registry) {
        return registry.register("apple_pie", () -> new ModFoodItem(new Item.Properties()
                .food(fp(5, 6f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
    }

    public static DeferredItem<ModFoodItem> createCharSiuFriedRice(DeferredRegister.Items registry) {
        return registry.register("assorted_char_siu_fried_rice", () -> new ModFoodItem(new Item.Properties()
                .food(fp(5, 6f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
    }

    public static DeferredItem<ModFoodItem> createSaltedEggTofu(DeferredRegister.Items registry) {
        return registry.register("salted_egg_tofu", () -> new ModFoodItem(new Item.Properties()
                .food(fp(3, 3.8f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 1))));
    }

    public static DeferredItem<ModFoodItem> createSurstromming(DeferredRegister.Items registry) {
        return registry.register("surstromming", () -> new ModFoodItem(new Item.Properties()
                .food(fp(4, 5f)
                        .effect(() -> new MobEffectInstance(MobEffects.WITHER, 40, 1), 1.0f)
                        .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 280, 3), 1.0f)
                        .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 4800, 1), 1.0f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
    }

    public static DeferredItem<ModFoodItem> createAmericanBurger(DeferredRegister.Items registry) {
        return registry.register("american_burger", () -> new ModFoodItem(new Item.Properties()
                .food(fp(8, 10f)
                        .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 1), 1.0f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
    }

    public static DeferredItem<Item> createHotdog(DeferredRegister.Items registry) {
        return registry.register("hotdog", () -> new Item(new Item.Properties()
                .food(fp(4, 5f)
                        .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0), 1.0f).build())));
    }

    public static DeferredItem<ModFoodItem> createPasta(DeferredRegister.Items registry) {
        return registry.register("pasta", () -> new ModFoodItem(new Item.Properties()
                .food(fp(4, 5f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
    }

    public static DeferredItem<ModFoodItem> createCookedRice(DeferredRegister.Items registry) {
        return registry.register("cooked_rice", () -> new ModFoodItem(new Item.Properties()
                .food(fp(5, 6.3f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
    }

    public static DeferredItem<ModFoodItem> createBeetBlossom(DeferredRegister.Items registry) {
        return registry.register("beet_blossom", () -> new ModFoodItem(new Item.Properties()
                .food(fp(3, 3.8f)
                        .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 0), 1.0f)
                        .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 3600, 0), 1.0f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 1))));
    }

    public static DeferredItem<ModFoodItem> createMisoSoup(DeferredRegister.Items registry) {
        return registry.register("miso_soup", () -> new ModFoodItem(new Item.Properties()
                .food(fp(6, 7.5f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));
    }

    // ===== Phase 27: Buff foods =====
    public static DeferredItem<Item> createPineapple(DeferredRegister.Items registry) {
        return registry.register("pineapple",
                () -> new Item(new Item.Properties().food(fp(3, 3.8f).build())));
    }

    public static DeferredItem<Item> createPineappleCore(DeferredRegister.Items registry) {
        return registry.register("pineapple_core",
                () -> new Item(new Item.Properties()));
    }

    public static DeferredItem<ModFoodItem> createBarbecue(DeferredRegister.Items registry) {
        return registry.register("barbecue", () -> new ModFoodItem(new Item.Properties()
                .food(fp(5, 6f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
    }

    public static DeferredItem<ModFoodItem> createBlackForestGateau(DeferredRegister.Items registry) {
        return registry.register("black_forest_gateau", () -> new ModFoodItem(new Item.Properties()
                .food(fp(5, 6f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("cake", 3))));
    }

    // ===== Phase 28: New Ingredients/Condiments =====
    public static DeferredItem<Item> createMilkIceCream(DeferredRegister.Items registry) { return registry.registerSimpleItem("milk_ice_cream"); }
    public static DeferredItem<Item> createWalnut(DeferredRegister.Items registry) { return registry.registerSimpleItem("walnut"); }
    public static DeferredItem<Item> createWalnutPowder(DeferredRegister.Items registry) { return registry.registerSimpleItem("walnut_powder"); }
    public static DeferredItem<Item> createEmbryoOfApplePie(DeferredRegister.Items registry) { return registry.registerSimpleItem("embryo_of_apple_pie"); }
    public static DeferredItem<Item> createEmbryoOfSalamiPizza(DeferredRegister.Items registry) { return registry.registerSimpleItem("embryo_of_salami_pizza"); }
    public static DeferredItem<Item> createBlackPepper(DeferredRegister.Items registry) { return registry.registerSimpleItem("black_pepper"); }
    public static DeferredItem<Item> createWhitePepper(DeferredRegister.Items registry) { return registry.registerSimpleItem("white_pepper"); }
    public static DeferredItem<Item> createCurryPowder(DeferredRegister.Items registry) { return registry.registerSimpleItem("curry_powder"); }
    public static DeferredItem<Item> createGinger(DeferredRegister.Items registry) { return registry.registerSimpleItem("ginger"); }
    public static DeferredItem<Item> createBlackTea(DeferredRegister.Items registry) { return registry.registerSimpleItem("black_tea"); }
    public static DeferredItem<Item> createSalami(DeferredRegister.Items registry) { return registry.registerSimpleItem("salami"); }
    public static DeferredItem<Item> createSlicedSalami(DeferredRegister.Items registry) { return registry.registerSimpleItem("sliced_salami"); }
    public static DeferredItem<Item> createAlmond(DeferredRegister.Items registry) { return registry.registerSimpleItem("almond"); }
    public static DeferredItem<Item> createAlmondPowder(DeferredRegister.Items registry) { return registry.registerSimpleItem("almond_powder"); }
    public static DeferredItem<Item> createWoodenBowl(DeferredRegister.Items registry) { return registry.registerSimpleItem("wooden_bowl"); }
    public static DeferredItem<Item> createWoodenBarrel(DeferredRegister.Items registry) { return registry.registerSimpleItem("wooden_barrel"); }

    // ===== Phase 28: Intermediate Products =====
    public static DeferredItem<Item> createBeansCan(DeferredRegister.Items registry) {
        return registry.register("beans_can", () -> new Item(new Item.Properties().stacksTo(16)));
    }

    public static DeferredItem<Item> createCatchup(DeferredRegister.Items registry) {
        return registry.register("catchup", () -> new Item(new Item.Properties().stacksTo(16)));
    }

    public static DeferredItem<Item> createBolognese(DeferredRegister.Items registry) {
        return registry.register("bolognese", () -> new Item(new Item.Properties().stacksTo(16)));
    }

    public static DeferredItem<Item> createBagel(DeferredRegister.Items registry) {
        return registry.register("bagel", () -> new Item(new Item.Properties().stacksTo(16)));
    }

    // ===== Phase 28: Juices and Jams =====
    public static DeferredItem<BottleFoodItem> createAppleJuice(DeferredRegister.Items registry) {
        return registry.register("apple_juice", () -> new BottleFoodItem(new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    }

    public static DeferredItem<BottleFoodItem> createAppleJam(DeferredRegister.Items registry) {
        return registry.register("apple_jam", () -> new BottleFoodItem(new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    }

    public static DeferredItem<BottleFoodItem> createWatermelonJuice(DeferredRegister.Items registry) {
        return registry.register("watermelon_juice", () -> new BottleFoodItem(new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    }

    public static DeferredItem<BottleFoodItem> createWatermelonJam(DeferredRegister.Items registry) {
        return registry.register("watermelon_jam", () -> new BottleFoodItem(new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    }

    public static DeferredItem<BottleFoodItem> createPineappleJam(DeferredRegister.Items registry) {
        return registry.register("pineapple_jam", () -> new BottleFoodItem(new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    }

    public static DeferredItem<BottleFoodItem> createChorusFruitJam(DeferredRegister.Items registry) {
        return registry.register("chorus_fruit_jam", () -> new BottleFoodItem(new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    }

    // ===== Phase 28: New Dishes =====
    public static DeferredItem<ModFoodItem> createTaptapIceCream(DeferredRegister.Items registry) {
        return registry.register("taptap_ice_cream", () -> new ModFoodItem(new Item.Properties()
                .food(fp(1, 1f)
                        .effect(() -> new MobEffectInstance(MobEffects.LUCK, 6000, 0), 1.0f)
                        .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2400, 0), 1.0f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 1))));
    }

    public static DeferredItem<ModFoodItem> createHeWeiDao(DeferredRegister.Items registry) {
        return registry.register("he_wei_dao", () -> new ModFoodItem(new Item.Properties()
                .food(fp(12, 16f)
                        .effect(() -> new MobEffectInstance(MobEffects.HEALTH_BOOST, 24000, 1), 1.0f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));
    }

    public static DeferredItem<ModFoodItem> createSaltyBeanCurd(DeferredRegister.Items registry) {
        return registry.register("salty_bean_curd", () -> new ModFoodItem(new Item.Properties()
                .food(fp(3, 3.8f)
                        .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 0), 1.0f)
                        .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 3600, 0), 1.0f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 1))));
    }

    public static DeferredItem<ModFoodItem> createPlatedRoyalNavalSaltedBeef(DeferredRegister.Items registry) {
        return registry.register("plated_royal_naval_salted_beef", () -> new ModFoodItem(new Item.Properties()
                .food(fp(8, 10f)
                        .effect(() -> new MobEffectInstance(MobEffects.HEALTH_BOOST, 3600, 0), 1.0f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 1))));
    }

    public static DeferredItem<ModFoodItem> createStargazyPie(DeferredRegister.Items registry) {
        return registry.register("stargazy_pie", () -> new ModFoodItem(new Item.Properties()
                .food(fp(6, 7.5f)
                        .effect(() -> new MobEffectInstance(MobEffects.WITHER, 40, 1), 1.0f)
                        .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 280, 3), 1.0f)
                        .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 800, 2), 1.0f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));
    }

    public static DeferredItem<ModFoodItem> createEggSandwich(DeferredRegister.Items registry) {
        return registry.register("egg_sandwich", () -> new ModFoodItem(new Item.Properties()
                .food(fp(5, 6.3f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
    }

    public static DeferredItem<ModFoodItem> createBaconSandwich(DeferredRegister.Items registry) {
        return registry.register("bacon_sandwich", () -> new ModFoodItem(new Item.Properties()
                .food(fp(6, 7.5f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
    }

    public static DeferredItem<ModFoodItem> createSalamiPizzaPieces(DeferredRegister.Items registry) {
        return registry.register("salami_pizza_pieces", () -> new ModFoodItem(new Item.Properties()
                .food(fp(3, 3.8f)
                        .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2400, 0), 1.0f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 1))));
    }

    public static DeferredItem<ModFoodItem> createBologneseLinguine(DeferredRegister.Items registry) {
        return registry.register("bolognese_linguine_recipe", () -> new ModFoodItem(new Item.Properties()
                .food(fp(8, 10f)
                        .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 0), 1.0f).build())
                .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
    }
}
