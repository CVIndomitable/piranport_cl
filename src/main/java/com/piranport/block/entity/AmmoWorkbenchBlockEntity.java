package com.piranport.block.entity;

import com.piranport.ammo.AmmoRecipe;
import com.piranport.ammo.AmmoRecipeRegistry;
import com.piranport.menu.AmmoWorkbenchMenu;
import com.piranport.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

public class AmmoWorkbenchBlockEntity extends BlockEntity implements MenuProvider {
    public static final int OUTPUT_SLOT = 0;
    public static final int TOTAL_SLOTS = 1;

    private static final int DATA_SIZE = 2;
    private static final int SAVE_INTERVAL_TICKS = 20;
    private static final int MAX_CRAFT_QUANTITY = 1_000_000;
    /** 未被任何玩家打开的 idle 累计 tick 上限：超过即释放 craftingOwner。
     *  用于断线 / crash / menu 关闭事件未送达时强制释放占用锁，避免工作台永久锁死。 */
    private static final int IDLE_RELEASE_TICKS = 100;
    private static final java.util.function.Predicate<ItemStack> NON_EMPTY_STACK =
            stack -> !stack.isEmpty();

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }
    };

    // H5: 与 WeaponWorkbenchBlockEntity 风格一致，字段保持 private 并暴露 getter
    private int craftingProgress = 0;
    private int craftingTotalTime = 0;
    private String craftingRecipeId = "";
    private int craftingQuantity = 0;

    // Materials taken from player inventory at crafting start — must be refunded on cancel.
    private NonNullList<ItemStack> pendingMaterials = NonNullList.create();
    // Result held when crafting completed but OUTPUT slot is blocked.
    private ItemStack pendingOutput = ItemStack.EMPTY;
    /**
     * 合成归属者 UUID，同时充当工作台的「占用者」（二人互斥锁）：
     * - 打开菜单时由 tryOpen 写入；菜单关闭 / idle 超时 / 合成取消时清空。
     * - 它的生命周期比单次合成更长：合成完成但产物还没被取走时依然保留，
     *   用来阻止旁人抢走产物（见 OutputSlot.mayPickup）并让关闭菜单只由归属者触发取消。
     */
    @Nullable
    private UUID craftingOwner;
    /** 记录占用者最近一次仍在与服务端交互的 tick。只在本地内存维护，不落盘。 */
    private long ownerActiveTick = 0;

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> craftingProgress;
                case 1 -> craftingTotalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (level != null && !level.isClientSide) return;
            switch (index) {
                case 0 -> craftingProgress = value;
                case 1 -> craftingTotalTime = value;
            }
        }

        @Override
        public int getCount() { return DATA_SIZE; }
    };

    public AmmoWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.AMMO_WORKBENCH.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }

    public boolean isCrafting() { return craftingTotalTime > 0; }

    /** @return 当前合成进度（tick） */
    public int getCraftingProgress() { return craftingProgress; }

    /** @return 当前合成总耗时（tick） */
    public int getCraftingTotalTime() { return craftingTotalTime; }

    @Nullable
    public UUID getCraftingOwner() { return craftingOwner; }

    // ===== 多人互斥（占用判定）=====
    // 弹药工作台不像武器工作台那样另设 currentUser：craftingOwner 本身就代表
    // 「谁在用这台工作台」——它既覆盖合成期间，也覆盖合成完成、产物尚未取走的窗口，
    // 所以直接复用，不再新增一个语义重叠的字段。

    /**
     * 尝试占用工作台。仅在原占用者已不可用时才允许抢占。
     * 抢占时必须先回收未取走的产物：原占用者已经拿不到了（离线 / 换维度 / 死亡），
     * 产物留在槽里只会永久卡住（OutputSlot.mayPickup 对所有人都不放行）。
     */
    public boolean tryOpen(Player player) {
        if (craftingOwner != null && level != null && !craftingOwner.equals(player.getUUID())) {
            Player existing = level.getPlayerByUUID(craftingOwner);
            if (existing != null && existing.isAlive()
                    && existing.containerMenu instanceof AmmoWorkbenchMenu) {
                // 原占用者活着且正开着菜单 → 真占用中，拒绝第二人
                ownerActiveTick = level.getGameTime();
                return false;
            }
            // H4 同款兜底：断线 / crash / menu 关闭事件未送达时靠 idle 超时释放。
            // 注意 lastSeen 为 0 表示占用者从未被 tick 观察到（例如刚被打开就换人），
            // 此时不能拿 gameTime 去减，否则在游戏早期就会误判超时。
            long now = level.getGameTime();
            boolean stale = ownerActiveTick > 0 && (now - ownerActiveTick) > IDLE_RELEASE_TICKS;
            if (!stale) {
                ownerActiveTick = now;
                return false;
            }
            releaseOccupant(true);
        }
        assignOwner(player.getUUID());
        return true;
    }

    /** 当前占用者是否为该玩家。 */
    public boolean isOwnedBy(Player player) {
        return craftingOwner != null && craftingOwner.equals(player.getUUID());
    }

    /** 显式释放占用（菜单关闭时调用）。owner 匹配才释放，旁观者关闭自己的菜单不影响占用者。 */
    public void releaseOccupantIfOwner(Player player) {
        if (craftingOwner != null && craftingOwner.equals(player.getUUID())) {
            releaseOccupant(true);
        }
    }

    /** 让出占用（不退还材料，材料由调用方决定去留）。 */
    public void releaseOccupant(boolean refund) {
        if (refund) refundPendingMaterials();
        craftingOwner = null;
        ownerActiveTick = 0;
        setChanged();
    }

    private void assignOwner(UUID uuid) {
        craftingOwner = uuid;
        ownerActiveTick = level != null ? level.getGameTime() : 0;
        setChanged();
    }

    /**
     * Begin a crafting job using materials already shrunk from the player's inventory.
     * The materials are moved into the BE's internal buffer and will be consumed at completion,
     * or refunded to the owning player (or dropped) if the job is cancelled.
     */
    public void startCrafting(Player owner, String recipeId, int quantity,
                              NonNullList<ItemStack> takenMaterials) {
        AmmoRecipe recipe = AmmoRecipeRegistry.findById(recipeId);
        if (recipe == null) return;
        this.craftingRecipeId = recipeId;
        this.craftingQuantity = quantity;
        this.craftingProgress = 0;
        long totalTime = (long) recipe.craftTimeTicks() * quantity;
        this.craftingTotalTime = (int) Math.min(totalTime, Integer.MAX_VALUE / 2);
        this.craftingOwner = owner.getUUID();
        // 占用者在操作（开始合成），刷新活跃时间戳，避免被误判 idle
        this.ownerActiveTick = level != null ? level.getGameTime() : 0;
        this.pendingMaterials = takenMaterials;
        setChanged();
    }

    /**
     * Validate a crafting job restored from NBT before it is allowed to progress.
     * Called on the first server tick or when the menu is opened.
     */
    public void validateSavedCrafting() {
        if (level == null || level.isClientSide || !isCrafting()) return;
        validateActiveJob();
    }

    private boolean validateActiveJob() {
        if (!isCrafting()) return false;

        boolean recipeIdValid = !craftingRecipeId.isBlank()
                && craftingRecipeId.length() <= 256
                && AmmoRecipeRegistry.findById(craftingRecipeId) != null;
        boolean quantityValid = craftingQuantity > 0 && craftingQuantity <= MAX_CRAFT_QUANTITY;
        boolean timeValid = craftingTotalTime > 0 && craftingProgress >= 0
                && craftingProgress <= craftingTotalTime;

        if (!recipeIdValid || !quantityValid || !timeValid) {
            cancelInvalidJob();
            return false;
        }
        return true;
    }

    private void cancelInvalidJob() {
        refundPendingMaterials();
        craftingProgress = 0;
        craftingTotalTime = 0;
        craftingRecipeId = "";
        craftingQuantity = 0;
        craftingOwner = null;
        ownerActiveTick = 0;
        setChanged();
    }

    /** Refund pending materials to the owner (or drop to world) and clear crafting state. */
    public void cancelCrafting() {
        refundPendingMaterials();
        craftingProgress = 0;
        craftingTotalTime = 0;
        craftingRecipeId = "";
        craftingQuantity = 0;
        craftingOwner = null;
        ownerActiveTick = 0;
        setChanged();
    }

    private void refundPendingMaterials() {
        if (pendingMaterials.isEmpty() || level == null) return;
        Player owner = craftingOwner != null ? level.getPlayerByUUID(craftingOwner) : null;
        for (ItemStack stack : pendingMaterials) {
            if (stack.isEmpty()) continue;
            // P1 #12: 玩家离线或背包满时直接掉落到工作台位置，不依赖玩家
            if (owner != null && owner.isAlive()) {
                if (!owner.addItem(stack)) {
                    owner.drop(stack, false);
                }
            } else {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(),
                        worldPosition.getZ(), stack);
            }
        }
        pendingMaterials = NonNullList.create();
    }

    /** Called from the Block's onRemove — dump buffered state to the world. */
    public void dumpContentsOnBreak() {
        if (level == null) return;
        double x = worldPosition.getX();
        double y = worldPosition.getY();
        double z = worldPosition.getZ();
        for (ItemStack stack : pendingMaterials) {
            if (!stack.isEmpty()) Containers.dropItemStack(level, x, y, z, stack);
        }
        pendingMaterials = NonNullList.create();
        if (!pendingOutput.isEmpty()) {
            Containers.dropItemStack(level, x, y, z, pendingOutput);
            pendingOutput = ItemStack.EMPTY;
        }
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            ItemStack stack = itemHandler.extractItem(i, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) Containers.dropItemStack(level, x, y, z, stack);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   AmmoWorkbenchBlockEntity be) {
        // Try to flush any pending output into the slot as it frees up.
        if (!be.pendingOutput.isEmpty()) {
            if (tryFlushPending(be)) be.setChanged();
        }

        // Clear owner once the job is finished AND the output has been fully collected.
        if (be.craftingTotalTime <= 0 && be.pendingOutput.isEmpty()
                && be.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty()
                && be.craftingOwner != null) {
            be.craftingOwner = null;
            be.ownerActiveTick = 0;
            be.setChanged();
        }

        // 占用者在线且仍开着本菜单（或正在合成）就持续刷新活跃时间戳；
        // 否则让时间戳自然变旧，由 tryOpen 的 idle 兜底回收。
        if (be.craftingOwner != null) {
            Player owner = level.getPlayerByUUID(be.craftingOwner);
            if (owner != null && owner.isAlive()
                    && (owner.containerMenu instanceof AmmoWorkbenchMenu || be.isCrafting())) {
                be.ownerActiveTick = level.getGameTime();
            }
        }

        if (be.craftingTotalTime <= 0) return;

        // Read jobs are validated lazily after world load; invalid jobs refund materials.
        if (!be.validateActiveJob()) return;

        if (be.craftingProgress < be.craftingTotalTime) {
            be.craftingProgress++;
        }

        if (be.craftingProgress >= be.craftingTotalTime) {
            AmmoRecipe recipe = AmmoRecipeRegistry.findById(be.craftingRecipeId);
            if (recipe == null) {
                be.cancelInvalidJob();
                return;
            }
            ItemStack result = recipe.getResultStack(be.craftingQuantity);
            be.pendingMaterials = NonNullList.create();

            if (!tryPutIntoOutput(be, result)) {
                be.pendingOutput = result;
            }
            be.craftingProgress = 0;
            be.craftingTotalTime = 0;
            be.craftingRecipeId = "";
            be.craftingQuantity = 0;
            // Keep craftingOwner until the output has been collected.
            be.setChanged();
            return;
        }

        if (be.craftingProgress == 0
                || be.craftingProgress % SAVE_INTERVAL_TICKS == 0) {
            be.setChanged();
        }
    }

    private static boolean tryPutIntoOutput(AmmoWorkbenchBlockEntity be, ItemStack result) {
        ItemStack current = be.itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (current.isEmpty()) {
            be.itemHandler.setStackInSlot(OUTPUT_SLOT, result);
            return true;
        }
        if (ItemStack.isSameItem(current, result)
                && current.getCount() + result.getCount() <= current.getMaxStackSize()) {
            current.grow(result.getCount());
            be.itemHandler.setStackInSlot(OUTPUT_SLOT, current);
            return true;
        }
        return false;
    }

    private static boolean tryFlushPending(AmmoWorkbenchBlockEntity be) {
        if (be.pendingOutput.isEmpty()) return false;
        if (tryPutIntoOutput(be, be.pendingOutput)) {
            be.pendingOutput = ItemStack.EMPTY;
            return true;
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("craftingProgress", craftingProgress);
        tag.putInt("craftingTotalTime", craftingTotalTime);
        tag.putString("craftingRecipeId", craftingRecipeId);
        tag.putInt("craftingQuantity", craftingQuantity);
        if (craftingOwner != null) tag.putUUID("craftingOwner", craftingOwner);

        ListTag pending = new ListTag();
        for (ItemStack stack : pendingMaterials) {
            if (stack.isEmpty()) continue;
            Tag encoded = stack.save(registries);
            pending.add(encoded);
        }
        tag.put("pendingMaterials", pending);

        if (!pendingOutput.isEmpty()) {
            tag.put("pendingOutput", pendingOutput.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        craftingProgress = Math.max(0, Math.min(Integer.MAX_VALUE / 2, tag.getInt("craftingProgress")));
        craftingTotalTime = Math.max(0, Math.min(Integer.MAX_VALUE / 2, tag.getInt("craftingTotalTime")));
        craftingRecipeId = tag.getString("craftingRecipeId");
        craftingQuantity = Math.max(0, tag.getInt("craftingQuantity"));
        craftingOwner = tag.hasUUID("craftingOwner") ? tag.getUUID("craftingOwner") : null;

        pendingMaterials = NonNullList.create();
        if (tag.contains("pendingMaterials", Tag.TAG_LIST)) {
            ListTag list = tag.getList("pendingMaterials", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack.parse(registries, list.getCompound(i))
                        .filter(NON_EMPTY_STACK)
                        .ifPresent(pendingMaterials::add);
            }
        }

        if (tag.contains("pendingOutput", Tag.TAG_COMPOUND)) {
            pendingOutput = ItemStack.parse(registries, tag.getCompound("pendingOutput"))
                    .orElse(ItemStack.EMPTY);
        } else {
            pendingOutput = ItemStack.EMPTY;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.piranport.ammo_workbench");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AmmoWorkbenchMenu(containerId, playerInventory, this);
    }
}
