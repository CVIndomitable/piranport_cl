package com.piranport.block.entity;

import com.piranport.menu.CookingPotMenu;
import com.piranport.recipe.CookingPotRecipe;
import com.piranport.recipe.CookingPotRecipeInput;
import com.piranport.registry.ModBlocks;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CookingPotBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOT = 9;
    public static final int TOTAL_SLOTS = 10;

    private CookingPotRecipe currentRecipe;
    private net.minecraft.resources.ResourceLocation currentRecipeId;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot < INPUT_SLOTS) markInputsDirty();
        }
    };

    /**
     * Phase 29: Input-only view of slots 0-8 (top/side faces).
     * External handlers may insert but not extract from input slots.
     */
    private final IItemHandler inputHandler = new IItemHandler() {
        @Override public int getSlots() { return INPUT_SLOTS; }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
    };

    /**
     * Phase 29: Output-only view of slot 9 (bottom face).
     * External handlers may extract but not insert into the output slot.
     */
    private final IItemHandler outputHandler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(OUTPUT_SLOT); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return itemHandler.extractItem(OUTPUT_SLOT, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(OUTPUT_SLOT); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    };

    int cookingProgress = 0;
    int cookingTotalTime = 0;

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cookingProgress;
                case 1 -> cookingTotalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> cookingProgress = value;
                case 1 -> cookingTotalTime = value;
            }
        }

        @Override
        public int getCount() { return 2; }
    };

    public CookingPotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.COOKING_POT.get(), pos, state);
    }

    /** @deprecated Use {@link #getItemHandler(Direction)} for direction-aware access. */
    @Deprecated
    public ItemStackHandler getFullItemHandler() { return itemHandler; }

    /** Phase 29: Direction-aware handler — DOWN = output-only, others = input-only. */
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == Direction.DOWN) return outputHandler;
        return inputHandler;
    }

    private boolean inputsDirty = true;

    public void markInputsDirty() { this.inputsDirty = true; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CookingPotBlockEntity be) {
        // Phase 30: 每 20 tick 吸取上方掉落物到输入槽
        if (level.getGameTime() % 20 == 0) {
            be.suckAboveDrops();
            // Phase 31: 每 20 tick 向相邻容器输出产物 (横向)
            be.ejectToSideContainers();
        }

        if (!hasHeatSource(level, pos)) {
            if (be.cookingProgress > 0) {
                be.cookingProgress = 0;
                be.setChanged();
            }
            return;
        }

        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack s = be.itemHandler.getStackInSlot(i);
            if (!s.isEmpty()) inputs.add(s);
        }
        if (inputs.isEmpty()) {
            be.cookingProgress = 0;
            be.currentRecipe = null;
            return;
        }

        // Only re-query the recipe when inputs have changed (throttle expensive lookup)
        if (be.inputsDirty) {
            be.inputsDirty = false;
            CookingPotRecipeInput input = new CookingPotRecipeInput(inputs);
            Optional<RecipeHolder<CookingPotRecipe>> opt =
                    level.getRecipeManager().getRecipeFor(ModRecipeTypes.COOKING_POT_TYPE.get(), input, level);

            if (opt.isEmpty()) {
                be.cookingProgress = 0;
                be.currentRecipe = null;
                return;
            }

            CookingPotRecipe recipe = opt.get().value();
            if (be.currentRecipe == null && opt.get().id().equals(be.currentRecipeId)) {
                // Resume after load: same recipe id, keep saved progress
                be.currentRecipe = recipe;
            } else if (be.currentRecipe == null
                    || !opt.get().id().equals(be.currentRecipeId)) {
                be.currentRecipe = recipe;
                be.currentRecipeId = opt.get().id();
                be.cookingProgress = 0;
                be.cookingTotalTime = recipe.getCookingTime();
            }
        }

        if (be.currentRecipe == null) return;

        // Check output slot availability
        ItemStack output = be.itemHandler.getStackInSlot(OUTPUT_SLOT);
        ItemStack result = be.currentRecipe.getResult();
        if (!output.isEmpty() && !(ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize())) {
            return;
        }

        be.cookingProgress++;
        if (be.cookingProgress >= be.cookingTotalTime) {
            CookingPotRecipeInput currentInput = new CookingPotRecipeInput(inputs);
            be.craftItem(be.currentRecipe, currentInput);
            be.cookingProgress = 0;
            be.currentRecipe = null;
            be.currentRecipeId = null;
            be.inputsDirty = true; // re-check after crafting
        }
        be.setChanged();
    }

    private void craftItem(CookingPotRecipe recipe, CookingPotRecipeInput input) {
        // Consume ingredients — allow stacked items to satisfy multiple same-type ingredients
        List<net.minecraft.world.item.crafting.Ingredient> remaining = new ArrayList<>(recipe.getIngredients());
        for (int i = 0; i < INPUT_SLOTS && !remaining.isEmpty(); i++) {
            ItemStack slot = itemHandler.getStackInSlot(i);
            if (slot.isEmpty()) continue;
            int consumed = 0;
            for (int j = remaining.size() - 1; j >= 0 && consumed < slot.getCount(); j--) {
                if (remaining.get(j).test(slot)) {
                    remaining.remove(j);
                    consumed++;
                }
            }
            if (consumed > 0) {
                itemHandler.setStackInSlot(i, slot.copyWithCount(slot.getCount() - consumed));
            }
        }
        // Add result
        ItemStack result = recipe.assemble(input, level.registryAccess());
        ItemStack current = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (current.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        } else {
            int maxStackSize = Math.min(current.getMaxStackSize(), result.getMaxStackSize());
            int mergedCount = Math.min(maxStackSize, current.getCount() + result.getCount());
            itemHandler.setStackInSlot(OUTPUT_SLOT, current.copyWithCount(mergedCount));
        }
    }

    /** Phase 30: 锅自动吸取上方掉落物 — 把上方块的 EntityItem 拉入第一个空输入槽。 */
    private void suckAboveDrops() {
        if (level == null) return;
        BlockPos abovePos = worldPosition.above();
        List<net.minecraft.world.entity.item.ItemEntity> items =
                level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(abovePos),
                        e -> e.isAlive() && !e.getItem().isEmpty());
        for (var itemEntity : items) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;
            ItemStack remainder = itemHandler.insertItem(firstEmptyInputSlot(), stack, false);
            if (remainder.isEmpty()) {
                itemEntity.discard();
            } else if (remainder.getCount() < stack.getCount()) {
                itemEntity.setItem(remainder);
            }
        }
    }

    private int firstEmptyInputSlot() {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (itemHandler.getStackInSlot(i).isEmpty()) return i;
        }
        return 0; // 满时回到 slot 0 让 insertItem 自然返回原 stack
    }

    /** Phase 31: 锅向相邻 (北/南/东/西) 容器输出产物。 */
    private void ejectToSideContainers() {
        if (level == null) return;
        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) return;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = worldPosition.relative(side);
            net.minecraft.world.level.block.entity.BlockEntity neighbor =
                    level.getBlockEntity(neighborPos);
            if (!(neighbor instanceof net.minecraft.world.Container container)) continue;

            // 先合并到已有同类物品，再使用空槽位
            for (int slot = 0; slot < container.getContainerSize() && !output.isEmpty(); slot++) {
                ItemStack inSlot = container.getItem(slot);
                if (inSlot.isEmpty()) continue;
                if (!ItemStack.isSameItemSameComponents(inSlot, output)) continue;
                int maxStackSize = Math.min(inSlot.getMaxStackSize(), output.getMaxStackSize());
                int space = maxStackSize - inSlot.getCount();
                if (space <= 0) continue;
                int move = Math.min(space, output.getCount());
                container.setItem(slot, inSlot.copyWithCount(inSlot.getCount() + move));
                output = output.copyWithCount(output.getCount() - move);
                container.setChanged();
            }

            // 空槽位也必须可用，否则单格产物会滞留在锅底
            for (int slot = 0; slot < container.getContainerSize() && !output.isEmpty(); slot++) {
                if (!container.getItem(slot).isEmpty()) continue;
                container.setItem(slot, output.copy());
                output = ItemStack.EMPTY;
                container.setChanged();
            }

            if (output.isEmpty()) break;
        }
        if (output.getCount() != itemHandler.getStackInSlot(OUTPUT_SLOT).getCount()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, output);
        }
    }

    private static boolean hasHeatSource(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState bs = level.getBlockState(below);
        if (bs.is(Blocks.FIRE) || bs.is(Blocks.SOUL_FIRE)) return true;
        if (bs.is(ModBlocks.STOVE.get())) return true;
        if (bs.is(Blocks.MAGMA_BLOCK)) return true;
        if (bs.getFluidState().is(Fluids.LAVA)) return true;
        if (bs.is(Blocks.CAMPFIRE) && bs.getValue(CampfireBlock.LIT)) return true;
        if (bs.is(Blocks.SOUL_CAMPFIRE) && bs.getValue(CampfireBlock.LIT)) return true;
        if (bs.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                && bs.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                && bs.getBlock() instanceof net.minecraft.world.level.block.AbstractFurnaceBlock) return true;
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("cookingProgress", cookingProgress);
        tag.putInt("cookingTotalTime", cookingTotalTime);
        if (currentRecipeId != null) {
            tag.putString("currentRecipeId", currentRecipeId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        cookingProgress = tag.getInt("cookingProgress");
        cookingTotalTime = tag.getInt("cookingTotalTime");
        if (tag.contains("currentRecipeId")) {
            currentRecipeId = net.minecraft.resources.ResourceLocation.tryParse(tag.getString("currentRecipeId"));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.piranport.cooking_pot");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CookingPotMenu(containerId, playerInventory, this);
    }

    public void writeScreenOpeningData(ServerPlayer player, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeBlockPos(this.worldPosition);
    }
}
