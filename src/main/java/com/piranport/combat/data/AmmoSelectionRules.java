package com.piranport.combat.data;

import com.piranport.ammo.CreativeAmmoCache;
import com.piranport.ammo.CreativeAmmoCatalog;
import com.piranport.artillery.ArtilleryItem;
import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.AircraftDefinitionService;
import com.piranport.item.AircraftItem;
import com.piranport.item.TorpedoItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.item.ShipCoreCombat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Weapon-specific candidate and possession rules shared by the ammo wheel and server validation. */
public final class AmmoSelectionRules {
    private AmmoSelectionRules() {}

    public static boolean supports(ItemStack weapon) {
        if (weapon.isEmpty()) return false;
        if (weapon.getItem() instanceof ArtilleryItem || weapon.getItem() instanceof TorpedoLauncherItem) return true;
        if (!(weapon.getItem() instanceof AircraftItem)) return false;
        AircraftDefinition definition = AircraftDefinitionService.resolve(weapon);
        return definition != null && definition.attackProfile() == AircraftDefinition.AttackProfile.LEVEL_BOMB;
    }

    public static boolean matches(ItemStack weapon, Item ammo, Player player) {
        if (weapon.getItem() instanceof ArtilleryItem) {
            return ShipCoreCombat.matchesCaliber(new ItemStack(ammo), weapon, player.level());
        }
        if (weapon.getItem() instanceof TorpedoLauncherItem launcher) {
            return ammo instanceof TorpedoItem torpedo && torpedo.getCaliber() == launcher.getCaliber();
        }
        if (weapon.getItem() instanceof AircraftItem) {
            AircraftDefinition definition = AircraftDefinitionService.resolve(weapon);
            if (definition == null || definition.attackProfile() != AircraftDefinition.AttackProfile.LEVEL_BOMB) return false;
            if (ammo == com.piranport.registry.ModItems.AERIAL_BOMB.get()) return true;
            return ammo == com.piranport.registry.ModItems.NUCLEAR_AERIAL_BOMB.get()
                    && definition.canCarryNuclearBomb();
        }
        return false;
    }

    public static List<Item> available(Player player, ItemStack weapon) {
        List<Item> result = new ArrayList<>();
        if (!supports(weapon)) return result;

        boolean creative = player.getAbilities().instabuild;
        if (creative) {
            for (Item item : BuiltInRegistries.ITEM) {
                if (matches(weapon, item, player)) result.add(item);
            }
            return result;
        }

        for (ItemStack stack : player.getInventory().items) {
            addIfAvailable(result, player, weapon, stack);
        }
        addIfAvailable(result, player, weapon, player.getInventory().offhand.get(0));
        for (Item item : CreativeAmmoCatalog.allItems()) {
            if (CreativeAmmoCache.isSelected(player, item) && matches(weapon, item, player)
                    && !result.contains(item)) result.add(item);
        }
        return result;
    }

    public static boolean hasAvailable(Player player, ItemStack weapon, Item ammo) {
        if (!matches(weapon, ammo, player)) return false;
        if (player.getAbilities().instabuild || CreativeAmmoCache.isInfinite(player, ammo)) return true;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == ammo) return true;
        }
        ItemStack offhand = player.getInventory().offhand.get(0);
        return !offhand.isEmpty() && offhand.getItem() == ammo;
    }

    private static void addIfAvailable(List<Item> result, Player player, ItemStack weapon, ItemStack stack) {
        if (!stack.isEmpty() && matches(weapon, stack.getItem(), player) && !result.contains(stack.getItem())) {
            result.add(stack.getItem());
        }
    }
}
