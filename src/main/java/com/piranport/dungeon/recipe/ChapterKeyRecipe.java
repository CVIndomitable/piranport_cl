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
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
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
        return width * height >= 5;
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
        ItemStack key = new ItemStack(ModItems.DUNGEON_KEY.get());
        key.set(ModDataComponents.DUNGEON_STAGE_ID.get(), "chapter_" + chapter);
        return key;
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
