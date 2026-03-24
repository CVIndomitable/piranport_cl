package com.piranport.combat;

import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public class TransformationManager {

    public static boolean isTransformed(ItemStack coreStack) {
        return coreStack.getOrDefault(ModDataComponents.SHIP_CORE_TRANSFORMED.get(), false);
    }

    public static void setTransformed(ItemStack coreStack, boolean transformed) {
        coreStack.set(ModDataComponents.SHIP_CORE_TRANSFORMED.get(), transformed);
    }

    public static int getWeaponIndex(ItemStack coreStack) {
        return coreStack.getOrDefault(ModDataComponents.SHIP_CORE_WEAPON_INDEX.get(), 0);
    }

    public static void setWeaponIndex(ItemStack coreStack, int index) {
        coreStack.set(ModDataComponents.SHIP_CORE_WEAPON_INDEX.get(), index);
    }

    /** Cycle to the next weapon slot, skipping empty slots. Called from server via network packet. */
    public static void cycleWeapon(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof ShipCoreItem sci)) return;
        if (!isTransformed(mainHand)) return;

        int weaponSlots = sci.getShipType().weaponSlots;
        ItemContainerContents contents = mainHand.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(sci.getShipType().totalSlots(), ItemStack.EMPTY);
        contents.copyInto(items);

        int current = getWeaponIndex(mainHand);
        // Find next non-empty slot, cycling up to weaponSlots times
        int next = -1;
        for (int i = 1; i <= weaponSlots; i++) {
            int candidate = (current + i) % weaponSlots;
            if (candidate < items.size() && !items.get(candidate).isEmpty()) {
                next = candidate;
                break;
            }
        }
        if (next == -1) return; // No equipped weapons to switch to

        setWeaponIndex(mainHand, next);
        player.displayClientMessage(
                Component.translatable("message.piranport.weapon_selected", items.get(next).getHoverName()), true);
    }
}
