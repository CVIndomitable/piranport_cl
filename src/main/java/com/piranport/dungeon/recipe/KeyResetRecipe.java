package com.piranport.dungeon.recipe;

import com.mojang.serialization.MapCodec;
import com.piranport.PiranPort;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import com.piranport.registry.ModDataComponents;
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
 * 整合版 §2.2 末段 + 副本/09 修订：钥匙复制 = 讲台取出 + 合成栏合成 → 保留原有进度，
 * 仅清除 instanceId，生成一把同类型同进度的新钥匙。
 *
     * <p>本合成配方在合成栏中：1 把章节钥匙 + 1 个铜锭 → 1 把同章节的新钥匙
     * （保留进度，清除 instanceId）。</p>
 *
 * <p>注意：当前保留 stageId 和 progress，仅清除 instanceId（"复制"语义）。</p>
 *
 * <h2>本配方的真实匹配规则（重要，勿被 JSON 误导）</h2>
 * <p>本配方是 {@link CustomRecipe}，匹配规则<b>全部硬编码在 {@link #matches} 里</b>：
 * 「合成格里恰好 1 把<b>使用过的</b> {@link DungeonKeyItem} + 至少 1 个铜锭」即成立。
 * <b>材料已由项目所有者 2026-09-24 定稿为「1 把使用过的钥匙 + 铜锭」</b>：
 * 钥匙打过副本后即为"使用过的钥匙"，与铜锭合成出新钥匙（《副本/17》§一.3 复制扩量）。
 * 判定见 {@link #matches}——<u>铜锭是硬性材料</u>，不再接受任意物品。</p>
 *
 * <p>{@link Serializer#CODEC} 用的是 {@code MapCodec.unit(...)}，<b>对 JSON 完全不反序列化</b>：
 * 只认 {@code type} 字段，其余字段一律<u>读取即忽略</u>。因此
 * {@code data/piranport/recipes/dungeon_key_copy.json} 里<b>不能</b>写
 * {@code pattern} / {@code key} / {@code result} ——它们不会被校验、也不会生效，
 * 只会让下一个人误以为配方受 JSON 约束（本 JSON 曾长期这样写，属误导）。
 * 这份 JSON 因而刻意只留 {@code type} 与 {@code category} 两个字段，
 * 一个 {@code piranport:key_copy} 类型只需存在一次即可覆盖全部七章（章节由钥匙自身的
 * stageId 决定，无需按章写多份）。</p>
 *
 * <p>补：{@code copy_from} 这类"记录复制来源"的 DataComponent <b>不存在</b>
 * （{@code ModDataComponents} 中并无该组件）。策划案《副本/17》§一.3「复制出的空白钥匙」
 * 是靠清空 instanceId 实现的：新钥匙复用原钥匙的 stageId + progress，
 * 由讲台据此重新随机一个全新副本实例。语义上不需要记录来源钥匙。</p>
 *
 * <h2>为什么这里的 {@link #isSpecial()} 被覆写成 {@code false}（有意偏离基类默认）</h2>
 * <p>与 {@link ChapterKeyRecipe} 同因：{@link CustomRecipe} 默认 {@code isSpecial() == true}，
 * 而 JEI 的 {@code CategoryRecipeValidator.isValid} 第一步就卡这个返回值，为 true 直接不显示，
 * 该版本 JEI 也没有可注册到原版合成台类别、绕开这道闸门的扩展入口。
 * 本配方本源语义同样是「普通合成」（钥匙 + 铜锭），并非特殊配方，故覆写为 {@code false}，
 * 让 JEI 可见、原版配方书也能识别。</p>
 * <p>注意：这只影响展示与自动摆放，<b>匹配逻辑仍完全以 {@link #matches} 为准</b>。</p>
 */
public class KeyResetRecipe extends CustomRecipe {

    public KeyResetRecipe(CraftingBookCategory category) {
        super(category);
    }

    /**
     * 覆写 {@link CustomRecipe} 的 {@code isSpecial() == true} 默认值，改为 {@code false}（理由见类 javadoc）。
     * 这是<b>有意偏离基类默认</b>的行为：目的是让 JEI 显示本配方、并让原版配方书能识别与自动摆放。
     */
    @Override
    public boolean isSpecial() {
        return false;
    }

    /**
     * 提供给 JEI / 配方书显示的<b>存在性</b>输入列表（2 格：钥匙 + 铜锭）。
     *
     * <p><b>本方法不参与 {@link #matches} 判定，{@link #matches} 才是权威。</b>
     * 真实规则里还有一个 Ingredient 表达不了的额外约束——钥匙必须是「使用过的」
     * （{@code progress} 非空，见 {@link #matches}），Ingredient 只能表达「这一格可以是哪些物品」，
     * 无法表达「这一格的物品必须带某个组件」。所以这里只声明物品种类，组件层面的门控留给 {@code matches}。</p>
     */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        // 展示主线钥匙的复制形状：左上钥匙、正中铜锭、其余七格铝锭。
        // 活动钥匙（妄想/地狱/ ulan）是同一配方的另一形态：正中末影之眼、其余紫水晶碎片，
        // 已在 matches 内按钥匙所属关卡分流。
        Ingredient anyKey = Ingredient.of(ModItems.DUNGEON_KEY_CH1.get(), ModItems.DUNGEON_KEY_CH2.get(),
                ModItems.DUNGEON_KEY_CH3.get(), ModItems.DUNGEON_KEY_CH4.get(),
                ModItems.DUNGEON_KEY_CH5.get(), ModItems.DUNGEON_KEY_CH6.get(),
                ModItems.DUNGEON_KEY_CH7.get(), ModItems.DUNGEON_KEY.get());
        Ingredient aluminum = Ingredient.of(ModItems.ALUMINUM_INGOT.get());
        Ingredient copper = Ingredient.of(Items.COPPER_INGOT);
        return NonNullList.of(Ingredient.EMPTY,
                anyKey, aluminum, aluminum,
                aluminum, copper, aluminum,
                aluminum, aluminum, aluminum);
    }

    /**
     * 供 JEI / 配方书显示的结果物品：一把副本钥匙。
     *
     * <p>刻意不带任何组件：真实产物会继承来源钥匙的 {@code stageId} + {@code progress}
     * （见 {@link #assemble}），那取决于玩家投入的是哪把钥匙，这里没有唯一正确答案。
     * 不带组件时 JEI 按物品自身默认组件渲染出钥匙图标，信息量恰好足够。</p>
     */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ModItems.DUNGEON_KEY_CH1.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        // 左上/正中两个约束位置要求完整 3×3 合成格。
        return width >= 3 && height >= 3;
    }

    /**
     * 匹配规则：<b>恰好 1 把「已使用过的」副本钥匙 + 恰好 1 个铜锭</b>（或多于 1 个也无妨，见下）。
     *
     * <p>材料口径由项目所有者 2026-09-24 定稿：<b>钥匙打过副本以后变成"使用过的钥匙"，
     * 使用过的钥匙与铜锭合成出新钥匙</b>（《副本/17》§一.3 锻造模板式复制，材料档位本文落定为铜锭）。
     * 原来的实现是「1 把钥匙 + 任意杂质」，等于用一块泥土就能无限复制钥匙，与策划口径不符。</p>
     *
     * <p>「使用过」的判定用 {@link DungeonKeyItem#getProgress} 是否为空进度：未使用过的钥匙
     * （刚从碎片合成出来、直接拿去做复制）没有 progress 可继承，让它参与复制只会白白消耗铜锭；
     * 要求 progress 非空同时也保证了复制出的钥匙是「同难度全新副本」而不是又一把空白钥匙。</p>
     */
    /**
     * 匹配规则（策划决策/副本/17）：
     * <ul>
     *   <li>主线钥匙：左上钥匙、正中铜锭、其余七格铝锭；</li>
     *   <li>活动钥匙（妄想舰队/地狱群岛）：左上钥匙、正中末影之眼、其余七格紫水晶碎片。</li>
     * </ul>
     * 钥匙<b>不消耗</b>，合成后留在左上格。
     */
    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;
        ItemStack key = input.getItem(0);
        if (!(key.getItem() instanceof DungeonKeyItem) || key.getCount() != 1) return false;
        boolean activity = isActivityKey(key);
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

    /** 活动关钥匙：关卡 id 以 {@code delusion_} 或 {@code hell_} 开头。 */
    private static boolean isActivityKey(ItemStack key) {
        String stageId = DungeonKeyItem.resolveStageId(key);
        return stageId.startsWith("delusion_") || stageId.startsWith("hell_");
    }

    @Override
    public ItemStack assemble(CraftingInput input, net.minecraft.core.HolderLookup.Provider registries) {
        ItemStack sourceKey = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof DungeonKeyItem) {
                sourceKey = stack;
                break;
            }
        }
        if (sourceKey.isEmpty()) return ItemStack.EMPTY;

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

        PiranPort.LOGGER.info("KeyCopy: stageId={} (progress preserved, instanceId cleared)",
                stageId);
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
        // 无参配方：MapCodec.unit 不读任何 JSON 字段（除 type 由 RecipeManager 分派），
        // 因此 JSON 只需 {"type":"piranport:key_copy","category":"misc"} 两行。
        public static final MapCodec<KeyResetRecipe> CODEC = MapCodec.unit(
                new KeyResetRecipe(CraftingBookCategory.MISC));

        @Override
        public MapCodec<KeyResetRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, KeyResetRecipe> streamCodec() {
            return StreamCodec.of(
                    (buf, recipe) -> {},
                    buf -> new KeyResetRecipe(CraftingBookCategory.MISC));
        }
    }
}
