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
import com.piranport.component.EquipmentTier;
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
            ITEMS.register("submarine_ship_core",
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
     * <p>贴图、合成配方沿用已删除的 {@code small_gun}。数值自 2026-09-30 起按
     * 策划决策/数值/07（5英寸双联·初期：面板 5、装填 39 tick、负重 12），见 {@link com.piranport.artillery.CannonCatalog}。
     */
    public static final DeferredItem<Item> JAPANESE_127MM_TWIN_GUN =
            ITEMS.register("japanese_127mm_twin_gun", () -> catalogGun("japanese_127mm_twin_gun"));
    /**
     * 中国双联140毫米炮。
     *
     * <p>数值按《数值/06》公式以真实口径 5.51 英寸（140/25.4）、双联、初期档派生：
     * 单发面板 6、装填 45.63 tick、负重 14。炮口左右各一（±0.3），与
     * {@link #JAPANESE_127MM_TWIN_GUN} 的双联约定一致；双联两管轮流击发，{@code salvoInterval} 5.0。
     */
    public static final DeferredItem<Item> MEDIUM_GUN =
            ITEMS.register("chinese_twin_140mm_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                    .durability(1000),
                    new ArtilleryCannonData(8, 2, 0.0f, 0.0f, 1000, 3.0f,
                            List.of(new MuzzlePos(0.3, 0.2, 0),
                                    new MuzzlePos(-0.3, 0.2, 0)),
                            3.0f, 0.01f, 9.8f, 1.5f, 0.0f,
                            15, 1, 5.0f,
                            200.0f, 0.0f, 0.0f, 50.0f, 3.0f, "MANUAL",
                            5.51f, "initial", "standard", 0.0f), "chinese_twin_140mm_gun"));
    // neural_ballistic_test_gun（神经网络弹道实验炮）与 large_gun（大型火炮，旧试验炮）
    // 已删除；原用处改指 07 表「英国三联16英寸炮」。
    /** 德国双联380毫米炮：策划决策/数值/07（15英寸双联·标准：面板 31、装填 135.2 tick、负重 54）。 */
    public static final DeferredItem<Item> GERMAN_TWIN_380MM_GUN =
            ITEMS.register("german_twin_380mm_gun", () -> catalogGun("german_twin_380mm_gun"));

    /**
     * 策划决策/数值/07 注册表其余火炮（id → 物品），顺序同 07 表。
     * 日本12.7厘米连装炮、德国双联380毫米炮已有独立字段，不在此重复注册。
     */
    public static final java.util.Map<String, DeferredItem<Item>> CATALOG_GUNS = registerCatalogGuns();

    private static java.util.Map<String, DeferredItem<Item>> registerCatalogGuns() {
        java.util.Map<String, DeferredItem<Item>> map = new java.util.LinkedHashMap<>();
        for (com.piranport.artillery.CannonCatalog.Entry e : com.piranport.artillery.CannonCatalog.ENTRIES) {
            String id = e.id();
            if (id.equals("japanese_127mm_twin_gun") || id.equals("german_twin_380mm_gun")) continue;
            map.put(id, ITEMS.register(id, () -> catalogGun(id)));
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    private static ArtilleryItem catalogGun(String id) {
        ArtilleryCannonData data = com.piranport.artillery.CannonCatalog.fallbackData(id);
        return new ArtilleryItem(new Item.Properties().stacksTo(1)
                .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                .durability(data.durability()), data, id);
    }

    /** 07 表全部 60 门（含两门独立字段），按表顺序。 */
    public static java.util.List<Item> allCatalogGuns() {
        java.util.List<Item> list = new java.util.ArrayList<>();
        for (com.piranport.artillery.CannonCatalog.Entry e : com.piranport.artillery.CannonCatalog.ENTRIES) {
            if (e.id().equals("japanese_127mm_twin_gun")) list.add(JAPANESE_127MM_TWIN_GUN.get());
            else if (e.id().equals("german_twin_380mm_gun")) list.add(GERMAN_TWIN_380MM_GUN.get());
            else list.add(CATALOG_GUNS.get(e.id()).get());
        }
        return list;
    }

    /**
     * 法国四联380毫米炮 — 大型火炮，四联装。
     *
     * <p>按《数值/06》以真实口径 15 英寸、四联、改良档派生：单发面板 31、齐射 124。
     * 装填采用对照表定稿值 167.7 tick（06 公式四联改良为 160.55，此处以项目定稿值为准）。
     * 物理参数沿用 380 口径族（与 {@link #GERMAN_TWIN_380MM_GUN} 一致），保证弹道解算相同。
     */
    public static final DeferredItem<Item> FRENCH_QUAD_380MM_GUN =
            ITEMS.register("french_quad_380mm_gun", () -> new ArtilleryItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)
                    .durability(2600),
                    new ArtilleryCannonData(16, 4, 0.0f, 167.7f, 2600, 4.0f,
                            List.of(new MuzzlePos(0.3, 0.3, 0),
                                    new MuzzlePos(0.1, 0.3, 0),
                                    new MuzzlePos(-0.1, 0.3, 0),
                                    new MuzzlePos(-0.3, 0.3, 0)),
                            3.5f, 0.008f, 9.8f, 2.0f, 0.0f,
                            20, 4, 3.0f,
                            800.0f, 0.0f, 0.0f, 45.0f, 3.0f, "MANUAL",
                            15.0f, "improved", "standard", 0.0f), "french_quad_380mm_gun"));

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
                            533, 3, ModProjectilesConfig.TRIPLE_TORPEDO_LAUNCHER_COOLDOWN::get, EquipmentTier.INITIAL));
    public static final DeferredItem<TorpedoLauncherItem> QUAD_TORPEDO_LAUNCHER =
            ITEMS.register("quad_torpedo_launcher",
                    () -> new TorpedoLauncherItem(
                            new Item.Properties().stacksTo(1).durability(32)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.TORPEDO),
                            610, 4, ModProjectilesConfig.QUAD_TORPEDO_LAUNCHER_COOLDOWN::get, EquipmentTier.IMPROVED));

    // Phase 27：策划 §3.3 五联装鱼雷发射器
    public static final DeferredItem<TorpedoLauncherItem> QUINTUPLE_TORPEDO_LAUNCHER =
            ITEMS.register("quintuple_torpedo_launcher",
                    () -> new TorpedoLauncherItem(
                            new Item.Properties().stacksTo(1).durability(24)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.TORPEDO),
                            610, 5, ModProjectilesConfig.QUINTUPLE_TORPEDO_LAUNCHER_COOLDOWN::get, EquipmentTier.IMPROVED));

    // ===== 图鉴鱼雷发射器（策划决策/数值/08，负重按公式，见 TorpedoLauncherItem#computeWeight）=====
    public static final DeferredItem<TorpedoLauncherItem> TRIPLE_533MM_TORPEDO = catalogLauncher("triple_533mm_torpedo", 533, 3, EquipmentTier.INITIAL,
            48, ModProjectilesConfig.TRIPLE_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> QUAD_533MM_TORPEDO = catalogLauncher("quad_533mm_torpedo", 533, 4, EquipmentTier.STANDARD,
            32, ModProjectilesConfig.QUAD_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> QUAD_533MM_TORPEDO_UR_HIGH_SPEED = catalogLauncher("quad_533mm_torpedo_ur_high_speed", 533, 4, EquipmentTier.STANDARD,
            32, ModProjectilesConfig.QUAD_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> QUINTUPLE_533MM_TORPEDO = catalogLauncher("quintuple_533mm_torpedo", 533, 5, EquipmentTier.IMPROVED,
            24, ModProjectilesConfig.QUINTUPLE_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> QUINTUPLE_533MM_TORPEDO_MK17 = catalogLauncher("quintuple_533mm_torpedo_mk17", 533, 5, EquipmentTier.IMPROVED,
            24, ModProjectilesConfig.QUINTUPLE_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> QUINTUPLE_21IN_TORPEDO_MK35 = catalogLauncher("quintuple_21in_torpedo_mk35", 533, 5, EquipmentTier.IMPROVED,
            24, ModProjectilesConfig.QUINTUPLE_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> QUINTUPLE_TORPEDO_LAUNCHER_53_56 = catalogLauncher("quintuple_torpedo_launcher_53_56", 533, 5, EquipmentTier.IMPROVED,
            24, ModProjectilesConfig.QUINTUPLE_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> QUINTUPLE_TORPEDO_LAUNCHER_53_65 = catalogLauncher("quintuple_torpedo_launcher_53_65", 533, 5, EquipmentTier.IMPROVED,
            24, ModProjectilesConfig.QUINTUPLE_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> TRIPLE_61CM_TORPEDO = catalogLauncher("triple_61cm_torpedo", 610, 3, EquipmentTier.STANDARD,
            48, ModProjectilesConfig.TRIPLE_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> TRIPLE_61CM_OXYGEN_TORPEDO = catalogLauncher("triple_61cm_oxygen_torpedo", 610, 3, EquipmentTier.STANDARD,
            48, ModProjectilesConfig.TRIPLE_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> QUAD_61CM_TORPEDO = catalogLauncher("quad_61cm_torpedo", 610, 4, EquipmentTier.IMPROVED,
            32, ModProjectilesConfig.QUAD_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> QUAD_61CM_OXYGEN_TORPEDO = catalogLauncher("quad_61cm_oxygen_torpedo", 610, 4, EquipmentTier.IMPROVED,
            32, ModProjectilesConfig.QUAD_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> TORPEDO_LAUNCHER_61CM_TYPE9 = catalogLauncher("torpedo_launcher_61cm_type9", 610, 4, EquipmentTier.IMPROVED,
            32, ModProjectilesConfig.QUAD_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> QUINTUPLE_61CM_TORPEDO = catalogLauncher("quintuple_61cm_torpedo", 610, 5, EquipmentTier.IMPROVED,
            24, ModProjectilesConfig.QUINTUPLE_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> QUINTUPLE_61CM_OXYGEN_TORPEDO = catalogLauncher("quintuple_61cm_oxygen_torpedo", 610, 5, EquipmentTier.IMPROVED,
            24, ModProjectilesConfig.QUINTUPLE_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> JAPANESE_SEXTUPLE_61CM_TORPEDO = catalogLauncher("japanese_sextuple_61cm_torpedo", 610, 6, EquipmentTier.ADVANCED,
            24, ModProjectilesConfig.QUINTUPLE_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> SEXTUPLE_61CM_TORPEDO_LAUNCHER_93M3 = catalogLauncher("sextuple_61cm_torpedo_launcher_93m3", 610, 6, EquipmentTier.ADVANCED,
            24, ModProjectilesConfig.QUINTUPLE_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> SEPTUPLE_TORPEDO_LAUNCHER = catalogLauncher("septuple_torpedo_launcher", 610, 7, EquipmentTier.ADVANCED,
            24, ModProjectilesConfig.QUINTUPLE_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> SEPTUPLE_TORPEDO_LAUNCHER_93M3 = catalogLauncher("septuple_torpedo_launcher_93m3", 610, 7, EquipmentTier.ADVANCED,
            24, ModProjectilesConfig.QUINTUPLE_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> JAPANESE_TRIPLE_72CM_TORPEDO = catalogLauncher("japanese_triple_72cm_torpedo", 720, 3, EquipmentTier.ADVANCED,
            48, ModProjectilesConfig.TRIPLE_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> QUAD_TYPE0_72CM_TORPEDO = catalogLauncher("quad_type0_72cm_torpedo", 720, 4, EquipmentTier.ADVANCED,
            32, ModProjectilesConfig.QUAD_TORPEDO_LAUNCHER_COOLDOWN::get);
    public static final DeferredItem<TorpedoLauncherItem> CLT_QUAD_TORPEDO_LAUNCHER_TYPE0 = catalogLauncher("clt_quad_torpedo_launcher_type0", 720, 4, EquipmentTier.ADVANCED,
            32, ModProjectilesConfig.QUAD_TORPEDO_LAUNCHER_COOLDOWN::get);

    /** 数值/08 图鉴发射器全表，供创造栏、装填条装饰器遍历。 */
    public static final java.util.List<DeferredItem<TorpedoLauncherItem>> CATALOG_TORPEDO_LAUNCHERS = java.util.List.of(
            TRIPLE_533MM_TORPEDO,
            QUAD_533MM_TORPEDO,
            QUAD_533MM_TORPEDO_UR_HIGH_SPEED,
            QUINTUPLE_533MM_TORPEDO,
            QUINTUPLE_533MM_TORPEDO_MK17,
            QUINTUPLE_21IN_TORPEDO_MK35,
            QUINTUPLE_TORPEDO_LAUNCHER_53_56,
            QUINTUPLE_TORPEDO_LAUNCHER_53_65,
            TRIPLE_61CM_TORPEDO,
            TRIPLE_61CM_OXYGEN_TORPEDO,
            QUAD_61CM_TORPEDO,
            QUAD_61CM_OXYGEN_TORPEDO,
            TORPEDO_LAUNCHER_61CM_TYPE9,
            QUINTUPLE_61CM_TORPEDO,
            QUINTUPLE_61CM_OXYGEN_TORPEDO,
            JAPANESE_SEXTUPLE_61CM_TORPEDO,
            SEXTUPLE_61CM_TORPEDO_LAUNCHER_93M3,
            SEPTUPLE_TORPEDO_LAUNCHER,
            SEPTUPLE_TORPEDO_LAUNCHER_93M3,
            JAPANESE_TRIPLE_72CM_TORPEDO,
            QUAD_TYPE0_72CM_TORPEDO,
            CLT_QUAD_TORPEDO_LAUNCHER_TYPE0);

    private static DeferredItem<TorpedoLauncherItem> catalogLauncher(String id, int caliber, int tubes,
            EquipmentTier tier, int durability, java.util.function.IntSupplier cooldown) {
        return ITEMS.register(id, () -> new TorpedoLauncherItem(
                new Item.Properties().stacksTo(1).durability(durability)
                        .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.TORPEDO),
                caliber, tubes, cooldown, tier));
    }

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

    // ===== 装备专属蓝图（策划决策/数值/09）=====
    /**
     * 注册名 = 装备注册名 + {@code _blueprint}；堆叠 1，改良 UNCOMMON、先进 RARE。
     * 覆盖可合成的火炮、鱼雷发射器、导弹发射器、深弹投射器·先进、改良/先进飞机、
     * 雷达三代与改良/先进引擎（数值/09、火控/08、副本/21）。
     * 初期/标准无需蓝图；弹药只有 VT 与三种三式弹另有蓝图（在 AmmoRecipe 侧）。
     */
    public static final java.util.Map<String, DeferredItem<Item>> EQUIPMENT_BLUEPRINTS = registerEquipmentBlueprints();

    private static java.util.Map<String, DeferredItem<Item>> registerEquipmentBlueprints() {
        java.util.Map<String, DeferredItem<Item>> map = new java.util.LinkedHashMap<>();
        // 火炮：07 表按档位；法系四联380 是表外独立注册的改良炮。
        for (com.piranport.artillery.CannonCatalog.Entry e : com.piranport.artillery.CannonCatalog.ENTRIES) {
            if (e.tier() == EquipmentTier.INITIAL || e.tier() == EquipmentTier.STANDARD) continue;
            if (com.piranport.crafting.WeaponCraftability.isUncraftableId(e.id())) continue;
            registerBlueprint(map, e.id(), e.tier());
        }
        registerBlueprint(map, "french_quad_380mm_gun", EquipmentTier.IMPROVED);
        // 鱼雷发射器：通用三件 + 08 表图鉴发射器，只列可合成的改良/先进。
        registerLauncherBlueprint(map, "quad_torpedo_launcher", EquipmentTier.IMPROVED);
        registerLauncherBlueprint(map, "quintuple_torpedo_launcher", EquipmentTier.IMPROVED);
        registerLauncherBlueprint(map, "quintuple_533mm_torpedo", EquipmentTier.IMPROVED);
        registerLauncherBlueprint(map, "quintuple_533mm_torpedo_mk17", EquipmentTier.IMPROVED);
        registerLauncherBlueprint(map, "quintuple_21in_torpedo_mk35", EquipmentTier.IMPROVED);
        registerLauncherBlueprint(map, "quintuple_torpedo_launcher_53_56", EquipmentTier.IMPROVED);
        registerLauncherBlueprint(map, "quintuple_torpedo_launcher_53_65", EquipmentTier.IMPROVED);
        registerLauncherBlueprint(map, "quad_61cm_torpedo", EquipmentTier.IMPROVED);
        registerLauncherBlueprint(map, "quad_61cm_oxygen_torpedo", EquipmentTier.IMPROVED);
        registerLauncherBlueprint(map, "torpedo_launcher_61cm_type9", EquipmentTier.IMPROVED);
        registerLauncherBlueprint(map, "quintuple_61cm_torpedo", EquipmentTier.IMPROVED);
        registerLauncherBlueprint(map, "quintuple_61cm_oxygen_torpedo", EquipmentTier.IMPROVED);
        registerLauncherBlueprint(map, "japanese_sextuple_61cm_torpedo", EquipmentTier.ADVANCED);
        // 雷达三代（火控/08）：改良、先进各要自己的蓝图；标准不查蓝图，不进池。
        registerBlueprint(map, "improved_surface_radar", EquipmentTier.IMPROVED);
        registerBlueprint(map, "advanced_surface_radar", EquipmentTier.ADVANCED);
        registerBlueprint(map, "improved_aa_radar", EquipmentTier.IMPROVED);
        registerBlueprint(map, "advanced_aa_radar", EquipmentTier.ADVANCED);
        registerBlueprint(map, "improved_fire_control_radar", EquipmentTier.IMPROVED);
        registerBlueprint(map, "advanced_fire_control_radar", EquipmentTier.ADVANCED);
        // 导弹发射器：全部先进档（数值/09）。深弹投射器·先进按改良档。
        registerBlueprint(map, "ship_rocket_launcher", EquipmentTier.ADVANCED);
        registerBlueprint(map, "seacat_launcher", EquipmentTier.ADVANCED);
        registerBlueprint(map, "sea_dart_launcher", EquipmentTier.ADVANCED);
        registerBlueprint(map, "terrier_launcher", EquipmentTier.ADVANCED);
        registerBlueprint(map, "mk14_harpoon_launcher", EquipmentTier.ADVANCED);
        registerBlueprint(map, "sy1_launcher", EquipmentTier.ADVANCED);
        registerBlueprint(map, "depth_charge_launcher_advanced", EquipmentTier.IMPROVED);
        // 飞机：只给已注册的改良/先进机体（数值/09 分档表）。
        registerBlueprint(map, "f4u_corsair", EquipmentTier.IMPROVED);
        registerBlueprint(map, "f6f_hellcat_rocket", EquipmentTier.IMPROVED);
        registerBlueprint(map, "suisei_bomber", EquipmentTier.IMPROVED);
        registerBlueprint(map, "tenzan_torpedo", EquipmentTier.IMPROVED);
        registerBlueprint(map, "tbf_torpedo", EquipmentTier.IMPROVED);
        registerBlueprint(map, "tbf_asw", EquipmentTier.IMPROVED);
        registerBlueprint(map, "saiun_recon", EquipmentTier.IMPROVED);
        registerBlueprint(map, "seiun_kai_bomber", EquipmentTier.ADVANCED);
        registerBlueprint(map, "b25_bomber", EquipmentTier.ADVANCED);
        // 引擎：改良/先进蓝图进 4-2 奖池（副本/21）。
        registerBlueprint(map, "improved_engine", EquipmentTier.IMPROVED);
        registerBlueprint(map, "advanced_engine", EquipmentTier.ADVANCED);
        return java.util.Collections.unmodifiableMap(map);
    }

    private static void registerLauncherBlueprint(java.util.Map<String, DeferredItem<Item>> map, String id,
                                                  EquipmentTier tier) {
        if (com.piranport.crafting.WeaponCraftability.isUncraftableId(id)) return;
        registerBlueprint(map, id, tier);
    }

    private static void registerBlueprint(java.util.Map<String, DeferredItem<Item>> map, String equipmentId,
                                          EquipmentTier tier) {
        var rarity = tier == EquipmentTier.ADVANCED
                ? net.minecraft.world.item.Rarity.RARE
                : net.minecraft.world.item.Rarity.UNCOMMON;
        map.put(equipmentId, ITEMS.register(equipmentId + "_blueprint",
                () -> new Item(new Item.Properties().stacksTo(1).rarity(rarity))));
    }

    private WeaponItems() {}
}
