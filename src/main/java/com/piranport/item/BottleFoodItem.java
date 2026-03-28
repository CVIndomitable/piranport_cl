package com.piranport.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Phase 27: Food item served in a glass bottle.
 * Consuming it returns one empty glass bottle (like Honey Bottle / Potion).
 */
public class BottleFoodItem extends Item {

    public BottleFoodItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            if (result.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
            // Stack had count > 1 — return glass bottle to inventory
            player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
        }
        return result;
    }
}
