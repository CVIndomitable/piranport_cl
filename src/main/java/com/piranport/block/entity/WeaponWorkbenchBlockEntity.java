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
import net.minecraft.world.Containers;
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
            // 合成中禁止再塞料，否则合成进度算的是旧配方快照、消耗的却是新料，可越权刷出产物
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

    // 占用者。持久化到 NBT：多人互斥不能只依赖 removed() 回收 —— 异常离场（未送达 removed）
    // 或原版跨维度传送强制切换 containerMenu 时，旧实现重载后 currentUser==null，
    // 第二人 tryOpen 直接放行，原主进行中的合成料被整箱取走。
    @Nullable
    private UUID currentUser;
    // 占用者最后一次「确认持有本工作台菜单」的服务端 tick；用于 idle 超时强制释放。
    // 落盘保存，否则区块卸载重载后时间基准丢失。
    private long currentUserSinceTick = 0;
    // 时间基准来自哪个 level。本工作台无跨维度传送逻辑，跨 level 即视为过期，
    // 避免用另一维度的 gameTime 相减得出无意义的 stale 判断。
    private boolean tickBaselineValid = false;

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

    /** @return 当前占用者的 UUID；无人占用时为 null */
    @Nullable
    public UUID getCurrentUser() { return currentUser; }

    /**
     * 占用者是否仍是「活跃占用」。
     * 任一条件不满足即视为可释放（返回 false）：
     *   1) 占用者已不在本工作台所在维度（离线 / 换维度）；
     *   2) 占用者已死亡 —— 死亡会原版关菜单，此时不该继续替他锁着台子；
     *   3) 占用者的容器菜单已经不是本工作台 —— 传送/被顶替等强制切菜单的情况；
     *   4) idle 超时 —— 断线 / crash / menu close 事件未送达时的兜底，保证永不永久锁死。
     * 判定只依赖占用者自身状态 + 时间，不需要「本台子上有第二人在看」这类额外前提。
     */
    private boolean isOccupancyLive(long gameTime) {
        if (currentUser == null) return false;
        if (level == null) return false;
        Player existing = level.getPlayerByUUID(currentUser);
        if (existing == null || !existing.isAlive()) return false;
        if (!(existing.containerMenu instanceof WeaponWorkbenchMenu menu)) return false;
        // 菜单指向的必须正是本工作台：否则是「同一玩家开着别的台子」，
        // 不加这条会把另一台工作台误判为被占用。
        if (menu.getBlockEntity() != this) return false;
        return tickBaselineValid && (gameTime - currentUserSinceTick) <= IDLE_RELEASE_TICKS;
    }

    /** 释放占用（不回收物品，物品回收见 refundAllSlotsTo）。 */
    public void releaseOccupancy() {
        if (currentUser == null) return;
        currentUser = null;
        currentUserSinceTick = 0;
        tickBaselineValid = false;
        setChanged();
    }

    /**
     * 把占用者标记为本玩家。
     * 若台上已有进行中的合成却被别人占用，先把台上的料回收给原占用者，
     * 再开始新的占用 —— 既不会让新玩家拿走原主材料，也不会让工作台永久锁死。
     */
    public boolean tryOpen(Player player) {
        if (currentUser != null) {
            long gameTime = level != null ? level.getGameTime() : 0L;
            if (isOccupancyLive(gameTime) && !currentUser.equals(player.getUUID())) {
                return false;
            }
            if (currentUser.equals(player.getUUID())) {
                // 本人重复开台：只刷新时间戳，绝不回收自己台面上的料
                if (level != null) {
                    currentUserSinceTick = level.getGameTime();
                    tickBaselineValid = true;
                }
                return true;
            }
            // 占用已失效或已换人：若还留着未完成的合成，先还给原主
            if (isCrafting) reclaimOrphanedCraft();
            cancelCrafting();
            currentUser = null;
        }
        currentUser = player.getUUID();
        currentUserSinceTick = level != null ? level.getGameTime() : 0L;
        tickBaselineValid = level != null;
        setChanged();
        return true;
    }

    public void setCurrentUser(@Nullable UUID uuid) {
        if (uuid == null) {
            // 显式释放：清零计时，避免下次 tryOpen 因超时立即放行（H4）
            this.currentUser = null;
            this.currentUserSinceTick = 0L;
            this.tickBaselineValid = false;
        } else {
            this.currentUser = uuid;
            this.currentUserSinceTick = level != null ? level.getGameTime() : 0L;
            this.tickBaselineValid = level != null;
        }
        setChanged();
    }

    /**
     * 把某个槽位整体退还给原占用者。
     * 占用者离线 / 已死 / 背包塞不下时掉落在此方块位置，保证物品永不蒸发
     * （对齐 AmmoWorkbenchBlockEntity.refundPendingMaterials 的做法）。
     */
    private boolean refundSlotToOwner(int slot, @Nullable UUID owner, Player onlineOwner) {
        ItemStack stack = itemHandler.extractItem(slot, Integer.MAX_VALUE, false);
        if (stack.isEmpty()) return false;
        if (onlineOwner != null && onlineOwner.isAlive()) {
            if (!onlineOwner.addItem(stack)) onlineOwner.drop(stack, false);
        } else if (level != null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(),
                    worldPosition.getZ(), stack);
        }
        return true;
    }

    /** 占用者离线/已死时，把台上所有物品交给占用人（离线则掉落原地）。 */
    private void refundAllSlotsTo(@Nullable UUID owner) {
        Player onlineOwner = (level != null && owner != null) ? level.getPlayerByUUID(owner) : null;
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            refundSlotToOwner(i, owner, onlineOwner);
        }
    }

    /**
     * 占用已失效但台上还留着未完成的合成：把料还给原占用者并取消合成。
     * 这是「原主异常离场」时材料的可靠回收路径 —— 不依赖 Menu.removed 有没有被送达。
     */
    private void reclaimOrphanedCraft() {
        UUID owner = currentUser;
        cancelCrafting();
        refundAllSlotsTo(owner);
        if (owner != null) {
            // 顺手通知在线的原主，让他知道合成被中断了
            Player onlineOwner = level != null ? level.getPlayerByUUID(owner) : null;
            if (onlineOwner != null && onlineOwner.isAlive()) {
                onlineOwner.displayClientMessage(
                        Component.translatable("message.piranport.workbench_craft_interrupted"), false);
            }
        }
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
        // L1: idle 超时是「玩家异常离场」时唯一的保底释放途径，因此必须在无合成时也运行，
        // 不能只挂在 isCrafting 分支上 —— 否则原主在未开工状态下掉线，台子会被锁到区块卸载为止。
        long gameTime = level.getGameTime();
        if (be.currentUser != null && !be.isOccupancyLive(gameTime)) {
            // 台上还有未完成的合成时，先连料带产物一起还给原占用者，再释放
            if (be.isCrafting) be.reclaimOrphanedCraft();
            be.releaseOccupancy();
        }

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
        // 占用者连同时间基准一起落盘：区块卸载/存档重载后仍能判断占用是否还有效，
        // 不再无条件把 currentUser 丢掉（那是「第二人可趁原主异常离场开箱取料」的根因）。
        if (currentUser != null) {
            tag.putUUID("currentUser", currentUser);
            tag.putLong("currentUserSinceTick", currentUserSinceTick);
        }
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
        // 恢复占用者。tickBaselineValid 置 false：NBT 里的 tick 可能来自上一个存档会话，
        // 与当前 level.getGameTime() 不同基准，直接相减不可靠；首次 serverTick 复核时
        // 若占用者确实还开着本台子，时间戳会被立即刷新为当前 tick。
        currentUser = tag.hasUUID("currentUser") ? tag.getUUID("currentUser") : null;
        currentUserSinceTick = tag.getLong("currentUserSinceTick");
        tickBaselineValid = false;
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
