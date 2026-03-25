package com.piranport.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.piranport.registry.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class CookingPotRecipe implements Recipe<CookingPotRecipeInput> {
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final int cookingTime;

    public CookingPotRecipe(List<Ingredient> ingredients, ItemStack result, int cookingTime) {
        NonNullList<Ingredient> nnl = NonNullList.create();
        nnl.addAll(ingredients);
        this.ingredients = nnl;
        this.result = result;
        this.cookingTime = cookingTime;
    }

    @Override
    public boolean matches(CookingPotRecipeInput input, Level level) {
        List<ItemStack> available = new ArrayList<>();
        for (ItemStack stack : input.items()) {
            if (!stack.isEmpty()) available.add(stack);
        }
        if (available.size() < ingredients.size()) return false;
        List<Ingredient> remaining = new ArrayList<>(ingredients);
        for (ItemStack avail : available) {
            remaining.removeIf(ing -> ing.test(avail));
        }
        return remaining.isEmpty();
    }

    @Override
    public ItemStack assemble(CookingPotRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return true; }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }

    @Override
    public NonNullList<Ingredient> getIngredients() { return ingredients; }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipeTypes.COOKING_POT_SERIALIZER.get(); }

    @Override
    public RecipeType<?> getType() { return ModRecipeTypes.COOKING_POT_TYPE.get(); }

    public ItemStack getResult() { return result; }
    public int getCookingTime() { return cookingTime; }

    public static final MapCodec<CookingPotRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(CookingPotRecipe::getIngredients),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CookingPotRecipe::getResult),
            net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("cookingTime", 200).forGetter(CookingPotRecipe::getCookingTime)
    ).apply(i, CookingPotRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CookingPotRecipe> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public CookingPotRecipe decode(RegistryFriendlyByteBuf buf) {
                    int count = buf.readVarInt();
                    List<Ingredient> ings = new ArrayList<>(count);
                    for (int j = 0; j < count; j++) {
                        ings.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                    }
                    ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                    int time = buf.readVarInt();
                    return new CookingPotRecipe(ings, result, time);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, CookingPotRecipe recipe) {
                    buf.writeVarInt(recipe.ingredients.size());
                    for (Ingredient ing : recipe.ingredients) {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
                    }
                    ItemStack.STREAM_CODEC.encode(buf, recipe.result);
                    buf.writeVarInt(recipe.cookingTime);
                }
            };

    public static class Serializer implements RecipeSerializer<CookingPotRecipe> {
        @Override
        public MapCodec<CookingPotRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CookingPotRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
