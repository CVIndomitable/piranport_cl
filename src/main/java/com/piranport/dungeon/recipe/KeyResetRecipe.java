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
 * <p>本合成配方在合成栏中：1 把 DungeonKeyItem + 任意 1 个材料 → 1 把新 DungeonKeyItem
 * （同 stageId + 同 progress，instanceId 移除）。</p>
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
        return NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(ModItems.DUNGEON_KEY.get()),
                Ingredient.of(Items.COPPER_INGOT));
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
        return new ItemStack(ModItems.DUNGEON_KEY.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        // 2x2 或 3x3 任意位置均可合成
        return width * height >= 2;
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
    @Override
    public boolean matches(CraftingInput input, Level level) {
        int keyCount = 0;
        int copperCount = 0;
        boolean keyUsed = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof DungeonKeyItem) {
                keyCount++;
                // 只有"打过副本"的钥匙才可复制：progress 非空即视为已使用。
                if (!DungeonKeyItem.getProgress(stack).equals(DungeonProgress.EMPTY)) {
                    keyUsed = true;
                }
            } else if (stack.is(Items.COPPER_INGOT)) {
                copperCount++;
            } else {
                // 出现任何第三种物品就不成配方：避免用无关垃圾凑数。
                return false;
            }
        }
        // 1 把已使用的钥匙 + 至少 1 个铜锭；多余的铜锭不额外产出（一次只复制一把）。
        return keyCount == 1 && keyUsed && copperCount >= 1;
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
        String stageId = DungeonKeyItem.getStageId(sourceKey);
        if (!stageId.isEmpty()) {
            result.set(ModDataComponents.DUNGEON_STAGE_ID.get(), stageId);
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
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            // 钥匙消耗，其他材料也消耗（不允许剩）
            if (!stack.isEmpty()) {
                remaining.set(i, ItemStack.EMPTY);
            }
        }
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