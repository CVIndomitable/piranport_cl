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

    /** 标签页 0=查看（蓝图/Paper） 1=复印（CopySlot）。由客户端 Screen 点击切换并同步。 */
    private int currentTab = 0;

    private final BlueprintChestBlockEntity blockEntity;

    /**
     * 指定菜单槽位在当前标签页下是否可见/可交互。
     * 0–8 蓝图槽与 9 纸槽只在「查看」页；10–18 复印槽只在「复印」页（与蓝图槽坐标重叠）；
     * 玩家背包 19–54 始终可见。抽成静态函数，便于在不加载客户端 Screen 的测试里钉死映射。
     */
    public static boolean isSlotVisibleOnTab(int slotIndex, int tab) {
        if (slotIndex < BlueprintChestBlockEntity.TOTAL_SLOTS) return tab == 0;
        if (slotIndex < PLAYER_INV_START) return tab == 1;
        return true;
    }

    private class BlueprintSlot extends SlotItemHandler {
        /** 对应的 ItemStackHandler 下标（同时等于菜单下标 0–8）。 */
        private final int handlerIndex;

        public BlueprintSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
            this.handlerIndex = index;
        }

        /**
         * 同种蓝图最多一格（策划决策 资源/01）。
         *
         * <p>原版所有「把物品放进槽里」的 GUI 路径都先问 mayPlace：左键 safeInsert、
         * 数字键/中键 SWAP、拖拽分配（QUICK_CRAFT）、shift 快速移动（moveItemStackTo）。
         * 在这一层拒绝，物品会原样留在玩家手里；若改去拦 setByPlayer，safeInsert 已经
         * split 过栈，拒绝就会直接吞掉物品。</p>
         */
        @Override
        public boolean mayPlace(ItemStack stack) {
            return BlueprintChestBlockEntity.isBlueprint(stack)
                    && !blockEntity.hasBlueprintElsewhere(stack, handlerIndex);
        }

        @Override
        public int getMaxStackSize() { return 1; }

        @Override
        public boolean isActive() {
            return isSlotVisibleOnTab(handlerIndex, BlueprintChestMenu.this.getCurrentTab());
        }
    }

    private class PaperSlot extends SlotItemHandler {
        public PaperSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) { return stack.is(Items.PAPER); }

        @Override
        public boolean isActive() {
            return isSlotVisibleOnTab(BlueprintChestBlockEntity.PAPER_SLOT,
                    BlueprintChestMenu.this.getCurrentTab());
        }
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

        @Override
        public boolean isActive() {
            return isSlotVisibleOnTab(COPY_SLOT_START + blueprintIndex,
                    BlueprintChestMenu.this.getCurrentTab());
        }
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
            addSlot(new BlueprintSlot(handler, col, 8 + col * 18, 34));
        }

        addSlot(new PaperSlot(handler, BlueprintChestBlockEntity.PAPER_SLOT, 80, 67));

        for (int col = 0; col < BlueprintChestBlockEntity.BLUEPRINT_SLOTS; col++) {
            addSlot(new CopySlot(col, 8 + col * 18, 34));
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

    public int getCurrentTab() { return currentTab; }

    public void setCurrentTab(int tab) {
        if (tab != 0 && tab != 1) return;
        this.currentTab = tab;
    }

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
            blockEntity.consumePaperForCopy();
            // 一次 shift 点击只复印一份（策划决策 资源/01：一张纸一份，没有批量复印）。
            // 原版 doClick 的 QUICK_MOVE 分支是
            //   while (!moved.isEmpty() && isSameItem(slot.getItem(), moved)) moved = quickMoveStack(...);
            // 这里返回空栈让这个循环第一次就结束；否则复印槽的投影 getItem() 永远非空，
            // 会一直复印到纸用完或背包塞满。再次点击才会再复印一份。
            // 复印槽是投影槽，remove()/set() 都不经过容器，返回空栈不会留下「物品没移走」的状态。
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
