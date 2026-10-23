package com.piranport.block.entity;

import com.piranport.ammo.CreativeAmmoCache;
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
import net.minecraft.world.item.Item;
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
 * 槽位1: 鱼雷/导弹弹药（原料槽2，侧面漏斗输入）
 * 槽位2: 备用弹药（原料槽3，策划 4 格）；槽位1/2 也可放创造弹药库，
 *        设施读取其勾选弹种并且不消耗弹药库
 * 槽位3: 装填完毕的发射器（产品槽，底部漏斗输出）
 * 装填时间: 6 秒 (120 tick) — 策划决策/武器/鱼雷-再装填机制 2026-10-19 修订
 */
public class ReloadFacilityBlockEntity extends BlockEntity implements MenuProvider {
    public static final int LAUNCHER_SLOT = 0;
    public static final int AMMO_SLOT = 1;
    public static final int EXTRA_AMMO_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int TOTAL_SLOTS = 4;

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
                        || stack.getItem() instanceof MissileItem
                        || CreativeAmmoCache.isCacheStack(stack);
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
            if (!(stack.getItem() instanceof TorpedoItem) && !(stack.getItem() instanceof MissileItem)
                    && !CreativeAmmoCache.isCacheStack(stack)) return stack;
            int targetSlot = slot == 0 ? AMMO_SLOT : EXTRA_AMMO_SLOT;
            return itemHandler.insertItem(targetSlot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof TorpedoItem || stack.getItem() instanceof MissileItem
                    || CreativeAmmoCache.isCacheStack(stack);
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
        // 创造弹药库无限弹药：把弹药库物品放进任一弹药槽，设施直接读取它勾选的弹种，
        // 既不要求实体弹药也不消耗。弹药库本身留在槽里，可以反复使用。
        String creativeAmmoId = null;

        ItemStack inputAmmo1 = be.itemHandler.getStackInSlot(AMMO_SLOT);
        ItemStack inputAmmo2 = be.itemHandler.getStackInSlot(EXTRA_AMMO_SLOT);
        // 只统计真实弹药数量；创造弹药库不是弹药堆，不能拿来凑数量。
        int totalAmmoCount = physicalAmmoCount(inputAmmo1) + physicalAmmoCount(inputAmmo2);

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
            creativeAmmoId = creativeTorpedoAmmoId(inputAmmo1, inputAmmo2,
                    torpedoLauncher.getCaliber());
            boolean ammo1Valid = !inputAmmo1.isEmpty() && inputAmmo1.getItem() instanceof TorpedoItem t1
                    && t1.getCaliber() == torpedoLauncher.getCaliber();
            boolean ammo2Valid = !inputAmmo2.isEmpty() && inputAmmo2.getItem() instanceof TorpedoItem t2
                    && t2.getCaliber() == torpedoLauncher.getCaliber();
            // 两个真实弹药槽都非空时必须同种；创造弹药库槽不参与混填判断。
            validCombo = creativeAmmoId != null
                    || ((ammo1Valid || ammo2Valid) && samePhysicalAmmo(inputAmmo1, inputAmmo2));
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
            Item ammoItem = missileLauncher.getAmmoItem();
            creativeAmmoId = creativeMissileAmmoId(inputAmmo1, inputAmmo2, ammoItem);
            boolean ammo1Valid = !inputAmmo1.isEmpty() && inputAmmo1.is(ammoItem);
            boolean ammo2Valid = !inputAmmo2.isEmpty() && inputAmmo2.is(ammoItem);
            // 同上：两个真实弹药槽都非空时必须同种；创造弹药库槽不参与。
            validCombo = creativeAmmoId != null
                    || ((ammo1Valid || ammo2Valid) && samePhysicalAmmo(inputAmmo1, inputAmmo2));
        } else {
            be.resetProgress();
            return;
        }

        // 创造弹药库命中时不要求实体弹药数量；否则按槽内实际数量检查。
        if (!validCombo || (creativeAmmoId == null && totalAmmoCount < needed)) {
            be.resetProgress();
            return;
        }

        // Start/continue processing
        if (be.reloadTotalTime == 0) {
            // 装填节奏进调试终端（global.reload_facility.reload_time，按秒显示）；
            // 水上/陆地共用同一值，避免同一种设备有两种隐藏节奏。
            be.reloadTotalTime = modEquipmentReloadTime();
            be.reloadProgress = 0;
        }

