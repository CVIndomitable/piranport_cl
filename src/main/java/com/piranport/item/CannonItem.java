package com.piranport.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CannonItem extends Item {
    private final float damage;
    private final int cooldownTicks;
    private final int barrelCount;

    public CannonItem(Properties properties, float damage, int cooldownTicks, int barrelCount) {
        super(properties);
        this.damage = damage;
        this.cooldownTicks = cooldownTicks;
        this.barrelCount = barrelCount;
    }

    public float getDamage() { return damage; }
    public int getCooldownTicks() { return cooldownTicks; }
    public int getBarrelCount() { return barrelCount; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Placeholder: full implementation in later phase
        return InteractionResultHolder.pass(stack);
    }
}
