package com.piranport.registry;

import com.piranport.item.AmmoItem;
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
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM =
            ITEMS.register("torpedo_610mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 610));
    public static final DeferredItem<TorpedoItem> MAGNETIC_TORPEDO_533MM =
            ITEMS.register("magnetic_torpedo_533mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533, true));
    public static final DeferredItem<TorpedoItem> WIRE_GUIDED_TORPEDO_533MM =
            ITEMS.register("wire_guided_torpedo_533mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533, false, true));
    public static final DeferredItem<TorpedoItem> ACOUSTIC_TORPEDO_533MM =
            ITEMS.register("acoustic_torpedo_533mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533, false, false, true));

    // Phase 27：策划 §3.3 氧气鱼雷（95 式氧气鱼雷原型）
    public static final DeferredItem<TorpedoItem> OXYGEN_TORPEDO_610MM =
            ITEMS.register("oxygen_torpedo_610mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            610, 32f, 30, 1.0f, false, false, false, true));

    // ===== Torpedo Ammo (named variants) =====
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_G7A =
            ITEMS.register("torpedo_533mm_g7a",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 27f, 18, 0.817f, false, false, false, false));
    public static final DeferredItem<TorpedoItem> MAGNETIC_TORPEDO_533MM_G7A =
            ITEMS.register("magnetic_torpedo_533mm_g7a",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 27f, 18, 0.817f, true, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_MK17 =
            ITEMS.register("torpedo_533mm_mk17",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 55.5f, 49, 0.854f, false, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE91 =
            ITEMS.register("torpedo_610mm_type91",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            610, 49.5f, 30, 0.743f, false, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE93_MK1 =
            ITEMS.register("torpedo_610mm_type93_mk1",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            610, 49.5f, 60, 0.929f, false, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE93_MK3 =
            ITEMS.register("torpedo_610mm_type93_mk3",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            610, 78f, 90, 0.706f, false, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_720MM_TYPE0 =
            ITEMS.register("torpedo_720mm_type0",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            720, 55.5f, 70, 0.743f, false, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_MK14 =
            ITEMS.register("torpedo_533mm_mk14",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 24f, 25, 0.576f, false, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_MK16 =
            ITEMS.register("torpedo_533mm_mk16",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 55.5f, 47, 0.854f, false, false, false, false));
    public static final DeferredItem<TorpedoItem> MAGNETIC_TORPEDO_533MM_G7E =
            ITEMS.register("magnetic_torpedo_533mm_g7e",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 21f, 25, 0.669f, true, false, false, false));
    public static final DeferredItem<TorpedoItem> ACOUSTIC_TORPEDO_533MM_G7E =
            ITEMS.register("acoustic_torpedo_533mm_g7e",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 21f, 17, 0.446f, false, false, true, false));
    public static final DeferredItem<TorpedoItem> WIRE_GUIDED_TORPEDO_533MM_G7E =
            ITEMS.register("wire_guided_torpedo_533mm_g7e",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 21f, 15, 0.557f, false, true, false, false));
    public static final DeferredItem<TorpedoItem> ACOUSTIC_TORPEDO_533MM_MK27 =
            ITEMS.register("acoustic_torpedo_533mm_mk27",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 24f, 25, 0.669f, false, false, true, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_530MM_TYPE95 =
            ITEMS.register("torpedo_530mm_type95",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            530, 39f, 23, 0.854f, false, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE95_MK2 =
            ITEMS.register("torpedo_610mm_type95_mk2",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            610, 55.5f, 45, 0.929f, false, false, false, false));

    // 数值配置/05 鱼雷补缺（2026-09-07 项目所有者定稿）
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE92 =
            ITEMS.register("torpedo_610mm_type92",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            610, 42f, 30, 0.80f, false, false, false, true));
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_MK13 =
            ITEMS.register("torpedo_533mm_mk13",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 22f, 25, 0.60f, false, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_53_38 =
            ITEMS.register("torpedo_533mm_53_38",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 24f, 20, 0.75f, false, false, false, false));

    // ===== Aviation Ammo (Phase 18) =====
    public static final DeferredItem<Item> AVIATION_FUEL =
            ITEMS.register("aviation_fuel",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.aviation_fuel"));
    // Legacy items kept for world compatibility — unified into AERIAL_BOMB below
    @Deprecated public static final DeferredItem<Item> AERIAL_BOMB_SMALL =
            ITEMS.register("aerial_bomb_small",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.aerial_bomb"));
    @Deprecated public static final DeferredItem<Item> AERIAL_BOMB_MEDIUM =
            ITEMS.register("aerial_bomb_medium",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.aerial_bomb"));
    public static final DeferredItem<Item> AERIAL_TORPEDO =
            ITEMS.register("aerial_torpedo",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.aerial_torpedo"));
    // Unified aerial bomb (replaces small/medium distinction)
    public static final DeferredItem<Item> AERIAL_BOMB =
            ITEMS.register("aerial_bomb",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.aerial_bomb"));
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