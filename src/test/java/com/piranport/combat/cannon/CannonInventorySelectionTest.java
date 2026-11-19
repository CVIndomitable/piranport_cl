package com.piranport.combat.cannon;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CannonInventorySelectionTest {
    @Test
    void selectedGunIsFirstEvenWhenAnotherReadyGunHasLowerSlot() {
        assertEquals(3, CannonInventory.chooseReadySlot(36, 3, Set.of(0, 3, 8)::contains));
    }

    @Test
    void fallsBackToReadyInventoryThenOffhand() {
        assertEquals(2, CannonInventory.chooseReadySlot(36, 3, Set.of(2, 8, 40)::contains));
        assertEquals(40, CannonInventory.chooseReadySlot(36, 3, Set.of(40)::contains));
        assertEquals(-1, CannonInventory.chooseReadySlot(36, 3, i -> false));
    }

    @Test
    void heldFireCyclesAcrossAllReadyGunsAndWraps() {
        Set<Integer> ready = Set.of(1, 3, 7, 40);
        assertEquals(7, CannonInventory.chooseNextReadySlot(36, 3, ready::contains));
        assertEquals(40, CannonInventory.chooseNextReadySlot(36, 7, ready::contains));
        assertEquals(1, CannonInventory.chooseNextReadySlot(36, 40, ready::contains));
        assertEquals(3, CannonInventory.chooseNextReadySlot(36, 1, ready::contains));
        assertEquals(3, CannonInventory.chooseNextReadySlot(36, 3, Set.of(3)::contains));
        assertEquals(-1, CannonInventory.chooseNextReadySlot(36, 3, i -> false));
    }

    @Test
    @Tag("mc-bootstrap")
    void onlyAnotherMatchingCannonInInventoryOrOffhandDisablesSingleGunReloadWait() {
        SharedConstants.tryDetectVersion();
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        Bootstrap.bootStrap();
        Inventory inventory = new Inventory(null);
        var cannon = Items.BOW;
        inventory.selected = 3;
        inventory.setItem(3, new ItemStack(cannon));
        inventory.setItem(4, new ItemStack(Items.CROSSBOW));
        assertFalse(CannonInventory.hasOtherCannonOfType(inventory, cannon));

        inventory.setItem(9, new ItemStack(cannon));
        assertTrue(CannonInventory.hasOtherCannonOfType(inventory, cannon));
        inventory.setItem(9, ItemStack.EMPTY);
        inventory.setItem(40, new ItemStack(cannon));
        assertTrue(CannonInventory.hasOtherCannonOfType(inventory, cannon));
    }
}
