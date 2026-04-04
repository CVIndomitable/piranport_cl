package com.piranport.menu;

import com.piranport.block.entity.ReloadFacilityBlockEntity;
import com.piranport.item.TorpedoItem;
import com.piranport.item.TorpedoLauncherItem;
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

public class ReloadFacilityMenu extends AbstractContainerMenu {
    private final ReloadFacilityBlockEntity blockEntity;
    private final Level level;

    // Slot that only accepts torpedo launchers
    private static class LauncherSlot extends SlotItemHandler {
        public LauncherSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }
        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof TorpedoLauncherItem;
        }
        @Override
        public int getMaxStackSize() { return 1; }
    }

    // Slot that only accepts torpedo ammo
    private static class AmmoSlot extends SlotItemHandler {
        public AmmoSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }
        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof TorpedoItem;
        }
    }

    // Output-only slot
    private static class OutputSlot extends SlotItemHandler {
        public OutputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }
        @Override
        public boolean mayPlace(ItemStack stack) { return false; }
    }

    public static ReloadFacilityMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof ReloadFacilityBlockEntity be) {
            return new ReloadFacilityMenu(containerId, playerInventory, be);
        }
        throw new IllegalStateException("ReloadFacilityBlockEntity not found at " + pos);
    }

    public ReloadFacilityMenu(int containerId, Inventory playerInventory, ReloadFacilityBlockEntity be) {
        super(ModMenuTypes.RELOAD_FACILITY_MENU.get(), containerId);
        this.blockEntity = be;
        this.level = playerInventory.player.level();

        IItemHandler handler = be.getItemHandler();

        // Slot 0: Launcher input (left side)
        addSlot(new LauncherSlot(handler, 0, 35, 35));
        // Slot 1: Ammo input (center)
        addSlot(new AmmoSlot(handler, 1, 71, 35));
        // Slot 2: Output (right side)
        addSlot(new OutputSlot(handler, 2, 131, 35));

        // Player inventory (slots 3-29)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // Player hotbar (slots 30-38)
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
            if (index < 3) {
                // From machine to player
                if (!moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY;
            } else {
                // From player to machine
                if (stack.getItem() instanceof TorpedoLauncherItem) {
                    if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (stack.getItem() instanceof TorpedoItem) {
                    if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
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
