package com.piranport.compat.jei;

import com.piranport.ammo.AmmoRecipe;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record AmmoWorkbenchJEIRecipe(
        List<ItemStack> inputs,
        ItemStack output,
        int craftTimeTicks,
        String typeName,
        String caliberName
) {
    public static AmmoWorkbenchJEIRecipe fromAmmoRecipe(AmmoRecipe recipe) {
        List<ItemStack> inputs = recipe.materials().stream()
                .map(m -> new ItemStack(m.item().get(), m.count()))
                .toList();
        return new AmmoWorkbenchJEIRecipe(
                inputs, recipe.getResultStack(1),
                recipe.craftTimeTicks(), recipe.typeName(), recipe.caliberName()
        );
    }
}
