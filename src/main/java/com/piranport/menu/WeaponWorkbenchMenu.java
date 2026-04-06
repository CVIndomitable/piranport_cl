package com.piranport.menu;

import com.piranport.block.entity.WeaponWorkbenchBlockEntity;
import com.piranport.crafting.WeaponWorkbenchRecipeRegistry;
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
 * 武器合成台菜单。
 * 槽位: 0=蓝图, 1-6=原料, 7=产物, 8-34=背包, 35-43=快捷栏
 * 通过 clickMenuButton 接收客户端交互（标签/配方/合成）。
 */
public class WeaponWorkbenchMenu extends AbstractContainerMenu {
    private final WeaponWorkbenchBlockEntity blockEntity;
    private final Level level;

    // 合成中锁定的槽位
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

    // 产物槽——只能取出
    private static class OutputSlot extends SlotItemHandler {
        public OutputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) { return false; }
    }

    // ===== 构造 =====

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
        this.level = playerInventory.player.level();

        IItemHandler handler = be.getItemHandler();

        // Slot 0: 蓝图
        addSlot(new LockedSlot(handler, 0, 172, 20, be));

        // Slots 1-6: 原料 (2列×3行)
        addSlot(new LockedSlot(handler, 1, 154, 42, be));
        addSlot(new LockedSlot(handler, 2, 172, 42, be));
        addSlot(new LockedSlot(handler, 3, 154, 60, be));
        addSlot(new LockedSlot(handler, 4, 172, 60, be));
        addSlot(new LockedSlot(handler, 5, 154, 78, be));
        addSlot(new LockedSlot(handler, 6, 172, 78, be));

        // Slot 7: 产物
        addSlot(new OutputSlot(handler, 7, 163, 128));

        // Player inventory (slots 8-34)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + col,
                        17 + col * 18, 156 + row * 18));
            }
        }
        // Hotbar (slots 35-43)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 17 + col * 18, 214));
        }

        addDataSlots(be.dataAccess);
    }

    // ===== 数据访问（客户端通过 ContainerData 读取） =====

    public int getSelectedTab()      { return blockEntity.dataAccess.get(0); }
    public int getSelectedRecipe()   { return blockEntity.dataAccess.get(1); }
    public int getCraftingProgress() { return blockEntity.dataAccess.get(2); }
    public int getCraftingTotalTime(){ return blockEntity.dataAccess.get(3); }
    public boolean isCrafting()      { return blockEntity.dataAccess.get(4) != 0; }

    // ===== 按钮事件：0-4=标签, 100+=配方, 200=合成 =====

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id <= 4) {
            blockEntity.setSelectedTab(id);
            blockEntity.setSelectedRecipe(0);
            blockEntity.cancelCrafting();
            return true;
        } else if (id >= 100 && id < 200) {
            int recipeIdx = id - 100;
            var recipes = WeaponWorkbenchRecipeRegistry.getRecipesForTab(blockEntity.getSelectedTab());
            if (recipeIdx >= 0 && recipeIdx < recipes.size()) {
                blockEntity.setSelectedRecipe(recipeIdx);
                if (blockEntity.isCrafting()) blockEntity.cancelCrafting();
            }
            return true;
        } else if (id == 200) {
            if (!blockEntity.isCrafting()) {
                blockEntity.startCrafting();
            }
            return true;
        }
        return false;
    }

    // ===== Shift-click =====

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 8) {
                // 合成台 → 背包
                if (!moveItemStackTo(stack, 8, 44, true)) return ItemStack.EMPTY;
            } else {
                // 背包 → 原料槽 (1-6)
                if (!moveItemStackTo(stack, 1, 7, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    // ===== 关闭时归还物品 =====

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && blockEntity != null) {
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
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player,
                level.getBlockState(blockEntity.getBlockPos()).getBlock());
    }
}
