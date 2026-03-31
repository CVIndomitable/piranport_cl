package com.piranport.block.entity;

import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PlaceableFoodBlockEntity extends BlockEntity {
    private ResourceLocation foodItemId = ResourceLocation.withDefaultNamespace("air");
    private int remainingServings = 0;
    private int totalServings = 1;

    public PlaceableFoodBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.PLACEABLE_FOOD.get(), pos, state);
    }

    public void initialize(ResourceLocation id, int servings) {
        this.foodItemId = id;
        this.remainingServings = servings;
        this.totalServings = servings;
    }

    public ResourceLocation getFoodItemId() { return foodItemId; }
    public int getRemainingServings() { return remainingServings; }
    public boolean isEmpty() { return remainingServings <= 0; }

    public void eat(Player player) {
        if (remainingServings <= 0 || level == null) return;

        Item foodItem = BuiltInRegistries.ITEM.get(foodItemId);
        if (foodItem == Items.AIR) return;

        FoodProperties food = new ItemStack(foodItem).getFoodProperties(player);
        if (food == null) return;

        int nutritionPerBite = (int) Math.ceil((double) food.nutrition() / totalServings);
        float satModPerBite = (float) Math.ceil(food.saturation() / totalServings * 10) / 10f;
        player.getFoodData().eat(nutritionPerBite, satModPerBite);

        for (FoodProperties.PossibleEffect pe : food.effects()) {
            MobEffectInstance orig = pe.effect();
            int dur = totalServings > 1
                    ? (int) Math.ceil((double) orig.getDuration() / (totalServings - 1))
                    : orig.getDuration();
            if (player.getRandom().nextFloat() < pe.probability()) {
                player.addEffect(new MobEffectInstance(orig.getEffect(), dur, orig.getAmplifier()));
            }
        }

        remainingServings--;
        if (remainingServings <= 0) {
            // Drop bowl if bowl container
            if (getBlockState().is(ModBlocks.BOWL_FOOD.get())) {
                Block.popResource(level, worldPosition, new ItemStack(Items.BOWL));
            }
            level.removeBlock(worldPosition, false);
        } else {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
        tag.putString("foodItemId", foodItemId.toString());
        tag.putInt("remainingServings", remainingServings);
        tag.putInt("totalServings", totalServings);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("foodItemId")) {
            foodItemId = ResourceLocation.parse(tag.getString("foodItemId"));
        }
        remainingServings = tag.getInt("remainingServings");
        totalServings = tag.getInt("totalServings");
    }
}
