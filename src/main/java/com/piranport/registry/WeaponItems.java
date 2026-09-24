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
    /**
     * 日本12.7厘米连装炮 — 八九式十二糎七高角砲。
     *
     * <p>数值、贴图、合成配方均与已删除的 {@code small_gun} 一致（策划要求）。原先两者
     * 并存时 12.7 厘米炮只是小型火炮换皮，现由本炮直接取代小型火炮的位置。
     */
    public static final DeferredItem<Item> JAPANESE_127MM_TWIN_GUN =
            ITEMS.register("japanese_127mm_twin_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                    .durability(500),
                    new ArtilleryCannonData(4, 2, 6.0f, 30, 500, 2.0f,
                            List.of(new MuzzlePos(0.3, 0.2, 0),
                                    new MuzzlePos(-0.3, 0.2, 0)),
                            2.5f, 0.015f, 9.8f, 1.0f, 0.0f,
                            10, 1, 5.0f), "japanese_127mm_twin_gun"));
    /**
     * 中国双联140毫米炮。
     *
     * <p>原为单装「中型火炮」（注册 ID {@code medium_gun}），现改为双联并启用中文命名。
     * 炮口位置相应改为左右各一（±0.3），与 {@link #JAPANESE_127MM_TWIN_GUN} 的双联
     * 约定一致；双联两管轮流击发，故 {@code salvoInterval} 由 0 改为 5.0。
     */
    public static final DeferredItem<Item> MEDIUM_GUN =
            ITEMS.register("chinese_twin_140mm_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                    .durability(1000),
                    new ArtilleryCannonData(8, 2, 12.0f, 50, 1000, 3.0f,
                            List.of(new MuzzlePos(0.3, 0.2, 0),
                                    new MuzzlePos(-0.3, 0.2, 0)),
                            3.0f, 0.01f, 9.8f, 1.5f, 0.0f,
                            15, 1, 5.0f), "chinese_twin_140mm_gun"));
    /**
     * 神经网络弹道解算实验炮。
     *
     * <p>数值完全复制 {@link #MEDIUM_GUN}（口径 8 / 单装 / v₀=3.0 / drag=0.01 / g=9.8 /
     * 散布 0.8° / 仰角 −5°~50°），但解算路径走 {@code combat.neural.BallisticNet}
     * 而非 {@code BallisticSolver}。路由按注册 ID 判定（见 {@code CannonAiming}），
     * 不依赖物理参数——因为 {@code ConfigOverrideManager} 会运行时改动参数，
     * 用参数路由会在玩家改覆盖值时误判。
     *
     * <p>注意：中国双联140毫米炮已改为双联，本炮仍是单装，两者数值不再逐项相同，
     * 仅口径 / 初速 / 落点物理一致，便于对照弹道解算结果。
     *
     * <p>依据：{@code docs/策划决策/武器/火炮-神经网络弹道解算实验方案.md} 4.1 / 4.2。
     */
    public static final DeferredItem<Item> NEURAL_BALLISTIC_TEST_GUN =
            ITEMS.register("neural_ballistic_test_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                    .durability(1000),
                    new ArtilleryCannonData(8, 1, 12.0f, 50, 1000, 3.0f,
                            List.of(new MuzzlePos(0.3, 0.2, 0)),
                            3.0f, 0.01f, 9.8f, 1.5f, 0.0f,
                            15, 1, 0.0f), "neural_ballistic_test_gun"));
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
    public static final DeferredItem<Item> GERMAN_TWIN_380MM_GUN =
            ITEMS.register("german_twin_380mm_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                    .durability(2500),
                    new ArtilleryCannonData(16, 2, 20.0f, 60, 2500, 4.0f,
                            List.of(new MuzzlePos(0.2, 0.3, 0),
                                    new MuzzlePos(-0.2, 0.3, 0)),
                            3.5f, 0.008f, 9.8f, 2.0f, 0.0f,
                            20, 2, 3.0f), "german_twin_380mm_gun"));

    /**
     * 法国四联380毫米炮 — 大型火炮，四联装。
     *
     * <p><b>数值均为占位值</b>，待策划定案后调整。取法：口径/初速/弹道参数照
     * {@link #GERMAN_TWIN_380MM_GUN}（同为 380 口径，保证弹道解算一致），
     * 联装数改 4、装填介于双联(60)与七联(100)之间、耐久略高于双联。</p>
     */
    public static final DeferredItem<Item> FRENCH_QUAD_380MM_GUN =
            ITEMS.register("french_quad_380mm_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                    .durability(2600),
                    new ArtilleryCannonData(16, 4, 20.0f, 70, 2600, 4.0f,
                            List.of(new MuzzlePos(0.3, 0.3, 0),
                                    new MuzzlePos(0.1, 0.3, 0),
                                    new MuzzlePos(-0.1, 0.3, 0),
                                    new MuzzlePos(-0.3, 0.3, 0)),
                            3.5f, 0.008f, 9.8f, 2.0f, 0.0f,
                            20, 4, 3.0f), "french_quad_380mm_gun"));

    /**
     * 一星期主炮群 — 14联装极限齐射火炮。
     * 用途：测试更大规模齐射性能和散布系统。
     *
     * 参数说明（按大型火炮线性外推，14联装 ≈ 3.5倍大型火炮）：
     * - 齐射数：14发（barrels=14）
     * - 伤害：70.0（大型火炮的3.5倍）
     * - 装填时间：80tick（与大型火炮相同）
     * - 初速：12.25（大型火炮的3.5倍）
     * - 爆炸威力：7.0（大型火炮的3.5倍）
     * - 散布角：1.8度（补偿更多炮管）
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
                            1.0f, com.piranport.config.ModEquipmentConfig.CIWS_20MM));
    public static final DeferredItem<AutoCIWSItem> AUTO_CIWS_40MM =
            ITEMS.register("auto_ciws_40mm",
                    () -> new AutoCIWSItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ENHANCEMENT),
                            1.5f, com.piranport.config.ModEquipmentConfig.CIWS_40MM));
    public static final DeferredItem<AutoCIWSItem> AUTO_CIWS_76MM =
            ITEMS.register("auto_ciws_76mm",
                    () -> new AutoCIWSItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ENHANCEMENT),
                            2.0f, com.piranport.config.ModEquipmentConfig.CIWS_76MM));

    // ===== Torpedo Launchers =====
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
