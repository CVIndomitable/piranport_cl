package com.piranport.combat.data;

import com.piranport.component.CreativeAmmoSelection;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import com.piranport.testtools.PiranPortTestTools;
import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@Tag("mc-bootstrap")
class AmmoConsumptionTest {
    @BeforeAll
    static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        Bootstrap.bootStrap();
        bind(ModDataComponents.DATA_COMPONENTS, ModDataComponents.CREATIVE_AMMO_SELECTION);
        bind(ModItems.ITEMS, ModItems.CREATIVE_AMMO_CACHE);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bind(DeferredRegister<?> deferred, DeferredHolder<?, ?> target) throws Exception {
        Registry registry = BuiltInRegistries.REGISTRY.get(deferred.getRegistryKey().location());
        if (registry.containsKey(target.getId())) return;
        if (registry instanceof MappedRegistry<?> mapped) mapped.unfreeze();
        Field field = DeferredRegister.class.getDeclaredField("entries");
        field.setAccessible(true);
        Map<DeferredHolder<?, ?>, Supplier<?>> entries =
                (Map<DeferredHolder<?, ?>, Supplier<?>>) field.get(deferred);
        Registry.register(registry, target.getId(), entries.get(target).get());
    }

    @AfterEach
    void closeTestMode() { PiranPortTestTools.closeAll(); }

    @Test
    void consumesAcrossStacksAndRejectsInsufficientAmmoWithoutPartialLoss() {
        Inventory inventory = new Inventory(null);
        inventory.setItem(0, new ItemStack(Items.IRON_INGOT, 2));
        inventory.setItem(1, new ItemStack(Items.IRON_INGOT, 4));
        AmmoInventory ammo = new AmmoInventory(inventory, 40, 8);
        assertTrue(ammo.consumeAmmo(UUID.randomUUID(), Items.IRON_INGOT, 3));
        assertTrue(inventory.getItem(0).isEmpty());
        assertEquals(3, inventory.getItem(1).getCount());
        assertFalse(ammo.consumeAmmo(UUID.randomUUID(), Items.IRON_INGOT, 4));
        assertEquals(3, inventory.getItem(1).getCount());
        assertTrue(ammo.consumeAmmo(UUID.randomUUID(), Items.IRON_INGOT, 3));
        assertTrue(inventory.getItem(1).isEmpty());
    }

    @Test
    void testModeOwnerStillConsumesItems() {
        UUID owner = UUID.randomUUID();
        assertEquals(PiranPortTestTools.ToggleResult.OPENED, PiranPortTestTools.toggleFor(owner, true));
        Inventory inventory = new Inventory(null);
        inventory.setItem(0, new ItemStack(Items.IRON_INGOT, 4));
        assertTrue(new AmmoInventory(inventory, 40, 8).consumeAmmo(owner, Items.IRON_INGOT, 3));
        assertEquals(1, inventory.getItem(0).getCount());
        ItemStack fuel = new ItemStack(Items.COAL, 2);
        PiranPortTestTools.consumeAmmo(owner, fuel, 1);
        assertEquals(1, fuel.getCount());
    }

    @Test
    void explicitlySelectedCreativeCacheRemainsInfinite() {
        Inventory inventory = new Inventory(null);
        inventory.setItem(0, new ItemStack(Items.IRON_INGOT, 4));
        ItemStack cache = new ItemStack(ModItems.CREATIVE_AMMO_CACHE.get());
        cache.set(ModDataComponents.CREATIVE_AMMO_SELECTION.get(),
                new CreativeAmmoSelection(List.of("minecraft:iron_ingot")));
        inventory.setItem(1, cache);
        assertTrue(new AmmoInventory(inventory, 40, 8).consumeAmmo(UUID.randomUUID(), Items.IRON_INGOT, 3));
        assertEquals(4, inventory.getItem(0).getCount());
    }

    @Test
    void cannonReloadDoesNotBypassInventoryForCreativePlayers() throws Exception {
        String reload = Files.readString(Path.of("src/main/java/com/piranport/combat/cannon/CannonReloading.java"));
        String selection = Files.readString(Path.of("src/main/java/com/piranport/combat/data/AmmoSelectionRules.java"));
        assertFalse(reload.contains("instabuild"));
        assertTrue(reload.contains("!ammo.consumeAmmo(player.getUUID(), type, barrels)"));
        assertTrue(selection.contains("player.getAbilities().instabuild && !(weapon.getItem() instanceof ArtilleryItem)"));
    }
}
