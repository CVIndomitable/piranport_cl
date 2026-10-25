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
        List<ItemStack> inputs = new java.util.ArrayList<>(recipe.materials().stream()
                .map(m -> new ItemStack(m.item().get(), m.count()))
                .toList());
        // 蓝图作为额外输入展示：它不消耗，但合成时必须在背包里。
        if (recipe.requiredBlueprint() != null) {
            inputs.add(new ItemStack(recipe.requiredBlueprint().get()));
        }
        return new AmmoWorkbenchJEIRecipe(
                inputs, recipe.getResultStack(1),
                recipe.craftTimeTicks(), recipe.typeName(), recipe.caliberName()
        );
    }

    /** 类别/口径展示文字：口径为空时不要留下「标准 - 」这样的尾巴。 */
    public String displayLabel() {
        return caliberName == null || caliberName.isBlank()
                ? typeName
                : typeName + " - " + caliberName;
    }
}
