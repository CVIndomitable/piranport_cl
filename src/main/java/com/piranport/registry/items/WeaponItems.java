package com.piranport.registry.items;

import com.piranport.artillery.ArtilleryItem;
import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.MuzzlePos;
import com.piranport.component.WeaponCategory;
import com.piranport.item.AmmoItem;
import com.piranport.item.ArtilleryItem;
import com.piranport.item.DepthChargeLauncherItem;
import com.piranport.item.FlareLauncherItem;
import com.piranport.item.GungnirItem;
import com.piranport.item.HatsuyukiMainGunItem;
import com.piranport.item.MissileItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.MysteriousWeaponItem;
import com.piranport.item.CommandSwordItem;
import com.piranport.item.TorpedoItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * 武器物品注册工厂 — 火炮、鱼雷、导弹、深弹发射器、特殊武器。
 */
public class WeaponItems {

    private WeaponItems() {}

    // ===== Guns =====
    public static DeferredItem<Item> createSingleSmallGun(DeferredRegister.Items registry) {
        return registry.register("single_small_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                .durability(500),
                new ArtilleryCannonData(4, 1, 6.0f, 30, 500, 2.0f,
                        List.of(new MuzzlePos(0.2, 0.15, 0)),
                        2.5f, 0.015f, 9.8f, 1.0f, 0.0f)));
    }

    public static DeferredItem<Item> createSmallGun(DeferredRegister.Items registry) {
        return registry.register("small_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                .durability(500),
                new ArtilleryCannonData(4, 2, 6.0f, 30, 500, 2.0f,
                        List.of(new MuzzlePos(0.3, 0.2, 0), new MuzzlePos(-0.3, 0.2, 0)),
                        2.5f, 0.015f, 9.8f, 1.0f, 0.0f)));
    }

    public static DeferredItem<Item> createMediumGun(DeferredRegister.Items registry) {
        return registry.register("medium_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                .durability(1000),
                new ArtilleryCannonData(8, 1, 12.0f, 50, 1000, 3.0f,
                        List.of(new MuzzlePos(0.3, 0.2, 0)),
                        3.0f, 0.01f, 9.8f, 1.5f, 0.0f)));
    }

    public static DeferredItem<Item> createLargeGun(DeferredRegister.Items registry) {
        return registry.register("large_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                .durability(2000),
                new ArtilleryCannonData(16, 3, 20.0f, 80, 2000, 4.0f,
                        List.of(new MuzzlePos(0.5, 0.25, 0), new MuzzlePos(0, 0.25, 0), new MuzzlePos(-0.5, 0.25, 0)),
                        3.5f, 0.008f, 9.8f, 2.0f, 0.0f)));
    }

    public static DeferredItem<Item> createFrenchQuad380mm(DeferredRegister.Items registry) {
        return registry.register("french_quad_380mm_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                .durability(2500),
                new ArtilleryCannonData(16, 4, 20.0f, 60, 2500, 4.0f,
                        List.of(new MuzzlePos(0.6, 0.3, 0), new MuzzlePos(0.2, 0.3, 0),
                                new MuzzlePos(-0.2, 0.3, 0), new MuzzlePos(-0.6, 0.3, 0)),
                        3.5f, 0.008f, 9.8f, 2.0f, 0.0f)));
    }

    public static DeferredItem<Item> createSevenBarrelGun(DeferredRegister.Items registry) {
        return registry.register("seven_barrel_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                .durability(3000),
                new ArtilleryCannonData(16, 7, 20.0f, 100, 3000, 4.0f,
                        List.of(new MuzzlePos(0.9, 0.3, 0), new MuzzlePos(0.6, 0.3, 0),
                                new MuzzlePos(0.3, 0.3, 0), new MuzzlePos(0.0, 0.3, 0),
                                new MuzzlePos(-0.3, 0.3, 0), new MuzzlePos(-0.6, 0.3, 0),
                                new MuzzlePos(-0.9, 0.3, 0)),
                        3.5f, 0.008f, 9.8f, 2.0f, 1.5f)));
    }

    public static DeferredItem<Item> createSalvoTestGun(DeferredRegister.Items registry) {
        return registry.register("salvo_test_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                .durability(2000),
                new ArtilleryCannonData(16, 12, 60.0f, 80, 2000, 4.0f,
                        List.of(new MuzzlePos(1.65, 0.3, 0), new MuzzlePos(1.35, 0.3, 0),
                                new MuzzlePos(1.05, 0.3, 0), new MuzzlePos(0.75, 0.3, 0),
                                new MuzzlePos(0.45, 0.3, 0), new MuzzlePos(0.15, 0.3, 0),
                                new MuzzlePos(-0.15, 0.3, 0), new MuzzlePos(-0.45, 0.3, 0),
                                new MuzzlePos(-0.75, 0.3, 0), new MuzzlePos(-1.05, 0.3, 0),
                                new MuzzlePos(-1.35, 0.3, 0), new MuzzlePos(-1.65, 0.3, 0)),
                        10.5f, 0.008f, 9.8f, 6.0f, 0.5f)));
    }

    // ===== Torpedo Ammo (legacy generic) =====
    public static DeferredItem<TorpedoItem> createTorpedo533mm(DeferredRegister.Items registry) {
        return registry.register("torpedo_533mm",
                () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533));
    }

    public static DeferredItem<TorpedoItem> createTorpedo610mm(DeferredRegister.Items registry) {
        return registry.register("torpedo_610mm",
                () -> new TorpedoItem(new Item.Properties().stacksTo(16), 610));
    }

    public static DeferredItem<TorpedoItem> createMagneticTorpedo533mm(DeferredRegister.Items registry) {
        return registry.register("magnetic_torpedo_533mm",
                () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533, true));
    }

    public static DeferredItem<TorpedoItem> createWireGuidedTorpedo533mm(DeferredRegister.Items registry) {
        return registry.register("wire_guided_torpedo_533mm",
                () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533, false, true));
    }

    public static DeferredItem<TorpedoItem> createAcousticTorpedo533mm(DeferredRegister.Items registry) {
        return registry.register("acoustic_torpedo_533mm",
                () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533, false, false, true));
    }

    // ===== Torpedo Launchers =====
    public static DeferredItem<TorpedoLauncherItem> createTwinTorpedoLauncher(DeferredRegister.Items registry) {
        return registry.register("twin_torpedo_launcher",
                () -> new TorpedoLauncherItem(new Item.Properties().stacksTo(1), 2));
    }

    public static DeferredItem<TorpedoLauncherItem> createTripleTorpedoLauncher(DeferredRegister.Items registry) {
        return registry.register("triple_torpedo_launcher",
                () -> new TorpedoLauncherItem(new Item.Properties().stacksTo(1), 3));
    }

    public static DeferredItem<TorpedoLauncherItem> createQuadTorpedoLauncher(DeferredRegister.Items registry) {
        return registry.register("quad_torpedo_launcher",
                () -> new TorpedoLauncherItem(new Item.Properties().stacksTo(1), 4));
    }

    // ===== Depth Charge Launchers =====
    public static DeferredItem<DepthChargeLauncherItem> createDepthChargeLauncher(DeferredRegister.Items registry) {
        return registry.register("depth_charge_launcher",
                () -> new DepthChargeLauncherItem(new Item.Properties().stacksTo(1), false));
    }

    public static DeferredItem<DepthChargeLauncherItem> createDepthChargeLauncherImproved(DeferredRegister.Items registry) {
        return registry.register("depth_charge_launcher_improved",
                () -> new DepthChargeLauncherItem(new Item.Properties().stacksTo(1), false));
    }

    public static DeferredItem<DepthChargeLauncherItem> createDepthChargeLauncherAdvanced(DeferredRegister.Items registry) {
        return registry.register("depth_charge_launcher_advanced",
                () -> new DepthChargeLauncherItem(new Item.Properties().stacksTo(1), false));
    }

    // ===== Missile / Rocket Ammo =====
    public static DeferredItem<MissileItem> createRocketAmmo(DeferredRegister.Items registry) {
        return registry.register("rocket_ammo",
                () -> new MissileItem(new Item.Properties().stacksTo(16), MissileItem.Type.ROCKET));
    }

    public static DeferredItem<MissileItem> createMissileAmmo(DeferredRegister.Items registry) {
        return registry.register("missile_ammo",
                () -> new MissileItem(new Item.Properties().stacksTo(16), MissileItem.Type.MISSILE));
    }

    // ===== Missile Launchers =====
    public static DeferredItem<MissileLauncherItem> createTerrierLauncher(DeferredRegister.Items registry) {
        return registry.register("terrier_launcher",
                () -> new MissileLauncherItem(new Item.Properties().stacksTo(1).durability(300), MissileItem.Type.MISSILE, 2));
    }

    public static DeferredItem<MissileLauncherItem> createSeaDartLauncher(DeferredRegister.Items registry) {
        return registry.register("sea_dart_launcher",
                () -> new MissileLauncherItem(new Item.Properties().stacksTo(1).durability(250), MissileItem.Type.MISSILE, 2));
    }

    public static DeferredItem<MissileLauncherItem> createSeacatLauncher(DeferredRegister.Items registry) {
        return registry.register("seacat_launcher",
                () -> new MissileLauncherItem(new Item.Properties().stacksTo(1).durability(200), MissileItem.Type.MISSILE, 1));
    }

    public static DeferredItem<MissileLauncherItem> createSy1Launcher(DeferredRegister.Items registry) {
        return registry.register("sy1_launcher",
                () -> new MissileLauncherItem(new Item.Properties().stacksTo(1).durability(350), MissileItem.Type.MISSILE, 2));
    }

    public static DeferredItem<MissileLauncherItem> createMk14HarpoonLauncher(DeferredRegister.Items registry) {
        return registry.register("mk14_harpoon_launcher",
                () -> new MissileLauncherItem(new Item.Properties().stacksTo(1).durability(400), MissileItem.Type.MISSILE, 4));
    }

    public static DeferredItem<MissileLauncherItem> createShipRocketLauncher(DeferredRegister.Items registry) {
        return registry.register("ship_rocket_launcher",
                () -> new MissileLauncherItem(new Item.Properties().stacksTo(1).durability(200), MissileItem.Type.ROCKET, 6));
    }

    // ===== Flare Launcher =====
    public static DeferredItem<FlareLauncherItem> createFlareLauncher(DeferredRegister.Items registry) {
        return registry.register("flare_launcher",
                () -> new FlareLauncherItem(new Item.Properties().stacksTo(1).durability(50)));
    }

    // ===== Special Weapons =====
    public static DeferredItem<MysteriousWeaponItem> createMysteriousWeapon(DeferredRegister.Items registry) {
        return registry.register("mysterious_weapon",
                () -> new MysteriousWeaponItem(new Item.Properties().stacksTo(1)));
    }

    public static DeferredItem<CommandSwordItem> createCommandSword(DeferredRegister.Items registry) {
        return registry.register("command_sword",
                () -> new CommandSwordItem(new Item.Properties().stacksTo(1)));
    }

    public static DeferredItem<HatsuyukiMainGunItem> createHatsuyukiMainGun(DeferredRegister.Items registry) {
        return registry.register("hatsuyuki_main_gun",
                () -> new HatsuyukiMainGunItem(new Item.Properties().stacksTo(1)));
    }

    public static DeferredItem<GungnirItem> createGungnir(DeferredRegister.Items registry) {
        return registry.register("gungnir",
                () -> new GungnirItem(new Item.Properties().stacksTo(1)));
    }
}
