package com.piranport.menu;

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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AmmoWorkbenchMenu extends AbstractContainerMenu {
    private final AmmoWorkbenchBlockEntity blockEntity;
    private final Level level;

    // Player inventory x-offset to center in 230-wide GUI
    private static final int INV_X = 34;

    private static class OutputSlot extends SlotItemHandler {
        public OutputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) { return false; }
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
        this.level = playerInventory.player.level();

        IItemHandler handler = be.getItemHandler();

        // Slot 0: Output
        addSlot(new OutputSlot(handler, 0, 207, 131));

        // Player inventory (slots 1-27)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        INV_X + col * 18, 148 + row * 18));
            }
        }
        // Player hotbar (slots 28-36)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, INV_X + col * 18, 206));
        }

        addDataSlots(be.dataAccess);
    }

    public int getProgress() { return blockEntity.dataAccess.get(0); }
    public int getTotalTime() { return blockEntity.dataAccess.get(1); }
    public boolean isCrafting() { return getTotalTime() > 0; }
    public BlockPos getBlockPos() { return blockEntity.getBlockPos(); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == 0) {
                // From output to player
                if (!moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
            } else {
                // Player to output not allowed; shift-click does nothing special
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player,
                level.getBlockState(blockEntity.getBlockPos()).getBlock());
    }
}
