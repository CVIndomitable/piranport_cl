package com.piranport.registry;

import com.piranport.PiranPort;
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
}
