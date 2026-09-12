package com.piranport.registry;

import com.piranport.item.ShipType;

import com.piranport.component.FuelData;
import com.piranport.component.WeaponCategory;
import com.piranport.config.ModProjectilesConfig;
import com.piranport.item.AutoCIWSItem;
import com.piranport.artillery.ArtilleryItem;
import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.MuzzlePos;
import com.piranport.item.DepthChargeLauncherItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.TorpedoLauncherItem;
import java.util.List;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 武器类物品注册表（船芯 / 火炮 / 鱼雷发射器 / 深弹发射器 / 导弹发射器 / 自动近防炮）。
 *
 * <p>共享 {@link ModItems#ITEMS} 同一个 DeferredRegister，避免重复注册。
 * 通过 {@link ModItems} 的同名静态字段以薄包装方式对外暴露，保持原有调用方不变。</p>
 */
public final class WeaponItems {
    // 共享 ModItems 的注册表
    private static final DeferredRegister.Items ITEMS = ModItems.ITEMS;

    // ===== Ship Cores =====
    public static final DeferredItem<ShipCoreItem> SMALL_SHIP_CORE =
            ITEMS.register("small_ship_core",
                    () -> new ShipCoreItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.SHIP_CORE_FUEL.get(),
                                    new FuelData(0, ShipType.SMALL.fuelCapacity)),
                            ShipType.SMALL));
    public static final DeferredItem<ShipCoreItem> MEDIUM_SHIP_CORE =
            ITEMS.register("medium_ship_core",
                    () -> new ShipCoreItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.SHIP_CORE_FUEL.get(),
                                    new FuelData(0, ShipType.MEDIUM.fuelCapacity)),
                            ShipType.MEDIUM));
    public static final DeferredItem<ShipCoreItem> LARGE_SHIP_CORE =
            ITEMS.register("large_ship_core",
                    () -> new ShipCoreItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.SHIP_CORE_FUEL.get(),
                                    new FuelData(0, ShipType.LARGE.fuelCapacity)),
                            ShipType.LARGE));
    public static final DeferredItem<ShipCoreItem> SUBMARINE_CORE =
            ITEMS.register("submarine_core",
                    () -> new ShipCoreItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.SHIP_CORE_FUEL.get(),
                                    new FuelData(0, ShipType.SUBMARINE.fuelCapacity)),
                            ShipType.SUBMARINE));

    // ===== Guns =====
    public static final DeferredItem<Item> SINGLE_SMALL_GUN =
            ITEMS.register("single_small_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                    .durability(500),
                    new ArtilleryCannonData(4, 1, 6.0f, 30, 500, 2.0f,
                            List.of(new MuzzlePos(0.2, 0.15, 0)),
                            2.5f, 0.015f, 9.8f, 1.0f, 0.0f,
                            10, 1, 0.0f), "single_small_gun"));
    public static final DeferredItem<Item> SMALL_GUN =
            ITEMS.register("small_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                    .durability(500),
                    new ArtilleryCannonData(4, 2, 6.0f, 30, 500, 2.0f,
                            List.of(new MuzzlePos(0.3, 0.2, 0),
                                    new MuzzlePos(-0.3, 0.2, 0)),
                            2.5f, 0.015f, 9.8f, 1.0f, 0.0f,
                            10, 1, 5.0f), "small_gun"));
    public static final DeferredItem<Item> MEDIUM_GUN =
            ITEMS.register("medium_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                    .durability(1000),
                    new ArtilleryCannonData(8, 1, 12.0f, 50, 1000, 3.0f,
                            List.of(new MuzzlePos(0.3, 0.2, 0)),
                            3.0f, 0.01f, 9.8f, 1.5f, 0.0f,
                            15, 1, 0.0f), "medium_gun"));
    public static final DeferredItem<Item> LARGE_GUN =
            ITEMS.register("large_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                    .durability(2000),
                    new ArtilleryCannonData(16, 3, 20.0f, 80, 2000, 4.0f,
                            List.of(new MuzzlePos(0.5, 0.25, 0),
                                    new MuzzlePos(0, 0.25, 0),
                                    new MuzzlePos(-0.5, 0.25, 0)),
                            3.5f, 0.008f, 9.8f, 2.0f, 0.0f,
                            20, 3, 5.0f), "large_gun"));
    public static final DeferredItem<Item> FRENCH_QUAD_380MM_GUN =
            ITEMS.register("french_quad_380mm_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                    .durability(2500),
                    new ArtilleryCannonData(16, 4, 20.0f, 60, 2500, 4.0f,
                            List.of(new MuzzlePos(0.6, 0.3, 0),
                                    new MuzzlePos(0.2, 0.3, 0),
                                    new MuzzlePos(-0.2, 0.3, 0),
                                    new MuzzlePos(-0.6, 0.3, 0)),
                            3.5f, 0.008f, 9.8f, 2.0f, 0.0f,
                            20, 4, 3.0f), "french_quad_380mm_gun"));

    /**
     * 七联装主炮群 — 测试用极限齐射火炮。
     * 用途：测试齐射数值、散布系统、负重平衡。
     *
     * 参数说明：
     * - 齐射数：7发（barrels=7）
     * - 散布角：1.5度（可调整测试不同精度）
     * - 装填时间：100tick（5秒，可调整测试DPS）
     * - 武器重量：35（比large_gun的30更重）
     */
    public static final DeferredItem<Item> SEVEN_BARREL_GUN =
            ITEMS.register("seven_barrel_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                    .durability(3000),
                    new ArtilleryCannonData(
                            16,      // caliber: 大口径
                            7,       // barrels: 7联装
                            20.0f,   // damage: 与large_gun相同
                            100,     // reloadTime: 5秒装填
                            3000,    // durability: 高耐久
                            4.0f,    // scopeZoom: 与large_gun相同
                            List.of( // muzzles: 7个炮口横向排列
                                    new MuzzlePos(0.9, 0.3, 0),
                                    new MuzzlePos(0.6, 0.3, 0),
                                    new MuzzlePos(0.3, 0.3, 0),
                                    new MuzzlePos(0.0, 0.3, 0),
                                    new MuzzlePos(-0.3, 0.3, 0),
                                    new MuzzlePos(-0.6, 0.3, 0),
                                    new MuzzlePos(-0.9, 0.3, 0)
                            ),
                            3.5f,    // initialSpeed: 与large_gun相同
                            0.008f,  // dragCoeff: 与large_gun相同
                            9.8f,    // gravity: 标准重力
                            2.0f,    // explosionPower: 与large_gun相同
                            1.5f,    // dispersion: 1.5度散布
                            15,      // fireCooldown
                            7,       // salvoCount
                            3.0f     // salvoInterval
                    ), "seven_barrel_gun"));

    /**
     * 齐射测试 — 三倍大型火炮数值，12联装。
     * 用途：测试极限齐射性能和散布系统。
     *
     * 参数说明：
     * - 齐射数：12发（barrels=12）
     * - 伤害：60.0（大型火炮的3倍）
     * - 装填时间：80tick（与大型火炮相同）
     * - 初速：10.5（大型火炮的3倍）
     * - 爆炸威力：6.0（大型火炮的3倍）
     */
    public static final DeferredItem<Item> SALVO_TEST_GUN =
            ITEMS.register("salvo_test_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                    .durability(2000),
                    new ArtilleryCannonData(
                            16,      // caliber: 大口径
                            12,      // barrels: 12联装
                            60.0f,   // damage: 大型火炮的3倍
                            80,      // reloadTime: 与大型火炮相同
                            2000,    // durability: 与大型火炮相同
                            4.0f,    // scopeZoom: 与大型火炮相同
                            List.of( // muzzles: 12个炮口横向排列
                                    new MuzzlePos(1.65, 0.3, 0),
                                    new MuzzlePos(1.35, 0.3, 0),
                                    new MuzzlePos(1.05, 0.3, 0),
                                    new MuzzlePos(0.75, 0.3, 0),
                                    new MuzzlePos(0.45, 0.3, 0),
                                    new MuzzlePos(0.15, 0.3, 0),
                                    new MuzzlePos(-0.15, 0.3, 0),
                                    new MuzzlePos(-0.45, 0.3, 0),
                                    new MuzzlePos(-0.75, 0.3, 0),
                                    new MuzzlePos(-1.05, 0.3, 0),
                                    new MuzzlePos(-1.35, 0.3, 0),
                                    new MuzzlePos(-1.65, 0.3, 0)
                            ),
                            10.5f,   // initialSpeed: 大型火炮的3倍
                            0.008f,  // dragCoeff: 与大型火炮相同
                            9.8f,    // gravity: 标准重力
                            6.0f,    // explosionPower: 大型火炮的3倍
                            0.5f,    // dispersion: 默认散布
                            10,      // fireCooldown
                            12,      // salvoCount
                            2.0f     // salvoInterval
                    ), "salvo_test_gun"));

    /**
     * 一星期主炮群 — 14联装极限齐射火炮。
     * 用途：测试更大规模齐射性能和散布系统。
     *
     * 参数说明（按 salvo_test_gun 的 3 倍缩放线性外推，14联装 ≈ 3.5倍大型火炮）：
     * - 齐射数：14发（barrels=14）
     * - 伤害：70.0（大型火炮的3.5倍）
     * - 装填时间：80tick（与大型火炮相同）
     * - 初速：12.25（大型火炮的3.5倍）
     * - 爆炸威力：7.0（大型火炮的3.5倍）
     * - 散布角：1.8度（比12联装稍大，补偿更多炮管）
     */
    public static final DeferredItem<Item> FOURTEEN_BARREL_GUN =
            ITEMS.register("fourteen_barrel_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                    .durability(3500),
                    new ArtilleryCannonData(
                            16,      // caliber: 大口径
                            14,      // barrels: 14联装
                            70.0f,   // damage: 大型火炮的3.5倍
                            80,      // reloadTime: 与大型火炮相同
                            3500,    // durability: 高耐久
                            4.0f,    // scopeZoom: 与大型火炮相同
                            List.of( // muzzles: 14个炮口横向排列，y=0.3，间距0.3
                                    new MuzzlePos(1.95, 0.3, 0),
                                    new MuzzlePos(1.65, 0.3, 0),
                                    new MuzzlePos(1.35, 0.3, 0),
                                    new MuzzlePos(1.05, 0.3, 0),
                                    new MuzzlePos(0.75, 0.3, 0),
                                    new MuzzlePos(0.45, 0.3, 0),
                                    new MuzzlePos(0.15, 0.3, 0),
                                    new MuzzlePos(-0.15, 0.3, 0),
                                    new MuzzlePos(-0.45, 0.3, 0),
                                    new MuzzlePos(-0.75, 0.3, 0),
                                    new MuzzlePos(-1.05, 0.3, 0),
                                    new MuzzlePos(-1.35, 0.3, 0),
                                    new MuzzlePos(-1.65, 0.3, 0),
                                    new MuzzlePos(-1.95, 0.3, 0)
                            ),
                            12.25f,  // initialSpeed: 大型火炮的3.5倍
                            0.008f,  // dragCoeff: 与大型火炮相同
                            9.8f,    // gravity: 标准重力
                            7.0f,    // explosionPower: 大型火炮的3.5倍
                            1.8f,    // dispersion: 比12联装稍大
                            10,      // fireCooldown
                            14,      // salvoCount
                            2.0f     // salvoInterval
                    ), "fourteen_barrel_gun"));

    // ===== Auto CIWS（强化部件槽 — 策划决策/舰装/舰装-自动近防炮系统.md）=====
    public static final DeferredItem<AutoCIWSItem> AUTO_CIWS_20MM =
            ITEMS.register("auto_ciws_20mm",
                    () -> new AutoCIWSItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ENHANCEMENT),
                            1.0f));
    public static final DeferredItem<AutoCIWSItem> AUTO_CIWS_40MM =
            ITEMS.register("auto_ciws_40mm",
                    () -> new AutoCIWSItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ENHANCEMENT),
                            1.5f));
    public static final DeferredItem<AutoCIWSItem> AUTO_CIWS_76MM =
            ITEMS.register("auto_ciws_76mm",
                    () -> new AutoCIWSItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ENHANCEMENT),
                            2.0f));

    // ===== Torpedo Launchers =====
    public static final DeferredItem<TorpedoLauncherItem> TWIN_TORPEDO_LAUNCHER =
            ITEMS.register("twin_torpedo_launcher",
                    () -> new TorpedoLauncherItem(
                            new Item.Properties().stacksTo(1).durability(64)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.TORPEDO),
                            533, 2, ModProjectilesConfig.TWIN_TORPEDO_LAUNCHER_COOLDOWN::get));
    public static final DeferredItem<TorpedoLauncherItem> TRIPLE_TORPEDO_LAUNCHER =
            ITEMS.register("triple_torpedo_launcher",
                    () -> new TorpedoLauncherItem(
                            new Item.Properties().stacksTo(1).durability(48)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.TORPEDO),
                            533, 3, ModProjectilesConfig.TRIPLE_TORPEDO_LAUNCHER_COOLDOWN::get));
    public static final DeferredItem<TorpedoLauncherItem> QUAD_TORPEDO_LAUNCHER =
            ITEMS.register("quad_torpedo_launcher",
                    () -> new TorpedoLauncherItem(
                            new Item.Properties().stacksTo(1).durability(32)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.TORPEDO),
                            610, 4, ModProjectilesConfig.QUAD_TORPEDO_LAUNCHER_COOLDOWN::get));

    // Phase 27：策划 §3.3 五联装鱼雷发射器
    public static final DeferredItem<TorpedoLauncherItem> QUINTUPLE_TORPEDO_LAUNCHER =
            ITEMS.register("quintuple_torpedo_launcher",
                    () -> new TorpedoLauncherItem(
                            new Item.Properties().stacksTo(1).durability(24)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.TORPEDO),
                            610, 5, ModProjectilesConfig.QUINTUPLE_TORPEDO_LAUNCHER_COOLDOWN::get));

    // ===== Depth Charge Launchers =====
    public static final DeferredItem<DepthChargeLauncherItem> DEPTH_CHARGE_LAUNCHER =
            ITEMS.register("depth_charge_launcher",
                    () -> new DepthChargeLauncherItem(
                            new Item.Properties().stacksTo(1).durability(64)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.DEPTH_CHARGE),
                            1, 60, DepthChargeLauncherItem.SpreadPattern.SINGLE));
    public static final DeferredItem<DepthChargeLauncherItem> DEPTH_CHARGE_LAUNCHER_IMPROVED =
            ITEMS.register("depth_charge_launcher_improved",
                    () -> new DepthChargeLauncherItem(
                            new Item.Properties().stacksTo(1).durability(48)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.DEPTH_CHARGE),
                            2, 80, DepthChargeLauncherItem.SpreadPattern.FRONT_BACK));
    public static final DeferredItem<DepthChargeLauncherItem> DEPTH_CHARGE_LAUNCHER_ADVANCED =
            ITEMS.register("depth_charge_launcher_advanced",
                    () -> new DepthChargeLauncherItem(
                            new Item.Properties().stacksTo(1).durability(32)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.DEPTH_CHARGE),
                            3, 100, DepthChargeLauncherItem.SpreadPattern.TRIANGLE));

    // ===== Missile Launchers =====
    // 上游一号（反舰导弹）: 伤害30+6穿甲, 连装2, 负重25
    public static final DeferredItem<MissileLauncherItem> SY1_LAUNCHER =
            ITEMS.register("sy1_launcher",
                    () -> new MissileLauncherItem(
                            new Item.Properties().stacksTo(1)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.MISSILE),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_SHIP,
                            30f, 6f, 0f, 2, 0,
                            () -> ModItems.SY1_MISSILE.get()));
    // MK14鱼叉（反舰导弹）: 伤害24, 连装4, 负重22
    public static final DeferredItem<MissileLauncherItem> MK14_HARPOON_LAUNCHER =
            ITEMS.register("mk14_harpoon_launcher",
                    () -> new MissileLauncherItem(
                            new Item.Properties().stacksTo(1)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.MISSILE),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_SHIP,
                            24f, 0f, 0f, 4, 0,
                            () -> ModItems.HARPOON_MISSILE.get()));
    // 小猎犬（防空导弹）: 伤害9, 冷却60s, 负重14
    public static final DeferredItem<MissileLauncherItem> TERRIER_LAUNCHER =
            ITEMS.register("terrier_launcher",
                    () -> new MissileLauncherItem(
                            new Item.Properties().stacksTo(1)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.MISSILE),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_AIR,
                            9f, 0f, 2.0f, 1, 1200,
                            () -> ModItems.TERRIER_MISSILE.get()));
    // 舰载火箭弹: 伤害6, 连装6, 负重32
    public static final DeferredItem<MissileLauncherItem> SHIP_ROCKET_LAUNCHER =
            ITEMS.register("ship_rocket_launcher",
                    () -> new MissileLauncherItem(
                            new Item.Properties().stacksTo(1)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.MISSILE),
                            com.piranport.entity.MissileEntity.MissileType.ROCKET,
                            6f, 0f, 2.0f, 6, 0,
                            () -> ModItems.ROCKET_AMMO.get()));
    // 箭型防空导弹（Sea Dart）: 伤害6, 冷却60s, 负重7
    public static final DeferredItem<MissileLauncherItem> SEA_DART_LAUNCHER =
            ITEMS.register("sea_dart_launcher",
                    () -> new MissileLauncherItem(
                            new Item.Properties().stacksTo(1)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.MISSILE),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_AIR,
                            6f, 0f, 1.5f, 1, 1200,
                            () -> ModItems.ANTI_AIR_MISSILE.get()));
    // 海猫防空导弹（Seacat）: 伤害6, 冷却60s, 负重6
    public static final DeferredItem<MissileLauncherItem> SEACAT_LAUNCHER =
            ITEMS.register("seacat_launcher",
                    () -> new MissileLauncherItem(
                            new Item.Properties().stacksTo(1)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.MISSILE),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_AIR,
                            6f, 0f, 1.5f, 1, 1200,
                            () -> ModItems.ANTI_AIR_MISSILE.get()));

    private WeaponItems() {}
}
