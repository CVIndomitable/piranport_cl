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

public class StoneMillRecipe implements Recipe<StoneMillRecipeInput> {
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;

    public StoneMillRecipe(List<Ingredient> ingredients, ItemStack result) {
        NonNullList<Ingredient> nnl = NonNullList.create();
        nnl.addAll(ingredients);
        this.ingredients = nnl;
        this.result = result;
    }

    @Override
    public boolean matches(StoneMillRecipeInput input, Level level) {
        List<ItemStack> available = new ArrayList<>();
        for (ItemStack stack : input.items()) {
            if (!stack.isEmpty()) available.add(stack);
        }
        if (available.size() < ingredients.size()) return false;

        List<Ingredient> remaining = new ArrayList<>(ingredients);
        for (ItemStack avail : available) {
            for (int i = 0; i < remaining.size(); i++) {
                if (remaining.get(i).test(avail)) {
                    remaining.remove(i);
                    break;
                }
            }
        }
        return remaining.isEmpty();
    }

    @Override
    public ItemStack assemble(StoneMillRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.STONE_MILL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.STONE_MILL_TYPE.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    public ItemStack getResult() {
        return result;
    }

    // ===== Codec =====

    public static final MapCodec<StoneMillRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(StoneMillRecipe::getIngredients),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(StoneMillRecipe::getResult)
    ).apply(i, StoneMillRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StoneMillRecipe> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public StoneMillRecipe decode(RegistryFriendlyByteBuf buf) {
                    int count = buf.readVarInt();
                    List<Ingredient> ings = new ArrayList<>(count);
                    for (int j = 0; j < count; j++) {
                        ings.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                    }
                    ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                    return new StoneMillRecipe(ings, result);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, StoneMillRecipe recipe) {
                    buf.writeVarInt(recipe.ingredients.size());
                    for (Ingredient ing : recipe.ingredients) {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
                    }
                    ItemStack.STREAM_CODEC.encode(buf, recipe.result);
                }
            };

    public static class Serializer implements RecipeSerializer<StoneMillRecipe> {
        @Override
        public MapCodec<StoneMillRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StoneMillRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
