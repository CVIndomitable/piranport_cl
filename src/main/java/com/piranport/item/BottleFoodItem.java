package com.piranport.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Phase 27: Food item served in a glass bottle.
 * Consuming it returns one empty glass bottle (like Honey Bottle / Potion).
 *
 * <p>继承 {@link ModFoodItem} 以获得潜行右击放置食物方块的能力（策划决策/食物/08）。
 * 只有携带 PLACEABLE_INFO 组件的物品才真正可放置，纯饮料（苹果汁等）不受影响。</p>
 */
public class BottleFoodItem extends ModFoodItem {

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
            // Stack had count > 1 — return glass bottle to inventory, drop if full
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }
        return result;
    }
}
