package com.piranport.recipe;

import com.piranport.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;

public class ModBrewingRecipes {
    public static void register(PotionBrewing.Builder builder) {
        // Water bottle + flour/rice_flour/sugar → yeast
        builder.addRecipe(waterBottleTo(ModItems.FLOUR.get(), ModItems.YEAST.get()));
        builder.addRecipe(waterBottleTo(ModItems.RICE_FLOUR.get(), ModItems.YEAST.get()));
        builder.addRecipe(waterBottleTo(Items.SUGAR, ModItems.YEAST.get()));

        // Water bottle + salt → brine
        builder.addRecipe(waterBottleTo(ModItems.SALT.get(), ModItems.BRINE.get()));

        // Yeast + rice_flour → cooking wine
        builder.addRecipe(customTo(ModItems.YEAST.get(), ModItems.RICE_FLOUR.get(), ModItems.COOKING_WINE.get()));

        // Yeast + wheat → beer
        builder.addRecipe(customTo(ModItems.YEAST.get(), Items.WHEAT, ModItems.BEER.get()));

        // Phase 28: bread-based kvass (replaces/augments cooking_pot kvass)
        builder.addRecipe(customTo(ModItems.YEAST.get(), Items.BREAD, ModItems.KVASS.get()));
        builder.addRecipe(customTo(ModItems.YEAST.get(), ModItems.NEW_RYE_BREAD.get(), ModItems.KVASS.get()));
    }

    private static IBrewingRecipe waterBottleTo(net.minecraft.world.item.Item ingredient,
                                                 net.minecraft.world.item.Item output) {
        return new IBrewingRecipe() {
            @Override
            public boolean isInput(ItemStack stack) {
                if (!stack.is(Items.POTION)) return false;
                PotionContents contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
                return contents != null && contents.is(Potions.WATER);
            }

            @Override
            public boolean isIngredient(ItemStack stack) { return stack.is(ingredient); }

            @Override
            public ItemStack getOutput(ItemStack input, ItemStack ing) {
                return new ItemStack(output);
            }
        };
    }

    private static IBrewingRecipe customTo(net.minecraft.world.item.Item input,
                                            net.minecraft.world.item.Item ingredient,
                                            net.minecraft.world.item.Item output) {
        return new IBrewingRecipe() {
            @Override
            public boolean isInput(ItemStack stack) { return stack.is(input); }

            @Override
            public boolean isIngredient(ItemStack stack) { return stack.is(ingredient); }

            @Override
            public ItemStack getOutput(ItemStack inp, ItemStack ing) {
                return new ItemStack(output);
            }
        };
    }
}
