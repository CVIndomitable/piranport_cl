package com.piranport.registry;

import com.piranport.component.WeaponCategory;
import com.piranport.entitycore.EntityCoreDefinitions;
import com.piranport.item.AbyssalReportItem;
import com.piranport.item.ArtilleryConfigToolItem;
import com.piranport.item.CommandSwordItem;
import com.piranport.item.ConfigInspectorItem;
import com.piranport.item.DamageControlItem;
import com.piranport.item.EntityCoreItem;
import com.piranport.item.EugenShieldItem;
import com.piranport.item.ExperienceShellItem;
import com.piranport.item.FlareLauncherItem;
import com.piranport.item.FootballArmorItem;
import com.piranport.item.GungnirItem;
import com.piranport.item.HatsuyukiMainGunItem;
import com.piranport.item.KirinHeadbandItem;
import com.piranport.item.MysteriousWeaponItem;
import com.piranport.item.RepairKitItem;
import com.piranport.item.ShipGirlContractItem;
import com.piranport.item.ShoukakuScytheItem;
import com.piranport.item.SkinCoreItem;
import com.piranport.item.SmokeCandleItem;
import com.piranport.item.TaihouUmbrellaItem;
import com.piranport.item.TooltipItem;
import com.piranport.item.TorpedoReloadItem;
import com.piranport.item.UnicornHarpItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 特殊物品注册表（鱼雷装填强化 / 副本系统 / 皮肤核心 / 实体核心 / 燃料 / 工具 / 道具 /
 * 快速维修 / 配置检查器 / 炮术配置工具 / 烟幕蜡烛 / 信号弹发射器 / 修理包 / 麒麟头带 /
 * 神秘武器 / 黎塞留指挥剑 / 舰娘契约 / 大凤伞 / 欧根盾 / 翔鹤镰刀 / 道具栏图标 /
 * 足球巨星套装 / 初雪主炮 / 冈格尼尔 / 深海遗迹奖励 / 深海生成蛋）。
 *
 * <p>共享 {@link ModItems#ITEMS} 同一个 DeferredRegister，避免重复注册。
 * 通过 {@link ModItems} 的同名静态字段以薄包装方式对外暴露，保持原有调用方不变。</p>
 */
public final class SpecialtyItems {
    private SpecialtyItems() {}

    // 共享 ModItems 的注册表
    private static final DeferredRegister.Items ITEMS = ModItems.ITEMS;

    // ===== Torpedo Reload Enhancement =====
    public static final DeferredItem<TorpedoReloadItem> TORPEDO_RELOAD =
            ITEMS.register("torpedo_reload",
                    () -> new TorpedoReloadItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR), 8));

    // ===== Dungeon System (v0.0.8) =====
    public static final DeferredItem<com.piranport.dungeon.key.DungeonKeyItem> DUNGEON_KEY =
            ITEMS.register("dungeon_key",
                    () -> new com.piranport.dungeon.key.DungeonKeyItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.DUNGEON_STAGE_ID.get(), "")
                            .component(ModDataComponents.DUNGEON_PROGRESS.get(),
                                    com.piranport.dungeon.key.DungeonProgress.EMPTY)));

    public static final DeferredItem<com.piranport.dungeon.item.TownScrollItem> TOWN_SCROLL =
            ITEMS.register("town_scroll",
                    () -> new com.piranport.dungeon.item.TownScrollItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> DUNGEON_LECTERN =
            ITEMS.registerSimpleBlockItem(ModBlocks.DUNGEON_LECTERN);

    // ===== Skin Cores =====
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_4 =
            ITEMS.register("skin_core_4",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 4));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_5 =
            ITEMS.register("skin_core_5",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 5));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_6 =
            ITEMS.register("skin_core_6",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 6));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_7 =
            ITEMS.register("skin_core_7",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 7));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_8 =
            ITEMS.register("skin_core_8",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 8));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_9 =
            ITEMS.register("skin_core_9",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 9));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_10 =
            ITEMS.register("skin_core_10",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 10));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_11 =
            ITEMS.register("skin_core_11",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 11));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_12 =
            ITEMS.register("skin_core_12",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 12));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_13 =
            ITEMS.register("skin_core_13",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 13));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_14 =
            ITEMS.register("skin_core_14",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 14));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_15 =
            ITEMS.register("skin_core_15",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 15));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_16 =
            ITEMS.register("skin_core_16",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 16));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_17 =
            ITEMS.register("skin_core_17",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 17));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_18 =
            ITEMS.register("skin_core_18",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 18));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_19 =
            ITEMS.register("skin_core_19",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 19));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_20 =
            ITEMS.register("skin_core_20",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 20));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_21 =
            ITEMS.register("skin_core_21",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 21));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_22 =
            ITEMS.register("skin_core_22",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 22));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_23 =
            ITEMS.register("skin_core_23",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 23));

    // ===== Entity Cores =====
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_SUPPLY =
            ITEMS.register("entity_core_deep_ocean_supply",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_SUPPLY));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_ARCHIVIST =
            ITEMS.register("entity_core_deep_ocean_archivist",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_ARCHIVIST));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_ENGINEER =
            ITEMS.register("entity_core_deep_ocean_engineer",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_ENGINEER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_NAVIGATOR =
            ITEMS.register("entity_core_deep_ocean_navigator",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_NAVIGATOR));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_QUARTERMASTER =
            ITEMS.register("entity_core_deep_ocean_quartermaster",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_QUARTERMASTER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_DESTROYER =
            ITEMS.register("entity_core_deep_ocean_destroyer",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_DESTROYER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_LIGHT_CRUISER =
            ITEMS.register("entity_core_deep_ocean_light_cruiser",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_LIGHT_CRUISER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_HEAVY_CRUISER =
            ITEMS.register("entity_core_deep_ocean_heavy_cruiser",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_HEAVY_CRUISER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_BATTLE_CRUISER =
            ITEMS.register("entity_core_deep_ocean_battle_cruiser",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_BATTLE_CRUISER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_BATTLESHIP =
            ITEMS.register("entity_core_deep_ocean_battleship",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_BATTLESHIP));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_LIGHT_CARRIER =
            ITEMS.register("entity_core_deep_ocean_light_carrier",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_LIGHT_CARRIER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_CARRIER =
            ITEMS.register("entity_core_deep_ocean_carrier",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_CARRIER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_SUBMARINE =
            ITEMS.register("entity_core_deep_ocean_submarine",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_SUBMARINE));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_FLAGSHIP =
            ITEMS.register("entity_core_deep_ocean_flagship",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_FLAGSHIP));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_SHIP_GIRL =
            ITEMS.register("entity_core_ship_girl",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.SHIP_GIRL));

    // ===== Fuel =====
    public static final DeferredItem<Item> FUEL =
            ITEMS.registerSimpleItem("fuel");

    // ===== Tools =====
    public static final DeferredItem<UnicornHarpItem> UNICORN_HARP =
            ITEMS.register("unicorn_harp",
                    () -> new UnicornHarpItem(new Item.Properties().stacksTo(1)));

    // ===== 道具 =====
    public static final DeferredItem<Item> ELITE_DAMAGE_CONTROL =
            ITEMS.register("elite_damage_control",
                    () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<DamageControlItem> DAMAGE_CONTROL =
            ITEMS.register("damage_control",
                    () -> new DamageControlItem(new Item.Properties().stacksTo(1)));

    // ===== Quick Repair =====
    public static final DeferredItem<com.piranport.item.QuickRepairItem> QUICK_REPAIR =
            ITEMS.register("quick_repair",
                    () -> new com.piranport.item.QuickRepairItem(new Item.Properties().stacksTo(1)));

    // ===== Config Inspector =====
    public static final DeferredItem<ConfigInspectorItem> CONFIG_INSPECTOR =
            ITEMS.register("config_inspector",
                    () -> new ConfigInspectorItem(new Item.Properties().stacksTo(1)));

    // ===== Artillery Config Tool =====
    public static final DeferredItem<ArtilleryConfigToolItem> ARTILLERY_CONFIG_TOOL =
            ITEMS.register("artillery_config_tool",
                    () -> new ArtilleryConfigToolItem(new Item.Properties().stacksTo(1)));

    // ===== Smoke Candle =====
    public static final DeferredItem<SmokeCandleItem> SMOKE_CANDLE =
            ITEMS.register("smoke_candle",
                    () -> new SmokeCandleItem(new Item.Properties().stacksTo(1).durability(128)));

    // ===== Flare Launcher =====
    public static final DeferredItem<FlareLauncherItem> FLARE_LAUNCHER =
            ITEMS.register("flare_launcher",
                    () -> new FlareLauncherItem(new Item.Properties().stacksTo(1).durability(4096)));

    // ===== Repair Kit =====
    public static final DeferredItem<RepairKitItem> REPAIR_KIT =
            ITEMS.register("repair_kit",
                    () -> new RepairKitItem(new Item.Properties().stacksTo(1)));

    // ===== Kirin Headband =====
    public static final DeferredItem<KirinHeadbandItem> KIRIN_HEADBAND =
            ITEMS.register("kirin_headband",
                    () -> new KirinHeadbandItem(new Item.Properties().stacksTo(1)));

    // ===== Mysterious Weapon =====
    public static final DeferredItem<MysteriousWeaponItem> MYSTERIOUS_WEAPON =
            ITEMS.register("mysterious_weapon",
                    () -> new MysteriousWeaponItem(new Item.Properties().stacksTo(1).durability(128)));

    // ===== Richelieu's Command Sword =====
    public static final DeferredItem<CommandSwordItem> RICHELIEU_COMMAND_SWORD =
            ITEMS.register("richelieu_command_sword",
                    () -> new CommandSwordItem(new Item.Properties().stacksTo(1)));

    // ===== Ship Girl Contract =====
    public static final DeferredItem<ShipGirlContractItem> SHIP_GIRL_CONTRACT =
            ITEMS.register("ship_girl_contract",
                    () -> new ShipGirlContractItem(new Item.Properties().stacksTo(1),
                            "tooltip.piranport.ship_girl_contract"));

    // ===== Taihou's Umbrella (Shield) =====
    public static final DeferredItem<TaihouUmbrellaItem> TAIHOU_UMBRELLA =
            ITEMS.register("taihou_umbrella",
                    () -> new TaihouUmbrellaItem(new Item.Properties().stacksTo(1)
                            .durability(1520)
                            .attributes(TaihouUmbrellaItem.createAttributes())));

    // ===== Eugen's Ship Shield =====
    public static final DeferredItem<EugenShieldItem> EUGEN_SHIELD =
            ITEMS.register("eugen_shield",
                    () -> new EugenShieldItem(new Item.Properties().stacksTo(1)
                            .durability(1200)
                            .attributes(EugenShieldItem.createAttributes())));

    // ===== Shoukaku's Scythe (策划 §3.5 表 3.5; 深海翔鹤/旗舰掉落) =====
    public static final DeferredItem<ShoukakuScytheItem> SHOUKAKU_SCYTHE =
            ITEMS.register("shoukaku_scythe",
                    () -> new ShoukakuScytheItem(new Item.Properties().stacksTo(1)
                            .durability(1024)));

    // ===== Props Tab Icon =====
    public static final DeferredItem<Item> HENTAI_TROPHY =
            ITEMS.registerSimpleItem("hentai_trophy");

    // ===== Football Superstar Set (足球巨星套装) =====
    public static final DeferredItem<FootballArmorItem> SPIDER_GLOVES =
            ITEMS.register("spider_gloves",
                    () -> new FootballArmorItem(ModArmorMaterials.FOOTBALL,
                            net.minecraft.world.item.ArmorItem.Type.HELMET,
                            new Item.Properties().durability(
                                    net.minecraft.world.item.ArmorItem.Type.HELMET.getDurability(10))));
    public static final DeferredItem<FootballArmorItem> BLUE_JERSEY =
            ITEMS.register("blue_jersey",
                    () -> new FootballArmorItem(ModArmorMaterials.FOOTBALL,
                            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                            new Item.Properties().durability(
                                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE.getDurability(10))));
    public static final DeferredItem<FootballArmorItem> RED_BLACK_SOCKS =
            ITEMS.register("red_black_socks",
                    () -> new FootballArmorItem(ModArmorMaterials.FOOTBALL,
                            net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
                            new Item.Properties().durability(
                                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS.getDurability(10))));
    public static final DeferredItem<FootballArmorItem> MIRACLE_BOOTS =
            ITEMS.register("miracle_boots",
                    () -> new FootballArmorItem(ModArmorMaterials.FOOTBALL,
                            net.minecraft.world.item.ArmorItem.Type.BOOTS,
                            new Item.Properties().durability(
                                    net.minecraft.world.item.ArmorItem.Type.BOOTS.getDurability(10))));

    // ===== Hatsuyuki's Main Gun (初雪的主炮) =====
    public static final DeferredItem<HatsuyukiMainGunItem> HATSUYUKI_MAIN_GUN =
            ITEMS.register("hatsuyuki_main_gun",
                    () -> new HatsuyukiMainGunItem(new Item.Properties().stacksTo(1)));

    // ===== Gungnir (冈格尼尔) =====
    public static final DeferredItem<GungnirItem> GUNGNIR =
            ITEMS.register("gungnir",
                    () -> new GungnirItem(new Item.Properties()
                            .durability(512)
                            .attributes(GungnirItem.createAttributes())
                            .stacksTo(1)));

    // ===== v0.0.11 Ruins — Reward Items =====
    // Abyssal Report (深海作战档案)
    public static final DeferredItem<AbyssalReportItem> ABYSSAL_REPORT =
            ITEMS.register("abyssal_report",
                    () -> new AbyssalReportItem(new Item.Properties().stacksTo(16),
                            "tooltip.piranport.abyssal_report"));

    // Chaos Shards (无序意志碎片 α~ι)
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_ALPHA =
            ITEMS.register("chaos_shard_alpha",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_BETA =
            ITEMS.register("chaos_shard_beta",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_GAMMA =
            ITEMS.register("chaos_shard_gamma",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_DELTA =
            ITEMS.register("chaos_shard_delta",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_EPSILON =
            ITEMS.register("chaos_shard_epsilon",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_ZETA =
            ITEMS.register("chaos_shard_zeta",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_ETA =
            ITEMS.register("chaos_shard_eta",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_THETA =
            ITEMS.register("chaos_shard_theta",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_IOTA =
            ITEMS.register("chaos_shard_iota",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));

    // Portal Activation Core (传送门激活核心)
    public static final DeferredItem<TooltipItem> PORTAL_ACTIVATION_CORE =
            ITEMS.register("portal_activation_core",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.portal_activation_core"));

    // National Flags (各国国旗)
    public static final DeferredItem<TooltipItem> FLAG_J =
            ITEMS.register("flag_j",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.flag"));
    public static final DeferredItem<TooltipItem> FLAG_E =
            ITEMS.register("flag_e",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.flag"));
    public static final DeferredItem<TooltipItem> FLAG_U =
            ITEMS.register("flag_u",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.flag"));
    public static final DeferredItem<TooltipItem> FLAG_G =
            ITEMS.register("flag_g",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.flag"));
    public static final DeferredItem<TooltipItem> FLAG_F =
            ITEMS.register("flag_f",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.flag"));
    public static final DeferredItem<TooltipItem> FLAG_I =
            ITEMS.register("flag_i",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.flag"));
    public static final DeferredItem<TooltipItem> FLAG_C =
            ITEMS.register("flag_c",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.flag"));

    // Experience Shell (经验炮弹)
    public static final DeferredItem<ExperienceShellItem> EXP_SHELL =
            ITEMS.register("exp_shell",
                    () -> new ExperienceShellItem(new Item.Properties(),
                            "tooltip.piranport.exp_shell"));

    // ===== Deep Ocean Spawn Eggs (深海生成蛋) =====
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_SUPPLY_SPAWN_EGG =
            ITEMS.register("deep_ocean_supply_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_SUPPLY,
                            0x2D2D3D, 0x8888AA, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_ARCHIVIST_SPAWN_EGG =
            ITEMS.register("deep_ocean_archivist_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_ARCHIVIST,
                            0x1C2638, 0xB8A6FF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_ENGINEER_SPAWN_EGG =
            ITEMS.register("deep_ocean_engineer_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_ENGINEER,
                            0x202A2D, 0x66D6C8, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_NAVIGATOR_SPAWN_EGG =
            ITEMS.register("deep_ocean_navigator_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_NAVIGATOR,
                            0x17223D, 0x77B7FF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_QUARTERMASTER_SPAWN_EGG =
            ITEMS.register("deep_ocean_quartermaster_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_QUARTERMASTER,
                            0x242735, 0xE3B85A, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_DESTROYER_SPAWN_EGG =
            ITEMS.register("deep_ocean_destroyer_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_DESTROYER,
                            0x2D2D3D, 0xCC4444, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_LIGHT_CRUISER_SPAWN_EGG =
            ITEMS.register("deep_ocean_light_cruiser_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_LIGHT_CRUISER,
                            0x2D2D3D, 0xDD8844, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_HEAVY_CRUISER_SPAWN_EGG =
            ITEMS.register("deep_ocean_heavy_cruiser_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_HEAVY_CRUISER,
                            0x2D2D3D, 0xAA6622, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_BATTLE_CRUISER_SPAWN_EGG =
            ITEMS.register("deep_ocean_battle_cruiser_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_BATTLE_CRUISER,
                            0x2D2D3D, 0x884488, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_BATTLESHIP_SPAWN_EGG =
            ITEMS.register("deep_ocean_battleship_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_BATTLESHIP,
                            0x2D2D3D, 0x444444, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_LIGHT_CARRIER_SPAWN_EGG =
            ITEMS.register("deep_ocean_light_carrier_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_LIGHT_CARRIER,
                            0x2D2D3D, 0x44AA44, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_CARRIER_SPAWN_EGG =
            ITEMS.register("deep_ocean_carrier_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_CARRIER,
                            0x2D2D3D, 0x2288AA, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_SUBMARINE_SPAWN_EGG =
            ITEMS.register("deep_ocean_submarine_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_SUBMARINE,
                            0x2D2D3D, 0x334466, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_FLAGSHIP_SPAWN_EGG =
            ITEMS.register("deep_ocean_flagship_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_FLAGSHIP,
                            0x161624, 0xD9D1FF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> SHIP_GIRL_SPAWN_EGG =
            ITEMS.register("ship_girl_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.SHIP_GIRL,
                            0xFFDDCC, 0x4488FF, new Item.Properties()));
}