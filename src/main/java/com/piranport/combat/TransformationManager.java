package com.piranport.combat;

import com.piranport.PiranPort;
import com.piranport.item.ArmorPlateItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.SonarItem;
import com.piranport.item.TorpedoReloadItem;
import com.piranport.item.EngineItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMobEffects;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
    public static final ResourceLocation HEALTH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "ship_core_health");
    public static final ResourceLocation TOUGHNESS_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "ship_core_toughness");
    public static final ResourceLocation WATER_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "ship_core_water_speed");

    public static boolean isTransformed(ItemStack coreStack) {
        return coreStack.getOrDefault(ModDataComponents.SHIP_CORE_TRANSFORMED.get(), false);
    }

    /** Find the active transformed ship core. No-GUI mode: offhand only. GUI mode: main hand → inventory → offhand. */
    public static ItemStack findTransformedCore(net.minecraft.world.entity.player.Player player) {
        if (!com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
            // No-GUI mode: only offhand core counts
            ItemStack offhand = player.getOffhandItem();
            if (offhand.getItem() instanceof ShipCoreItem && isTransformed(offhand)) return offhand;
            return ItemStack.EMPTY;
        }
        // GUI mode: scan all slots
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof ShipCoreItem && isTransformed(mainHand)) return mainHand;
        for (ItemStack s : player.getInventory().items) {
            if (s.getItem() instanceof ShipCoreItem && isTransformed(s)) return s;
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof ShipCoreItem && isTransformed(offhand)) return offhand;
        return ItemStack.EMPTY;
    }

    /** Check if the player has any transformed ship core. */
    public static boolean isPlayerTransformed(net.minecraft.world.entity.player.Player player) {
        return !findTransformedCore(player).isEmpty();
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

        if (!com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
            // No-GUI mode: weapon is determined by what the player is holding, no cycling needed
            return;
        }

        ShipCoreItem sci = (ShipCoreItem) mainHand.getItem();
        int weaponSlots = sci.getShipType().weaponSlots;
        ItemContainerContents contents = mainHand.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(sci.getShipType().totalSlots(), ItemStack.EMPTY);
        contents.copyInto(items);

        // Validate weaponSlots against actual container size to prevent index overflow
        if (weaponSlots > items.size()) {
            com.piranport.PiranPort.LOGGER.warn("Weapon slots ({}) exceeds container size ({}) for core {}",
                    weaponSlots, items.size(), sci.getShipType());
            weaponSlots = items.size();
        }

        int current = Math.min(getWeaponIndex(mainHand), weaponSlots - 1);
        int next = -1;
        for (int i = 1; i <= weaponSlots; i++) {
            int candidate = (current + i) % weaponSlots;
            if (candidate >= 0 && candidate < items.size() && !items.get(candidate).isEmpty()) {
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
        if (stack.getItem() instanceof SonarItem) return false;
        if (stack.getItem() instanceof EngineItem) return false;
        if (stack.getItem() instanceof TorpedoReloadItem) return false;
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

        if (!com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
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
        double engineSpeedBonus = 0;
        int totalLoad = 0;

        // Weapon slots contribute to load
        for (int i = 0; i < type.weaponSlots; i++) {
            totalLoad += getItemLoad(items.get(i));
        }
        // Enhancement slots contribute to load, armor, and engine speed
        int eStart = type.weaponSlots + type.ammoSlots;
        for (int i = eStart; i < type.totalSlots(); i++) {
            ItemStack item = items.get(i);
            totalLoad += getItemLoad(item);
            if (item.getItem() instanceof ArmorPlateItem plate) {
                armorBonus += plate.getArmorBonus();
            } else if (item.getItem() instanceof EngineItem engine) {
                engineSpeedBonus += engine.getSpeedBonus();
            }
        }

        double loadRatio = type.maxLoad > 0 ? (double) totalLoad / type.maxLoad : 0;
        double speedMult = type.emptySpeed - (type.emptySpeed - type.fullLoadSpeed) * Math.min(loadRatio, 1.0);
        speedMult += engineSpeedBonus;

        removeTransformationAttributes(player);
        applyTypeAttributes(player, type, armorBonus, speedMult);
        applyOverweightPenalty(player, totalLoad, type.maxLoad);
    }

    /** Hotbar slot count (slots 0–8 in Inventory.items). */
    private static final int HOTBAR_SIZE = 9;

    /**
     * Inventory mode (GUI disabled): scan the player's inventory for load and attributes.
     * Only hotbar slots (0–8) are considered for weight calculation.
     * The ship core with the highest maxLoad provides the weight capacity.
     */
    private static void applyAttributesInventoryMode(Player player) {
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        // Find the ship core with the highest maxLoad and track its ShipType
        ShipCoreItem.ShipType bestType = null;
        int scanLimit = HOTBAR_SIZE;
        for (int i = 0; i < scanLimit; i++) {
            ItemStack stack = inv.items.get(i);
            if (stack.getItem() instanceof ShipCoreItem sci) {
                if (bestType == null || sci.getShipType().maxLoad > bestType.maxLoad) {
                    bestType = sci.getShipType();
                }
            }
        }
        // Always check offhand — no-GUI mode requires the core to be in offhand
        {
            ItemStack offhandStack = inv.offhand.get(0);
            if (offhandStack.getItem() instanceof ShipCoreItem sci) {
                if (bestType == null || sci.getShipType().maxLoad > bestType.maxLoad) {
                    bestType = sci.getShipType();
                }
            }
        }

        removeTransformationAttributes(player);
        if (bestType == null) return;

        // Armor & engine bonuses come from items stored inside the offhand core
        ItemStack offhandCore = inv.offhand.get(0);
        int armorBonus = getCoreArmorBonus(offhandCore);
        double engineSpeedBonus = getCoreEngineSpeedBonus(offhandCore);
        int armorLoad  = getCoreArmorLoad(offhandCore);
        int totalLoad  = getInventoryWeaponLoad(inv) + armorLoad;

        double loadRatio = bestType.maxLoad > 0 ? (double) totalLoad / bestType.maxLoad : 0;
        double speedMult = bestType.emptySpeed - (bestType.emptySpeed - bestType.fullLoadSpeed) * Math.min(loadRatio, 1.0);
        speedMult += engineSpeedBonus;

        applyTypeAttributes(player, bestType, armorBonus, speedMult);
        applyOverweightPenalty(player, totalLoad, bestType.maxLoad);
    }

    /**
     * Sum the load of all weapons and armor plates (not ship cores) in hotbar. Used in inventory mode.
     * Only scans hotbar slots (0–8).
     */
    public static int getInventoryWeaponLoad(net.minecraft.world.entity.player.Inventory inv) {
        int total = 0;
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack stack = inv.items.get(i);
            if (isLoadItem(stack)) total += getItemLoad(stack);
        }
        return total;
    }

    /** Sum armor bonus from ArmorPlateItems in the player's hotbar. Used in inventory mode.
     * Only scans hotbar slots (0–8). */
    public static int getInventoryArmorBonus(net.minecraft.world.entity.player.Inventory inv) {
        int total = 0;
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack stack = inv.items.get(i);
            if (stack.getItem() instanceof ArmorPlateItem plate) total += plate.getArmorBonus();
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
     * Returns the total protection level from ArmorPlateItems stored inside a ship core's
     * SHIP_CORE_ARMOR DataComponent (no-GUI mode).
     */
    public static int getCoreProtectionLevel(ItemStack coreStack) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return 0;
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> stored = NonNullList.withSize(sci.getShipType().enhancementSlots, ItemStack.EMPTY);
        contents.copyInto(stored);
        int total = 0;
        for (ItemStack s : stored) {
            if (s.getItem() instanceof ArmorPlateItem plate) total += plate.getProtectionLevel();
        }
        return total;
    }

    /**
     * Returns the total protection level from ArmorPlateItems in GUI mode
     * (read from SHIP_CORE_CONTENTS enhancement slots) or no-GUI mode (SHIP_CORE_ARMOR).
     */
    public static int getEquippedProtectionLevel(Player player, ItemStack coreStack) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return 0;
        ShipCoreItem.ShipType type = sci.getShipType();

        if (com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
            ItemContainerContents contents = coreStack.getOrDefault(
                    ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
            NonNullList<ItemStack> items = NonNullList.withSize(type.totalSlots(), ItemStack.EMPTY);
            contents.copyInto(items);
            int total = 0;
            int eStart = type.weaponSlots + type.ammoSlots;
            for (int i = eStart; i < type.totalSlots(); i++) {
                if (items.get(i).getItem() instanceof ArmorPlateItem plate) total += plate.getProtectionLevel();
            }
            return total;
        } else {
            return getCoreProtectionLevel(coreStack);
        }
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
            else if (s.getItem() instanceof SonarItem sonar) total += sonar.getWeight();
            else if (s.getItem() instanceof EngineItem engine) total += engine.getWeight();
            else if (s.getItem() instanceof TorpedoReloadItem tr) total += tr.getWeight();
        }
        return total;
    }

    /**
     * Returns the total speed bonus from EngineItems stored inside a ship core's
     * SHIP_CORE_ARMOR DataComponent (no-GUI mode).
     */
    public static double getCoreEngineSpeedBonus(ItemStack coreStack) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return 0;
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> stored = NonNullList.withSize(sci.getShipType().enhancementSlots, ItemStack.EMPTY);
        contents.copyInto(stored);
        double total = 0;
        for (ItemStack s : stored) {
            if (s.getItem() instanceof EngineItem engine) total += engine.getSpeedBonus();
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
        // Enhancement items only count load when stored inside the core (SHIP_CORE_ARMOR)
        if (stack.getItem() instanceof ArmorPlateItem) return false;
        if (stack.getItem() instanceof EngineItem) return false;
        if (stack.getItem() instanceof SonarItem) return false;
        if (stack.getItem() instanceof TorpedoReloadItem) return false;
        return getItemLoad(stack) > 0;
    }

    /**
     * Apply overweight debuffs based on how much totalLoad exceeds maxLoad.
     * cost = maxLoad - totalLoad. Negative cost means overweight.
     *   cost < 0:    (speed penalty only, no extra debuff)
     *   cost < -20:  Mining Fatigue I
     *   cost < -50:  Mining Fatigue III + Weakness II
     *   cost < -100: Mining Fatigue III + Poison II
     * Effects last 60 ticks (3s), refreshed each recalculation.
     */
    public static void applyOverweightPenalty(Player player, int totalLoad, int maxLoad) {
        if (player.level().isClientSide()) return;
        int cost = maxLoad - totalLoad;
        // Remove previous overweight debuffs if no longer overweight
        if (cost >= -20) {
            player.removeEffect(MobEffects.DIG_SLOWDOWN);
        }
        if (cost >= -50) {
            player.removeEffect(MobEffects.WEAKNESS);
            player.removeEffect(MobEffects.POISON);
        }
        if (cost >= 0) return;

        int duration = 60; // 3 seconds, refreshed each recalc
        if (cost < -100) {
            // Mining Fatigue III + Poison II
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 2, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 1, false, false, true));
            player.removeEffect(MobEffects.WEAKNESS);
        } else if (cost < -50) {
            // Mining Fatigue III + Weakness II
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 2, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 1, false, false, true));
            player.removeEffect(MobEffects.POISON);
        } else if (cost < -20) {
            // Mining Fatigue I + Weakness I
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, false, true));
            player.removeEffect(MobEffects.POISON);
        }
    }

    /** Remove overweight debuffs. Call when player un-transforms. */
    public static void removeOverweightPenalty(Player player) {
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.POISON);
    }

    /** Remove ship core attribute modifiers. Call when player un-transforms. */
    public static void removeTransformationAttributes(Player player) {
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance toughnessAttr = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        AttributeInstance waterAttr = player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (armorAttr != null) armorAttr.removeModifier(ARMOR_MODIFIER_ID);
        if (speedAttr != null) speedAttr.removeModifier(SPEED_MODIFIER_ID);
        if (healthAttr != null) healthAttr.removeModifier(HEALTH_MODIFIER_ID);
        if (toughnessAttr != null) toughnessAttr.removeModifier(TOUGHNESS_MODIFIER_ID);
        if (waterAttr != null) waterAttr.removeModifier(WATER_SPEED_MODIFIER_ID);
    }

    /**
     * Apply all type-based attribute modifiers: health, armor (base + plates), toughness, speed.
     */
    private static void applyTypeAttributes(Player player, ShipCoreItem.ShipType type,
                                             int plateArmorBonus, double speedMult) {
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        AttributeInstance toughnessAttr = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (healthAttr != null && type.healthBonus != 0) {
            healthAttr.addTransientModifier(new AttributeModifier(
                    HEALTH_MODIFIER_ID, type.healthBonus, AttributeModifier.Operation.ADD_VALUE));
        }
        int totalArmor = type.baseArmor + plateArmorBonus;
        if (armorAttr != null && totalArmor > 0) {
            armorAttr.addTransientModifier(new AttributeModifier(
                    ARMOR_MODIFIER_ID, totalArmor, AttributeModifier.Operation.ADD_VALUE));
        }
        if (toughnessAttr != null && type.armorToughness > 0) {
            toughnessAttr.addTransientModifier(new AttributeModifier(
                    TOUGHNESS_MODIFIER_ID, type.armorToughness, AttributeModifier.Operation.ADD_VALUE));
        }
        if (speedAttr != null && speedMult != 1.0) {
            speedAttr.addTransientModifier(new AttributeModifier(
                    SPEED_MODIFIER_ID, speedMult - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        // 舰娘变身态：免除水阻力。WATER_MOVEMENT_EFFICIENCY 原版 0，设 1 表示水中与陆地同速。
        AttributeInstance waterAttr = player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (waterAttr != null) {
            waterAttr.removeModifier(WATER_SPEED_MODIFIER_ID);
            waterAttr.addTransientModifier(new AttributeModifier(
                    WATER_SPEED_MODIFIER_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
        }
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

    // P3 #36: data-driven weapon load map (static initialization for thread safety)
    // IdentityHashMap is correct here: Item instances are registry singletons,
    // so reference equality (==) is both safe and faster than hashCode/equals.
    private static final java.util.Map<net.minecraft.world.item.Item, Integer> WEAPON_LOAD_MAP;

    static {
        WEAPON_LOAD_MAP = new java.util.IdentityHashMap<>();
        WEAPON_LOAD_MAP.put(ModItems.SINGLE_SMALL_GUN.get(), 4);
        WEAPON_LOAD_MAP.put(ModItems.SMALL_GUN.get(), 6);
        WEAPON_LOAD_MAP.put(ModItems.MEDIUM_GUN.get(), 16);
        WEAPON_LOAD_MAP.put(ModItems.LARGE_GUN.get(), 30);
        WEAPON_LOAD_MAP.put(ModItems.TWIN_TORPEDO_LAUNCHER.get(), 8);
        WEAPON_LOAD_MAP.put(ModItems.TRIPLE_TORPEDO_LAUNCHER.get(), 12);
        WEAPON_LOAD_MAP.put(ModItems.QUAD_TORPEDO_LAUNCHER.get(), 20);
        WEAPON_LOAD_MAP.put(ModItems.SY1_LAUNCHER.get(), 14);
        WEAPON_LOAD_MAP.put(ModItems.MK14_HARPOON_LAUNCHER.get(), 16);
        WEAPON_LOAD_MAP.put(ModItems.TERRIER_LAUNCHER.get(), 10);
        WEAPON_LOAD_MAP.put(ModItems.SHIP_ROCKET_LAUNCHER.get(), 8);
        WEAPON_LOAD_MAP.put(ModItems.SEA_DART_LAUNCHER.get(), 12);
        WEAPON_LOAD_MAP.put(ModItems.SEACAT_LAUNCHER.get(), 6);
        WEAPON_LOAD_MAP.put(ModItems.DEPTH_CHARGE_LAUNCHER.get(), 2);
        WEAPON_LOAD_MAP.put(ModItems.DEPTH_CHARGE_LAUNCHER_IMPROVED.get(), 3);
        WEAPON_LOAD_MAP.put(ModItems.DEPTH_CHARGE_LAUNCHER_ADVANCED.get(), 5);
    }

    public static int getItemLoad(ItemStack stack) {
        Integer load = WEAPON_LOAD_MAP.get(stack.getItem());
        if (load != null) return load;
        if (stack.getItem() instanceof ArmorPlateItem plate) return plate.getWeight();
        if (stack.getItem() instanceof SonarItem sonar) return sonar.getWeight();
        if (stack.getItem() instanceof EngineItem engine) return engine.getWeight();
        if (stack.getItem() instanceof TorpedoReloadItem tr) return tr.getWeight();
        if (stack.getItem() instanceof com.piranport.item.AircraftItem) {
            com.piranport.component.AircraftInfo info =
                    stack.get(ModDataComponents.AIRCRAFT_INFO.get());
            return info != null ? info.weight() : 0;
        }
        return 0;
    }

    /**
     * Check if the transformed player has a SonarItem equipped in enhancement slots.
     * Works in both GUI mode (SHIP_CORE_CONTENTS) and no-GUI mode (SHIP_CORE_ARMOR).
     */
    public static boolean hasSonarEquipped(Player player, ItemStack coreStack) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return false;
        ShipCoreItem.ShipType type = sci.getShipType();

        if (com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
            // GUI mode: check enhancement slots in SHIP_CORE_CONTENTS
            ItemContainerContents contents = coreStack.getOrDefault(
                    ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
            NonNullList<ItemStack> items = NonNullList.withSize(type.totalSlots(), ItemStack.EMPTY);
            contents.copyInto(items);
            int eStart = type.weaponSlots + type.ammoSlots;
            for (int i = eStart; i < type.totalSlots(); i++) {
                if (items.get(i).getItem() instanceof SonarItem) return true;
            }
        } else {
            // No-GUI mode: check SHIP_CORE_ARMOR storage
            ItemContainerContents contents = coreStack.getOrDefault(
                    ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
            NonNullList<ItemStack> stored = NonNullList.withSize(type.enhancementSlots, ItemStack.EMPTY);
            contents.copyInto(stored);
            for (ItemStack s : stored) {
                if (s.getItem() instanceof SonarItem) return true;
            }
        }
        return false;
    }

    /**
     * Check if the transformed player has a TorpedoReloadItem equipped in enhancement slots.
     * Works in both GUI mode (SHIP_CORE_CONTENTS) and no-GUI mode (SHIP_CORE_ARMOR).
     */
    public static boolean hasTorpedoReloadEquipped(Player player, ItemStack coreStack) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return false;
        ShipCoreItem.ShipType type = sci.getShipType();

        if (com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
            // GUI mode: check enhancement slots in SHIP_CORE_CONTENTS
            ItemContainerContents contents = coreStack.getOrDefault(
                    ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
            NonNullList<ItemStack> items = NonNullList.withSize(type.totalSlots(), ItemStack.EMPTY);
            contents.copyInto(items);
            int eStart = type.weaponSlots + type.ammoSlots;
            for (int i = eStart; i < type.totalSlots(); i++) {
                if (items.get(i).getItem() instanceof TorpedoReloadItem) return true;
            }
        } else {
            // No-GUI mode: check SHIP_CORE_ARMOR storage
            ItemContainerContents contents = coreStack.getOrDefault(
                    ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
            NonNullList<ItemStack> stored = NonNullList.withSize(type.enhancementSlots, ItemStack.EMPTY);
            contents.copyInto(stored);
            for (ItemStack s : stored) {
                if (s.getItem() instanceof TorpedoReloadItem) return true;
            }
        }
        return false;
    }
}
