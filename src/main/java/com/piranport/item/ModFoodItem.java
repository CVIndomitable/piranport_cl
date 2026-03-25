package com.piranport.item;

import com.piranport.block.PlaceableFoodBlock;
import com.piranport.block.entity.PlaceableFoodBlockEntity;
import com.piranport.component.PlaceableInfo;
import com.piranport.registry.ModBlocks;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;

public class ModFoodItem extends Item {
    public ModFoodItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level level = ctx.getLevel();
        if (player != null && player.isShiftKeyDown() && ctx.getClickedFace() == Direction.UP) {
            ItemStack held = ctx.getItemInHand();
            PlaceableInfo info = held.get(ModDataComponents.PLACEABLE_INFO.get());
            if (info != null) {
                BlockPos target = ctx.getClickedPos().above();
                if (level.getBlockState(target).canBeReplaced()) {
                    if (!level.isClientSide) {
                        Block block = switch (info.containerType()) {
                            case "bowl" -> ModBlocks.BOWL_FOOD.get();
                            case "cake" -> ModBlocks.CAKE_FOOD.get();
                            default -> ModBlocks.PLATE_FOOD.get();
                        };
                        level.setBlockAndUpdate(target, block.defaultBlockState());
                        BlockEntity be = level.getBlockEntity(target);
                        if (be instanceof PlaceableFoodBlockEntity foodBE) {
                            ResourceLocation id = BuiltInRegistries.ITEM.getKey(held.getItem());
                            foodBE.initialize(id, info.servings());
                            foodBE.setChanged();
                            level.sendBlockUpdated(target, block.defaultBlockState(), block.defaultBlockState(), 3);
                        }
                        if (!player.isCreative()) {
                            held.shrink(1);
                        }
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return InteractionResult.PASS;
    }
}
