package com.piranport.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * 黎塞留的指挥刀 — Richelieu's Command Sword
 * +8 Attack Damage, 1.4 Attack Speed, 1520 Durability
 */
public class CommandSwordItem extends SwordItem {

    public static final Tier COMMAND_SWORD_TIER = new Tier() {
        @Override public int getUses() { return 1520; }
        @Override public float getSpeed() { return 0; }
        @Override public float getAttackDamageBonus() { return 0; }
        @Override public int getEnchantmentValue() { return 15; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.IRON_INGOT); }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_IRON_TOOL; }
    };

    public CommandSwordItem(Properties properties) {
        super(COMMAND_SWORD_TIER, properties
                .attributes(SwordItem.createAttributes(COMMAND_SWORD_TIER, 7, -2.6f)));
    }
}
