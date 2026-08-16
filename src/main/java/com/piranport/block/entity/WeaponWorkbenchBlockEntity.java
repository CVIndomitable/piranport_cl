package com.piranport.block.entity;

import com.piranport.crafting.WeaponWorkbenchRecipe;
import com.piranport.crafting.WeaponWorkbenchRecipeRegistry;
import com.piranport.menu.WeaponWorkbenchMenu;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class WeaponWorkbenchBlockEntity extends BlockEntity implements MenuProvider {
    public static final int BLUEPRINT_SLOT = 0;
    public static final int MATERIAL_START = 1;
    public static final int MATERIAL_END = 6;
    public static final int OUTPUT_SLOT = 7;
    public static final int TOTAL_SLOTS = 8;

    private static final int DATA_SIZE = 5;
    private static final int SAVE_INTERVAL_TICKS = 20;
    private static final int TAB_COUNT = 5;
    // H4: 如果 tryOpen 锁住后玩家异常退出（断线/crash/menu 关闭事件未送达），
    // 用一个服务端 tick 超时强制释放 currentUser，避免永久锁死工作台。
    private static final int IDLE_RELEASE_TICKS = 100;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == OUTPUT_SLOT) return false;
            return super.isItemValid(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == OUTPUT_SLOT) return stack;
            if (isCrafting) return stack;
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (isCrafting && slot != OUTPUT_SLOT) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }
    };

    private int selectedTab = 0;
    private int selectedRecipe = 0;
    private int craftingProgress = 0;
    private int craftingTotalTime = 0;
    private boolean isCrafting = false;

    // transient, not persisted — used for multi-player mutex only
    @Nullable
    private UUID currentUser;
    // H4: 记录当前用户最后一次成功 tryOpen 的服务端 tick；用于 idle 超时强制释放
    private long currentUserSinceTick = 0;

    public int getSelectedTab() { return selectedTab; }

    public void setSelectedTab(int v) {
        if (v < 0 || v >= TAB_COUNT) return;
        selectedTab = v;
    }

    public int getSelectedRecipe() { return selectedRecipe; }

    public void setSelectedRecipe(int v) {
        if (v < 0) return;
        int size = WeaponWorkbenchRecipeRegistry.getRecipesForTab(selectedTab).size();
        if (v >= size) return;
        selectedRecipe = v;
    }

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
            // Server-authoritative: ignore writes on server side; accept on client (vanilla sync)
            if (level != null && !level.isClientSide) return;
            switch (index) {
                case 0 -> selectedTab = value;
                case 1 -> selectedRecipe = value;
                case 2 -> craftingProgress = value;
                case 3 -> craftingTotalTime = value;
                case 4 -> isCrafting = value != 0;
            }
        }

        @Override
        public int getCount() { return DATA_SIZE; }
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
                // H4: 三道闸门 — 1) 玩家已不存在；2) 玩家已死亡；3) 玩家容器菜单不再是工作台；
                // 4) idle 超时（断线/crash/menu close 事件未送达时的兜底）
                long gameTime = level.getGameTime();
                boolean stale = (gameTime - currentUserSinceTick) > IDLE_RELEASE_TICKS;
                if (existing != null && existing.isAlive()
                        && existing.containerMenu instanceof WeaponWorkbenchMenu
                        && !stale) {
                    return false;
                }
            }
            currentUser = null;
        }
        currentUser = player.getUUID();
        if (level != null) {
            currentUserSinceTick = level.getGameTime();
        }
        return true;
    }

    public void setCurrentUser(@Nullable UUID uuid) {
        this.currentUser = uuid;
        // H4: 显式释放时（如 Menu.removed）立即清零计时，避免下次 tryOpen 因超时立即释放
        this.currentUserSinceTick = uuid == null ? 0L : (level != null ? level.getGameTime() : 0L);
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
        ItemStack bp = itemHandler.getStackInSlot(BLUEPRINT_SLOT);
        if (bp.isEmpty()) return false;
        boolean isCreativeBp = bp.is(ModItems.CREATIVE_BLUEPRINT.get());
        if (!isCreativeBp) {
            // H3: requiredBlueprint 可能为 null（旧配方或被禁用的配方），bp.is(null) 会抛 NPE
            if (recipe.requiredBlueprint() == null) return false;
            if (!bp.is(recipe.requiredBlueprint())) return false;
        }
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
        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (!output.isEmpty()) {
            ItemStack result = recipe.getResultStack();
            if (!ItemStack.isSameItem(output, result)) return false;
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
                    inSlot.shrink(consume);
                    itemHandler.setStackInSlot(i, inSlot);
                    toConsume -= consume;
                }
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  WeaponWorkbenchBlockEntity be) {
        if (!be.isCrafting) return;

        if (be.craftingProgress < be.craftingTotalTime) {
            be.craftingProgress++;
        }

        if (be.craftingProgress >= be.craftingTotalTime) {
            WeaponWorkbenchRecipe recipe =
                    WeaponWorkbenchRecipeRegistry.getRecipe(be.selectedTab, be.selectedRecipe);
            if (recipe == null) {
                // Recipe disappeared (mod update) — safely cancel, no materials deducted yet.
                be.cancelCrafting();
            } else if (be.canCraft(recipe)) {
                be.consumeMaterials(recipe);
                ItemStack result = recipe.getResultStack();
                ItemStack current = be.itemHandler.getStackInSlot(OUTPUT_SLOT);
                if (current.isEmpty()) {
                    be.itemHandler.setStackInSlot(OUTPUT_SLOT, result);
                } else {
                    current.grow(result.getCount());
                    be.itemHandler.setStackInSlot(OUTPUT_SLOT, current);
                }
                be.isCrafting = false;
                be.craftingProgress = 0;
                be.craftingTotalTime = 0;
            }
            // else: hold at max progress until player unblocks (clears output / restores materials)
        }

        // Throttle setChanged to avoid flooding chunk-save queue every tick.
        if (be.craftingProgress == 0
                || be.craftingProgress >= be.craftingTotalTime
                || be.craftingProgress % SAVE_INTERVAL_TICKS == 0) {
            be.setChanged();
        }
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
        selectedTab = Math.max(0, Math.min(TAB_COUNT - 1, tag.getInt("selectedTab")));
        selectedRecipe = Math.max(0, tag.getInt("selectedRecipe"));
        craftingProgress = Math.max(0, tag.getInt("craftingProgress"));
        craftingTotalTime = Math.max(0, tag.getInt("craftingTotalTime"));
        isCrafting = tag.getBoolean("isCrafting");
        // currentUser is transient — ensure null after load
        currentUser = null;
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
}
