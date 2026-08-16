package com.piranport.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 翔鹤的镰刀 — 策划 §3.5 表 3.5 "翔鹤镰刀"。
 *
 * <p>定位：深海翔鹤/旗舰掉落的高阶近战武器，耐久 1024，攻击伤害 +7.0，攻速 -2.4（挥镰）。
 * Tier 自定义以匹配耐久与材质定位（Netherite 级）。
 */
public class ShoukakuScytheItem extends SwordItem {

    public static final Tier SHOUKAKU_SCYTHE_TIER = new Tier() {
        @Override public int getUses() { return 1024; }
        @Override public float getSpeed() { return 9.0f; }
        @Override public float getAttackDamageBonus() { return 0.0f; }
        @Override public int getEnchantmentValue() { return 15; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; }
    };

    public ShoukakuScytheItem(Properties properties) {
        super(SHOUKAKU_SCYTHE_TIER, properties
                .attributes(SwordItem.createAttributes(SHOUKAKU_SCYTHE_TIER, 7, -2.4f)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.piranport.shoukaku_scythe.desc")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.piranport.shoukaku_scythe.source")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}