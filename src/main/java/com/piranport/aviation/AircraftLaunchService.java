package com.piranport.aviation;

import com.piranport.component.AircraftInfo;
import com.piranport.entity.AircraftEntity;
import com.piranport.item.AircraftItem;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Shared construction boundary for autonomous aircraft.
 *
 * <p>Callers identify an aircraft by its registered item ID rather than
 * maintaining their own item or definition switch. The service resolves the
 * immutable definition once, fills the launch copy, and passes the target and
 * NPC owner through the same low-level entity constructor.</p>
 */
public final class AircraftLaunchService {
    private AircraftLaunchService() {}

    /**
     * Creates an autonomous aircraft from a stable registered item ID.
     *
     * <p>The returned entity is not added to the level. This mirrors the other
     * projectile services: the caller decides when the entity enters the
     * world, while all aircraft state initialization stays here.</p>
     */
    public static AircraftEntity createAutonomous(Level level, Vec3 spawnPosition,
                                                   ResourceLocation aircraftItemId,
                                                   @Nullable LivingEntity target,
                                                   @Nullable AbstractDeepOceanEntity aircraftOwner) {
        Objects.requireNonNull(aircraftItemId, "aircraftItemId");
        ItemStack stack = new ItemStack(resolveAircraftItem(aircraftItemId));
        return createAutonomous(level, spawnPosition, stack, target, aircraftOwner);
    }

    /** String convenience overload for command and JSON-backed callers. */
    public static AircraftEntity createAutonomous(Level level, Vec3 spawnPosition,
                                                   String aircraftItemId,
                                                   @Nullable LivingEntity target,
                                                   @Nullable AbstractDeepOceanEntity aircraftOwner) {
        return createAutonomous(level, spawnPosition, parseItemId(aircraftItemId), target, aircraftOwner);
    }

    /**
     * Creates an autonomous aircraft while preserving stack-specific data such
     * as experience-shell bonuses. The registered item ID is still resolved and
     * validated before the copy is initialized.
     */
    public static AircraftEntity createAutonomous(Level level, Vec3 spawnPosition,
                                                   ItemStack aircraftStack,
                                                   @Nullable LivingEntity target,
                                                   @Nullable AbstractDeepOceanEntity aircraftOwner) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(spawnPosition, "spawnPosition");
        validatePosition(spawnPosition);
        Objects.requireNonNull(aircraftStack, "aircraftStack");

        ResourceLocation itemId = itemIdOf(aircraftStack);
        Item registeredItem = resolveAircraftItem(itemId);
        if (aircraftStack.getItem() != registeredItem) {
            throw new IllegalArgumentException("aircraft stack item does not match its registered ID: " + itemId);
        }
        if (target != null && target.level() != level) {
            throw new IllegalArgumentException("aircraft target must be in the launch level");
        }
        if (aircraftOwner != null && aircraftOwner.level() != level) {
            throw new IllegalArgumentException("aircraft owner must be in the launch level");
        }

        ItemStack launchStack = aircraftStack.copy();
        AircraftInfo info = launchStack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (info == null) {
            throw new IllegalArgumentException("aircraft item has no AIRCRAFT_INFO component: " + itemId);
        }

        // Resolve by the registered item ID before constructing the entity. The
        // definition may be data-pack backed and can differ from legacy type
        // defaults, so autonomous aircraft must start with its effective capacity.
        AircraftDefinition definition = AircraftDefinitionService.resolve(launchStack);
        if (definition == null) {
            throw new IllegalArgumentException("aircraft item has no definition: " + itemId);
        }
        launchStack.set(ModDataComponents.AIRCRAFT_INFO.get(),
                withFullFuel(info, definition.fuelCapacity()));

        // Keep createAutonomous on AircraftEntity as the low-level field mapper;
        // this service owns the stable ID, definition and context preparation.
        return AircraftEntity.createAutonomous(level, spawnPosition, launchStack, target, aircraftOwner);
    }

    /** Resolves and validates a registered aircraft item from its stable ID. */
    public static Item resolveAircraftItem(ResourceLocation aircraftItemId) {
        Objects.requireNonNull(aircraftItemId, "aircraftItemId");
        Item item = BuiltInRegistries.ITEM.get(aircraftItemId);
        if (!(item instanceof AircraftItem)) {
            throw new IllegalArgumentException("registered item is not an aircraft: " + aircraftItemId);
        }
        return item;
    }

    /** Resolves the definition attached to a stable registered aircraft item ID. */
    public static AircraftDefinition resolveDefinition(ResourceLocation aircraftItemId) {
        ItemStack stack = new ItemStack(resolveAircraftItem(aircraftItemId));
        AircraftDefinition definition = AircraftDefinitionService.resolve(stack);
        if (definition == null) {
            throw new IllegalArgumentException("aircraft item has no definition: " + aircraftItemId);
        }
        return definition;
    }

    /** Returns the stable registered item ID carried by an aircraft stack. */
    public static ResourceLocation itemIdOf(ItemStack aircraftStack) {
        Objects.requireNonNull(aircraftStack, "aircraftStack");
        if (aircraftStack.isEmpty()) {
            throw new IllegalArgumentException("aircraft stack must not be empty");
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(aircraftStack.getItem());
        if (itemId == null) {
            throw new IllegalArgumentException("aircraft item is not registered");
        }
        return itemId;
    }

    private static ResourceLocation parseItemId(String aircraftItemId) {
        if (aircraftItemId == null || aircraftItemId.isBlank()) {
            throw new IllegalArgumentException("aircraftItemId must not be blank");
        }
        ResourceLocation parsed = ResourceLocation.tryParse(aircraftItemId.trim());
        if (parsed == null) {
            throw new IllegalArgumentException("invalid aircraft item ID: " + aircraftItemId);
        }
        return parsed;
    }

    private static void validatePosition(Vec3 position) {
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
            throw new IllegalArgumentException("spawnPosition must be finite");
        }
    }

    private static AircraftInfo withFullFuel(AircraftInfo info, int fuelCapacity) {
        return new AircraftInfo(info.aircraftType(), fuelCapacity, info.ammoCapacity(), fuelCapacity,
                info.panelDamage(), info.panelSpeed(), info.weight(), info.bombingMode(),
                info.payloadLoaded(), info.definitionId());
    }
}
