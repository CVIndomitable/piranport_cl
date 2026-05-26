package com.piranport.registry.items;

import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.lobby.DungeonCompassItem;
import com.piranport.dungeon.lobby.DungeonLobbyItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 副本物品注册工厂 — 地牢钥匙、大厅物品、刷怪蛋、遗迹占位物品。
 */
public class DungeonItems {

    private DungeonItems() {}

    // ===== Dungeon System (v0.0.8) =====
    public static DeferredItem<DungeonLobbyItem> createDungeonLobby(DeferredRegister.Items registry) {
        return registry.register("dungeon_lobby",
                () -> new DungeonLobbyItem(new Item.Properties().stacksTo(1)));
    }

    public static DeferredItem<DungeonCompassItem> createDungeonCompass(DeferredRegister.Items registry) {
        return registry.register("dungeon_compass",
                () -> new DungeonCompassItem(new Item.Properties().stacksTo(1)));
    }

    public static DeferredItem<DungeonKeyItem> createDungeonKey(DeferredRegister.Items registry) {
        return registry.register("dungeon_key",
                () -> new DungeonKeyItem(new Item.Properties().stacksTo(1)));
    }

    // ===== v0.0.11 Ruins — Placeholder Items =====
    public static DeferredItem<Item> createAbyssalFragment(DeferredRegister.Items registry) {
        return registry.registerSimpleItem("abyssal_fragment", new Item.Properties().stacksTo(64));
    }

    public static DeferredItem<Item> createAbyssalCrystal(DeferredRegister.Items registry) {
        return registry.registerSimpleItem("abyssal_crystal", new Item.Properties().stacksTo(16));
    }

    public static DeferredItem<Item> createAbyssalCore(DeferredRegister.Items registry) {
        return registry.registerSimpleItem("abyssal_core", new Item.Properties().stacksTo(1));
    }

    public static DeferredItem<Item> createCrystallizedPith(DeferredRegister.Items registry) {
        return registry.registerSimpleItem("crystallized_pith");
    }

    // ===== Deep Ocean Spawn Eggs =====
    public static DeferredItem<? extends Item> createDeepOceanSupplySpawnEgg(DeferredRegister.Items registry) {
        return registry.register("deep_ocean_supply_spawn_egg",
                () -> new DeferredSpawnEggItem(
                        com.piranport.registry.ModEntityTypes.DEEP_OCEAN_SUPPLY, 0x888888, 0x444444,
                        new Item.Properties()));
    }

    public static DeferredItem<? extends Item> createDeepOceanDestroyerSpawnEgg(DeferredRegister.Items registry) {
        return registry.register("deep_ocean_destroyer_spawn_egg",
                () -> new DeferredSpawnEggItem(
                        com.piranport.registry.ModEntityTypes.DEEP_OCEAN_DESTROYER, 0x666666, 0x333333,
                        new Item.Properties()));
    }

    public static DeferredItem<? extends Item> createDeepOceanLightCruiserSpawnEgg(DeferredRegister.Items registry) {
        return registry.register("deep_ocean_light_cruiser_spawn_egg",
                () -> new DeferredSpawnEggItem(
                        com.piranport.registry.ModEntityTypes.DEEP_OCEAN_LIGHT_CRUISER, 0x555555, 0x222222,
                        new Item.Properties()));
    }

    public static DeferredItem<? extends Item> createDeepOceanHeavyCruiserSpawnEgg(DeferredRegister.Items registry) {
        return registry.register("deep_ocean_heavy_cruiser_spawn_egg",
                () -> new DeferredSpawnEggItem(
                        com.piranport.registry.ModEntityTypes.DEEP_OCEAN_HEAVY_CRUISER, 0x444444, 0x111111,
                        new Item.Properties()));
    }

    public static DeferredItem<? extends Item> createDeepOceanBattleCruiserSpawnEgg(DeferredRegister.Items registry) {
        return registry.register("deep_ocean_battle_cruiser_spawn_egg",
                () -> new DeferredSpawnEggItem(
                        com.piranport.registry.ModEntityTypes.DEEP_OCEAN_BATTLE_CRUISER, 0x333333, 0x000000,
                        new Item.Properties()));
    }

    public static DeferredItem<? extends Item> createDeepOceanBattleshipSpawnEgg(DeferredRegister.Items registry) {
        return registry.register("deep_ocean_battleship_spawn_egg",
                () -> new DeferredSpawnEggItem(
                        com.piranport.registry.ModEntityTypes.DEEP_OCEAN_BATTLESHIP, 0x222222, 0x000000,
                        new Item.Properties()));
    }

    public static DeferredItem<? extends Item> createDeepOceanLightCarrierSpawnEgg(DeferredRegister.Items registry) {
        return registry.register("deep_ocean_light_carrier_spawn_egg",
                () -> new DeferredSpawnEggItem(
                        com.piranport.registry.ModEntityTypes.DEEP_OCEAN_LIGHT_CARRIER, 0x777777, 0x555555,
                        new Item.Properties()));
    }

    public static DeferredItem<? extends Item> createDeepOceanCarrierSpawnEgg(DeferredRegister.Items registry) {
        return registry.register("deep_ocean_carrier_spawn_egg",
                () -> new DeferredSpawnEggItem(
                        com.piranport.registry.ModEntityTypes.DEEP_OCEAN_CARRIER, 0x999999, 0x666666,
                        new Item.Properties()));
    }

    public static DeferredItem<? extends Item> createDeepOceanSubmarineSpawnEgg(DeferredRegister.Items registry) {
        return registry.register("deep_ocean_submarine_spawn_egg",
                () -> new DeferredSpawnEggItem(
                        com.piranport.registry.ModEntityTypes.DEEP_OCEAN_SUBMARINE, 0x555555, 0x333333,
                        new Item.Properties()));
    }

    public static DeferredItem<? extends Item> createShipGirlSpawnEgg(DeferredRegister.Items registry) {
        return registry.register("ship_girl_spawn_egg",
                () -> new DeferredSpawnEggItem(
                        com.piranport.registry.ModEntityTypes.SHIP_GIRL, 0xFFB6C1, 0xFFFFFF,
                        new Item.Properties()));
    }
}
