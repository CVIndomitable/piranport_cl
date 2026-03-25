package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.recipe.CookingPotRecipe;
import com.piranport.recipe.CuttingBoardRecipe;
import com.piranport.recipe.StoneMillRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, PiranPort.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, PiranPort.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<StoneMillRecipe>> STONE_MILL_TYPE =
            RECIPE_TYPES.register("stone_mill", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "stone_mill").toString();
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, StoneMillRecipe.Serializer> STONE_MILL_SERIALIZER =
            RECIPE_SERIALIZERS.register("stone_mill", StoneMillRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CuttingBoardRecipe>> CUTTING_BOARD_TYPE =
            RECIPE_TYPES.register("cutting_board", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "cutting_board").toString();
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, CuttingBoardRecipe.Serializer> CUTTING_BOARD_SERIALIZER =
            RECIPE_SERIALIZERS.register("cutting_board", CuttingBoardRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CookingPotRecipe>> COOKING_POT_TYPE =
            RECIPE_TYPES.register("cooking_pot", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "cooking_pot").toString();
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, CookingPotRecipe.Serializer> COOKING_POT_SERIALIZER =
            RECIPE_SERIALIZERS.register("cooking_pot", CookingPotRecipe.Serializer::new);
}
