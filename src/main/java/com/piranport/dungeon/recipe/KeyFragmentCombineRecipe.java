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
 * 钥匙碎片合成配方（未接线的占位实现，本类自身永远不会参与合成）。
 *
 * <p>两点事实决定了它是死配方：</p>
 * <ol>
 *   <li>{@link #matches} <b>恒返回 {@code false}</b>，{@link #assemble} 恒返回
 *       {@link ItemStack#EMPTY}——它从设计上就不可能匹配成功。</li>
 *   <li>它<b>没有任何配方 JSON 引用</b>：{@code data/piranport/recipe/} 下不存在
 *       {@code type: piranport:key_fragment_combine} 的 JSON。序列化器虽然在
 *       {@code ModRecipeSerializers} 注册过，但配方管理器根本不会加载出这一步配方实例。</li>
 * </ol>
 *
 * <p><b>实际的碎片 → 钥匙合成路径是原版有序合成</b>：
 * {@code data/piranport/recipe/chapter_key_1.json}～{@code chapter_key_7.json}
 * （Ch1 = 4×本章碎片；Ch2 起 = 4×本章碎片 + 1×前一章纪念章，对应 {@link ChapterKeyRecipe}
 * javadoc 描述的摆放规则）。注意 1.21.1 的配方目录是<b>单数 {@code recipe/}</b>；
 * 曾因误用复数 {@code recipes/} 导致全部配方 JSON 静默不加载、钥匙"合不出来"。
 * 保留本类不删的理由是：它的序列化器已在 {@code ModRecipeSerializers} 注册为
 * {@code piranport:key_fragment_combine}，贸然删除会让该注册项失去类型、影响面未知。</p>
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
