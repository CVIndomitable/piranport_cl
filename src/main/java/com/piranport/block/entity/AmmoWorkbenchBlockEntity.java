package com.piranport.block.entity;

import com.piranport.ammo.AmmoRecipe;
import com.piranport.ammo.AmmoRecipeRegistry;
import com.piranport.menu.AmmoWorkbenchMenu;
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
import org.jetbrains.annotations.Nullable;

public class AmmoWorkbenchBlockEntity extends BlockEntity implements MenuProvider {
    public static final int OUTPUT_SLOT = 0;
    public static final int TOTAL_SLOTS = 1;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    int craftingProgress = 0;
    int craftingTotalTime = 0;
    private String craftingRecipeId = "";
    private int craftingQuantity = 0;

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
            switch (index) {
                case 0 -> craftingProgress = value;
                case 1 -> craftingTotalTime = value;
            }
        }

        @Override
        public int getCount() { return 2; }
    };

    public AmmoWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.AMMO_WORKBENCH.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }

    public boolean isCrafting() { return craftingTotalTime > 0; }

    public void startCrafting(String recipeId, int quantity) {
        AmmoRecipe recipe = AmmoRecipeRegistry.findById(recipeId);
        if (recipe == null) return;
        this.craftingRecipeId = recipeId;
        this.craftingQuantity = quantity;
        this.craftingProgress = 0;
        this.craftingTotalTime = recipe.craftTimeTicks() * quantity;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   AmmoWorkbenchBlockEntity be) {
        if (be.craftingTotalTime <= 0) return;

        be.craftingProgress++;
        if (be.craftingProgress >= be.craftingTotalTime) {
            AmmoRecipe recipe = AmmoRecipeRegistry.findById(be.craftingRecipeId);
            if (recipe != null) {
                ItemStack result = recipe.getResultStack(be.craftingQuantity);
                ItemStack current = be.itemHandler.getStackInSlot(OUTPUT_SLOT);
                if (current.isEmpty()) {
                    be.itemHandler.setStackInSlot(OUTPUT_SLOT, result);
                } else if (ItemStack.isSameItemSameComponents(current, result)
                        && current.getCount() + result.getCount() <= current.getMaxStackSize()) {
                    current.grow(result.getCount());
                }
                // If output slot can't hold, items are lost — server should pre-validate
            }
            be.craftingProgress = 0;
            be.craftingTotalTime = 0;
            be.craftingRecipeId = "";
            be.craftingQuantity = 0;
        }
        be.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("craftingProgress", craftingProgress);
        tag.putInt("craftingTotalTime", craftingTotalTime);
        tag.putString("craftingRecipeId", craftingRecipeId);
        tag.putInt("craftingQuantity", craftingQuantity);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        craftingProgress = tag.getInt("craftingProgress");
        craftingTotalTime = tag.getInt("craftingTotalTime");
        craftingRecipeId = tag.getString("craftingRecipeId");
        craftingQuantity = tag.getInt("craftingQuantity");
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

    public void writeScreenOpeningData(ServerPlayer player, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeBlockPos(this.worldPosition);
    }
}
