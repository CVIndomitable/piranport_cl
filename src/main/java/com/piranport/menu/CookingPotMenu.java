package com.piranport.menu;

import com.piranport.block.entity.CookingPotBlockEntity;
import com.piranport.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CookingPotMenu extends AbstractContainerMenu {
    private final CookingPotBlockEntity blockEntity;
    private final Level level;

    // Output-only slot
    private static class OutputSlot extends SlotItemHandler {
        public OutputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }
        @Override
        public boolean mayPlace(ItemStack stack) { return false; }
    }

    public static CookingPotMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        CookingPotBlockEntity be = (CookingPotBlockEntity) playerInventory.player.level().getBlockEntity(pos);
        return new CookingPotMenu(containerId, playerInventory, be);
    }

    public CookingPotMenu(int containerId, Inventory playerInventory, CookingPotBlockEntity be) {
        super(ModMenuTypes.COOKING_POT_MENU.get(), containerId);
        this.blockEntity = be;
        this.level = playerInventory.player.level();

        IItemHandler handler = be.getItemHandler();

        // Input slots 0-8 (3x3 grid), slot indices 0-8
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(handler, row * 3 + col, 8 + col * 18, 17 + row * 18));
            }
        }
        // Output slot (index 9)
        addSlot(new OutputSlot(handler, 9, 124, 35));

        // Player inventory (slots 10-36)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // Player hotbar (slots 37-45)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        addDataSlots(be.dataAccess);
    }

    public int getProgress() { return blockEntity.dataAccess.get(0); }
    public int getTotalTime() { return blockEntity.dataAccess.get(1); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 10) {
                // From machine to player
                if (!moveItemStackTo(stack, 10, 46, true)) return ItemStack.EMPTY;
            } else {
                // From player to machine input
                if (!moveItemStackTo(stack, 0, 9, false)) return ItemStack.EMPTY;
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
