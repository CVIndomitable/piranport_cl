package com.piranport.block.entity;

import com.piranport.component.LoadedAmmo;
import com.piranport.item.MissileItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.TorpedoItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.menu.ReloadFacilityMenu;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 装填设施方块实体 — 将鱼雷装入鱼雷发射器。
 * 槽位0: 鱼雷发射器（原料槽1，顶面漏斗输入）
 * 槽位1: 鱼雷弹药（原料槽2，侧面漏斗输入）
 * 槽位2: 装填完毕的发射器（产品槽，底部漏斗输出）
 * 装填时间: 200 ticks (10秒)
 */
public class ReloadFacilityBlockEntity extends BlockEntity implements MenuProvider {
    public static final int LAUNCHER_SLOT = 0;
    public static final int AMMO_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int TOTAL_SLOTS = 3;
    public static final int RELOAD_TIME = 200; // 10 seconds

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case LAUNCHER_SLOT -> stack.getItem() instanceof TorpedoLauncherItem
                        || (stack.getItem() instanceof MissileLauncherItem ml && ml.isManualReload());
                case AMMO_SLOT -> stack.getItem() instanceof TorpedoItem
                        || stack.getItem() instanceof MissileItem;
                case OUTPUT_SLOT -> false; // output only
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            // Launchers stack to 1, torpedoes stack to 16, output stack to 1
            return slot == AMMO_SLOT ? 64 : 1;
        }
    };

    // Top hopper → slot 0 (launcher) only
    private final IItemHandler topHandler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(LAUNCHER_SLOT); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!(stack.getItem() instanceof TorpedoLauncherItem)
                    && !(stack.getItem() instanceof MissileLauncherItem ml && ml.isManualReload())) return stack;
            return itemHandler.insertItem(LAUNCHER_SLOT, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof TorpedoLauncherItem
                    || (stack.getItem() instanceof MissileLauncherItem ml && ml.isManualReload());
        }
    };

    // Side hopper → slot 1 (ammo) only
    private final IItemHandler sideHandler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(AMMO_SLOT); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!(stack.getItem() instanceof TorpedoItem) && !(stack.getItem() instanceof MissileItem)) return stack;
            return itemHandler.insertItem(AMMO_SLOT, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof TorpedoItem || stack.getItem() instanceof MissileItem;
        }
    };

    // Bottom hopper → extract from slot 2 (output) only
    private final IItemHandler bottomHandler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(OUTPUT_SLOT); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return itemHandler.extractItem(OUTPUT_SLOT, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    };

    int reloadProgress = 0;
    int reloadTotalTime = 0;

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> reloadProgress;
                case 1 -> reloadTotalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> reloadProgress = value;
                case 1 -> reloadTotalTime = value;
            }
        }

        @Override
        public int getCount() { return 2; }
    };

    public ReloadFacilityBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.RELOAD_FACILITY.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }

    /** Direction-aware handler: DOWN=output, UP=launcher input, sides=ammo input. */
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == Direction.DOWN) return bottomHandler;
        if (side == Direction.UP) return topHandler;
        return sideHandler;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ReloadFacilityBlockEntity be) {
        ItemStack launcherStack = be.itemHandler.getStackInSlot(LAUNCHER_SLOT);
        ItemStack ammoStack = be.itemHandler.getStackInSlot(AMMO_SLOT);
        ItemStack outputStack = be.itemHandler.getStackInSlot(OUTPUT_SLOT);

        // Output must be empty (launchers don't stack)
        if (!outputStack.isEmpty()) {
            be.resetProgress();
            return;
        }

        // Determine reload parameters based on launcher type
        int maxLoad;
        int needed;
        boolean validCombo;

        // 部分装填补齐时，必须与现有弹种一致；不同弹种禁止混装，避免旧弹被静默替换
        String ammoIdNow = ammoStack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(ammoStack.getItem()).toString();

        if (launcherStack.getItem() instanceof TorpedoLauncherItem torpedoLauncher) {
            // Torpedo launcher reload
            LoadedAmmo existing = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            maxLoad = torpedoLauncher.getTubeCount();
            if (existing.hasAmmo() && existing.count() >= maxLoad) {
                be.resetProgress();
                return;
            }
            if (existing.hasAmmo() && !existing.ammoItemId().equals(ammoIdNow)) {
                be.resetProgress();
                return;
            }
            needed = maxLoad - (existing.hasAmmo() ? existing.count() : 0);
            validCombo = ammoStack.getItem() instanceof TorpedoItem torpedo
                    && torpedo.getCaliber() == torpedoLauncher.getCaliber();
        } else if (launcherStack.getItem() instanceof MissileLauncherItem missileLauncher
                && missileLauncher.isManualReload()) {
            // Missile launcher reload (anti-ship / rocket)
            LoadedAmmo existing = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            maxLoad = missileLauncher.getBurstCount();
            if (existing.hasAmmo() && existing.count() >= maxLoad) {
                be.resetProgress();
                return;
            }
            if (existing.hasAmmo() && !existing.ammoItemId().equals(ammoIdNow)) {
                be.resetProgress();
                return;
            }
            needed = maxLoad - (existing.hasAmmo() ? existing.count() : 0);
            validCombo = ammoStack.is(missileLauncher.getAmmoItem());
        } else {
            be.resetProgress();
            return;
        }

        if (!validCombo || ammoStack.getCount() < needed) {
            be.resetProgress();
            return;
        }

        // Start/continue processing
        if (be.reloadTotalTime == 0) {
            be.reloadTotalTime = RELOAD_TIME;
            be.reloadProgress = 0;
        }

        be.reloadProgress++;
        if (be.reloadProgress >= be.reloadTotalTime) {
            // Complete: produce loaded launcher
            String ammoId = BuiltInRegistries.ITEM.getKey(ammoStack.getItem()).toString();
            ItemStack result = launcherStack.copy();
            result.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(maxLoad, ammoId));

            be.itemHandler.setStackInSlot(OUTPUT_SLOT, result);
            be.itemHandler.setStackInSlot(LAUNCHER_SLOT, ItemStack.EMPTY);
            ammoStack.shrink(needed);

            be.reloadProgress = 0;
            be.reloadTotalTime = 0;
        }
        be.setChanged();
    }

    private void resetProgress() {
        if (reloadProgress > 0 || reloadTotalTime > 0) {
            reloadProgress = 0;
            reloadTotalTime = 0;
            setChanged();
        }
    }

    /** Comparator output: 15 when output slot has item, 0 otherwise. */
    public int getComparatorOutput() {
        return itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() ? 0 : 15;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("reloadProgress", reloadProgress);
        tag.putInt("reloadTotalTime", reloadTotalTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        reloadProgress = tag.getInt("reloadProgress");
        reloadTotalTime = tag.getInt("reloadTotalTime");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.piranport.reload_facility");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReloadFacilityMenu(containerId, playerInventory, this);
    }

    public void writeScreenOpeningData(ServerPlayer player, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeBlockPos(this.worldPosition);
    }
}
