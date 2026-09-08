package com.piranport.dungeon.recipe;

import com.piranport.PiranPort;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 副本系统自定义 RecipeSerializer 注册（整合版 §2.2 钥匙重置配方）。
 */
public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, PiranPort.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KeyResetRecipe>> KEY_RESET =
            RECIPE_SERIALIZERS.register("key_reset", () -> new KeyResetRecipe.Serializer());
}