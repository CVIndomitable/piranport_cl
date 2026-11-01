package com.piranport.dungeon.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.piranport.PiranPort;
import com.piranport.compat.MaterialCompat;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 钥匙复制（《副本/17》：副本/09 修订）：保留 stageId 与 progress，仅清除 instanceId，
 * 生成一把同种钥匙的新副本。左上钥匙不消耗，留在格内。
 *
 * <h2>按钥匙物品拆成多条配方</h2>
 * <p>每条配方只认<u>一种</u>钥匙物品，由 {@code key} 字段声明（例如
 * {@code piranport:dungeon_key_1_1}）。这样 JEI「+」/原版配方书自动摆放时，
 * 左上 Ingredient 只有这一种物品，不会从背包里抓错关卡；{@link #getResultItem}
 * 展示的也就是这把钥匙本身。</p>
 *
 * <p>通用活动钥匙（金猫猫 / ulan / 妄想 / 地狱）没有固定物品，仍走
 * {@code chapter: 0} + {@code activity: true} 的旧摆法：左上 {@code piranport:dungeon_key}，
 * 正中末影之眼，其余七格紫水晶碎片。主线关卡钥匙则用铜锭 + 铝锭。</p>
 *
 * <p>材料口径（策划决策/副本/17）：主线钥匙 = 左上钥匙 + 正中铜锭 + 其余七格铝锭；
 * 活动钥匙 = 左上钥匙 + 正中末影之眼 + 其余七格紫水晶碎片。钥匙<b>不消耗</b>。
 * 铜锭/铝锭判定走 {@link MaterialCompat}，接受 {@code c:ingots/*} 标签里的等价材料。</p>
 *
 * <h2>JSON 字段</h2>
 * <p>{@code key}：要复制的钥匙物品 ID（30 把主线钥匙各一条）；
 * {@code chapter} / {@code activity}：仅通用活动钥匙使用。
 * 旧版「每章一把」钥匙的复制配方已随物品一并删除。</p>
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

    /** 要复制的钥匙物品；null 表示通用活动钥匙。 */
    @Nullable
    private final ResourceLocation keyId;

    /** 仅通用活动钥匙使用；关卡钥匙恒为 0。保留字段是为了继续读 {@code chapter: 0} 的旧 JSON。 */
    private final int chapter;

    /** 仅通用活动钥匙有效：true = 活动钥匙摆法（末影之眼 + 紫水晶碎片）。 */
    private final boolean activity;

    /** 关卡钥匙复制配方。 */
    public KeyResetRecipe(ResourceLocation keyId, CraftingBookCategory category) {
        this(keyId, 0, false, category);
    }

    /** 通用活动钥匙复制配方。 */
    public KeyResetRecipe(int chapter, boolean activity, CraftingBookCategory category) {
        this(null, chapter, activity, category);
    }

    public KeyResetRecipe(@Nullable ResourceLocation keyId, int chapter, boolean activity,
                          CraftingBookCategory category) {
        super(category);
        this.keyId = keyId;
        this.chapter = keyId == null ? chapter : 0;
        this.activity = keyId == null && activity;
    }

    public int chapter() {
        return chapter;
    }

    public boolean activity() {
        return activity;
    }

    @Nullable
    public ResourceLocation keyId() {
        return keyId;
    }

    /** 这条配方复制的钥匙物品。通用活动钥匙返回 piranport:dungeon_key。 */
    private Item sourceItem() {
        if (keyId != null) {
            return BuiltInRegistries.ITEM.get(keyId);
        }
        return ModItems.DUNGEON_KEY.get();
    }

    /**
     * 覆写 {@link CustomRecipe} 的 {@code isSpecial() == true} 默认值，改为 {@code false}（理由见类 javadoc）。
     */
    @Override
    public boolean isSpecial() {
        return false;
    }

    /**
     * JEI / 配方书展示用输入：左上<b>本配方指定</b>的钥匙，正中催化剂，其余七格材料。
     *
     * <p>关键约束：左上 Ingredient 必须只包含这一种钥匙。旧实现把七章钥匙放进同一个
     * Ingredient，自动摆放时就会从背包里挑到第一章钥匙（根因见类 javadoc）。</p>
     */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        Item source = sourceItem();
        Ingredient key = source == null || source == Items.AIR
                ? Ingredient.EMPTY : Ingredient.of(source);
        Ingredient filler = activity
                ? Ingredient.of(Items.AMETHYST_SHARD)
                : Ingredient.of(MaterialCompat.INGOTS_ALUMINUM);
        Ingredient catalyst = activity
                ? Ingredient.of(Items.ENDER_EYE)
                : Ingredient.of(Tags.Items.INGOTS_COPPER);
        return NonNullList.of(Ingredient.EMPTY,
                key, filler, filler,
                filler, catalyst, filler,
                filler, filler, filler);
    }

    /**
     * JEI / 配方书展示结果 = 本配方指定的钥匙。因为配方已经按物品拆分，这里的答案唯一，
     * 不再需要「随便给一把通用钥匙」的妥协。注意不能返回 {@link ItemStack#EMPTY}：
     * JEI 的 {@code CategoryRecipeValidator} 会把无产物配方整个过滤掉。
     */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        Item source = sourceItem();
        return source == null || source == Items.AIR ? ItemStack.EMPTY : new ItemStack(source);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        // 左上/正中两个约束位置要求完整 3×3 合成格。
        return width >= 3 && height >= 3;
    }

    /**
     * 匹配规则（策划决策/副本/17）：
     * <ul>
     *   <li>左上必须是<b>本条配方指定</b>的那把钥匙，且数量为 1；</li>
     *   <li>主线：正中铜锭，其余七格铝锭；活动：正中末影之眼，其余七格紫水晶碎片；</li>
     *   <li>钥匙<b>不消耗</b>，合成后留在左上格。</li>
     * </ul>
     *
     * <p>刚合成出来的空白钥匙也应该可以再复制一把，因此不要求钥匙「使用过」。</p>
     */
    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;
        Item source = sourceItem();
        if (source == null || source == Items.AIR) return false;
        ItemStack key = input.getItem(0);
        if (key.getCount() != 1 || !key.is(source)) return false;
        // 通用钥匙有两种摆法：活动关（金猫猫/ulan/妄想/地狱）与旧存档主线钥匙。
        // 关卡钥匙由物品类型唯一确定，不再看组件里的关卡。
        if (keyId == null && activity != isActivityStage(DungeonKeyItem.resolveStageId(key))) return false;

        ItemStack catalyst = input.getItem(4);
        if (catalyst.getCount() != 1) return false;
        if (activity ? !catalyst.is(Items.ENDER_EYE)
                : !MaterialCompat.matches(Items.COPPER_INGOT, catalyst)) return false;
        for (int i = 0; i < 9; i++) {
            if (i == 0 || i == 4) continue;
            ItemStack stack = input.getItem(i);
            if (stack.getCount() != 1) return false;
            if (activity ? !stack.is(Items.AMETHYST_SHARD)
                    : !MaterialCompat.matches(ModItems.ALUMINUM_INGOT.get(), stack)) return false;
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
        // 通用活动钥匙的关卡在组件里，必须把解析出的关卡一起写回。
        // 关卡钥匙的关卡由物品类型决定，setStageId 会自动忽略。
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

        PiranPort.LOGGER.info("KeyCopy: key={} chapter={} stageId={} (progress preserved, instanceId cleared)",
                keyId, chapter, stageId);
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
         * {@code key}：要复制的钥匙物品 ID；省略时按通用钥匙处理。
         * {@code chapter}：仅通用钥匙使用（0 = 通用）；{@code activity}：仅通用钥匙且 chapter=0 时有效。
         * 每条钥匙一条 JSON，物品由数据包显式声明，不再靠运行时猜「任意钥匙」。
         */
        public static final MapCodec<KeyResetRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                        Codec.intRange(0, 7).optionalFieldOf("chapter", 0)
                                .forGetter(KeyResetRecipe::chapter),
                        Codec.BOOL.optionalFieldOf("activity", false)
                                .forGetter(KeyResetRecipe::activity),
                        ResourceLocation.CODEC.optionalFieldOf("key")
                                .forGetter(r -> Optional.ofNullable(r.keyId)))
                .apply(i, (chapter, activity, key) -> new KeyResetRecipe(
                        key.orElse(null), chapter, activity, CraftingBookCategory.MISC)));

        @Override
        public MapCodec<KeyResetRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, KeyResetRecipe> streamCodec() {
            return StreamCodec.of(
                    (buf, recipe) -> {
                        buf.writeBoolean(recipe.keyId != null);
                        if (recipe.keyId != null) buf.writeResourceLocation(recipe.keyId);
                        buf.writeVarInt(recipe.chapter);
                        buf.writeBoolean(recipe.activity);
                    },
                    buf -> {
                        ResourceLocation key = buf.readBoolean() ? buf.readResourceLocation() : null;
                        return new KeyResetRecipe(key, buf.readVarInt(), buf.readBoolean(),
                                CraftingBookCategory.MISC);
                    });
        }
    }
}
