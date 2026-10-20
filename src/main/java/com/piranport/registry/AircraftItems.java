package com.piranport.registry;

import com.piranport.component.AircraftState;
import com.piranport.component.WeaponCategory;
import com.piranport.item.AircraftItem;
import com.piranport.item.EngineItem;
import com.piranport.item.FireControlRadarItem;
import com.piranport.item.RadarItem;
import com.piranport.item.SonarItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class AircraftItems {
    private AircraftItems() {}

    // 共享 ModItems 的 DeferredRegister 实例（同一注册表，保证 ID 不冲突）
    private static final DeferredRegister.Items ITEMS = ModItems.ITEMS;

    /** 飞机物品只保存运行时状态；静态数值全部来自数据包定义。 */
    private static DeferredItem<AircraftItem> aircraft(String id) {
        return ITEMS.register(id, () -> new AircraftItem(new Item.Properties().stacksTo(1)
                .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                .component(ModDataComponents.AIRCRAFT_STATE.get(), AircraftState.EMPTY)));
    }

    // ===== Aircraft =====
    public static final DeferredItem<AircraftItem> XTB2D = aircraft("xtb2d");

    // ===== Named Aircraft =====

    // --- 鱼雷机 ---
    public static final DeferredItem<AircraftItem> SWORDFISH_TORPEDO = aircraft("swordfish_torpedo");

    /** 剑鱼（反潜）— 6×深弹8, HP4, 52节 */
    public static final DeferredItem<AircraftItem> SWORDFISH_ASW = aircraft("swordfish_asw");

    /** TBF（鱼雷）— 1×533鱼雷21, HP5, 56节 */
    public static final DeferredItem<AircraftItem> TBF_TORPEDO = aircraft("tbf_torpedo");

    /** TBF（反潜）— 4×深弹8, HP5, 56节 */
    public static final DeferredItem<AircraftItem> TBF_ASW = aircraft("tbf_asw");

    /** 天山（鱼雷）— 610鱼雷24, HP4, 64节 */
    public static final DeferredItem<AircraftItem> TENZAN_TORPEDO = aircraft("tenzan_torpedo");

    /** 九七舰攻（鱼雷）— 610鱼雷21, HP4, 64节 */
    public static final DeferredItem<AircraftItem> TYPE97_TORPEDO = aircraft("type97_torpedo");

    /** 空中海盗（鱼雷）— 4×533鱼雷12, HP9, 64节 */
    public static final DeferredItem<AircraftItem> SKY_PIRATE_TORPEDO = aircraft("sky_pirate_torpedo");

    // --- 俯冲轰炸机 ---
    /** 海燕（轰炸）— 1/咬+俯冲轰炸10, HP4, 56节 */
    public static final DeferredItem<AircraftItem> PETREL_BOMBER = aircraft("petrel_bomber");

    /** 九九舰爆（轰炸）— 俯冲轰炸12, HP4, 64节 */
    public static final DeferredItem<AircraftItem> TYPE99_DIVE_BOMBER = aircraft("type99_dive_bomber");

    /** SBD（轰炸）— 1/咬+俯冲轰炸12, HP5, 64节 */
    public static final DeferredItem<AircraftItem> SBD_DAUNTLESS = aircraft("sbd_dauntless");

    /** 萤火虫AS.MK5（轰炸）— 2/咬+俯冲轰炸14, HP5, 64节 */
    public static final DeferredItem<AircraftItem> FIREFLY_AS_MK5 = aircraft("firefly_as_mk5");

    /** 彗星（轰炸）— 俯冲轰炸18, HP4, 72节 */
    public static final DeferredItem<AircraftItem> SUISEI_BOMBER = aircraft("suisei_bomber");

    // --- 水平轰炸机 ---
    /** 景云改（轰炸）— 水平轰炸26, HP6, 68节 */
    public static final DeferredItem<AircraftItem> SEIUN_KAI_BOMBER = aircraft("seiun_kai_bomber");

    /** B25（轰炸）— 水平轰炸30, HP15, 64节 */
    public static final DeferredItem<AircraftItem> B25_BOMBER = aircraft("b25_bomber");

    /** XA2J（轰炸）— 水平轰炸46, HP15, 72节 */
    public static final DeferredItem<AircraftItem> XA2J_BOMBER = aircraft("xa2j_bomber");

    // --- 战斗机 ---
    /** F6F地狱猫（火箭弹）— 火箭机：对空子弹(2/咬)+对地/海6枚火箭弹(6爆炸伤害), HP6, 72节 */
    public static final DeferredItem<AircraftItem> F6F_HELLCAT_ROCKET = aircraft("f6f_hellcat_rocket");

    /** 海喷火 — 3/咬, HP5, 80节 */
    public static final DeferredItem<AircraftItem> SEAFIRE = aircraft("seafire");

    /** 零战五二型 — 2/咬, HP5, 80节 */
    public static final DeferredItem<AircraftItem> ZERO_MODEL52 = aircraft("zero_model52");

    /** F4F野猫 — 2/咬, HP5, 72节 */
    public static final DeferredItem<AircraftItem> F4F_WILDCAT = aircraft("f4f_wildcat");

    /** F4U冰激凌 — 无伤害, HP5, 航速暂无 */
    public static final DeferredItem<AircraftItem> F4U_CORSAIR_ICE = aircraft("f4u_corsair_ice");

    /** F4U海盗 — 3/咬+6×火箭弹6, HP5, 80节 */
    public static final DeferredItem<AircraftItem> F4U_CORSAIR = aircraft("f4u_corsair");

    /** F2H女妖 — 5/咬, HP8, 100节 */
    public static final DeferredItem<AircraftItem> F2H_BANSHEE = aircraft("f2h_banshee");

    // --- 以下机型美术未到位，模型与贴图为占位（见 AircraftVisualIds.PLACEHOLDER），待替换 ---
    /** 九六舰战（战斗）— 1/咬 */
    public static final DeferredItem<AircraftItem> TYPE96_FIGHTER = aircraft("type96_fighter");
    /** F2A水牛（战斗）— 1/咬 */
    public static final DeferredItem<AircraftItem> F2A_BUFFALO = aircraft("f2a_buffalo");
    /** 烈风（战斗）— 4/咬 */
    public static final DeferredItem<AircraftItem> REPPU = aircraft("reppu");
    /** 海毒牙（战斗）— 4/咬 */
    public static final DeferredItem<AircraftItem> SEAFANG = aircraft("seafang");
    /** 飞行圆饼 XF5U（战斗）— 3/咬 */
    public static final DeferredItem<AircraftItem> XF5U = aircraft("xf5u");
    /** F4U-7（火箭）— 8/枚火箭弹 */
    public static final DeferredItem<AircraftItem> F4U7 = aircraft("f4u7");
    /** Ju87C斯图卡（俯冲轰炸）— 俯冲轰炸13 */
    public static final DeferredItem<AircraftItem> JU87C = aircraft("ju87c");
    /** BTD毁灭者（俯冲轰炸）— 俯冲轰炸16 */
    public static final DeferredItem<AircraftItem> BTD_DESTROYER = aircraft("btd_destroyer");
    /** 飞龙（俯冲轰炸）— 俯冲轰炸22，不能挂核航弹 */
    public static final DeferredItem<AircraftItem> WYVERN = aircraft("wyvern");
    /** 天袭者（俯冲轰炸）— 俯冲轰炸12 × 3 */
    public static final DeferredItem<AircraftItem> SKYRAIDER = aircraft("skyraider");
    /** 梭鱼（鱼雷）— 鱼雷20 */
    public static final DeferredItem<AircraftItem> BARRACUDA = aircraft("barracuda");
    /** 流星（鱼雷）— 鱼雷28 */
    public static final DeferredItem<AircraftItem> RYUSEI = aircraft("ryusei");
    /** 天河（鱼雷）— 鱼雷26 */
    public static final DeferredItem<AircraftItem> TENGA = aircraft("tenga");

    // --- 深海编队 ---
    // 敌方航母使用的舰载机，数值见 ../docs/策划决策/数值配置/04-飞机数值表.md「深海编队数值表」。
    // 注册名与 data/piranport/aircraft/<id>.json 一一对应，定义 ID 由文件路径生成。

    /** 深海战斗机 1 型 — 1/咬, HP16, 12.8m/s */
    public static final DeferredItem<AircraftItem> DEEP_SEA_FIGHTER_1 = aircraft("deep_sea_fighter_1");

    /** 深海战斗机 2 型 — 3/咬, HP24, 14.4m/s */
    public static final DeferredItem<AircraftItem> DEEP_SEA_FIGHTER_2 = aircraft("deep_sea_fighter_2");

    /** 深海轰炸机 1 型 — 俯冲轰炸16, HP20, 9.6m/s */
    public static final DeferredItem<AircraftItem> DEEP_SEA_BOMBER_1 = aircraft("deep_sea_bomber_1");

    /** 深海轰炸机 2 型 — 俯冲轰炸24, HP30, 12.8m/s */
    public static final DeferredItem<AircraftItem> DEEP_SEA_BOMBER_2 = aircraft("deep_sea_bomber_2");

    /** 深海鱼雷机 1 型 — 鱼雷24, HP20, 9.6m/s */
    public static final DeferredItem<AircraftItem> DEEP_SEA_TORPEDO_BOMBER_1 = aircraft("deep_sea_torpedo_bomber_1");

    /** 深海鱼雷机 2 型 — 鱼雷32, HP28, 12.8m/s */
    public static final DeferredItem<AircraftItem> DEEP_SEA_TORPEDO_BOMBER_2 = aircraft("deep_sea_torpedo_bomber_2");

    // --- 侦察机 ---
    /** 零式水侦 — HP4, 航程10240, 160节 */
    public static final DeferredItem<AircraftItem> TYPE0_RECON = aircraft("type0_recon");

    /** C-1侦察机 — HP5, 航程12800, 120节 */
    public static final DeferredItem<AircraftItem> C1_RECON = aircraft("c1_recon");

    /** 彩云舰侦 — HP5, 航程25600, 200节 */
    public static final DeferredItem<AircraftItem> SAIUN_RECON = aircraft("saiun_recon");

    /** 所有飞机物品的注册名。创建标签、调试命令与测试从这里遍历。 */
    public static final List<DeferredItem<AircraftItem>> AIRCRAFT_ITEMS = List.of(
            XTB2D, SWORDFISH_TORPEDO, SWORDFISH_ASW, TBF_TORPEDO, TBF_ASW,
            TENZAN_TORPEDO, TYPE97_TORPEDO, SKY_PIRATE_TORPEDO,
            PETREL_BOMBER, TYPE99_DIVE_BOMBER, SBD_DAUNTLESS, FIREFLY_AS_MK5, SUISEI_BOMBER,
            SEIUN_KAI_BOMBER, B25_BOMBER, XA2J_BOMBER,
            F6F_HELLCAT_ROCKET, SEAFIRE, ZERO_MODEL52, F4F_WILDCAT, F4U_CORSAIR_ICE,
            F4U_CORSAIR, F2H_BANSHEE,
            TYPE96_FIGHTER, F2A_BUFFALO, REPPU, SEAFANG, XF5U, F4U7,
            JU87C, BTD_DESTROYER, WYVERN, SKYRAIDER,
            BARRACUDA, RYUSEI, TENGA,
            TYPE0_RECON, C1_RECON, SAIUN_RECON,
            DEEP_SEA_FIGHTER_1, DEEP_SEA_FIGHTER_2,
            DEEP_SEA_BOMBER_1, DEEP_SEA_BOMBER_2,
            DEEP_SEA_TORPEDO_BOMBER_1, DEEP_SEA_TORPEDO_BOMBER_2);

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
    // 策划决策/火控/08：对海、对空各三代，共六件；标准 32、改良 48、先进 64 格，
    // 负重统一 4/6/8。标准不查蓝图，改良/先进各要自己的蓝图。
    public static final DeferredItem<RadarItem> STANDARD_SURFACE_RADAR =
            ITEMS.register("standard_surface_radar",
                    () -> radar(4, 32, RadarItem.RadarTarget.SURFACE));
    public static final DeferredItem<RadarItem> IMPROVED_SURFACE_RADAR =
            ITEMS.register("improved_surface_radar",
                    () -> radar(6, 48, RadarItem.RadarTarget.SURFACE));
    public static final DeferredItem<RadarItem> ADVANCED_SURFACE_RADAR =
            ITEMS.register("advanced_surface_radar",
                    () -> radar(8, 64, RadarItem.RadarTarget.SURFACE));

    public static final DeferredItem<RadarItem> STANDARD_AIR_RADAR =
            ITEMS.register("standard_aa_radar",
                    () -> radar(4, 32, RadarItem.RadarTarget.AIR));
    public static final DeferredItem<RadarItem> IMPROVED_AA_RADAR =
            ITEMS.register("improved_aa_radar",
                    () -> radar(6, 48, RadarItem.RadarTarget.AIR));
    public static final DeferredItem<RadarItem> ADVANCED_AA_RADAR =
            ITEMS.register("advanced_aa_radar",
                    () -> radar(8, 64, RadarItem.RadarTarget.AIR));

    private static RadarItem radar(int weight, int range, RadarItem.RadarTarget target) {
        return new RadarItem(new Item.Properties().stacksTo(1)
                .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR),
                weight, range, target);
    }

    // ===== Fire Control Radar =====
    // 与上面两台索敌雷达是不同东西：那两台是「自动扫描并高亮目标」，这台不给玩家做任何
    // 高亮/瞄准辅助，而是给炮弹做追踪（策划决策/火控/05）。所以它是 FireControlRadarItem
    // 而不是 RadarItem 的第四个枚举值 —— 两者作用对象（高亮目标 vs 偏转炮弹）完全不同。
    public static final DeferredItem<FireControlRadarItem> STANDARD_FIRE_CONTROL_RADAR =
            ITEMS.register("standard_fire_control_radar", () -> fireControlRadar(4, 1));
    public static final DeferredItem<FireControlRadarItem> IMPROVED_FIRE_CONTROL_RADAR =
            ITEMS.register("improved_fire_control_radar", () -> fireControlRadar(6, 2));
    public static final DeferredItem<FireControlRadarItem> ADVANCED_FIRE_CONTROL_RADAR =
            ITEMS.register("advanced_fire_control_radar", () -> fireControlRadar(8, 3));

    private static FireControlRadarItem fireControlRadar(int weight, int level) {
        return new FireControlRadarItem(new Item.Properties().stacksTo(1)
                .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR),
                weight, level);
    }

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
