package com.piranport.registry;

import com.piranport.component.AircraftInfo;
import com.piranport.component.WeaponCategory;
import com.piranport.item.AircraftItem;
import com.piranport.item.EngineItem;
import com.piranport.item.FireControlRadarItem;
import com.piranport.item.RadarItem;
import com.piranport.item.SonarItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AircraftItems {
    private AircraftItems() {}

    // 共享 ModItems 的 DeferredRegister 实例（同一注册表，保证 ID 不冲突）
    private static final DeferredRegister.Items ITEMS = ModItems.ITEMS;

    // ===== Aircraft Squadrons (Phase 18) =====
    public static final DeferredItem<AircraftItem> XTB2D =
            ITEMS.register("xtb2d",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 4, 0, 30f, 1.5f, 22, AircraftInfo.BombingMode.DIVE, false))));

    // ===== Named Aircraft =====

    // --- 鱼雷机 ---
    public static final DeferredItem<AircraftItem> SWORDFISH_TORPEDO =
            ITEMS.register("swordfish_torpedo",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 1, 0, 18f, 1.0f, 18, AircraftInfo.BombingMode.DIVE, false))));

    /** 剑鱼（反潜）— 6×深弹8, HP4, 52节 */
    public static final DeferredItem<AircraftItem> SWORDFISH_ASW =
            ITEMS.register("swordfish_asw",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.ASW,
                                            1200, 6, 0, 8f, 1.0f, 16, AircraftInfo.BombingMode.LEVEL, false))));

    /** TBF（鱼雷）— 1×533鱼雷21, HP5, 56节 */
    public static final DeferredItem<AircraftItem> TBF_TORPEDO =
            ITEMS.register("tbf_torpedo",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 1, 0, 21f, 1.1f, 20, AircraftInfo.BombingMode.DIVE, false))));

    /** TBF（反潜）— 4×深弹8, HP5, 56节 */
    public static final DeferredItem<AircraftItem> TBF_ASW =
            ITEMS.register("tbf_asw",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.ASW,
                                            1200, 4, 0, 8f, 1.1f, 18, AircraftInfo.BombingMode.LEVEL, false))));

    /** 天山（鱼雷）— 610鱼雷24, HP4, 64节 */
    public static final DeferredItem<AircraftItem> TENZAN_TORPEDO =
            ITEMS.register("tenzan_torpedo",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 1, 0, 24f, 1.3f, 20, AircraftInfo.BombingMode.DIVE, false))));

    /** 九七舰攻（鱼雷）— 610鱼雷21, HP4, 64节 */
    public static final DeferredItem<AircraftItem> TYPE97_TORPEDO =
            ITEMS.register("type97_torpedo",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 1, 0, 21f, 1.3f, 18, AircraftInfo.BombingMode.DIVE, false))));

    /** 空中海盗（鱼雷）— 4×533鱼雷12, HP9, 64节 */
    public static final DeferredItem<AircraftItem> SKY_PIRATE_TORPEDO =
            ITEMS.register("sky_pirate_torpedo",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 4, 0, 12f, 1.3f, 22, AircraftInfo.BombingMode.DIVE, false))));

    // --- 俯冲轰炸机 ---
    /** 海燕（轰炸）— 1/咬+俯冲轰炸10, HP4, 56节 */
    public static final DeferredItem<AircraftItem> PETREL_BOMBER =
            ITEMS.register("petrel_bomber",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.DIVE_BOMBER,
                                            1200, 1, 0, 10f, 1.1f, 14, AircraftInfo.BombingMode.DIVE, false))));

    /** 九九舰爆（轰炸）— 俯冲轰炸12, HP4, 64节 */
    public static final DeferredItem<AircraftItem> TYPE99_DIVE_BOMBER =
            ITEMS.register("type99_dive_bomber",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.DIVE_BOMBER,
                                            1200, 1, 0, 12f, 1.3f, 16, AircraftInfo.BombingMode.DIVE, false))));

    /** SBD（轰炸）— 1/咬+俯冲轰炸12, HP5, 64节 */
    public static final DeferredItem<AircraftItem> SBD_DAUNTLESS =
            ITEMS.register("sbd_dauntless",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.DIVE_BOMBER,
                                            1200, 1, 0, 12f, 1.3f, 16, AircraftInfo.BombingMode.DIVE, false))));

    /** 萤火虫AS.MK5（轰炸）— 2/咬+俯冲轰炸14, HP5, 64节 */
    public static final DeferredItem<AircraftItem> FIREFLY_AS_MK5 =
            ITEMS.register("firefly_as_mk5",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.DIVE_BOMBER,
                                            1200, 1, 0, 14f, 1.3f, 18, AircraftInfo.BombingMode.DIVE, false))));

    /** 彗星（轰炸）— 俯冲轰炸18, HP4, 72节 */
    public static final DeferredItem<AircraftItem> SUISEI_BOMBER =
            ITEMS.register("suisei_bomber",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.DIVE_BOMBER,
                                            1200, 1, 0, 18f, 1.6f, 16, AircraftInfo.BombingMode.DIVE, false))));

    // --- 水平轰炸机 ---
    /** 景云改（轰炸）— 水平轰炸26, HP6, 68节 */
    public static final DeferredItem<AircraftItem> SEIUN_KAI_BOMBER =
            ITEMS.register("seiun_kai_bomber",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.LEVEL_BOMBER,
                                            1200, 32, 0, 26f, 1.4f, 16, AircraftInfo.BombingMode.LEVEL, false))));

    /** B25（轰炸）— 水平轰炸30, HP15, 64节 */
    public static final DeferredItem<AircraftItem> B25_BOMBER =
            ITEMS.register("b25_bomber",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.LEVEL_BOMBER,
                                            1200, 32, 0, 30f, 1.3f, 24, AircraftInfo.BombingMode.LEVEL, false))));

    /** XA2J（轰炸）— 水平轰炸46, HP15, 72节 */
    public static final DeferredItem<AircraftItem> XA2J_BOMBER =
            ITEMS.register("xa2j_bomber",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.LEVEL_BOMBER,
                                            1200, 32, 0, 46f, 1.6f, 20, AircraftInfo.BombingMode.LEVEL, false))));

    // --- 战斗机 ---
    /** F6F地狱猫（火箭弹）— 火箭机：对空子弹(2/咬)+对地/海6枚火箭弹(6爆炸伤害), HP6, 72节 */
    public static final DeferredItem<AircraftItem> F6F_HELLCAT_ROCKET =
            ITEMS.register("f6f_hellcat_rocket",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.ROCKET_FIGHTER,
                                            1200, 6, 0, 6f, 1.6f, 14, AircraftInfo.BombingMode.DIVE, false))));

    /** 海喷火 — 3/咬, HP5, 80节 */
    public static final DeferredItem<AircraftItem> SEAFIRE =
            ITEMS.register("seafire",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                                            1200, 64, 0, 3f, 1.8f, 12, AircraftInfo.BombingMode.DIVE, false))));

    /** 零战五二型 — 2/咬, HP5, 80节 */
    public static final DeferredItem<AircraftItem> ZERO_MODEL52 =
            ITEMS.register("zero_model52",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                                            1200, 64, 0, 2f, 1.8f, 10, AircraftInfo.BombingMode.DIVE, false))));

    /** F4F野猫 — 2/咬, HP5, 72节 */
    public static final DeferredItem<AircraftItem> F4F_WILDCAT =
            ITEMS.register("f4f_wildcat",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                                            1200, 64, 0, 2f, 1.6f, 12, AircraftInfo.BombingMode.DIVE, false))));

    /** F4U冰激凌 — 无伤害, HP5, 航速暂无 */
    public static final DeferredItem<AircraftItem> F4U_CORSAIR_ICE =
            ITEMS.register("f4u_corsair_ice",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                                            1200, 0, 0, 0f, 1.2f, 14, AircraftInfo.BombingMode.DIVE, false))));

    /** F4U海盗 — 3/咬+6×火箭弹6, HP5, 80节 */
    public static final DeferredItem<AircraftItem> F4U_CORSAIR =
            ITEMS.register("f4u_corsair",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                                            1200, 64, 0, 3f, 1.8f, 14, AircraftInfo.BombingMode.DIVE, false))));

    /** F2H女妖 — 5/咬, HP8, 100节 */
    public static final DeferredItem<AircraftItem> F2H_BANSHEE =
            ITEMS.register("f2h_banshee",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                                            1200, 64, 0, 5f, 2.4f, 14, AircraftInfo.BombingMode.DIVE, false))));

    // --- 侦察机 ---
    /** 零式水侦 — HP4, 航程10240, 160节 */
    public static final DeferredItem<AircraftItem> TYPE0_RECON =
            ITEMS.register("type0_recon",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.RECON,
                                            10240, 0, 0, 0f, 3.5f, 8, AircraftInfo.BombingMode.DIVE, false))));

    /** C-1侦察机 — HP5, 航程12800, 120节 */
    public static final DeferredItem<AircraftItem> C1_RECON =
            ITEMS.register("c1_recon",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.RECON,
                                            12800, 0, 0, 0f, 2.5f, 8, AircraftInfo.BombingMode.DIVE, false))));

    /** 彩云舰侦 — HP5, 航程25600, 200节 */
    public static final DeferredItem<AircraftItem> SAIUN_RECON =
            ITEMS.register("saiun_recon",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.RECON,
                                            25600, 0, 0, 0f, 4.5f, 8, AircraftInfo.BombingMode.DIVE, false))));

    // ===== Sonar =====
    public static final DeferredItem<SonarItem> STANDARD_SONAR =
            ITEMS.register("standard_sonar",
                    () -> new SonarItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR),
                            10, 24));

    // Phase 27：策划 §3.6 表 3.2 改进型/先进型声呐
    public static final DeferredItem<SonarItem> IMPROVED_SONAR =
            ITEMS.register("improved_sonar",
                    () -> new SonarItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR),
                            3, 32));

    public static final DeferredItem<SonarItem> ADVANCED_SONAR =
            ITEMS.register("advanced_sonar",
                    () -> new SonarItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR),
                            5, 40));

    // ===== Radar =====
    // 两台雷达各注册一台，索敌范围统一 32 区块、负重 1；
    // 差异化只体现在「索敌目标」上（对海/对空），二者互斥。
    public static final DeferredItem<RadarItem> STANDARD_SURFACE_RADAR =
            ITEMS.register("standard_surface_radar",
                    () -> new RadarItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR),
                            1, 32, RadarItem.RadarTarget.SURFACE));

    public static final DeferredItem<RadarItem> STANDARD_AIR_RADAR =
            ITEMS.register("standard_air_radar",
                    () -> new RadarItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR),
                            1, 32, RadarItem.RadarTarget.AIR));

    // ===== Fire Control Radar =====
    // 与上面三台索敌雷达是不同东西：那三台是「自动扫描并高亮目标」，这台是「手动开关的
    // 准星吸附（火炮瞄准辅助）」。所以它是 FireControlRadarItem 而不是 RadarItem 的第四个枚举值
    // —— 两者的驱动方式（自动 tick 扫描 vs 玩家按 0 键切换）与作用对象（高亮 vs 准星）都不同。
    public static final DeferredItem<FireControlRadarItem> STANDARD_FIRE_CONTROL_RADAR =
            ITEMS.register("standard_fire_control_radar",
                    () -> new FireControlRadarItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR),
                            1, 32));

    // ===== Engines =====
    public static final DeferredItem<EngineItem> STANDARD_ENGINE =
            ITEMS.register("standard_engine",
                    () -> new EngineItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ENGINE), 0.05, 5));
    public static final DeferredItem<EngineItem> IMPROVED_ENGINE =
            ITEMS.register("improved_engine",
                    () -> new EngineItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ENGINE), 0.10, 10));
    public static final DeferredItem<EngineItem> ADVANCED_ENGINE =
            ITEMS.register("advanced_engine",
                    () -> new EngineItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ENGINE), 0.15, 15));
    public static final DeferredItem<EngineItem> HIGH_PRESSURE_BOILER =
            ITEMS.register("high_pressure_boiler",
                    () -> new EngineItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ENGINE), 0.12, 20));
    public static final DeferredItem<EngineItem> DIESEL_ENGINE =
            ITEMS.register("diesel_engine",
                    () -> new EngineItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ENGINE), 0.08, 2));
}
