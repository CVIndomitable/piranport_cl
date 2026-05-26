package com.piranport.registry.items;

import com.piranport.component.FuelData;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipType;
import com.piranport.item.SkinCoreItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 舰装核心注册工厂 — ShipCoreItem + SkinCoreItem。
 */
public class ShipCoreItems {

    private ShipCoreItems() {}

    // ===== Ship Cores =====
    public static DeferredItem<ShipCoreItem> createSmallShipCore(DeferredRegister.Items registry) {
        return registry.register("small_ship_core",
                () -> new ShipCoreItem(new Item.Properties().stacksTo(1)
                        .component(ModDataComponents.SHIP_CORE_FUEL.get(),
                                new FuelData(0, ShipType.SMALL.fuelCapacity)),
                        ShipType.SMALL));
    }

    public static DeferredItem<ShipCoreItem> createMediumShipCore(DeferredRegister.Items registry) {
        return registry.register("medium_ship_core",
                () -> new ShipCoreItem(new Item.Properties().stacksTo(1)
                        .component(ModDataComponents.SHIP_CORE_FUEL.get(),
                                new FuelData(0, ShipType.MEDIUM.fuelCapacity)),
                        ShipType.MEDIUM));
    }

    public static DeferredItem<ShipCoreItem> createLargeShipCore(DeferredRegister.Items registry) {
        return registry.register("large_ship_core",
                () -> new ShipCoreItem(new Item.Properties().stacksTo(1)
                        .component(ModDataComponents.SHIP_CORE_FUEL.get(),
                                new FuelData(0, ShipType.LARGE.fuelCapacity)),
                        ShipType.LARGE));
    }

    public static DeferredItem<ShipCoreItem> createSubmarineCore(DeferredRegister.Items registry) {
        return registry.register("submarine_core",
                () -> new ShipCoreItem(new Item.Properties().stacksTo(1)
                        .component(ModDataComponents.SHIP_CORE_FUEL.get(),
                                new FuelData(0, ShipType.SUBMARINE.fuelCapacity)),
                        ShipType.SUBMARINE));
    }

    // ===== Skin Cores =====
    public static DeferredItem<SkinCoreItem> createSkinCore(DeferredRegister.Items registry) {
        return registry.register("skin_core",
                () -> new SkinCoreItem(new Item.Properties().stacksTo(1)));
    }
}
