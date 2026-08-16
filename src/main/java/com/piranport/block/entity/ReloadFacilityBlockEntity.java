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
import net.minecraft.world.level.material.Fluids;
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
 * 槽位2: 备用弹药（原料槽3，策划 4 格）
 * 槽位3: 装填完毕的发射器（产品槽，底部漏斗输出）
 * 装填时间: 10 秒 (200 tick) — 策划要求
 */
public class ReloadFacilityBlockEntity extends BlockEntity implements MenuProvider {
    public static final int LAUNCHER_SLOT = 0;
    public static final int AMMO_SLOT = 1;
    public static final int EXTRA_AMMO_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int TOTAL_SLOTS = 4;
    public static final int RELOAD_TIME_WATER = 200;  // 10 seconds
    public static final int RELOAD_TIME_LAND = 200;   // 10 seconds
    public static final int RELOAD_TIME_DEFAULT = 200; // 10 seconds (fallback)

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case LAUNCHER_SLOT -> stack.getItem() instanceof TorpedoLauncherItem
                        || (stack.getItem() instanceof MissileLauncherItem ml && ml.canReloadInFacility());
                case AMMO_SLOT, EXTRA_AMMO_SLOT -> stack.getItem() instanceof TorpedoItem
                        || stack.getItem() instanceof MissileItem;
                case OUTPUT_SLOT -> false; // output only
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            // Launchers stack to 1, torpedoes stack to 64, output stack to 1
            return slot == AMMO_SLOT || slot == EXTRA_AMMO_SLOT ? 64 : 1;
        }
    };

    // Top hopper → slot 0 (launcher) only
    private final IItemHandler topHandler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(LAUNCHER_SLOT); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!(stack.getItem() instanceof TorpedoLauncherItem)
                    && !(stack.getItem() instanceof MissileLauncherItem ml && ml.canReloadInFacility())) return stack;
            return itemHandler.insertItem(LAUNCHER_SLOT, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof TorpedoLauncherItem
                    || (stack.getItem() instanceof MissileLauncherItem ml && ml.canReloadInFacility());
        }
    };

    // Side hopper → slots 1-2 (ammo + extra ammo) only
    private final IItemHandler sideHandler = new IItemHandler() {
        @Override public int getSlots() { return 2; }
        @Override public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? itemHandler.getStackInSlot(AMMO_SLOT) : itemHandler.getStackInSlot(EXTRA_AMMO_SLOT);
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!(stack.getItem() instanceof TorpedoItem) && !(stack.getItem() instanceof MissileItem)) return stack;
            int targetSlot = slot == 0 ? AMMO_SLOT : EXTRA_AMMO_SLOT;
            return itemHandler.insertItem(targetSlot, stack, simulate);
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

        // 合并两个弹药槽数量
        int totalAmmoCount = be.itemHandler.getStackInSlot(AMMO_SLOT).getCount()
                + be.itemHandler.getStackInSlot(EXTRA_AMMO_SLOT).getCount();

        if (launcherStack.getItem() instanceof TorpedoLauncherItem torpedoLauncher) {
            // Torpedo launcher reload
            LoadedAmmo existing = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            maxLoad = torpedoLauncher.getTubeCount();

            // 已满：拒绝
            if (existing.hasAmmo() && existing.count() >= maxLoad) {
                be.resetProgress();
                return;
            }

            // 部分装填：拒绝（新增检查）
            if (existing.hasAmmo() && existing.count() > 0) {
                be.resetProgress();
                return;
            }

            // 空载：要求完全装填
            needed = maxLoad;
            ItemStack ammo1 = be.itemHandler.getStackInSlot(AMMO_SLOT);
            ItemStack ammo2 = be.itemHandler.getStackInSlot(EXTRA_AMMO_SLOT);
            boolean ammo1Valid = !ammo1.isEmpty() && ammo1.getItem() instanceof TorpedoItem t1
                    && t1.getCaliber() == torpedoLauncher.getCaliber();
            boolean ammo2Valid = !ammo2.isEmpty() && ammo2.getItem() instanceof TorpedoItem t2
                    && t2.getCaliber() == torpedoLauncher.getCaliber();
            validCombo = (ammo1Valid || ammo2Valid);
        } else if (launcherStack.getItem() instanceof MissileLauncherItem missileLauncher
                && missileLauncher.canReloadInFacility()) {
            // Missile launcher reload (anti-ship / rocket); anti-air 发射器自动装填，禁止走这里
            LoadedAmmo existing = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            maxLoad = missileLauncher.getBurstCount();

            // 已满：拒绝
            if (existing.hasAmmo() && existing.count() >= maxLoad) {
                be.resetProgress();
                return;
            }

            // 部分装填：拒绝（新增检查）
            if (existing.hasAmmo() && existing.count() > 0) {
                be.resetProgress();
                return;
            }

            // 空载：要求完全装填
            needed = maxLoad;
            ItemStack ammo1 = be.itemHandler.getStackInSlot(AMMO_SLOT);
            ItemStack ammo2 = be.itemHandler.getStackInSlot(EXTRA_AMMO_SLOT);
            boolean ammo1Valid = !ammo1.isEmpty() && ammo1.is(missileLauncher.getAmmoItem());
            boolean ammo2Valid = !ammo2.isEmpty() && ammo2.is(missileLauncher.getAmmoItem());
            validCombo = (ammo1Valid || ammo2Valid);
        } else {
            be.resetProgress();
            return;
        }

        if (!validCombo || totalAmmoCount < needed) {
            be.resetProgress();
            return;
        }

        // Start/continue processing
        if (be.reloadTotalTime == 0) {
            boolean inWater = be.isInWater();
            be.reloadTotalTime = inWater ? RELOAD_TIME_WATER : RELOAD_TIME_LAND;
            be.reloadProgress = 0;
        }

        be.reloadProgress++;
        if (be.reloadProgress >= be.reloadTotalTime) {
            // Complete: produce loaded launcher. Prefer AMMO_SLOT first, then EXTRA_AMMO_SLOT.
            ItemStack ammoSource = be.itemHandler.getStackInSlot(AMMO_SLOT).isEmpty()
                    ? be.itemHandler.getStackInSlot(EXTRA_AMMO_SLOT)
                    : be.itemHandler.getStackInSlot(AMMO_SLOT);
            String ammoId = BuiltInRegistries.ITEM.getKey(ammoSource.getItem()).toString();
            ItemStack result = launcherStack.copy();
            result.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(maxLoad, ammoId));

            be.itemHandler.setStackInSlot(OUTPUT_SLOT, result);
            be.itemHandler.setStackInSlot(LAUNCHER_SLOT, ItemStack.EMPTY);

            // 优先消耗 AMMO_SLOT，不够时再消耗 EXTRA_AMMO_SLOT
            int remaining = needed;
            ItemStack a1 = be.itemHandler.getStackInSlot(AMMO_SLOT);
            if (!a1.isEmpty()) {
                int take = Math.min(remaining, a1.getCount());
                be.itemHandler.setStackInSlot(AMMO_SLOT, a1.copyWithCount(a1.getCount() - take));
                remaining -= take;
            }
            if (remaining > 0) {
                ItemStack a2 = be.itemHandler.getStackInSlot(EXTRA_AMMO_SLOT);
                if (!a2.isEmpty()) {
                    int take = Math.min(remaining, a2.getCount());
                    be.itemHandler.setStackInSlot(EXTRA_AMMO_SLOT, a2.copyWithCount(a2.getCount() - take));
                    remaining -= take;
                }
            }

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

    private boolean isInWater() {
        if (level == null) return false;
        return level.getFluidState(worldPosition).is(Fluids.WATER);
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