        be.reloadProgress++;
        if (be.reloadProgress >= be.reloadTotalTime) {
            // Complete: produce loaded launcher.
            // 创造弹药库命中时用勾选的弹种写入，且不扣除任何实体弹药；
            // 否则优先消耗 AMMO_SLOT，不够时再消耗 EXTRA_AMMO_SLOT。
            String ammoId;
            if (creativeAmmoId != null) {
                ammoId = creativeAmmoId;
            } else {
                // 槽里可能同时放着创造弹药库：只从真实弹药堆里选弹种。
                ItemStack ammoSource = physicalAmmoStack(inputAmmo1)
                        ? inputAmmo1 : inputAmmo2;
                ammoId = BuiltInRegistries.ITEM.getKey(ammoSource.getItem()).toString();
            }
            ItemStack result = launcherStack.copy();
            result.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(maxLoad, ammoId));

            be.itemHandler.setStackInSlot(OUTPUT_SLOT, result);
            be.itemHandler.setStackInSlot(LAUNCHER_SLOT, ItemStack.EMPTY);

            if (creativeAmmoId == null) {
                int remaining = needed;
                ItemStack a1 = be.itemHandler.getStackInSlot(AMMO_SLOT);
                if (physicalAmmoStack(a1)) {
                    int take = Math.min(remaining, a1.getCount());
                    be.itemHandler.setStackInSlot(AMMO_SLOT, a1.copyWithCount(a1.getCount() - take));
                    remaining -= take;
                }
                if (remaining > 0) {
                    ItemStack a2 = be.itemHandler.getStackInSlot(EXTRA_AMMO_SLOT);
                    if (physicalAmmoStack(a2)) {
                        int take = Math.min(remaining, a2.getCount());
                        be.itemHandler.setStackInSlot(EXTRA_AMMO_SLOT, a2.copyWithCount(a2.getCount() - take));
                        remaining -= take;
                    }
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

    private static int modEquipmentReloadTime() {
        return com.piranport.config.ModEquipmentConfig.RELOAD_FACILITY_TIME_TICKS.get();
    }

    /** 是否为真实弹药堆（不是创造弹药库）。 */
    private static boolean physicalAmmoStack(ItemStack stack) {
        return !stack.isEmpty() && !CreativeAmmoCache.isCacheStack(stack)
                && (stack.getItem() instanceof TorpedoItem || stack.getItem() instanceof MissileItem);
    }

    /** 只统计真实弹药堆；创造弹药库物品不是弹药，不能用来凑装填数量。 */
    private static int physicalAmmoCount(ItemStack stack) {
        return physicalAmmoStack(stack) ? stack.getCount() : 0;
    }

    /**
     * 两个真实弹药槽都放弹时必须是同种弹药；任一槽放的是创造弹药库则跳过混填检查，
     * 因为弹药库不是实际弹药，真正弹种由它勾选的条目决定。
     */
    private static boolean samePhysicalAmmo(ItemStack first, ItemStack second) {
        boolean firstReal = !first.isEmpty() && !CreativeAmmoCache.isCacheStack(first);
        boolean secondReal = !second.isEmpty() && !CreativeAmmoCache.isCacheStack(second);
        return !(firstReal && secondReal) || ItemStack.isSameItem(first, second);
    }

    /** 弹药槽里的创造弹药库勾选了同口径鱼雷时返回其物品 id；用于免消耗装填。 */
    @Nullable
    private static String creativeTorpedoAmmoId(ItemStack first, ItemStack second, int caliber) {
        for (ItemStack stack : new ItemStack[]{first, second}) {
            TorpedoItem cached = CreativeAmmoCache.findSelectedTorpedo(stack, caliber);
            if (cached != null) return BuiltInRegistries.ITEM.getKey(cached).toString();
        }
        return null;
    }

    /** 弹药槽里的创造弹药库勾选了该发射器使用的导弹/火箭弹时返回其物品 id。 */
    @Nullable
    private static String creativeMissileAmmoId(ItemStack first, ItemStack second, Item ammoItem) {
        for (ItemStack stack : new ItemStack[]{first, second}) {
            Item selected = CreativeAmmoCache.findSelected(stack, item -> item == ammoItem);
            if (selected != null) return BuiltInRegistries.ITEM.getKey(selected).toString();
        }
        return null;
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
