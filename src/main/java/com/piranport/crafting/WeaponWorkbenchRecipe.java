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
        int craftingTime
) {
    public ItemStack getResultStack() {
        return new ItemStack(result);
    }
}
