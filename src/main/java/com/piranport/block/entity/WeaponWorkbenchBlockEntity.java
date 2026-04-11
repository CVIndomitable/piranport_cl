package com.piranport.block.entity;

import com.piranport.crafting.WeaponWorkbenchRecipe;
import com.piranport.crafting.WeaponWorkbenchRecipeRegistry;
import com.piranport.menu.WeaponWorkbenchMenu;
import com.piranport.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 武器合成台方块实体。
 * 不持久化物品（"不需要缓存"）：关闭GUI时物品归还玩家。
 */
public class WeaponWorkbenchBlockEntity extends BlockEntity implements MenuProvider {
    public static final int BLUEPRINT_SLOT = 0;
    public static final int MATERIAL_START = 1;
    public static final int MATERIAL_END = 6;
    public static final int OUTPUT_SLOT = 7;
    public static final int TOTAL_SLOTS = 8;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int selectedTab = 0;
    private int selectedRecipe = 0;
    private int craftingProgress = 0;
    private int craftingTotalTime = 0;
    private boolean isCrafting = false;

    public int getSelectedTab() { return selectedTab; }
    public void setSelectedTab(int v) { selectedTab = v; }
    public int getSelectedRecipe() { return selectedRecipe; }
    public void setSelectedRecipe(int v) { selectedRecipe = v; }

    @Nullable
    private UUID currentUser;

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> selectedTab;
                case 1 -> selectedRecipe;
                case 2 -> craftingProgress;
                case 3 -> craftingTotalTime;
                case 4 -> isCrafting ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> selectedTab = value;
                case 1 -> selectedRecipe = value;
                case 2 -> craftingProgress = value;
                case 3 -> craftingTotalTime = value;
                case 4 -> isCrafting = value != 0;
            }
        }

        @Override
        public int getCount() { return 5; }
    };

    public WeaponWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.WEAPON_WORKBENCH.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }

    public boolean isCrafting() { return isCrafting; }

    // ===== 多人互斥 =====

    public boolean tryOpen(Player player) {
        if (currentUser != null) {
            if (level != null) {
                Player existing = level.getPlayerByUUID(currentUser);
                if (existing != null && existing.isAlive()
                        && existing.containerMenu instanceof WeaponWorkbenchMenu) {
                    return false;
                }
            }
            // Clear stale user reference
            currentUser = null;
        }
        currentUser = player.getUUID();
        return true;
    }

    public void setCurrentUser(@Nullable UUID uuid) {
        this.currentUser = uuid;
    }

    // ===== 合成逻辑 =====

    public void cancelCrafting() {
        isCrafting = false;
        craftingProgress = 0;
        craftingTotalTime = 0;
    }

    public boolean startCrafting() {
        WeaponWorkbenchRecipe recipe = WeaponWorkbenchRecipeRegistry.getRecipe(selectedTab, selectedRecipe);
        if (recipe == null) return false;
        if (!canCraft(recipe)) return false;

        isCrafting = true;
        craftingProgress = 0;
        craftingTotalTime = recipe.craftingTime();
        setChanged();
        return true;
    }

    public boolean canCraft(WeaponWorkbenchRecipe recipe) {
        // 蓝图检查
        if (recipe.requiredBlueprint() != null) {
            ItemStack bp = itemHandler.getStackInSlot(BLUEPRINT_SLOT);
            if (bp.isEmpty() || !bp.is(recipe.requiredBlueprint())) {
                return false;
            }
        }
        // 原料检查
        for (ItemStack required : recipe.materials()) {
            int needed = required.getCount();
            for (int i = MATERIAL_START; i <= MATERIAL_END; i++) {
                ItemStack inSlot = itemHandler.getStackInSlot(i);
                if (inSlot.is(required.getItem())) {
                    needed -= inSlot.getCount();
                }
            }
            if (needed > 0) return false;
        }
        // 产物格检查
        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
        ItemStack result = recipe.getResultStack();
        if (!output.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(output, result)) return false;
            if (output.getCount() + result.getCount() > output.getMaxStackSize()) return false;
        }
        return true;
    }

    private void consumeMaterials(WeaponWorkbenchRecipe recipe) {
        for (ItemStack required : recipe.materials()) {
            int toConsume = required.getCount();
            for (int i = MATERIAL_START; i <= MATERIAL_END && toConsume > 0; i++) {
                ItemStack inSlot = itemHandler.getStackInSlot(i);
                if (inSlot.is(required.getItem())) {
                    int consume = Math.min(toConsume, inSlot.getCount());
                    itemHandler.setStackInSlot(i, inSlot.copyWithCount(inSlot.getCount() - consume));
                    toConsume -= consume;
                }
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  WeaponWorkbenchBlockEntity be) {
        if (!be.isCrafting) return;

        be.craftingProgress++;
        if (be.craftingProgress >= be.craftingTotalTime) {
            WeaponWorkbenchRecipe recipe =
                    WeaponWorkbenchRecipeRegistry.getRecipe(be.selectedTab, be.selectedRecipe);
            if (recipe != null) {
                // Check materials again before completion (in case they were removed)
                if (be.canCraft(recipe)) {
                    be.consumeMaterials(recipe);
                    ItemStack result = recipe.getResultStack();
                    ItemStack current = be.itemHandler.getStackInSlot(OUTPUT_SLOT);
                    if (current.isEmpty()) {
                        be.itemHandler.setStackInSlot(OUTPUT_SLOT, result);
                    } else {
                        be.itemHandler.setStackInSlot(OUTPUT_SLOT,
                                current.copyWithCount(current.getCount() + result.getCount()));
                    }
                } else {
                    // Materials insufficient — cancel crafting instead of waiting
                    be.cancelCrafting();
                    be.setChanged();
                    return;
                }
            }
            be.isCrafting = false;
            be.craftingProgress = 0;
            be.craftingTotalTime = 0;
        }
        be.setChanged();
    }

    // ===== Persistence =====

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("selectedTab", selectedTab);
        tag.putInt("selectedRecipe", selectedRecipe);
        tag.putInt("craftingProgress", craftingProgress);
        tag.putInt("craftingTotalTime", craftingTotalTime);
        tag.putBoolean("isCrafting", isCrafting);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        selectedTab = tag.getInt("selectedTab");
        selectedRecipe = tag.getInt("selectedRecipe");
        craftingProgress = tag.getInt("craftingProgress");
        craftingTotalTime = tag.getInt("craftingTotalTime");
        isCrafting = tag.getBoolean("isCrafting");
    }

    // ===== MenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.piranport.weapon_workbench");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WeaponWorkbenchMenu(containerId, playerInventory, this);
    }

    public void writeScreenOpeningData(ServerPlayer player, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeBlockPos(this.worldPosition);
    }
}
