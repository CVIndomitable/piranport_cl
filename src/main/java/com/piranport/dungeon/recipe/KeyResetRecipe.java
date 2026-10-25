package com.piranport.dungeon.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.piranport.PiranPort;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 钥匙复制（《副本/17》：副本/09 修订）：保留 stageId 与 progress，仅清除 instanceId，
 * 生成一把同章/同活动关的新钥匙。左上钥匙不消耗，留在格内。
 *
 * <h2>为什么按章节拆成多条配方（而不是一条「任意钥匙」配方）</h2>
 * <p>旧实现只有一条 {@code piranport:key_copy}，{@link #getIngredients()} 里把七把章节钥匙
 * 和通用钥匙塞进同一个 {@link Ingredient}。这条 Ingredient 在「玩家按配方摆放」时没问题，
 * 但<b>自动摆放（JEI 的 + 按钮 / 原版配方书）必须从背包里挑一把匹配的钥匙</b>：
 * JEI 按背包槽位顺序取第一个匹配项，原版 {@code StackedContents} 按 Ingredient 的物品顺序取；
 * 两条路径都会优先拿到<b>第一章钥匙</b>。玩家明明拿着 5 章钥匙点复制，产物却变成 1 章，
 * 而且 JEI 的结果图又被 {@code getResultItem} 硬编码成木钥匙——两者叠加就是
 * 「任意章节钥匙复制后都变成第一章钥匙」。</p>
 *
 * <p>因此每条配方只认<u>一种</u>钥匙物品（{@code chapter} 字段：1～7 对应
 * {@code dungeon_key_ch1..ch7}，0 对应旧版通用 {@code dungeon_key}）。自动摆放时
 * Ingredient 只有这一种物品，不会再从背包里抓别的章节；JEI 展示的结果也就是本章钥匙。
 * {@code activity} 字段只对 {@code chapter=0} 有意义：活动钥匙（金猫猫 / ulan / 妄想 / 地狱）
 * 用末影之眼 + 紫水晶碎片，旧存档留下的通用主线钥匙仍用铜锭 + 铝锭。</p>
 *
 * <p>材料口径（策划决策/副本/17）：主线钥匙 = 左上钥匙 + 正中铜锭 + 其余七格铝锭；
 * 活动钥匙 = 左上钥匙 + 正中末影之眼 + 其余七格紫水晶碎片。钥匙<b>不消耗</b>。</p>
 *
 * <h2>JSON 字段</h2>
 * <p>{@code data/piranport/recipe/dungeon_key_copy*.json} 只需要
 * {@code type / chapter / activity}；{@code category} 之类多余字段会被忽略。
 * 每种钥匙一条 JSON（ch1..ch7 + 活动 + 旧通用），见 {@link Serializer#CODEC}。</p>
 *
 * <p>补：{@code copy_from} 这类"记录复制来源"的 DataComponent 并不存在。策划案
 * 《副本/17》的「复制出的空白钥匙」是靠清空 instanceId 实现的：新钥匙复用原钥匙的
 * stageId + progress，由讲台据此建一个全新副本实例。</p>
 *
 * <h2>为什么 {@link #isSpecial()} 覆写为 {@code false}</h2>
 * <p>与 {@link ChapterKeyRecipe} 同因：{@link CustomRecipe} 默认 {@code isSpecial() == true}，
 * JEI 不显示特殊配方；本配方语义上是普通合成，故改为 {@code false} 让 JEI / 配方书可见。
 * 匹配逻辑仍完全以 {@link #matches} 为准。</p>
 */
public class KeyResetRecipe extends CustomRecipe {

    /** 1～7 = 对应章节钥匙；0 = 旧版通用钥匙 {@code dungeon_key}。 */
    private final int chapter;

    /** 仅 {@code chapter == 0} 有效：true = 活动钥匙摆法（末影之眼 + 紫水晶碎片）。 */
    private final boolean activity;

    public KeyResetRecipe(int chapter, boolean activity, CraftingBookCategory category) {
        super(category);
        this.chapter = chapter;
        // 章节钥匙（1～7）永远走铜锭摆法；只有通用钥匙才分活动/旧主线两种。
        this.activity = activity && chapter == 0;
    }

    public int chapter() {
        return chapter;
    }

    public boolean activity() {
        return activity;
    }

    /** 这条配方复制的钥匙物品。 */
    private Item sourceItem() {
        return ModItems.chapterDungeonKey(chapter);
    }

    /**
     * 覆写 {@link CustomRecipe} 的 {@code isSpecial() == true} 默认值，改为 {@code false}（理由见类 javadoc）。
     */
    @Override
    public boolean isSpecial() {
        return false;
    }

    /**
     * JEI / 配方书展示用输入：左上<b>本章</b>钥匙，正中催化剂，其余七格材料。
     *
     * <p>关键约束：左上 Ingredient 必须只包含本章这一种钥匙。旧实现把七章钥匙放进同一个
     * Ingredient，自动摆放时就会从背包里挑到第一章钥匙（根因见类 javadoc）。</p>
     */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        Ingredient key = Ingredient.of(sourceItem());
        Ingredient filler = activity
                ? Ingredient.of(Items.AMETHYST_SHARD)
                : Ingredient.of(ModItems.ALUMINUM_INGOT.get());
        Ingredient catalyst = activity
                ? Ingredient.of(Items.ENDER_EYE)
                : Ingredient.of(Items.COPPER_INGOT);
        return NonNullList.of(Ingredient.EMPTY,
                key, filler, filler,
                filler, catalyst, filler,
                filler, filler, filler);
    }

    /**
     * JEI / 配方书展示结果 = 本章钥匙。因为配方已经按章节拆分，这里的答案唯一，
     * 不再需要「随便给一把通用钥匙」的妥协。注意不能返回 {@link ItemStack#EMPTY}：
     * JEI 的 {@code CategoryRecipeValidator} 会把无产物配方整个过滤掉。
     */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(sourceItem());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        // 左上/正中两个约束位置要求完整 3×3 合成格。
        return width >= 3 && height >= 3;
    }

    /**
     * 匹配规则（策划决策/副本/17）：
     * <ul>
     *   <li>左上必须是<b>本条配方指定</b>的那把钥匙（物品类型即章节），且数量为 1；</li>
     *   <li>主线：正中铜锭，其余七格铝锭；活动：正中末影之眼，其余七格紫水晶碎片；</li>
     *   <li>钥匙<b>不消耗</b>，合成后留在左上格。</li>
     * </ul>
     *
     * <p>旧实现还要求钥匙「使用过」（progress 非空）；策划口径从未要求，现已去掉——
     * 刚合成出来的空白钥匙也应该可以再复制一把。</p>
     */
    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;
        ItemStack key = input.getItem(0);
        if (key.getCount() != 1 || !key.is(sourceItem())) return false;
        // 通用钥匙有两种摆法：活动关（金猫猫/ulan/妄想/地狱）与旧存档主线钥匙。
        if (chapter == 0 && activity != isActivityStage(DungeonKeyItem.resolveStageId(key))) return false;

        ItemStack catalyst = input.getItem(4);
        if (catalyst.getCount() != 1) return false;
        if (activity ? !catalyst.is(Items.ENDER_EYE) : !catalyst.is(Items.COPPER_INGOT)) return false;
        for (int i = 0; i < 9; i++) {
            if (i == 0 || i == 4) continue;
            ItemStack stack = input.getItem(i);
            if (stack.getCount() != 1) return false;
            if (activity ? !stack.is(Items.AMETHYST_SHARD)
                    : !stack.is(ModItems.ALUMINUM_INGOT.get())) return false;
        }
        return true;
    }

    /**
     * 活动关判定（《副本/18》）：金猫猫、ulan、妄想舰队、地狱群岛都用活动钥匙摆法。
     * 早先只认 {@code delusion_}/{@code hell_}，金猫猫与 ulan 会被错当成主线钥匙。
     */
    private static boolean isActivityStage(String stageId) {
        return stageId.startsWith("delusion_") || stageId.startsWith("hell_")
                || stageId.equals("goldencatcat") || stageId.equals("ulan");
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack sourceKey = input.getItem(0);
        if (sourceKey.isEmpty() || !(sourceKey.getItem() instanceof DungeonKeyItem)) {
            return ItemStack.EMPTY;
        }

        // 输出：新钥匙（同 stageId + 同 progress，仅 instanceId 移除 = 复制语义）
        ItemStack result = new ItemStack(sourceKey.getItem());
        // 章节钥匙可换关，当前关不能再靠物品类型推断，必须把解析出的关卡一起写回。
        String stageId = DungeonKeyItem.resolveStageId(sourceKey);
        if (!stageId.isEmpty()) {
            DungeonKeyItem.setStageId(result, stageId);
        }
        // 保留原有进度（副本/09：复制合成替代重置）
        DungeonProgress originalProgress = DungeonKeyItem.getProgress(sourceKey);
        if (originalProgress != null && !originalProgress.equals(DungeonProgress.EMPTY)) {
            result.set(ModDataComponents.DUNGEON_PROGRESS.get(), originalProgress);
        }
        // instanceId 不设置（新副本）

        PiranPort.LOGGER.info("KeyCopy: chapter={} stageId={} (progress preserved, instanceId cleared)",
                chapter, stageId);
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        // 钥匙不消耗，留在左上格；铜锭/末影之眼与七格材料消耗。
        ItemStack key = input.getItem(0);
        if (!key.isEmpty()) remaining.set(0, key.copy());
        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.KEY_COPY.get();
    }

    // ===== Serializer =====

    public static class Serializer implements RecipeSerializer<KeyResetRecipe> {

        /**
         * {@code chapter}：0～7（见类 javadoc）；{@code activity}：仅 chapter=0 时有效，默认 false。
         * 每条钥匙一条 JSON，章节/摆法由数据包显式声明，不再靠运行时猜「任意钥匙」。
         */
        public static final MapCodec<KeyResetRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                        Codec.intRange(0, 7).fieldOf("chapter").forGetter(KeyResetRecipe::chapter),
                        Codec.BOOL.optionalFieldOf("activity", false).forGetter(KeyResetRecipe::activity))
                .apply(i, (chapter, activity) ->
                        new KeyResetRecipe(chapter, activity, CraftingBookCategory.MISC)));

        @Override
        public MapCodec<KeyResetRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, KeyResetRecipe> streamCodec() {
            return StreamCodec.of(
                    (buf, recipe) -> {
                        buf.writeVarInt(recipe.chapter);
                        buf.writeBoolean(recipe.activity);
                    },
                    buf -> new KeyResetRecipe(buf.readVarInt(), buf.readBoolean(),
                            CraftingBookCategory.MISC));
        }
    }
}
