package com.piranport.block.entity;

import com.piranport.recipe.CuttingBoardRecipe;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CuttingBoardBlockEntity extends BlockEntity {
    private ItemStack storedItem = ItemStack.EMPTY;
    private int progress = 0;
    private int holdTicks = 0;       // Phase 30: 长按 tick 计数
    private static final int HOLD_INTERVAL = 5; // 每 5 tick 切一次 (策划 4 秒 = 80 tick)
    private static final int HOLDS_TO_CUT = 4;  // 4 次 hold 切 = 完成一次切割
    private java.util.UUID holdingPlayer = null; // 当前在长按的玩家

    /**
     * Phase 30: Single-slot IItemHandler for hopper/pipe automation.
     * insert  — accepted only when board is empty (storedItem.isEmpty())
     * extract — always EMPTY (output drops into the world, not extractable via capability)
     */
    private final IItemHandler itemHandler = new IItemHandler() {
        @Override public int getSlots() { return 1; }

        @Override public ItemStack getStackInSlot(int slot) { return storedItem; }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !storedItem.isEmpty()) return stack;
            if (!simulate) {
                storedItem = stack.copyWithCount(1);
                setChanged();
                if (level != null)
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
            // Return remainder (all but the one we took)
            return stack.getCount() > 1 ? stack.copyWithCount(stack.getCount() - 1) : ItemStack.EMPTY;
        }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return storedItem.isEmpty(); }
    };

    public IItemHandler getItemHandler(@Nullable Direction side) { return itemHandler; }

    public CuttingBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.CUTTING_BOARD.get(), pos, state);
    }

    public ItemStack getStoredItem() { return storedItem; }
    public int getProgress() { return progress; }

    public void setStoredItem(ItemStack stack) {
        this.storedItem = stack;
        this.progress = 0;
    }

    public void setProgress(int progress) { this.progress = progress; }

    public void cut(Level level) {
        if (storedItem.isEmpty() || level == null) return;
        SingleRecipeInput input = new SingleRecipeInput(storedItem);
        Optional<RecipeHolder<CuttingBoardRecipe>> opt =
                level.getRecipeManager().getRecipeFor(ModRecipeTypes.CUTTING_BOARD_TYPE.get(), input, level);
        if (opt.isEmpty()) return;

        CuttingBoardRecipe recipe = opt.get().value();
        progress++;
        if (progress >= recipe.getCuts()) {
            ItemStack result = recipe.assemble(input, level.registryAccess());
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), result);
            storedItem = ItemStack.EMPTY;
            progress = 0;
        }
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Phase 30: 标记玩家开始长按。每次 serverTick 自动+1 tick，达到 HOLDS_TO_CUT 完成切割。 */
    public void startHold(java.util.UUID playerUuid) {
        if (storedItem.isEmpty()) return;
        this.holdingPlayer = playerUuid;
        this.holdTicks = 0;
    }

    public void endHold(java.util.UUID playerUuid) {
        if (playerUuid.equals(this.holdingPlayer)) {
            this.holdingPlayer = null;
            this.holdTicks = 0;
        }
    }

    /** Phase 30: 服务端 tick — 检查长按玩家是否仍然靠近砧板，按 HOLD_INTERVAL 自动 cut。 */
    public void serverTick(Level level) {
        if (holdingPlayer == null) return;
        if (storedItem.isEmpty()) {
            holdingPlayer = null;
            holdTicks = 0;
            return;
        }
        net.minecraft.server.level.ServerPlayer player = level.getServer().getPlayerList().getPlayer(holdingPlayer);
        if (player == null || player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) > 25) {
            holdingPlayer = null;
            holdTicks = 0;
            return;
        }
        holdTicks++;
        if (holdTicks >= HOLD_INTERVAL) {
            holdTicks = 0;
            cut(level);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!storedItem.isEmpty()) {
            tag.put("item", storedItem.save(registries));
        }
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("item")) {
            storedItem = ItemStack.parse(registries, tag.getCompound("item")).orElse(ItemStack.EMPTY);
        } else {
            storedItem = ItemStack.EMPTY;
        }
        progress = tag.getInt("progress");
    }
}
