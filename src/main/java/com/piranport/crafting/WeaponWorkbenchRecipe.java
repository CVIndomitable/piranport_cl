package com.piranport.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record WeaponWorkbenchRecipe(
        int tab,
        Item result,
        List<ItemStack> materials,
        @Nullable Item requiredBlueprint,
        int craftingTime,
        /** 需要通关的主线关卡；null = 无门槛（副本/21、舰装/06）。 */
        @Nullable String unlockStage
) {
    public ItemStack getResultStack() {
        return new ItemStack(result);
    }
}
