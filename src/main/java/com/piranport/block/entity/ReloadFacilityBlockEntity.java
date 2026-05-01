package com.piranport.block.entity;

import com.piranport.item.MissileItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.TorpedoItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.menu.ReloadFacilityMenu;
import com.piranport.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReloadFacilityBlockEntity extends BlockEntity implements MenuProvider {
    public static final int LAUNCHER_SLOT = 0;
    public static final int AMMO_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int TOTAL_SLOTS = 3;
    public static final int RELOAD_TIME = 200;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case LAUNCHER_SLOT -> stack.getItem() instanceof TorpedoLauncherItem
                        || (stack.getItem() instanceof MissileLauncherItem ml && ml.canReloadInFacility());
                case AMMO_SLOT -> stack.getItem() instanceof TorpedoItem
                        || stack.getItem() instanceof MissileItem;
                case OUTPUT_SLOT -> false;
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == AMMO_SLOT ? 64 : 1;
        }
    };

    private final IItemHandler topHandler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(LAUNCHER_SLOT); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!(stack.getItem() instanceof TorpedoLauncherItem)
                    && !(stack.getItem() instanceof MissileLauncherItem ml && ml.canReloadInFacility())) return stack;
            return itemHandler.insertItem(LAUNCHER_SLOT, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof TorpedoLauncherItem
                    || (stack.getItem() instanceof MissileLauncherItem ml && ml.canReloadInFacility());
        }
    };

    private final IItemHandler sideHandler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(AMMO_SLOT); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!(stack.getItem() instanceof TorpedoItem) && !(stack.getItem() instanceof MissileItem)) return stack;
            return itemHandler.insertItem(AMMO_SLOT, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof TorpedoItem || stack.getItem() instanceof MissileItem;
        }
    };

    private final IItemHandler bottomHandler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(OUTPUT_SLOT); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return stack; }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return itemHandler.extractItem(OUTPUT_SLOT, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyTopHandler = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazySideHandler = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyBottomHandler = LazyOptional.empty();

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

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyTopHandler = LazyOptional.of(() -> topHandler);
        lazySideHandler = LazyOptional.of(() -> sideHandler);
        lazyBottomHandler = LazyOptional.of(() -> bottomHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyTopHandler.invalidate();
        lazySideHandler.invalidate();
        lazyBottomHandler.invalidate();
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == Direction.DOWN) return lazyBottomHandler.cast();
            if (side == Direction.UP) return lazyTopHandler.cast();
            if (side != null) return lazySideHandler.cast();
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ReloadFacilityBlockEntity be) {
        ItemStack launcherStack = be.itemHandler.getStackInSlot(LAUNCHER_SLOT);
        ItemStack ammoStack = be.itemHandler.getStackInSlot(AMMO_SLOT);
        ItemStack outputStack = be.itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (!outputStack.isEmpty()) {
            be.resetProgress();
            return;
        }

        int maxLoad;
        int needed;
        boolean validCombo;

        CompoundTag launcherTag = launcherStack.getOrCreateTag();
        String ammoIdNow = ammoStack.isEmpty() ? "" : net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(ammoStack.getItem()).toString();

        if (launcherStack.getItem() instanceof TorpedoLauncherItem torpedoLauncher) {
            maxLoad = torpedoLauncher.getTubeCount();
            int currentLoad = launcherTag.getInt("LoadedCount");
            String existingAmmo = launcherTag.getString("LoadedAmmo");

            if (currentLoad >= maxLoad) {
                be.resetProgress();
                return;
            }
            if (currentLoad > 0 && !existingAmmo.equals(ammoIdNow)) {
                be.resetProgress();
                return;
            }
            needed = maxLoad - currentLoad;
            validCombo = ammoStack.getItem() instanceof TorpedoItem torpedo
                    && torpedo.getCaliber() == torpedoLauncher.getCaliber();
        } else if (launcherStack.getItem() instanceof MissileLauncherItem missileLauncher
                && missileLauncher.canReloadInFacility()) {
            maxLoad = missileLauncher.getBurstCount();
            int currentLoad = launcherTag.getInt("LoadedCount");
            String existingAmmo = launcherTag.getString("LoadedAmmo");

            if (currentLoad >= maxLoad) {
                be.resetProgress();
                return;
            }
            if (currentLoad > 0 && !existingAmmo.equals(ammoIdNow)) {
                be.resetProgress();
                return;
            }
            needed = maxLoad - currentLoad;
            validCombo = ammoStack.is(missileLauncher.getAmmoItem());
        } else {
            be.resetProgress();
            return;
        }

        if (!validCombo || ammoStack.getCount() < needed) {
            be.resetProgress();
            return;
        }

        if (be.reloadTotalTime == 0) {
            be.reloadTotalTime = RELOAD_TIME;
            be.reloadProgress = 0;
        }

        be.reloadProgress++;
        if (be.reloadProgress >= be.reloadTotalTime) {
            String ammoId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(ammoStack.getItem()).toString();
            ItemStack result = launcherStack.copy();
            CompoundTag resultTag = result.getOrCreateTag();
            resultTag.putInt("LoadedCount", maxLoad);
            resultTag.putString("LoadedAmmo", ammoId);

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

    public int getComparatorOutput() {
        return itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() ? 0 : 15;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("reloadProgress", reloadProgress);
        tag.putInt("reloadTotalTime", reloadTotalTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) itemHandler.deserializeNBT(tag.getCompound("inventory"));
        reloadProgress = tag.getInt("reloadProgress");
        reloadTotalTime = tag.getInt("reloadTotalTime");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.piranport.reload_facility");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReloadFacilityMenu(containerId, playerInventory, this);
    }
}
