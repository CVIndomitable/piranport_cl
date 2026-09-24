package com.piranport.dungeon.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.piranport.PiranPort;
import com.piranport.item.DeployMedalItem;
import com.piranport.item.KeyFragmentItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import com.piranport.dungeon.key.DungeonKeyItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 章节钥匙合成配方。
 * <p>
 * Ch1：4 个 Ch1 钥匙碎片 → 1 把 Ch1 钥匙（无前置条件）<br>
 * Ch2+：4 个对应章节钥匙碎片 + 1 个前一章通关纪念章 → 1 把对应章节钥匙<br>
 * 如：Ch2 钥匙 = 4x 碎片 Ch2 + 1x 纪念章 Ch1
 * </p>
 *
 * <p><b>章节编号取自投入的碎片，而非 JSON 字段</b>（详见 {@link #matches}）：一份
 * {@code piranport:chapter_key} 配方 JSON 即可覆盖全部七章。</p>
 *
 * <h2>为什么这里的 {@link #isSpecial()} 被覆写成 {@code false}（有意偏离基类默认）</h2>
 * <p>{@link CustomRecipe} 的默认实现是 {@code isSpecial() == true}，这个默认值有服务端语义
 * （原版配方书不会自动摆放它），但它在 <b>JEI 侧是一道硬闸门</b>：JEI 的
 * {@code CategoryRecipeValidator.isValid(RecipeHolder)} 字节码第一步就是
 * {@code Recipe.isSpecial()}，为 true 立刻 {@code return false}，
 * <b>后面补什么 {@code getIngredients()}、什么 JEI 扩展都救不回来</b>
 * （JEI 该版本也没有任何可注册到「原版合成台」类别的入口——
 * {@code IExtendableCraftingRecipeCategory} 的唯一实现即 JEI 自建的 {@code CraftingRecipeCategory}，
 * 其扩展表在构造时就已被锁死，模组拿不到句柄）。</p>
 * <p>而 {@code chapter_key} 的本源语义就是<b>一张普通的 3x3 合成表</b>：
 * 四个角放同章节碎片，Ch2 起中间再放一枚上一章纪念章。
 * {@code isSpecial=true} 只是因为类继承了 {@code CustomRecipe} 而白捡的默认值，
 * 并不是本配方有意设计成「特殊配方」。</p>
 * <p>因此这里覆写为 {@code false} 是<b>有意的偏离</b>，目的有二：
 * ① JEI 能显示这条配方；② 原版配方书也能正常识别与自动摆放。
 * 配套必须一并提供 {@link #getIngredients()} 与 {@link #getResultItem(HolderLookup.Provider)}
 * ——JEI 在过了 {@code isSpecial} 闸门后，还会用「结果物品非空」和「输入物品数 &gt; 0」两道检查把关。</p>
 */
public class ChapterKeyRecipe extends CustomRecipe {

    private final int chapterNumber;

    public ChapterKeyRecipe(int chapterNumber, CraftingBookCategory category) {
        super(category);
        this.chapterNumber = chapterNumber;
    }

    /** 兜底章节编号。仅在 {@link #assemble} 拿不到碎片信息时使用；正常合成路径由碎片决定。 */
    public int chapter() {
        return chapterNumber;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        // 第一章只需要四个碎片，必须允许在 2x2 合成栏中制作；后续章节
        // 额外的通关纪念章会自然要求 3x3 合成栏。
        return width >= 2 && height >= 2;
    }

    /**
     * 逐格解析合成栏，得出「本章节的碎片数是几」与「带了哪一档纪念章」，其余物品一律视为杂质。
     *
     * <p>之所以要把这一步单独抽出来、并且同时被 {@link #matches} 和 {@link #assemble} 调用，
     * 是因为<b>章节编号的权威来源是投入的碎片，而不是 JSON 字段</b>——见 {@link #matches} 的说明。
     * 两边必须基于同一次解析结果，否则会出现"匹配通过但产物是另一章"。</p>
     */
    private Parsed parseStacks(CraftingInput input) {
        int fragmentChapter = 0;
        int fragmentCount = 0;
        int medalChapter = 0;
        boolean foreign = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof KeyFragmentItem frag) {
                // 不同章节的碎片混在一起就不成配方：碎片章节必须唯一。
                if (fragmentCount > 0 && frag.getChapterNumber() != fragmentChapter) {
                    return Parsed.INVALID;
                }
                fragmentChapter = frag.getChapterNumber();
                fragmentCount++;
            } else if (stack.getItem() instanceof DeployMedalItem medal) {
                if (medalChapter > 0 && medal.getChapterNumber() != medalChapter) {
                    return Parsed.INVALID;
                }
                medalChapter = medal.getChapterNumber();
            } else {
                foreign = true;
            }
        }
        return new Parsed(fragmentChapter, fragmentCount, medalChapter, foreign);
    }

    /** {@link #parseStacks} 的结果。{@code INVALID} 表示投入物自相矛盾（混章碎片/多枚纪念章）。 */
    private record Parsed(int fragmentChapter, int fragmentCount, int medalChapter, boolean foreign) {
        static final Parsed INVALID = new Parsed(0, 0, 0, true);
        boolean valid() { return fragmentChapter > 0; }
    }

    /**
     * 覆写 {@link CustomRecipe} 的 {@code isSpecial() == true} 默认值，改为 {@code false}。
     *
     * <p><b>这是有意偏离基类默认的行为</b>，原因见类 javadoc「为什么这里的 isSpecial() 被覆写成 false」：
     * JEI 的 {@code CategoryRecipeValidator.isValid} 第一步就检查 {@code isSpecial()}，
     * 为 true 直接拒绝显示，且该版本 JEI 不提供任何可绕开这道闸门的扩展入口；
     * 而本配方本身就是一张普通 3x3 合成表，并非特殊配方。改为 false 同时让原版配方书
     * 也能识别并自动摆放本配方。</p>
     *
     * <p>注意：{@code isSpecial()} 只影响「配方书/JEI 是否展示与自动摆放」，
     * <b>匹配逻辑完全由 {@link #matches} 决定</b>，两者互不干扰，
     * 所以改这个返回值不会让不该成立的组合变成可合成。</p>
     */
    @Override
    public boolean isSpecial() {
        return false;
    }

    /**
     * 提供给 JEI / 配方书显示的<b>存在性</b>输入列表。
     *
     * <p><b>本方法不参与 {@link #matches} 判定，{@link #matches} 才是权威。</b>
     * 因为真正的匹配规则是「四个同章节碎片，且章节必须一致；Ch2 起还要一枚<b>上一章</b>纪念章」，
     * 这种「跨格一致性约束」无法用 {@link Ingredient} 的有序列表表达
     * （Ingredient 只能表达「这一格可以是这些物品之一」）。
     * 所以这里退而求其次，返回一个能表达「4 碎片 + 1 纪念章」形状的列表，让 JEI 有东西可画。</p>
     *
     * <p>具体做法：把七个章节的碎片物品合并成<b>同一个通配 {@link Ingredient}</b>，再重复 4 次
     * ——这样 JEI 格子下方显示的候选物品是全部七种碎片（而不是「只有第一章碎片」这种误导）。
     * 第 5 格是同样处理过的纪念章通配 Ingredient。JEI 只取前 9 个 Ingredient 按顺序铺进 3x3 格，
     * 所以这 5 格会排成「第一行 3 碎片 + 第二行 1 碎片 + 1 纪念章」。</p>
     */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        Ingredient anyFragment = fragmentIngredient();
        Ingredient anyMedal = medalIngredient();
        return NonNullList.of(Ingredient.EMPTY,
                anyFragment, anyFragment, anyFragment, anyFragment, anyMedal);
    }

    /** 供 JEI / 配方书显示的本配方默认章节钥匙。 */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ModItems.chapterDungeonKey(chapterNumber));
    }

    /**
     * 把某一章的全部碎片物品合成一个通配 {@link Ingredient}。
     *
     * <p>逐个 {@code ModItems.KEY_FRAGMENT_CHn} 显式列举，而不是在此时扫
     * {@code BuiltInRegistries.ITEM} 做类型过滤：注册表的迭代顺序不保证稳定，
     * Ingredient 的 {@code items} 顺序会直接决定 JEI 悬停提示里候选物品的排列顺序，
     * 显式列举既稳定又可读。</p>
     */
    private static Ingredient fragmentIngredient() {
        return Ingredient.of(
                ModItems.KEY_FRAGMENT_CH1.get(), ModItems.KEY_FRAGMENT_CH2.get(),
                ModItems.KEY_FRAGMENT_CH3.get(), ModItems.KEY_FRAGMENT_CH4.get(),
                ModItems.KEY_FRAGMENT_CH5.get(), ModItems.KEY_FRAGMENT_CH6.get(),
                ModItems.KEY_FRAGMENT_CH7.get());
    }

    /** 把某一章的全部出击纪念章合成一个通配 {@link Ingredient}，理由同 {@link #fragmentIngredient()}。 */
    private static Ingredient medalIngredient() {
        return Ingredient.of(
                ModItems.DEPLOY_MEDAL_CH1.get(), ModItems.DEPLOY_MEDAL_CH2.get(),
                ModItems.DEPLOY_MEDAL_CH3.get(), ModItems.DEPLOY_MEDAL_CH4.get(),
                ModItems.DEPLOY_MEDAL_CH5.get(), ModItems.DEPLOY_MEDAL_CH6.get(),
                ModItems.DEPLOY_MEDAL_CH7.get());
    }

    /**
     * 匹配规则：<b>4 个同章节碎片</b>（Ch2 起再加 1 枚<b>上一章</b>纪念章），且不得混入杂质物品。
     *
     * <p><b>章节编号由碎片物品自身携带（{@link KeyFragmentItem#getChapterNumber()}）推断，
     * 而不是来自 JSON 的 {@code chapter} 字段。</b>因此游戏里只需存在<b>一份</b>
     * {@code type: piranport:chapter_key} 的配方 JSON，七个章节共用它即可——这从结构上
     * 消除了"七章各写一份雷同 JSON、漏写一章就永远合不出该章钥匙"的断裂
     * （即 260924 审查 P0-1 A 段：序列化器已注册、却无任何 JSON 引用，导致第一章钥匙
     * 在正常玩法下完全无法合成）。{@code chapter} 字段现在只剩「兜底默认值」意义。</p>
     */
    @Override
    public boolean matches(CraftingInput input, Level level) {
        Parsed p = parseStacks(input);
        if (!p.valid() || p.foreign() || p.fragmentCount() != 4) {
            return false;
        }
        int chapter = p.fragmentChapter();
        // Ch1 零门控保底（《副本/17》§一.1）；Ch2 起必须献祭上一章的出击纪念章（§一.2 章节链）。
        return chapter == 1 || p.medalChapter() == chapter - 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, net.minecraft.core.HolderLookup.Provider registries) {
        Parsed p = parseStacks(input);
        // matches() 通过才轮到 assemble()，但配方装配在创造模式/某些工具路径下可能被单独调用，
        // 所以这里仍要防御：解析不出章节就退回构造函数给的兜底编号，绝不产出 chapter_0 这种空关卡。
        int chapter = p.valid() ? p.fragmentChapter() : chapterNumber;
        return new ItemStack(ModItems.chapterDungeonKey(chapter));
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && !(stack.getItem() instanceof DungeonKeyItem)) {
                remaining.set(i, ItemStack.EMPTY);
            }
        }
        return remaining;
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

        /**
         * 从 JSON 反序列化，<b>chapterNumber 可选</b>：
         * 缺省或非法时回落到 1。
         *
         * <p>关卡编号不再由 JSON 声明，而是<b>按匹配到的碎片物品在 {@link #matches} 里判定</b>，
         * 所以一个 chapter_key 配方即可覆盖全部七章——这直接消除了"七章要写七份雷同 JSON、
         * 漏写一份就永远合不出对应章节钥匙"的断裂（P0-1 A 段根因）。</p>
         */
        public static final MapCodec<ChapterKeyRecipe> CODEC = RecordCodecBuilder.<ChapterKeyRecipe>mapCodec(instance ->
                instance.group(
                        Codec.INT.optionalFieldOf("chapter", 1)
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
