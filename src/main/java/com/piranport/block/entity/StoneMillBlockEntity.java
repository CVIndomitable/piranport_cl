package com.piranport.block.entity;

import com.piranport.menu.StoneMillMenu;
import com.piranport.recipe.StoneMillRecipe;
import com.piranport.recipe.StoneMillRecipeInput;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StoneMillBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOTS = 4;
    public static final int OUTPUT_SLOTS = 2;
    public static final int TOTAL_SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;

    private boolean processing = false;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // Recursion safe: processing flag prevents re-entry from insertOutput/consumeIngredients;
            // the while loop in processRecipes() re-checks after each successful recipe.
            if (level != null && !level.isClientSide() && !processing) {
                processRecipes();
            }
        }
    };

    /**
     * Phase 31: AE2/hopper automation — input-only view of slots 0-3 (top/side faces).
     * External automation may insert ingredients but not extract them.
     * RangedWrapper was replaced because it allowed extraction from input slots,
     * which could cause AE2 Storage Bus to accidentally pull out unprocessed ingredients.
     */
    private final IItemHandler inputHandler = new IItemHandler() {
        @Override public int getSlots() { return INPUT_SLOTS; }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
    };

    /**
     * Phase 31: output-only view of slots 4-5 (bottom face).
     * External automation may extract products but not insert into output slots.
     */
    private final IItemHandler outputHandler = new IItemHandler() {
        @Override public int getSlots() { return OUTPUT_SLOTS; }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(INPUT_SLOTS + slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return itemHandler.extractItem(INPUT_SLOTS + slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(INPUT_SLOTS + slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    };

    public StoneMillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.STONE_MILL.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == Direction.DOWN) return outputHandler;
        return inputHandler;
    }

    public ItemStack getItem(int slot) {
        return itemHandler.getStackInSlot(slot);
    }

    private void processRecipes() {
        if (processing || level == null) return;
        processing = true;
        try {
            boolean processed = true;
            while (processed) {
                processed = processOneRecipe();
            }
        } finally {
            processing = false;
        }
    }

    private boolean processOneRecipe() {
        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack s = itemHandler.getStackInSlot(i);
            if (!s.isEmpty()) inputs.add(s);
        }
        if (inputs.isEmpty()) return false;

        StoneMillRecipeInput input = new StoneMillRecipeInput(inputs);
        Optional<RecipeHolder<StoneMillRecipe>> opt =
                level.getRecipeManager().getRecipeFor(ModRecipeTypes.STONE_MILL_TYPE.get(), input, level);
        if (opt.isEmpty()) return false;

        StoneMillRecipe recipe = opt.get().value();
        ItemStack result = recipe.assemble(input, level.registryAccess());

        if (!canInsertOutput(result)) return false;

        consumeIngredients(recipe.getIngredients());
        insertOutput(result);
        return true;
    }

    private boolean canInsertOutput(ItemStack result) {
        for (int i = INPUT_SLOTS; i < TOTAL_SLOTS; i++) {
            ItemStack slot = itemHandler.getStackInSlot(i);
            if (slot.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(slot, result)
                    && slot.getCount() + result.getCount() <= slot.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private void insertOutput(ItemStack result) {
        // Stack with existing first
        for (int i = INPUT_SLOTS; i < TOTAL_SLOTS; i++) {
            ItemStack slot = itemHandler.getStackInSlot(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, result)) {
                int toAdd = Math.min(slot.getMaxStackSize() - slot.getCount(), result.getCount());
                itemHandler.setStackInSlot(i, slot.copyWithCount(slot.getCount() + toAdd));
                result.shrink(toAdd);
                if (result.isEmpty()) return;
            }
        }
        // Then empty slots
        for (int i = INPUT_SLOTS; i < TOTAL_SLOTS; i++) {
            if (itemHandler.getStackInSlot(i).isEmpty()) {
                itemHandler.setStackInSlot(i, result.copy());
                return;
            }
        }
    }

    private void consumeIngredients(List<net.minecraft.world.item.crafting.Ingredient> ingredients) {
        List<net.minecraft.world.item.crafting.Ingredient> remaining = new ArrayList<>(ingredients);
        for (int i = 0; i < INPUT_SLOTS && !remaining.isEmpty(); i++) {
            ItemStack slot = itemHandler.getStackInSlot(i);
            if (slot.isEmpty()) continue;
            for (int j = 0; j < remaining.size(); j++) {
                if (remaining.get(j).test(slot)) {
                    ItemStack updated = slot.copy();
                    updated.shrink(1);
                    itemHandler.setStackInSlot(i, updated);
                    remaining.remove(j);
                    break;
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.piranport.stone_mill");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StoneMillMenu(containerId, playerInventory, this);
    }

    public void writeScreenOpeningData(ServerPlayer player, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeBlockPos(this.worldPosition);
    }
}
