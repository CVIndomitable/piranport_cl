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
 * 整合版 §2.2 末段 + 副本/09 修订：钥匙复制 = 讲台取出 + 合成栏合成 → 保留原有进度，
 * 仅清除 instanceId，生成一把同类型同进度的新钥匙。
 *
 * <p>本合成配方在合成栏中：1 把 DungeonKeyItem + 任意 1 个材料 → 1 把新 DungeonKeyItem
 * （同 stageId + 同 progress，instanceId 移除）。</p>
 *
 * <p>注意：当前保留 stageId 和 progress，仅清除 instanceId（"复制"语义）。</p>
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