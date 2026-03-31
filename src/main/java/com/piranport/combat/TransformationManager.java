package com.piranport.combat;

import com.piranport.PiranPort;
import com.piranport.item.ArmorPlateItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMobEffects;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public class TransformationManager {

    public static final ResourceLocation ARMOR_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "ship_core_armor");
    public static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "ship_core_speed");

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
        if (!(mainHand.getItem() instanceof ShipCoreItem)) return;
        if (!isTransformed(mainHand)) return;

        if (!com.piranport.config.ModCommonConfig.SHIP_CORE_GUI_ENABLED.get()) {
            // No-GUI mode: weapon is determined by what the player is holding, no cycling needed
            return;
        }

        ShipCoreItem sci = (ShipCoreItem) mainHand.getItem();
        int weaponSlots = sci.getShipType().weaponSlots;
        ItemContainerContents contents = mainHand.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(sci.getShipType().totalSlots(), ItemStack.EMPTY);
        contents.copyInto(items);

        int current = getWeaponIndex(mainHand);
        int next = -1;
        for (int i = 1; i <= weaponSlots; i++) {
            int candidate = (current + i) % weaponSlots;
            if (candidate < items.size() && !items.get(candidate).isEmpty()) {
                next = candidate;
                break;
            }
        }
        if (next == -1) return;

        setWeaponIndex(mainHand, next);
        player.displayClientMessage(
                Component.translatable("message.piranport.weapon_selected", items.get(next).getHoverName()), true);
    }

    /** Returns true for items that can be fired/launched (guns, torpedo launchers, aircraft — not cores, armor, ammo). */
    public static boolean isFireableWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ShipCoreItem) return false;
        if (stack.getItem() instanceof ArmorPlateItem) return false;
        // Torpedo ammo, shells, and aviation consumables all have getItemLoad == 0, so excluded naturally
        return getItemLoad(stack) > 0;
    }

    /**
     * Apply armor and speed attribute modifiers based on current ship core equipment.
     * Call when player transforms or when GUI closes (to recalculate after changes).
     *
     * When SHIP_CORE_GUI_ENABLED is false: the ship core with the highest maxLoad in the
     * player's inventory provides the capacity; all weapon items in the inventory consume load.
     * When SHIP_CORE_GUI_ENABLED is true: reads weapons/armor from the core's ItemContainerContents.
     */
    public static void applyTransformationAttributes(Player player, ItemStack coreStack) {
        if (player.level().isClientSide()) return;
        if (!(coreStack.getItem() instanceof ShipCoreItem)) return;

        if (!com.piranport.config.ModCommonConfig.SHIP_CORE_GUI_ENABLED.get()) {
            applyAttributesInventoryMode(player);
            return;
        }

        // GUI mode: read equipment from the core's container slots
        ShipCoreItem sci = (ShipCoreItem) coreStack.getItem();
        ShipCoreItem.ShipType type = sci.getShipType();
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(type.totalSlots(), ItemStack.EMPTY);
        contents.copyInto(items);

        int armorBonus = 0;
        int totalLoad = 0;

        // Weapon slots contribute to load
        for (int i = 0; i < type.weaponSlots; i++) {
            totalLoad += getItemLoad(items.get(i));
        }
        // Enhancement slots contribute to both load and armor
        int eStart = type.weaponSlots + type.ammoSlots;
        for (int i = eStart; i < type.totalSlots(); i++) {
            ItemStack item = items.get(i);
            totalLoad += getItemLoad(item);
            if (item.getItem() instanceof ArmorPlateItem plate) {
                armorBonus += plate.getArmorBonus();
            }
        }

        double speedMult = Math.max(0.4, 1.0 - ((double) totalLoad / type.maxLoad) * 0.6);

        removeTransformationAttributes(player);

        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (armorAttr != null && armorBonus > 0) {
            armorAttr.addTransientModifier(new AttributeModifier(
                    ARMOR_MODIFIER_ID, armorBonus, AttributeModifier.Operation.ADD_VALUE));
        }
        if (speedAttr != null && speedMult < 1.0) {
            speedAttr.addTransientModifier(new AttributeModifier(
                    SPEED_MODIFIER_ID, speedMult - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** Hotbar slot count (slots 0–8 in Inventory.items). */
    private static final int HOTBAR_SIZE = 9;

    /**
     * Inventory mode (GUI disabled): scan the player's inventory for load and attributes.
     * When hotbarOnlyLoad=true, only hotbar slots (0–8) are considered.
     * When hotbarOnlyLoad=false (default), the entire inventory is scanned.
     * The ship core with the highest maxLoad provides the weight capacity.
     */
    private static void applyAttributesInventoryMode(Player player) {
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        boolean hotbarOnly = com.piranport.config.ModCommonConfig.HOTBAR_ONLY_LOAD.get();

        // Find the ship core with the highest maxLoad
        int maxLoad = 0;
        int scanLimit = hotbarOnly ? HOTBAR_SIZE : inv.items.size();
        for (int i = 0; i < scanLimit; i++) {
            ItemStack stack = inv.items.get(i);
            if (stack.getItem() instanceof ShipCoreItem sci) {
                maxLoad = Math.max(maxLoad, sci.getShipType().maxLoad);
            }
        }
        // Always check offhand — no-GUI mode requires the core to be in offhand
        {
            ItemStack offhandStack = inv.offhand.get(0);
            if (offhandStack.getItem() instanceof ShipCoreItem sci) {
                maxLoad = Math.max(maxLoad, sci.getShipType().maxLoad);
            }
        }

        removeTransformationAttributes(player);
        if (maxLoad == 0) return;

        long _t = System.nanoTime();
        // Armor comes from plates stored inside the offhand core, not from inventory scan
        ItemStack offhandCore = inv.offhand.get(0);
        int armorBonus = getCoreArmorBonus(offhandCore);
        int armorLoad  = getCoreArmorLoad(offhandCore);
        int totalLoad  = getInventoryWeaponLoad(inv) + armorLoad;
        com.piranport.debug.PiranPortDebug.perf("WeightScan", System.nanoTime() - _t,
                "player=" + player.getName().getString() + " load=" + totalLoad + "/" + maxLoad + " armor=" + armorBonus);

        double speedMult = Math.max(0.4, 1.0 - ((double) totalLoad / maxLoad) * 0.6);

        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (armorAttr != null && armorBonus > 0) {
            armorAttr.addTransientModifier(new AttributeModifier(
                    ARMOR_MODIFIER_ID, armorBonus, AttributeModifier.Operation.ADD_VALUE));
        }
        if (speedAttr != null && speedMult < 1.0) {
            speedAttr.addTransientModifier(new AttributeModifier(
                    SPEED_MODIFIER_ID, speedMult - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /**
     * Sum the load of all weapons and armor plates (not ship cores) in inventory. Used in inventory mode.
     * Respects hotbarOnlyLoad config: when true, only scans hotbar slots (0–8).
     */
    public static int getInventoryWeaponLoad(net.minecraft.world.entity.player.Inventory inv) {
        int total = 0;
        boolean hotbarOnly = com.piranport.config.ModCommonConfig.HOTBAR_ONLY_LOAD.get();
        int limit = hotbarOnly ? HOTBAR_SIZE : inv.items.size();
        for (int i = 0; i < limit; i++) {
            ItemStack stack = inv.items.get(i);
            if (isLoadItem(stack)) total += getItemLoad(stack);
        }
        if (!hotbarOnly) {
            ItemStack offhand = inv.offhand.get(0);
            if (isLoadItem(offhand)) total += getItemLoad(offhand);
        }
        return total;
    }

    /** Sum armor bonus from ArmorPlateItems in the player's inventory. Used in inventory mode.
     * Respects hotbarOnlyLoad config: when true, only scans hotbar slots (0–8). */
    public static int getInventoryArmorBonus(net.minecraft.world.entity.player.Inventory inv) {
        int total = 0;
        boolean hotbarOnly = com.piranport.config.ModCommonConfig.HOTBAR_ONLY_LOAD.get();
        int limit = hotbarOnly ? HOTBAR_SIZE : inv.items.size();
        for (int i = 0; i < limit; i++) {
            ItemStack stack = inv.items.get(i);
            if (stack.getItem() instanceof ArmorPlateItem plate) total += plate.getArmorBonus();
        }
        if (!hotbarOnly) {
            ItemStack offhand = inv.offhand.get(0);
            if (offhand.getItem() instanceof ArmorPlateItem plate) total += plate.getArmorBonus();
        }
        return total;
    }

    /**
     * Returns the total armor bonus from ArmorPlateItems stored inside a ship core's
     * SHIP_CORE_ARMOR DataComponent (no-GUI mode).
     */
    public static int getCoreArmorBonus(ItemStack coreStack) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return 0;
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> stored = NonNullList.withSize(sci.getShipType().enhancementSlots, ItemStack.EMPTY);
        contents.copyInto(stored);
        int total = 0;
        for (ItemStack s : stored) {
            if (s.getItem() instanceof ArmorPlateItem plate) total += plate.getArmorBonus();
        }
        return total;
    }

    /**
     * Returns the total load contributed by ArmorPlateItems stored inside a ship core's
     * SHIP_CORE_ARMOR DataComponent (no-GUI mode).
     */
    public static int getCoreArmorLoad(ItemStack coreStack) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return 0;
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> stored = NonNullList.withSize(sci.getShipType().enhancementSlots, ItemStack.EMPTY);
        contents.copyInto(stored);
        int total = 0;
        for (ItemStack s : stored) {
            if (s.getItem() instanceof ArmorPlateItem plate) total += plate.getWeight();
        }
        return total;
    }

    /**
     * Returns true for items that consume load in inventory mode (weapons only, not ship cores).
     * ArmorPlateItems are excluded here — they must be stored inside the ship core to count.
     */
    private static boolean isLoadItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ShipCoreItem) return false;
        if (stack.getItem() instanceof ArmorPlateItem) return false;
        return getItemLoad(stack) > 0;
    }

    /** Remove ship core attribute modifiers. Call when player un-transforms. */
    public static void removeTransformationAttributes(Player player) {
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (armorAttr != null) armorAttr.removeModifier(ARMOR_MODIFIER_ID);
        if (speedAttr != null) speedAttr.removeModifier(SPEED_MODIFIER_ID);
    }

    /**
     * Phase 26: Apply RELOAD_BOOST effect to a base cooldown duration.
     *   Level I  (amplifier 0) → cooldown ÷ 2  (2× reload speed)
     *   Level II (amplifier 1) → cooldown ÷ 3  (3× reload speed)
     * Only effective while the player is transformed (checked at call sites in ShipCoreItem).
     */
    public static int boostedCooldown(Player player, int baseTicks) {
        var effect = player.getEffect(ModMobEffects.RELOAD_BOOST);
        if (effect == null) return baseTicks;
        int divisor = effect.getAmplifier() + 2; // I=÷2, II=÷3
        return Math.max(1, baseTicks / divisor);
    }

    public static int getItemLoad(ItemStack stack) {
        if (stack.is(ModItems.SMALL_GUN.get())) return 6;
        if (stack.is(ModItems.MEDIUM_GUN.get())) return 16;
        if (stack.is(ModItems.LARGE_GUN.get())) return 30;
        if (stack.is(ModItems.TWIN_TORPEDO_LAUNCHER.get())) return 8;
        if (stack.is(ModItems.TRIPLE_TORPEDO_LAUNCHER.get())) return 12;
        if (stack.is(ModItems.QUAD_TORPEDO_LAUNCHER.get())) return 20;
        if (stack.getItem() instanceof ArmorPlateItem plate) return plate.getWeight();
        if (stack.getItem() instanceof com.piranport.item.AircraftItem) {
            com.piranport.component.AircraftInfo info =
                    stack.get(ModDataComponents.AIRCRAFT_INFO.get());
            return info != null ? info.weight() : 0;
        }
        return 0;
    }
}
