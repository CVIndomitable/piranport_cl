package com.piranport.menu;

import com.piranport.block.entity.StoneMillBlockEntity;
import com.piranport.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class StoneMillMenu extends AbstractContainerMenu {
    private final @Nullable StoneMillBlockEntity blockEntity;
    private final ItemStackHandler itemHandler;

    // Client-side constructor (from network)
    public static StoneMillMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        var be = inv.player.level().getBlockEntity(pos);
        StoneMillBlockEntity mill = be instanceof StoneMillBlockEntity m ? m : null;
        return new StoneMillMenu(id, inv, mill);
    }

    // Server-side constructor
    public StoneMillMenu(int containerId, Inventory playerInventory, @Nullable StoneMillBlockEntity blockEntity) {
        super(ModMenuTypes.STONE_MILL_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.itemHandler = blockEntity != null ? blockEntity.getItemHandler() : new ItemStackHandler(6);

        // Input slots 0-3: 2×2 grid at (8,20),(26,20),(8,38),(26,38)
        addSlot(new SlotItemHandler(itemHandler, 0, 8, 20));
        addSlot(new SlotItemHandler(itemHandler, 1, 26, 20));
        addSlot(new SlotItemHandler(itemHandler, 2, 8, 38));
        addSlot(new SlotItemHandler(itemHandler, 3, 26, 38));

        // Output slots 4-5: (78,20),(78,38) — output only
        addSlot(new OutputSlot(itemHandler, 4, 78, 20));
        addSlot(new OutputSlot(itemHandler, 5, 78, 38));

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, 86 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 144));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        ItemStack original = slotStack.copy();

        if (index < 6) {
            // Mill → player
            if (!moveItemStackTo(slotStack, 6, 42, true)) return ItemStack.EMPTY;
        } else {
            // Player → mill input
            if (!moveItemStackTo(slotStack, 0, 4, false)) return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        if (slotStack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, slotStack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return false;
        return player.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    private static class OutputSlot extends SlotItemHandler {
        public OutputSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
