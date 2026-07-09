package com.piranport.menu;

import com.piranport.block.BlueprintChestBlock;
import com.piranport.block.entity.BlueprintChestBlockEntity;
import com.piranport.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class BlueprintChestMenu extends AbstractContainerMenu {
    public static final int COPY_SLOT_START = 10;
    public static final int PLAYER_INV_START = 19;
    public static final int PLAYER_INV_END = 55;

    private final BlueprintChestBlockEntity blockEntity;

    private static class BlueprintSlot extends SlotItemHandler {
        public BlueprintSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return BlueprintChestBlockEntity.isBlueprint(stack);
        }

        @Override
        public int getMaxStackSize() { return 1; }
    }

    private static class PaperSlot extends SlotItemHandler {
        public PaperSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) { return stack.is(Items.PAPER); }
    }

    private class CopySlot extends Slot {
        private final int blueprintIndex;

        public CopySlot(int blueprintIndex, int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
            this.blueprintIndex = blueprintIndex;
        }

        @Override
        public boolean mayPlace(ItemStack stack) { return false; }

        @Override
        public boolean mayPickup(Player player) { return blockEntity.canCopy(blueprintIndex); }

        @Override
        public boolean hasItem() { return !getItem().isEmpty(); }

        @Override
        public ItemStack getItem() { return blockEntity.previewCopy(blueprintIndex); }

        @Override
        public ItemStack remove(int amount) {
            if (amount <= 0 || !blockEntity.canCopy(blueprintIndex)) return ItemStack.EMPTY;
            ItemStack copy = blockEntity.previewCopy(blueprintIndex);
            if (blockEntity.consumePaperForCopy()) {
                return copy;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void set(ItemStack stack) {}

        @Override
        public int getMaxStackSize() { return 1; }
    }

    public static BlueprintChestMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof BlueprintChestBlockEntity be) {
            return new BlueprintChestMenu(containerId, playerInventory, be);
        }
        throw new IllegalStateException("BlueprintChestBlockEntity not found at " + pos);
    }

    public BlueprintChestMenu(int containerId, Inventory playerInventory, BlueprintChestBlockEntity be) {
        super(ModMenuTypes.BLUEPRINT_CHEST_MENU.get(), containerId);
        this.blockEntity = be;

        IItemHandler handler = be.getItemHandler();
        for (int col = 0; col < BlueprintChestBlockEntity.BLUEPRINT_SLOTS; col++) {
            addSlot(new BlueprintSlot(handler, col, 8 + col * 18, 20));
        }

        addSlot(new PaperSlot(handler, BlueprintChestBlockEntity.PAPER_SLOT, 80, 54));

        for (int col = 0; col < BlueprintChestBlockEntity.BLUEPRINT_SLOTS; col++) {
            addSlot(new CopySlot(col, 8 + col * 18, 88));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, 132 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 190));
        }
    }

    public BlueprintChestBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        if (index >= COPY_SLOT_START && index < PLAYER_INV_START) {
            int blueprintIndex = index - COPY_SLOT_START;
            ItemStack copy = blockEntity.previewCopy(blueprintIndex);
            if (copy.isEmpty()) return ItemStack.EMPTY;
            ItemStack moving = copy.copy();
            if (!moveItemStackTo(moving, PLAYER_INV_START, PLAYER_INV_END, true)) {
                return ItemStack.EMPTY;
            }
            if (blockEntity.consumePaperForCopy()) {
                return copy;
            }
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < BlueprintChestBlockEntity.TOTAL_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true)) return ItemStack.EMPTY;
        } else {
            if (BlueprintChestBlockEntity.isBlueprint(stack)) {
                if (!moveItemStackTo(stack, 0, BlueprintChestBlockEntity.BLUEPRINT_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(Items.PAPER)) {
                if (!moveItemStackTo(stack, BlueprintChestBlockEntity.PAPER_SLOT,
                        BlueprintChestBlockEntity.PAPER_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        Level level = blockEntity.getLevel();
        if (level == null) return false;
        BlockPos pos = blockEntity.getBlockPos();
        if (!level.isLoaded(pos)) return false;
        if (!(level.getBlockState(pos).getBlock() instanceof BlueprintChestBlock)) return false;
        return stillValid(ContainerLevelAccess.create(level, pos), player,
                level.getBlockState(pos).getBlock());
    }
}
