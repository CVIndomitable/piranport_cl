package com.piranport.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * 初雪的主炮 — Hatsuyuki's Main Gun
 * Axe-like weapon: +12 Attack Damage, 1.4 Attack Speed, 960 Durability
 */
public class HatsuyukiMainGunItem extends AxeItem {

    public static final Tier HATSUYUKI_TIER = new Tier() {
        @Override public int getUses() { return 960; }
        @Override public float getSpeed() { return 8.0f; }
        @Override public float getAttackDamageBonus() { return 0; }
        @Override public int getEnchantmentValue() { return 15; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.IRON_INGOT); }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; }
    };

    public HatsuyukiMainGunItem(Properties properties) {
        super(HATSUYUKI_TIER, properties
                .attributes(AxeItem.createAttributes(HATSUYUKI_TIER, 11, -2.6f)));
    }
}
