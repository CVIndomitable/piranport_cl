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

    int craftingProgress = 0;
    int craftingTotalTime = 0;
    private String craftingRecipeId = "";
    private int craftingQuantity = 0;

    // Materials taken from player inventory at crafting start — must be refunded on cancel.
    private NonNullList<ItemStack> pendingMaterials = NonNullList.create();
    // Result held when crafting completed but OUTPUT slot is blocked.
    private ItemStack pendingOutput = ItemStack.EMPTY;
    @Nullable
    private UUID craftingOwner;

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

    @Nullable
    public UUID getCraftingOwner() { return craftingOwner; }

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
        this.pendingMaterials = takenMaterials;
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
        setChanged();
    }

    private void refundPendingMaterials() {
        if (pendingMaterials.isEmpty() || level == null) return;
        Player owner = craftingOwner != null ? level.getPlayerByUUID(craftingOwner) : null;
        for (ItemStack stack : pendingMaterials) {
            if (stack.isEmpty()) continue;
            if (owner != null && owner.isAlive() && !owner.addItem(stack)) {
                owner.drop(stack, false);
            } else if (owner == null || !owner.isAlive()) {
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
            be.setChanged();
        }

        if (be.craftingTotalTime <= 0) return;

        if (be.craftingProgress < be.craftingTotalTime) {
            be.craftingProgress++;
        }

        if (be.craftingProgress >= be.craftingTotalTime) {
            AmmoRecipe recipe = AmmoRecipeRegistry.findById(be.craftingRecipeId);
            if (recipe == null) {
                // Recipe vanished mid-craft (mod update). Refund materials to the owner.
                be.refundPendingMaterials();
                be.craftingProgress = 0;
                be.craftingTotalTime = 0;
                be.craftingRecipeId = "";
                be.craftingQuantity = 0;
                be.craftingOwner = null;
                be.setChanged();
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
        craftingProgress = Math.max(0, tag.getInt("craftingProgress"));
        craftingTotalTime = Math.max(0, tag.getInt("craftingTotalTime"));
        craftingRecipeId = tag.getString("craftingRecipeId");
        craftingQuantity = Math.max(0, tag.getInt("craftingQuantity"));
        craftingOwner = tag.hasUUID("craftingOwner") ? tag.getUUID("craftingOwner") : null;

        pendingMaterials = NonNullList.create();
        if (tag.contains("pendingMaterials", Tag.TAG_LIST)) {
            ListTag list = tag.getList("pendingMaterials", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack.parse(registries, list.getCompound(i)).ifPresent(pendingMaterials::add);
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
