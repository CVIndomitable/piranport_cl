package com.piranport.menu;

import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMenuTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public class ShipCoreMenu extends AbstractContainerMenu {
    private final ItemStack coreStack;
    private final int coreSlot;
    private final ShipCoreItem.ShipType shipType;
    private final SimpleContainer coreContainer;

    // Weapon weight constants
    public static final int SMALL_GUN_WEIGHT = 6;
    public static final int MEDIUM_GUN_WEIGHT = 16;
    public static final int LARGE_GUN_WEIGHT = 30;

    /** Client-side constructor from network */
    public static ShipCoreMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        return new ShipCoreMenu(containerId, playerInventory, buf.readVarInt());
    }

    /** Main constructor */
    public ShipCoreMenu(int containerId, Inventory playerInventory, int coreSlot) {
        super(ModMenuTypes.SHIP_CORE_MENU.get(), containerId);
        this.coreSlot = coreSlot;
        this.coreStack = playerInventory.getItem(coreSlot);

        if (coreStack.getItem() instanceof ShipCoreItem sci) {
            this.shipType = sci.getShipType();
        } else {
            this.shipType = ShipCoreItem.ShipType.SMALL;
        }

        int totalSlots = shipType.totalSlots();
        this.coreContainer = new SimpleContainer(totalSlots);

        // Load existing contents from DataComponent
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> tempItems = NonNullList.withSize(totalSlots, ItemStack.EMPTY);
        contents.copyInto(tempItems);
        for (int i = 0; i < totalSlots; i++) {
            coreContainer.setItem(i, tempItems.get(i));
        }

        // Weapon slots
        for (int i = 0; i < shipType.weaponSlots; i++) {
            addSlot(new WeaponSlot(coreContainer, i, 8 + i * 18, 20));
        }

        // Ammo slots
        for (int i = 0; i < shipType.ammoSlots; i++) {
            addSlot(new AmmoSlot(coreContainer, shipType.weaponSlots + i, 8 + i * 18, 46));
        }

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            if (col == coreSlot) {
                addSlot(new LockedSlot(playerInventory, col, 8 + col * 18, 142));
            } else {
                addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
            }
        }

        // Save on change (server only)
        if (!playerInventory.player.level().isClientSide) {
            coreContainer.addListener(c -> saveContents());
        }
    }

    private void saveContents() {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < coreContainer.getContainerSize(); i++) {
            items.add(coreContainer.getItem(i).copy());
        }
        coreStack.set(ModDataComponents.SHIP_CORE_CONTENTS.get(),
                ItemContainerContents.fromItems(items));
    }

    public int getCurrentLoad() {
        int load = 0;
        for (int i = 0; i < shipType.weaponSlots; i++) {
            load += getWeight(coreContainer.getItem(i));
        }
        return load;
    }

    public int getMaxLoad() {
        return shipType.maxLoad;
    }

    public ShipCoreItem.ShipType getShipType() {
        return shipType;
    }

    public static int getWeight(ItemStack stack) {
        if (stack.is(ModItems.SMALL_GUN.get())) return SMALL_GUN_WEIGHT;
        if (stack.is(ModItems.MEDIUM_GUN.get())) return MEDIUM_GUN_WEIGHT;
        if (stack.is(ModItems.LARGE_GUN.get())) return LARGE_GUN_WEIGHT;
        return 0;
    }

    public static boolean isGun(ItemStack stack) {
        return stack.is(ModItems.SMALL_GUN.get())
                || stack.is(ModItems.MEDIUM_GUN.get())
                || stack.is(ModItems.LARGE_GUN.get());
    }

    public static boolean isShell(ItemStack stack) {
        return stack.is(ModItems.SMALL_HE_SHELL.get())
                || stack.is(ModItems.MEDIUM_HE_SHELL.get())
                || stack.is(ModItems.LARGE_HE_SHELL.get())
                || stack.is(ModItems.SMALL_AP_SHELL.get())
                || stack.is(ModItems.MEDIUM_AP_SHELL.get())
                || stack.is(ModItems.LARGE_AP_SHELL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        ItemStack original = slotStack.copy();

        int weaponEnd = shipType.weaponSlots;
        int ammoEnd = weaponEnd + shipType.ammoSlots;
        int invEnd = ammoEnd + 27;
        int hotbarEnd = invEnd + 9;

        if (index < ammoEnd) {
            // From core container → player inventory
            if (!moveItemStackTo(slotStack, ammoEnd, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // From player inventory → core container
            if (isGun(slotStack)) {
                if (!moveItemStackTo(slotStack, 0, weaponEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (isShell(slotStack)) {
                if (!moveItemStackTo(slotStack, weaponEnd, ammoEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (slotStack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, slotStack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getInventory().getItem(coreSlot) == coreStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            saveContents();
        }
    }

    // ===== Custom Slot types =====

    private class WeaponSlot extends Slot {
        public WeaponSlot(SimpleContainer container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isGun(stack)) return false;
            int currentLoad = getCurrentLoad();
            int existingWeight = getWeight(getItem());
            int newWeight = getWeight(stack);
            return (currentLoad - existingWeight + newWeight) <= getMaxLoad();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class AmmoSlot extends Slot {
        public AmmoSlot(SimpleContainer container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isShell(stack);
        }
    }

    private static class LockedSlot extends Slot {
        public LockedSlot(Inventory inventory, int slot, int x, int y) {
            super(inventory, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
