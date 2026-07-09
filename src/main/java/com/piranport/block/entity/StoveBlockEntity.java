package com.piranport.block.entity;

import com.piranport.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StoveBlockEntity extends BlockEntity {
    public static final int SLOT_COUNT = 8;
    private static final int MIN_COOK_TIME = 40;

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private final int[] cookingProgress = new int[SLOT_COUNT];
    private final int[] cookingTotalTime = new int[SLOT_COUNT];

    public StoveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.STOVE.get(), pos, state);
    }

    public boolean isEmpty() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!items.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    public boolean tryInsert(ItemStack stack, boolean creative) {
        if (level == null || stack.isEmpty() || !hasFoodSmeltingRecipe(level, stack)) return false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (items.getStackInSlot(i).isEmpty()) {
                items.setStackInSlot(i, stack.copyWithCount(1));
                cookingProgress[i] = 0;
                cookingTotalTime[i] = getCookTime(level, stack);
                if (!creative) stack.shrink(1);
                setChanged();
                return true;
            }
        }
        return false;
    }

    public ItemStack removeLastUnfinishedItem() {
        for (int i = SLOT_COUNT - 1; i >= 0; i--) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                items.setStackInSlot(i, ItemStack.EMPTY);
                cookingProgress[i] = 0;
                cookingTotalTime[i] = 0;
                setChanged();
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public List<ItemStack> removeAllItems() {
        List<ItemStack> removed = new ArrayList<>();
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                removed.add(stack);
                items.setStackInSlot(i, ItemStack.EMPTY);
                cookingProgress[i] = 0;
                cookingTotalTime[i] = 0;
            }
        }
        setChanged();
        return removed;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StoveBlockEntity stove) {
        boolean changed = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack input = stove.items.getStackInSlot(i);
            if (input.isEmpty()) {
                stove.cookingProgress[i] = 0;
                stove.cookingTotalTime[i] = 0;
                continue;
            }

            Optional<RecipeHolder<SmeltingRecipe>> recipe = findFoodSmeltingRecipe(level, input);
            if (recipe.isEmpty()) {
                stove.cookingProgress[i] = 0;
                stove.cookingTotalTime[i] = 0;
                continue;
            }

            if (stove.cookingTotalTime[i] <= 0) {
                stove.cookingTotalTime[i] = getCookTime(recipe.get().value());
            }
            stove.cookingProgress[i]++;
            changed = true;

            if (stove.cookingProgress[i] >= stove.cookingTotalTime[i]) {
                ItemStack result = recipe.get().value()
                        .assemble(new SingleRecipeInput(input), level.registryAccess());
                stove.items.setStackInSlot(i, ItemStack.EMPTY);
                stove.cookingProgress[i] = 0;
                stove.cookingTotalTime[i] = 0;
                popCookedResult(level, pos, result.copy());
            }
        }
        if (changed) {
            stove.setChanged();
        }
    }

    public static boolean hasFoodSmeltingRecipe(Level level, ItemStack stack) {
        return findFoodSmeltingRecipe(level, stack).isPresent();
    }

    private static Optional<RecipeHolder<SmeltingRecipe>> findFoodSmeltingRecipe(Level level, ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        SingleRecipeInput input = new SingleRecipeInput(stack);
        Optional<RecipeHolder<SmeltingRecipe>> recipe =
                level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
        if (recipe.isEmpty()) return Optional.empty();
        ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
        return result.getFoodProperties(null) != null ? recipe : Optional.empty();
    }

    private static int getCookTime(Level level, ItemStack stack) {
        return findFoodSmeltingRecipe(level, stack)
                .map(holder -> getCookTime(holder.value()))
                .orElse(MIN_COOK_TIME);
    }

    private static int getCookTime(SmeltingRecipe recipe) {
        return Math.max(MIN_COOK_TIME, recipe.getCookingTime() / 2);
    }

    private static void popCookedResult(Level level, BlockPos pos, ItemStack result) {
        if (result.isEmpty()) return;
        ItemEntity entity = new ItemEntity(level,
                pos.getX() + 0.5,
                pos.getY() + 1.05,
                pos.getZ() + 0.5,
                result);
        entity.setDeltaMovement(0.0, 0.18, 0.0);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putIntArray("cookingProgress", cookingProgress);
        tag.putIntArray("cookingTotalTime", cookingTotalTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) {
            items.deserializeNBT(registries, tag.getCompound("items"));
        }
        int[] progress = tag.getIntArray("cookingProgress");
        int[] total = tag.getIntArray("cookingTotalTime");
        for (int i = 0; i < SLOT_COUNT; i++) {
            cookingProgress[i] = i < progress.length ? progress[i] : 0;
            cookingTotalTime[i] = i < total.length ? total[i] : 0;
        }
    }
}
