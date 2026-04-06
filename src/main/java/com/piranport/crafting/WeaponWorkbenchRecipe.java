package com.piranport.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 武器合成台配方数据。
 * @param tab            标签页 (0=火炮, 1=鱼雷发射器, 2=导弹发射器, 3=深弹, 4=飞机)
 * @param result         产物物品
 * @param materials      所需原料列表 (Item + count)，最多6种
 * @param requiredBlueprint 需要的蓝图物品（null = 无需蓝图）
 * @param craftingTime   合成耗时 (tick)
 */
public record WeaponWorkbenchRecipe(
        int tab,
        Item result,
        List<ItemStack> materials,
        @Nullable Item requiredBlueprint,
        int craftingTime
) {
    public ItemStack getResultStack() {
        return new ItemStack(result);
    }
}
