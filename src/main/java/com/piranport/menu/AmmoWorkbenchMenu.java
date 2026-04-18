package com.piranport.menu;

import com.piranport.block.AmmoWorkbenchBlock;
import com.piranport.block.entity.AmmoWorkbenchBlockEntity;
import com.piranport.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.UUID;

public class AmmoWorkbenchMenu extends AbstractContainerMenu {
    private final AmmoWorkbenchBlockEntity blockEntity;

    private static final int INV_X = 34;

    // Slot indices in this menu.
    private static final int OUTPUT_IDX = 0;
    private static final int INV_START = 1;
    private static final int HOTBAR_START = 28;
    private static final int TOTAL_SLOTS = 37;

    private static class OutputSlot extends SlotItemHandler {
        private final AmmoWorkbenchBlockEntity be;

        public OutputSlot(IItemHandler handler, int index, int x, int y,
                          AmmoWorkbenchBlockEntity be) {
            super(handler, index, x, y);
            this.be = be;
        }

        @Override
        public boolean mayPlace(ItemStack stack) { return false; }

        @Override
        public boolean mayPickup(Player player) {
            UUID owner = be.getCraftingOwner();
            return owner == null || owner.equals(player.getUUID());
        }
    }

    public static AmmoWorkbenchMenu fromNetwork(int containerId, Inventory playerInventory,
                                                 FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof AmmoWorkbenchBlockEntity be) {
            return new AmmoWorkbenchMenu(containerId, playerInventory, be);
        }
        throw new IllegalStateException("AmmoWorkbenchBlockEntity not found at " + pos);
    }

    public AmmoWorkbenchMenu(int containerId, Inventory playerInventory, AmmoWorkbenchBlockEntity be) {
        super(ModMenuTypes.AMMO_WORKBENCH_MENU.get(), containerId);
        this.blockEntity = be;

        IItemHandler handler = be.getItemHandler();

        addSlot(new OutputSlot(handler, 0, 207, 131, be));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        INV_X + col * 18, 148 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, INV_X + col * 18, 206));
        }

        addDataSlots(be.dataAccess);
    }

    private Level level() { return blockEntity.getLevel(); }

    public int getProgress() { return blockEntity.dataAccess.get(0); }
    public int getTotalTime() { return blockEntity.dataAccess.get(1); }
    public boolean isCrafting() { return getTotalTime() > 0; }
    public BlockPos getBlockPos() { return blockEntity.getBlockPos(); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return result;
        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index == OUTPUT_IDX) {
            // Output → inventory
            if (!moveItemStackTo(stack, INV_START, TOTAL_SLOTS, true)) return ItemStack.EMPTY;
        } else if (index >= INV_START && index < HOTBAR_START) {
            // Main inventory → hotbar
            if (!moveItemStackTo(stack, HOTBAR_START, TOTAL_SLOTS, false)) return ItemStack.EMPTY;
        } else if (index >= HOTBAR_START && index < TOTAL_SLOTS) {
            // Hotbar → main inventory
            if (!moveItemStackTo(stack, INV_START, HOTBAR_START, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        Level level = level();
        if (level == null) return false;
        BlockPos pos = blockEntity.getBlockPos();
        if (!level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AmmoWorkbenchBlock)) return false;
        return stillValid(ContainerLevelAccess.create(level, pos), player, state.getBlock());
    }
}
