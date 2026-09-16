package com.piranport.dungeon.recipe;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 章节钥匙合成配方。
 * <p>
 * Ch1：4 个 Ch1 钥匙碎片 → 1 把 Ch1 钥匙（无前置条件）<br>
 * Ch2+：4 个对应章节钥匙碎片 + 1 个前一章通关纪念章 → 1 把对应章节钥匙<br>
 * 如：Ch2 钥匙 = 4x 碎片 Ch2 + 1x 纪念章 Ch1
 * </p>
 */
public class ChapterKeyRecipe extends CustomRecipe {

    private final int chapterNumber;

    public ChapterKeyRecipe(int chapterNumber, CraftingBookCategory category) {
        super(category);
        this.chapterNumber = chapterNumber;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 5;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int fragmentCount = 0;
        boolean hasMedal = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof KeyFragmentItem frag) {
                if (frag.getChapterNumber() == chapterNumber) {
                    fragmentCount++;
                }
            } else if (stack.getItem() instanceof DeployMedalItem medal) {
                if (chapterNumber > 1 && medal.getChapterNumber() == chapterNumber - 1) {
                    hasMedal = true;
                }
            }
        }
        if (chapterNumber == 1) {
            return fragmentCount == 4;
        } else {
            return fragmentCount == 4 && hasMedal;
        }
    }

    @Override
    public ItemStack assemble(CraftingInput input, net.minecraft.core.HolderLookup.Provider registries) {
        ItemStack key = new ItemStack(ModItems.DUNGEON_KEY.get());
        String stageId = "chapter_" + chapterNumber;
        key.set(ModDataComponents.DUNGEON_STAGE_ID.get(), stageId);
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

        public static final MapCodec<ChapterKeyRecipe> CODEC = MapCodec.unit(
                new ChapterKeyRecipe(1, CraftingBookCategory.MISC));

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
