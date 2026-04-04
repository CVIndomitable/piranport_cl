package com.piranport.menu;

import com.piranport.component.FlightGroupData;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModMenuTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public class FlightGroupMenu extends AbstractContainerMenu {

    private final int coreSlot;
    private final ItemStack coreStack;
    private FlightGroupData groupData;
    private final List<ItemStack> weaponItems;
    private final int weaponSlotCount;

    public static FlightGroupMenu fromNetwork(int containerId, Inventory playerInventory,
                                              FriendlyByteBuf buf) {
        return new FlightGroupMenu(containerId, playerInventory, buf.readVarInt());
    }

    public FlightGroupMenu(int containerId, Inventory playerInventory, int coreSlot) {
        super(ModMenuTypes.FLIGHT_GROUP_MENU.get(), containerId);
        this.coreSlot = coreSlot;
        this.coreStack = playerInventory.getItem(coreSlot);

        // Load FlightGroupData from DataComponent
        this.groupData = coreStack.getOrDefault(
                ModDataComponents.FLIGHT_GROUP_DATA.get(), FlightGroupData.empty());

        // Determine weapon slot count and load weapon items
        ShipCoreItem.ShipType shipType = ShipCoreItem.ShipType.SMALL;
        if (coreStack.getItem() instanceof ShipCoreItem sci) {
            shipType = sci.getShipType();
        }
        this.weaponSlotCount = shipType.weaponSlots;

        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> allItems = NonNullList.withSize(shipType.totalSlots(), ItemStack.EMPTY);
        contents.copyInto(allItems);

        this.weaponItems = new ArrayList<>();
        for (int i = 0; i < weaponSlotCount; i++) {
            weaponItems.add(allItems.get(i).copy());
        }

        // Player inventory slots placed off-screen at (-2000, -2000).
        // AbstractContainerMenu requires player inventory slots to exist for quickMoveStack()
        // and shift-click behavior, but FlightGroupMenu uses C2S payloads instead of slot
        // interaction. Off-screen placement prevents rendering and accidental mouse interaction.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + col, -2000, -2000));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, -2000, -2000));
        }
    }

    public int getCoreSlot() {
        return coreSlot;
    }

    public FlightGroupData getGroupData() {
        return groupData;
    }

    public void setGroupData(FlightGroupData data) {
        this.groupData = data;
    }

    public List<ItemStack> getWeaponItems() {
        return weaponItems;
    }

    public int getWeaponSlotCount() {
        return weaponSlotCount;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getInventory().getItem(coreSlot) == coreStack;
    }
}
