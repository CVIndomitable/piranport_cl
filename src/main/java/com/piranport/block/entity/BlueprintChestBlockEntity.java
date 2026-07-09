package com.piranport.block.entity;

import com.piranport.menu.BlueprintChestMenu;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class BlueprintChestBlockEntity extends BlockEntity implements MenuProvider {
    public static final int BLUEPRINT_SLOTS = 9;
    public static final int PAPER_SLOT = 9;
    public static final int TOTAL_SLOTS = 10;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < BLUEPRINT_SLOTS ? 1 : 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot < BLUEPRINT_SLOTS) return isBlueprint(stack);
            return slot == PAPER_SLOT && stack.is(Items.PAPER);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !isItemValid(slot, stack)) return stack;
            if (slot < BLUEPRINT_SLOTS && hasBlueprint(stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }
    };

    public BlueprintChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.BLUEPRINT_CHEST.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }

    public static boolean isBlueprint(ItemStack stack) {
        return stack.is(ModItems.MEDIUM_GUN_BLUEPRINT.get())
                || stack.is(ModItems.LARGE_GUN_BLUEPRINT.get())
                || stack.is(ModItems.CREATIVE_BLUEPRINT.get());
    }

    public boolean hasBlueprint(ItemStack stack) {
        if (!isBlueprint(stack)) return false;
        for (int i = 0; i < BLUEPRINT_SLOTS; i++) {
            ItemStack stored = itemHandler.getStackInSlot(i);
            if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, stack)) {
                return true;
            }
        }
        return false;
    }

    public ItemStack getBlueprint(int index) {
        if (index < 0 || index >= BLUEPRINT_SLOTS) return ItemStack.EMPTY;
        return itemHandler.getStackInSlot(index);
    }

    public boolean canCopy(int index) {
        return !getBlueprint(index).isEmpty() && !itemHandler.getStackInSlot(PAPER_SLOT).isEmpty();
    }

    public ItemStack previewCopy(int index) {
        if (!canCopy(index)) return ItemStack.EMPTY;
        return getBlueprint(index).copyWithCount(1);
    }

    public boolean consumePaperForCopy() {
        ItemStack paper = itemHandler.getStackInSlot(PAPER_SLOT);
        if (paper.isEmpty()) return false;
        paper.shrink(1);
        if (paper.isEmpty()) {
            itemHandler.setStackInSlot(PAPER_SLOT, ItemStack.EMPTY);
        }
        setChanged();
        return true;
    }

    public List<ItemStack> removeAllItems() {
        List<ItemStack> removed = new ArrayList<>();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                removed.add(stack);
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        setChanged();
        return removed;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.piranport.blueprint_chest");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BlueprintChestMenu(containerId, playerInventory, this);
    }

    public void writeScreenOpeningData(ServerPlayer player, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeBlockPos(this.worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", itemHandler.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("items"));
        }
    }
}
