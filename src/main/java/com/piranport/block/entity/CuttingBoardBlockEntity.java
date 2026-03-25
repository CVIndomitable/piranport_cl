package com.piranport.block.entity;

import com.piranport.recipe.CuttingBoardRecipe;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModRecipeTypes;
import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CuttingBoardBlockEntity extends BlockEntity {
    private ItemStack storedItem = ItemStack.EMPTY;
    private int progress = 0;

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
