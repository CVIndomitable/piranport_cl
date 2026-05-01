package com.piranport.block.entity;

import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModBlocks;
import net.minecraft.core.BlockPos;
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
    private ResourceLocation foodItemId = new ResourceLocation("minecraft", "air");
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

        Item foodItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(foodItemId);
        if (foodItem == null || foodItem == Items.AIR) return;

        FoodProperties food = new ItemStack(foodItem).getFoodProperties(player);
        if (food == null) return;

        int bitesDone = totalServings - remainingServings;
        int nutritionPerBite = (int) ((long) food.getNutrition() * (bitesDone + 1) / totalServings)
                - (int) ((long) food.getNutrition() * bitesDone / totalServings);
        float satModPerBite = food.getSaturationModifier() * (bitesDone + 1) / totalServings
                - food.getSaturationModifier() * bitesDone / totalServings;
        player.getFoodData().eat(nutritionPerBite, satModPerBite);

        boolean isLastBite = (remainingServings == 1);
        if (isLastBite) {
            for (var pe : food.getEffects()) {
                MobEffectInstance orig = pe.getFirst();
                if (player.getRandom().nextFloat() < pe.getSecond()) {
                    player.addEffect(new MobEffectInstance(orig.getEffect(),
                            orig.getDuration(), orig.getAmplifier()));
                }
            }
        }

        remainingServings--;
        if (remainingServings <= 0) {
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
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("foodItemId", foodItemId.toString());
        tag.putInt("remainingServings", remainingServings);
        tag.putInt("totalServings", totalServings);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("foodItemId")) {
            foodItemId = new ResourceLocation(tag.getString("foodItemId"));
        }
        remainingServings = tag.getInt("remainingServings");
        totalServings = tag.getInt("totalServings");
    }
}
