package com.piranport.dungeon.recipe;

import com.mojang.serialization.MapCodec;
import com.piranport.PiranPort;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 钥匙碎片合成配方（占位实现，用于修复编译错误）。
 */
public class KeyFragmentCombineRecipe extends CustomRecipe {
    public KeyFragmentCombineRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.create();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.KEY_FRAGMENT_COMBINE.get();
    }

    public static class Serializer implements RecipeSerializer<KeyFragmentCombineRecipe> {
        public static final MapCodec<KeyFragmentCombineRecipe> CODEC = MapCodec.unit(
                new KeyFragmentCombineRecipe(CraftingBookCategory.MISC));

        @Override
        public MapCodec<KeyFragmentCombineRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, KeyFragmentCombineRecipe> streamCodec() {
            return StreamCodec.of(
                    (buf, recipe) -> {},
                    buf -> new KeyFragmentCombineRecipe(CraftingBookCategory.MISC));
        }
    }
}
