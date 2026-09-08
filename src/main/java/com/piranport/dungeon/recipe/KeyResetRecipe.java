package com.piranport.dungeon.recipe;

import com.mojang.serialization.MapCodec;
import com.piranport.PiranPort;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import com.piranport.registry.ModDataComponents;
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
 * 整合版 §2.2 末段：钥匙重置 = 讲台取出 + 合成栏合成 → 丢失原有信息，重新随机为
 * 相同类型、相同难度的副本。
 *
 * <p>本合成配方在合成栏中：1 把 DungeonKeyItem + 任意 1 个材料 → 1 把新 DungeonKeyItem
 * （同 stageId + 同 difficulty，progress 清空，instanceId 移除）。</p>
 *
 * <p>注意：当前仅清空 progress 与 instanceId，stageId 保持不变（"同类型"）。
 * difficulty 当前未在 DataComponent 中建模——若未来增加 DUNGEON_DIFFICULTY 组件，本配方需同步保留。</p>
 */
public class KeyResetRecipe extends CustomRecipe {

    public KeyResetRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        // 2x2 或 3x3 任意位置均可合成
        return width * height >= 2;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int keyCount = 0;
        int otherCount = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof DungeonKeyItem) {
                keyCount++;
            } else {
                otherCount++;
            }
        }
        // 必须 1 把钥匙 + 至少 1 个其他材料（防止 1 旧钥匙直接合成新钥匙无限刷）
        return keyCount == 1 && otherCount >= 1;
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

        // 输出：新钥匙（同 stageId、progress 清空、instanceId 移除）
        ItemStack result = new ItemStack(sourceKey.getItem());
        String stageId = DungeonKeyItem.getStageId(sourceKey);
        if (!stageId.isEmpty()) {
            result.set(ModDataComponents.DUNGEON_STAGE_ID.get(), stageId);
        }
        // 整合版 §2.2：progress 清空
        result.set(ModDataComponents.DUNGEON_PROGRESS.get(), DungeonProgress.EMPTY);
        // 整合版 §2.2：instanceId 移除（新副本）
        // （DUNGEON_INSTANCE_ID 不设置则为空，符合"新副本"语义）

        PiranPort.LOGGER.info("KeyReset: stageId={} (progress cleared, instanceId removed)",
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
        return ModRecipeSerializers.KEY_RESET.get();
    }

    // ===== Serializer =====

    public static class Serializer implements RecipeSerializer<KeyResetRecipe> {
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