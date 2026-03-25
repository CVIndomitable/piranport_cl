package com.piranport.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.piranport.registry.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public class CuttingBoardRecipe implements Recipe<SingleRecipeInput> {
    private final Ingredient ingredient;
    private final ItemStack result;
    private final int cuts;

    public CuttingBoardRecipe(Ingredient ingredient, ItemStack result, int cuts) {
        this.ingredient = ingredient;
        this.result = result;
        this.cuts = cuts;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return true; }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipeTypes.CUTTING_BOARD_SERIALIZER.get(); }

    @Override
    public RecipeType<?> getType() { return ModRecipeTypes.CUTTING_BOARD_TYPE.get(); }

    public Ingredient getIngredient() { return ingredient; }
    public ItemStack getResult() { return result; }
    public int getCuts() { return cuts; }

    public static final MapCodec<CuttingBoardRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CuttingBoardRecipe::getIngredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CuttingBoardRecipe::getResult),
            Codec.INT.optionalFieldOf("cuts", 4).forGetter(CuttingBoardRecipe::getCuts)
    ).apply(i, CuttingBoardRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CuttingBoardRecipe> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public CuttingBoardRecipe decode(RegistryFriendlyByteBuf buf) {
                    Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                    ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                    int cuts = buf.readVarInt();
                    return new CuttingBoardRecipe(ingredient, result, cuts);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, CuttingBoardRecipe recipe) {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient);
                    ItemStack.STREAM_CODEC.encode(buf, recipe.result);
                    buf.writeVarInt(recipe.cuts);
                }
            };

    public static class Serializer implements RecipeSerializer<CuttingBoardRecipe> {
        @Override
        public MapCodec<CuttingBoardRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CuttingBoardRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
