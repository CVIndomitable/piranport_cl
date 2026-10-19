package com.piranport.crafting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.Set;

/**
 * 武器可合成性判定（策划决策/数值/09）。
 *
 * <p>单独成类是为了让注册期（{@code WeaponItems}）和配方期（{@code WeaponWorkbenchRecipeRegistry}）
 * 共用同一份名单，同时避免两边的静态初始化互相触发。</p>
 */
public final class WeaponCraftability {
    private WeaponCraftability() {}

    /**
     * 无法合成清单——只收火炮和发射器。
     * 判据：舰R 图鉴不可开发，且其他获取方式是活动奖励或战利品兑换；
     * 这些装备由对应 Boss 每次通关掉落（副本/21），不进武器制造台。
     */
    public static final Set<String> UNCRAFTABLE_IDS = Set.of(
            // 火炮
            "japanese_triple_460mm_gun",
            "soviet_triple_460mm_gun",
            "us_twin_20inch_gun",
            "british_twin_20inch_gun",
            // 鱼雷发射器
            "japanese_triple_72cm_torpedo",
            "quad_type0_72cm_torpedo",
            "clt_quad_torpedo_launcher_type0",
            "sextuple_61cm_torpedo_launcher_93m3",
            "septuple_torpedo_launcher",
            "septuple_torpedo_launcher_93m3");

    /** 按注册名判断；注册名取自物品注册表。 */
    public static boolean isUncraftable(Item item) {
        var key = BuiltInRegistries.ITEM.getKey(item);
        return key != null && isUncraftableId(key.getPath());
    }

    /** 按注册 ID 判断（注册期物品实例尚不可用时使用）。 */
    public static boolean isUncraftableId(String id) {
        return UNCRAFTABLE_IDS.contains(id);
    }
}
