package com.piranport.compat.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface WeaponHandler {
    boolean handles(Item item);

    void fire(EntityMaid maid, LivingEntity target, ItemStack stack);

    int cooldownTicks(ItemStack stack);

    /** Resolves cooldown with the firing world available for runtime overrides. */
    default int cooldownTicks(EntityMaid maid, ItemStack stack) {
        return cooldownTicks(stack);
    }

    default boolean isOffensive() {
        return true;
    }

    default boolean hasAmmo(EntityMaid maid, ItemStack stack) {
        return true;
    }
}
