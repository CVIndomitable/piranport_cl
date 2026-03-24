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

    /** Cycle to the next weapon slot. Called from server via network packet. */
    public static void cycleWeapon(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof ShipCoreItem sci)) return;
        if (!isTransformed(mainHand)) return;

        int weaponSlots = sci.getShipType().weaponSlots;
        int current = getWeaponIndex(mainHand);
        int next = (current + 1) % weaponSlots;
        setWeaponIndex(mainHand, next);

        // Show selected weapon name
        ItemContainerContents contents = mainHand.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(sci.getShipType().totalSlots(), ItemStack.EMPTY);
        contents.copyInto(items);
        ItemStack weapon = next < items.size() ? items.get(next) : ItemStack.EMPTY;
        if (!weapon.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.piranport.weapon_selected", weapon.getHoverName()), true);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.piranport.slot_empty", next + 1), true);
        }
    }
}
