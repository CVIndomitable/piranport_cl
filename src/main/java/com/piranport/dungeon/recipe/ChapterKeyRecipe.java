package com.piranport.dungeon.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.piranport.item.DeployMedalItem;
import com.piranport.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 第 2 章及以后的主线钥匙合成——策划决策/副本/17。
 *
 * <p>摆法（3×3，位置固定）：</p>
 * <pre>
 * 奖章 铝 铝
 * 铝   铜 铝
 * 铝   铝 铝
 * </pre>
 *
 * <p>左上角放<b>上一章</b>的出击纪念章，正中铜锭，其余七格铝锭。
 * 奖章只是进度凭证，<b>不消耗</b>（留在合成格内），铜锭与铝锭消耗。
 * 章节号由奖章自身章节 +1 推断，因此一份 {@code piranport:chapter_key} JSON
 * 即可覆盖 2～7 章。</p>
 *
 * <p>第 1 章钥匙没有前置奖章，用普通有序合成 {@code chapter_key_1.json}。</p>
 */
public class ChapterKeyRecipe extends CustomRecipe {

    /** 兜底章节编号，仅用于 JEI 展示与异常路径；正常产物由奖章决定。 */
    private final int chapterNumber;

    public ChapterKeyRecipe(int chapterNumber, CraftingBookCategory category) {
        super(category);
        this.chapterNumber = chapterNumber;
    }

    public int chapter() {
        return chapterNumber;
    }

    /** 让 JEI 与配方书能展示本配方（理由同旧实现：CustomRecipe 默认 true 会被 JEI 直接过滤）。 */
    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    /** 从左上角奖章推出目标章节；不是 1～6 章奖章时返回 0。 */
    private static int chapterFromMedal(ItemStack medal) {
        if (!(medal.getItem() instanceof DeployMedalItem item)) return 0;
        int chapter = item.getChapterNumber() + 1;
        return chapter >= 2 && chapter <= 7 ? chapter : 0;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        // 必须正好 3×3，保证"左上/正中"两个约束位置有意义。
        if (input.width() != 3 || input.height() != 3) return false;
        if (input.getItem(0).getCount() != 1) return false;
        if (chapterFromMedal(input.getItem(0)) == 0) return false;
        if (!input.getItem(4).is(Items.COPPER_INGOT) || input.getItem(4).getCount() != 1) return false;
        for (int i = 0; i < 9; i++) {
            if (i == 0 || i == 4) continue;
            ItemStack stack = input.getItem(i);
            if (!stack.is(ModItems.ALUMINUM_INGOT.get()) || stack.getCount() != 1) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int chapter = chapterFromMedal(input.getItem(0));
        if (chapter == 0) chapter = chapterNumber;
        return new ItemStack(ModItems.chapterDungeonKey(chapter));
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        // 奖章留在左上格；其余材料（铜锭、铝锭）全部消耗。
        ItemStack medal = input.getItem(0);
        if (!medal.isEmpty()) remaining.set(0, medal.copy());
        return remaining;
    }

    /**
     * JEI / 配方书展示用输入。真实判定以 {@link #matches} 为准——
     * 「奖章章节 +1」这种跨格一致性无法用 Ingredient 表达，故这里给出形状：
     * 左上奖章通配，正中铜锭，其余七格铝锭。
     */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        Ingredient anyMedal = Ingredient.of(
                ModItems.DEPLOY_MEDAL_CH1.get(), ModItems.DEPLOY_MEDAL_CH2.get(),
                ModItems.DEPLOY_MEDAL_CH3.get(), ModItems.DEPLOY_MEDAL_CH4.get(),
                ModItems.DEPLOY_MEDAL_CH5.get(), ModItems.DEPLOY_MEDAL_CH6.get());
        Ingredient aluminum = Ingredient.of(ModItems.ALUMINUM_INGOT.get());
        Ingredient copper = Ingredient.of(Items.COPPER_INGOT);
        return NonNullList.of(Ingredient.EMPTY,
                anyMedal, aluminum, aluminum,
                aluminum, copper, aluminum,
                aluminum, aluminum, aluminum);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ModItems.chapterDungeonKey(Math.max(2, chapterNumber)));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.CHAPTER_KEY.get();
    }

    public static class Serializer implements RecipeSerializer<ChapterKeyRecipe> {
        public static final StreamCodec<RegistryFriendlyByteBuf, ChapterKeyRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buf, recipe) -> buf.writeVarInt(recipe.chapterNumber),
                        buf -> new ChapterKeyRecipe(buf.readVarInt(), CraftingBookCategory.MISC));

        public static final MapCodec<ChapterKeyRecipe> CODEC = RecordCodecBuilder.<ChapterKeyRecipe>mapCodec(instance ->
                instance.group(
                        Codec.INT.optionalFieldOf("chapter", 2)
                                .forGetter(ChapterKeyRecipe::chapter),
                        CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC)
                                .forGetter(ChapterKeyRecipe::category)
                ).apply(instance, ChapterKeyRecipe::new));

        @Override
        public MapCodec<ChapterKeyRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ChapterKeyRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
