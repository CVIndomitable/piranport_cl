package com.piranport.registry;

import com.piranport.item.AmmoItem;
import com.piranport.item.CreativeAmmoCacheItem;
import com.piranport.item.MissileItem;
import com.piranport.item.TorpedoItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AmmoItems {
    private AmmoItems() {}

    private static final DeferredRegister.Items ITEMS = ModItems.ITEMS;

    // ===== HE Shells =====
    public static final DeferredItem<Item> SMALL_HE_SHELL =
            ITEMS.register("small_he_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.he_shell"));
    public static final DeferredItem<Item> MEDIUM_HE_SHELL =
            ITEMS.register("medium_he_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.he_shell"));
    public static final DeferredItem<Item> LARGE_HE_SHELL =
            ITEMS.register("large_he_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.he_shell"));

    // ===== MK23 Nuclear Shell — 副本/08-Boss通关奖励阿尔法兵装.md =====
    // 2026-09-09 定稿：原"阿尔法兵装"占位废止，实体化为 MK23 核炮弹。
    // 限制：仅 LARGE_SHELLS 标签火炮可装填；威力按炮 HE 表值 ×10（写死查表）；
    // 装填/伤害模型与 HE 完全相同；水中到期规则同 HE。
    public static final DeferredItem<Item> MK23_NUCLEAR_SHELL =
            ITEMS.register("mk23_nuclear_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.mk23_nuclear"));

    // ===== AP Shells =====
    public static final DeferredItem<Item> SMALL_AP_SHELL =
            ITEMS.register("small_ap_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.ap_shell"));
    public static final DeferredItem<Item> MEDIUM_AP_SHELL =
            ITEMS.register("medium_ap_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.ap_shell"));
    public static final DeferredItem<Item> LARGE_AP_SHELL =
            ITEMS.register("large_ap_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.ap_shell"));

    // Phase 27：策划 §3.1 表 3.1 命名 AP 炮弹 (91 式 14-21in, 一式 16-21in, 超重弹 7-16in)
    public static final DeferredItem<Item> TYPE_91_AP_SHELL =
            ITEMS.register("type_91_ap_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.ap_shell"));
    public static final DeferredItem<Item> TYPE_1_AP_SHELL =
            ITEMS.register("type_1_ap_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.ap_shell"));
    public static final DeferredItem<Item> SUPER_HEAVY_AP_SHELL =
            ITEMS.register("super_heavy_ap_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.ap_shell"));

    // ===== VT Shells (proximity fuze, small caliber only) =====
    public static final DeferredItem<Item> SMALL_VT_SHELL =
            ITEMS.register("small_vt_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.vt_shell"));

    // ===== Type 3 (Sanshiki) Shells =====
    public static final DeferredItem<Item> SMALL_TYPE3_SHELL =
            ITEMS.register("small_type3_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.type3_shell"));
    public static final DeferredItem<Item> MEDIUM_TYPE3_SHELL =
            ITEMS.register("medium_type3_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.type3_shell"));
    public static final DeferredItem<Item> LARGE_TYPE3_SHELL =
            ITEMS.register("large_type3_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.type3_shell"));

    // ===== Torpedo Ammo (legacy generic) =====
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM =
            ITEMS.register("torpedo_533mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533, "torpedo_533mm"));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM =
            ITEMS.register("torpedo_610mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 610, "torpedo_610mm"));
    public static final DeferredItem<TorpedoItem> MAGNETIC_TORPEDO_533MM =
            ITEMS.register("magnetic_torpedo_533mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533, true, "magnetic_torpedo_533mm"));
    public static final DeferredItem<TorpedoItem> WIRE_GUIDED_TORPEDO_533MM =
            ITEMS.register("wire_guided_torpedo_533mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533, false, true, "wire_guided_torpedo_533mm"));
    public static final DeferredItem<TorpedoItem> ACOUSTIC_TORPEDO_533MM =
            ITEMS.register("acoustic_torpedo_533mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533, false, false, true, "acoustic_torpedo_533mm"));

    // ===== Torpedo Ammo (named variants) =====
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_G7A =
            ITEMS.register("torpedo_533mm_g7a",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 27f, 18, 0.3f, false, false, false, false, "torpedo_533mm_g7a"));
    public static final DeferredItem<TorpedoItem> MAGNETIC_TORPEDO_533MM_G7A =
            ITEMS.register("magnetic_torpedo_533mm_g7a",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 27f, 18, 0.3f, true, false, false, false, "magnetic_torpedo_533mm_g7a"));
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_MK17 =
            ITEMS.register("torpedo_533mm_mk17",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 55.5f, 49, 0.3f, false, false, false, false, "torpedo_533mm_mk17"));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE91 =
            ITEMS.register("torpedo_610mm_type91",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            610, 49.5f, 30, 0.3f, false, false, false, false, "torpedo_610mm_type91"));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE93_MK1 =
            ITEMS.register("torpedo_610mm_type93_mk1",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            610, 49.5f, 60, 0.3f, false, false, false, false, "torpedo_610mm_type93_mk1"));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE93_MK3 =
            ITEMS.register("torpedo_610mm_type93_mk3",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            610, 78f, 90, 0.3f, false, false, false, false, "torpedo_610mm_type93_mk3"));
    public static final DeferredItem<TorpedoItem> TORPEDO_720MM_TYPE0 =
            ITEMS.register("torpedo_720mm_type0",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            720, 55.5f, 70, 0.3f, false, false, false, false, "torpedo_720mm_type0"));
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_MK14 =
            ITEMS.register("torpedo_533mm_mk14",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 24f, 25, 0.3f, false, false, false, false, "torpedo_533mm_mk14"));
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_MK16 =
            ITEMS.register("torpedo_533mm_mk16",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 55.5f, 47, 0.3f, false, false, false, false, "torpedo_533mm_mk16"));
    public static final DeferredItem<TorpedoItem> MAGNETIC_TORPEDO_533MM_G7E =
            ITEMS.register("magnetic_torpedo_533mm_g7e",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 21f, 25, 0.3f, true, false, false, false, "magnetic_torpedo_533mm_g7e"));
    public static final DeferredItem<TorpedoItem> ACOUSTIC_TORPEDO_533MM_G7E =
            ITEMS.register("acoustic_torpedo_533mm_g7e",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 21f, 17, 0.3f, false, false, true, false, "acoustic_torpedo_533mm_g7e"));
    public static final DeferredItem<TorpedoItem> WIRE_GUIDED_TORPEDO_533MM_G7E =
            ITEMS.register("wire_guided_torpedo_533mm_g7e",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 21f, 15, 0.3f, false, true, false, false, "wire_guided_torpedo_533mm_g7e"));
    public static final DeferredItem<TorpedoItem> ACOUSTIC_TORPEDO_533MM_MK27 =
            ITEMS.register("acoustic_torpedo_533mm_mk27",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 24f, 25, 0.3f, false, false, true, false, "acoustic_torpedo_533mm_mk27"));
    public static final DeferredItem<TorpedoItem> TORPEDO_530MM_TYPE95 =
            ITEMS.register("torpedo_530mm_type95",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            530, 39f, 23, 0.3f, false, false, false, false, "torpedo_530mm_type95"));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE95_MK2 =
            ITEMS.register("torpedo_610mm_type95_mk2",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            610, 55.5f, 45, 0.3f, false, false, false, false, "torpedo_610mm_type95_mk2"));

    // 数值配置/05 鱼雷补缺（2026-09-07 项目所有者定稿）
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE92 =
            ITEMS.register("torpedo_610mm_type92",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            610, 42f, 30, 0.3f, false, false, false, true, "torpedo_610mm_type92"));
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_MK13 =
            ITEMS.register("torpedo_533mm_mk13",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 22f, 25, 0.3f, false, false, false, false, "torpedo_533mm_mk13"));
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_53_38 =
            ITEMS.register("torpedo_533mm_53_38",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 24f, 20, 0.3f, false, false, false, false, "torpedo_533mm_53_38"));

    // ===== 创造模式弹药库（调试工具，无生存配方） =====
    public static final DeferredItem<CreativeAmmoCacheItem> CREATIVE_AMMO_CACHE =
            ITEMS.register("creative_ammo_cache",
                    () -> new CreativeAmmoCacheItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> AERIAL_TORPEDO =
            ITEMS.register("aerial_torpedo",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.aerial_torpedo"));
    // Unified aerial bomb (replaces small/medium distinction)
    public static final DeferredItem<Item> AERIAL_BOMB =
            ITEMS.register("aerial_bomb",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.aerial_bomb"));
    // 核航弹（副本/08）：只给景云改、B25、XA2J 三架水平轰炸机挂载，威力为同机普通航弹的 10 倍。
    public static final DeferredItem<Item> NUCLEAR_AERIAL_BOMB =
            ITEMS.register("nuclear_aerial_bomb",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.nuclear_aerial_bomb"));
    // 深水炸弹
    public static final DeferredItem<Item> DEPTH_CHARGE =
            ITEMS.register("depth_charge",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.depth_charge"));
    // Fighter ammo (子弹)
    public static final DeferredItem<Item> FIGHTER_AMMO =
            ITEMS.register("fighter_ammo",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.fighter_ammo"));

    // 弹丸渲染用隐藏物品（不加入创造模式标签页）
    public static final DeferredItem<Item> PROJECTILE_BULLET =
            ITEMS.register("projectile_bullet", () -> new Item(new Item.Properties()));

    // ===== 炮弹 3D 渲染载体（隐藏物品，不加入创造模式标签页） =====
    // CannonProjectileRenderer 会按装填的炮弹口径选择其中一档，渲染其 3D 锥形模型。
    // 真实的炮弹物品（small_he_shell 等）图标保持原有 2D 贴图不变，
    // 只有飞行中的炮弹用这三档 3D 模型，故用隐藏载体物品承载模型。
    public static final DeferredItem<Item> SHELL_PROJECTILE_SMALL =
            ITEMS.register("shell_projectile_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SHELL_PROJECTILE_MEDIUM =
            ITEMS.register("shell_projectile_medium", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SHELL_PROJECTILE_LARGE =
            ITEMS.register("shell_projectile_large", () -> new Item(new Item.Properties()));

    // ===== 导弹 3D 渲染载体（隐藏物品，不加入创造模式标签页） =====
    // MissileProjectileRenderer 按飞行中导弹携带的弹药物品选择其一，渲染对应的细长弹体模型。
    // 真实的导弹物品（anti_air_missile / harpoon_missile 等）图标保持原有 2D 贴图不变，
    // 只有飞行中的导弹用这几个 3D 模型，故用隐藏载体物品承载模型。
    public static final DeferredItem<Item> MISSILE_PROJECTILE_ANTI_AIR =
            ITEMS.register("missile_projectile_anti_air", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MISSILE_PROJECTILE_ANTI_SHIP =
            ITEMS.register("missile_projectile_anti_ship", () -> new Item(new Item.Properties()));
    // 火箭弹沿用小型炮弹拉长的弹体（与防空导弹同一套几何），单独注册是为了
    // 日后火箭弹需要独立外观（如换成火箭弹专属尾焰贴图）时不必再动渲染逻辑。
    public static final DeferredItem<Item> MISSILE_PROJECTILE_ROCKET =
            ITEMS.register("missile_projectile_rocket", () -> new Item(new Item.Properties()));

    // ===== Missile / Rocket Ammo =====
    public static final DeferredItem<MissileItem> SY1_MISSILE =
            ITEMS.register("sy1_missile",
                    () -> new MissileItem(new Item.Properties().stacksTo(16),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_SHIP, 30f, 6f));
    public static final DeferredItem<MissileItem> HARPOON_MISSILE =
            ITEMS.register("harpoon_missile",
                    () -> new MissileItem(new Item.Properties().stacksTo(16),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_SHIP, 24f));
    public static final DeferredItem<MissileItem> TERRIER_MISSILE =
            ITEMS.register("terrier_missile",
                    () -> new MissileItem(new Item.Properties().stacksTo(16),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_AIR, 9f));
    public static final DeferredItem<MissileItem> ANTI_AIR_MISSILE =
            ITEMS.register("anti_air_missile",
                    () -> new MissileItem(new Item.Properties().stacksTo(16),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_AIR, 6f));
    public static final DeferredItem<MissileItem> ROCKET_AMMO =
            ITEMS.register("rocket_ammo",
                    () -> new MissileItem(new Item.Properties().stacksTo(16),
                            com.piranport.entity.MissileEntity.MissileType.ROCKET, 6f));
}