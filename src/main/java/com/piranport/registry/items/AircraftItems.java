package com.piranport.registry.items;

import com.piranport.component.AircraftInfo;
import com.piranport.component.WeaponCategory;
import com.piranport.item.AircraftItem;
import com.piranport.item.AmmoItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 飞机物品注册工厂 — AircraftItem + 航空弹药 + 浮靶。
 */
public class AircraftItems {

    private AircraftItems() {}

    // ===== Aircraft Squadrons (Phase 18) =====
    public static DeferredItem<AircraftItem> createF4fWildcat(DeferredRegister.Items registry) {
        return registry.register("f4f_wildcat",
                () -> new AircraftItem(new Item.Properties().stacksTo(1), AircraftInfo.builder()
                        .type("fighter").speed(0.6f).range(40).maxFuel(12000).damage(4).health(12)
                        .build()));
    }

    public static DeferredItem<AircraftItem> createF6fHellcat(DeferredRegister.Items registry) {
        return registry.register("f6f_hellcat",
                () -> new AircraftItem(new Item.Properties().stacksTo(1), AircraftInfo.builder()
                        .type("fighter").speed(0.65f).range(50).maxFuel(15000).damage(5).health(16)
                        .build()));
    }

    public static DeferredItem<AircraftItem> createB25Mitchell(DeferredRegister.Items registry) {
        return registry.register("b25_mitchell",
                () -> new AircraftItem(new Item.Properties().stacksTo(1), AircraftInfo.builder()
                        .type("bomber").speed(0.5f).range(60).maxFuel(18000).damage(12).health(20)
                        .build()));
    }

    public static DeferredItem<AircraftItem> createTbfAvenger(DeferredRegister.Items registry) {
        return registry.register("tbf_avenger",
                () -> new AircraftItem(new Item.Properties().stacksTo(1), AircraftInfo.builder()
                        .type("torpedo_bomber").speed(0.45f).range(50).maxFuel(15000).damage(8).health(18)
                        .build()));
    }

    public static DeferredItem<AircraftItem> createSbdDauntless(DeferredRegister.Items registry) {
        return registry.register("sbd_dauntless",
                () -> new AircraftItem(new Item.Properties().stacksTo(1), AircraftInfo.builder()
                        .type("dive_bomber").speed(0.55f).range(45).maxFuel(14000).damage(10).health(14)
                        .build()));
    }

    // ===== Named Aircraft =====
    public static DeferredItem<AircraftItem> createF4fLtCdrButch(DeferredRegister.Items registry) {
        return registry.register("f4f_ltcdr_butch",
                () -> new AircraftItem(new Item.Properties().stacksTo(1), AircraftInfo.builder()
                        .type("fighter").speed(0.65f).range(50).maxFuel(15000).damage(7).health(20)
                        .build()));
    }

    public static DeferredItem<AircraftItem> createB25RupturedDuck(DeferredRegister.Items registry) {
        return registry.register("b25_ruptured_duck",
                () -> new AircraftItem(new Item.Properties().stacksTo(1), AircraftInfo.builder()
                        .type("bomber").speed(0.55f).range(70).maxFuel(20000).damage(16).health(24)
                        .build()));
    }

    public static DeferredItem<AircraftItem> createB25Tondelayo(DeferredRegister.Items registry) {
        return registry.register("b25_tondelayo",
                () -> new AircraftItem(new Item.Properties().stacksTo(1), AircraftInfo.builder()
                        .type("bomber").speed(0.55f).range(70).maxFuel(20000).damage(14).health(22)
                        .build()));
    }

    public static DeferredItem<AircraftItem> createTbfCdrGill(DeferredRegister.Items registry) {
        return registry.register("tbf_cdr_gill",
                () -> new AircraftItem(new Item.Properties().stacksTo(1), AircraftInfo.builder()
                        .type("torpedo_bomber").speed(0.5f).range(60).maxFuel(18000).damage(10).health(22)
                        .build()));
    }

    // ===== Aviation Ammo (Phase 18) =====
    public static DeferredItem<AmmoItem> createAviationFuelTank(DeferredRegister.Items registry) {
        return registry.register("aviation_fuel_tank",
                () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.fuel_tank"));
    }

    public static DeferredItem<AmmoItem> createSmallBomb(DeferredRegister.Items registry) {
        return registry.register("small_bomb",
                () -> new AmmoItem(new Item.Properties().stacksTo(16)));
    }

    public static DeferredItem<AmmoItem> createMediumBomb(DeferredRegister.Items registry) {
        return registry.register("medium_bomb",
                () -> new AmmoItem(new Item.Properties().stacksTo(16)));
    }

    public static DeferredItem<AmmoItem> createLargeBomb(DeferredRegister.Items registry) {
        return registry.register("large_bomb",
                () -> new AmmoItem(new Item.Properties().stacksTo(16)));
    }

    // ===== Phase 19: Floating Target =====
    public static DeferredItem<Item> createFloatingTarget(DeferredRegister.Items registry) {
        return registry.register("floating_target",
                () -> new Item(new Item.Properties().stacksTo(1)));
    }
}
