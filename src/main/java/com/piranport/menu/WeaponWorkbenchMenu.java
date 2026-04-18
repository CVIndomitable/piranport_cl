package com.piranport.menu;

import com.piranport.block.WeaponWorkbenchBlock;
import com.piranport.block.entity.WeaponWorkbenchBlockEntity;
import com.piranport.crafting.WeaponWorkbenchRecipeRegistry;
import com.piranport.registry.ModItems;
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

/**
 * Slots: 0=blueprint, 1-6=materials, 7=output, 8-34=player inv, 35-43=hotbar
 *
 * Button IDs (via clickMenuButton):
 *   0..4  = tab select
 *   100+n = recipe select (for current tab)
 *   200   = start crafting
 *
 * Closing the GUI refunds every slot to the player and cancels any active craft —
 * materials are not consumed until the craft completes.
 */
public class WeaponWorkbenchMenu extends AbstractContainerMenu {
    private static final int BTN_TAB_MIN = 0;
    private static final int BTN_TAB_MAX = 4;
    private static final int BTN_RECIPE_BASE = 100;
    private static final int BTN_RECIPE_MAX = 199;
    private static final int BTN_CRAFT = 200;

    private final WeaponWorkbenchBlockEntity blockEntity;

    private static class LockedSlot extends SlotItemHandler {
        private final WeaponWorkbenchBlockEntity be;

        public LockedSlot(IItemHandler handler, int index, int x, int y,
                          WeaponWorkbenchBlockEntity be) {
            super(handler, index, x, y);
            this.be = be;
        }

        @Override
        public boolean mayPlace(ItemStack stack) { return !be.isCrafting(); }

        @Override
        public boolean mayPickup(Player player) { return !be.isCrafting(); }
    }

    private static class OutputSlot extends SlotItemHandler {
        public OutputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) { return false; }
    }

    public static WeaponWorkbenchMenu fromNetwork(int containerId, Inventory playerInventory,
                                                   FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos)
                instanceof WeaponWorkbenchBlockEntity be) {
            return new WeaponWorkbenchMenu(containerId, playerInventory, be);
        }
        throw new IllegalStateException("WeaponWorkbenchBlockEntity not found at " + pos);
    }

    public WeaponWorkbenchMenu(int containerId, Inventory playerInventory,
                               WeaponWorkbenchBlockEntity be) {
        super(ModMenuTypes.WEAPON_WORKBENCH_MENU.get(), containerId);
        this.blockEntity = be;

        IItemHandler handler = be.getItemHandler();

        addSlot(new LockedSlot(handler, 0, 172, 20, be));

        addSlot(new LockedSlot(handler, 1, 154, 42, be));
        addSlot(new LockedSlot(handler, 2, 172, 42, be));
        addSlot(new LockedSlot(handler, 3, 154, 60, be));
        addSlot(new LockedSlot(handler, 4, 172, 60, be));
        addSlot(new LockedSlot(handler, 5, 154, 78, be));
        addSlot(new LockedSlot(handler, 6, 172, 78, be));

        addSlot(new OutputSlot(handler, 7, 163, 128));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + col,
                        17 + col * 18, 156 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 17 + col * 18, 214));
        }

        addDataSlots(be.dataAccess);
    }

    private Level level() {
        return blockEntity.getLevel();
    }

    public int getSelectedTab()      { return blockEntity.dataAccess.get(0); }
    public int getSelectedRecipe()   { return blockEntity.dataAccess.get(1); }
    public int getCraftingProgress() { return blockEntity.dataAccess.get(2); }
    public int getCraftingTotalTime(){ return blockEntity.dataAccess.get(3); }
    public boolean isCrafting()      { return blockEntity.dataAccess.get(4) != 0; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!stillValid(player)) return false;

        if (id >= BTN_TAB_MIN && id <= BTN_TAB_MAX) {
            if (blockEntity.isCrafting()) return false;
            blockEntity.setSelectedTab(id);
            blockEntity.setSelectedRecipe(0);
            return true;
        } else if (id >= BTN_RECIPE_BASE && id <= BTN_RECIPE_MAX) {
            if (blockEntity.isCrafting()) return false;
            int recipeIdx = id - BTN_RECIPE_BASE;
            var recipes = WeaponWorkbenchRecipeRegistry.getRecipesForTab(blockEntity.getSelectedTab());
            if (recipeIdx >= 0 && recipeIdx < recipes.size()) {
                blockEntity.setSelectedRecipe(recipeIdx);
            }
            return true;
        } else if (id == BTN_CRAFT) {
            if (!blockEntity.isCrafting()) {
                blockEntity.startCrafting();
            }
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 8) {
                // workbench → inventory
                if (!moveItemStackTo(stack, 8, 44, true)) return ItemStack.EMPTY;
            } else {
                // inventory → workbench: route blueprints to slot 0, others to material slots 1-6
                boolean isBlueprint = stack.is(ModItems.CREATIVE_BLUEPRINT.get())
                        || stack.is(ModItems.MEDIUM_GUN_BLUEPRINT.get())
                        || stack.is(ModItems.LARGE_GUN_BLUEPRINT.get());
                if (isBlueprint) {
                    if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
                } else {
                    if (!moveItemStackTo(stack, 1, 7, false)) return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            blockEntity.cancelCrafting();
            var handler = blockEntity.getItemHandler();
            for (int i = 0; i < WeaponWorkbenchBlockEntity.TOTAL_SLOTS; i++) {
                ItemStack stack = handler.extractItem(i, Integer.MAX_VALUE, false);
                if (!stack.isEmpty()) {
                    if (!player.addItem(stack)) {
                        player.drop(stack, false);
                    }
                }
            }
            blockEntity.setCurrentUser(null);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        Level level = level();
        if (level == null) return false;
        BlockPos pos = blockEntity.getBlockPos();
        if (!level.isLoaded(pos)) return false;
        if (!(level.getBlockState(pos).getBlock() instanceof WeaponWorkbenchBlock)) return false;
        return stillValid(ContainerLevelAccess.create(level, pos), player,
                level.getBlockState(pos).getBlock());
    }
}
