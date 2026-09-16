package com.piranport.dungeon.recipe;

import com.piranport.PiranPort;
import com.piranport.dungeon.recipe.ChapterKeyRecipe;
import com.piranport.dungeon.recipe.KeyFragmentCombineRecipe;
import com.piranport.dungeon.recipe.KeyResetRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 副本系统自定义 RecipeSerializer 注册（整合版 §2.2 钥匙重置配方 + 章节钥匙 + 碎片合成）。
 */
public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, PiranPort.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KeyResetRecipe>> KEY_RESET =
            RECIPE_SERIALIZERS.register("key_reset", () -> new KeyResetRecipe.Serializer());

    // ===== Chapter Key Recipe (章节钥匙合成) =====
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChapterKeyRecipe>> CHAPTER_KEY =
            RECIPE_SERIALIZERS.register("chapter_key", () -> new ChapterKeyRecipe.Serializer());

    // ===== Key Fragment Combine Recipe (钥匙碎片合成) =====
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KeyFragmentCombineRecipe>> KEY_FRAGMENT_COMBINE =
            RECIPE_SERIALIZERS.register("key_fragment_combine", () -> new KeyFragmentCombineRecipe.Serializer());
}