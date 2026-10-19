package com.piranport.aviation;

import com.piranport.component.AircraftState;
import com.piranport.entity.AircraftEntity;
import com.piranport.item.AircraftItem;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.npc.deepocean.DeepOceanArmoredCarrierEntity;
import com.piranport.npc.deepocean.DeepOceanBossEntity;
import com.piranport.npc.deepocean.DeepOceanLightCarrierEntity;
import com.piranport.npc.deepocean.DeepOceanShinanoEntity;
import com.piranport.npc.deepocean.DeepOceanTaihouEntity;
import com.piranport.npc.deepocean.DeepOceanZuikakuEntity;
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
     * Select and launch one autonomous aircraft for a deep-ocean carrier.
     * Carrier loadout policy and spawn geometry live here so entity subclasses
     * only manage deck capacity and the returned aircraft reference.
     */
    @Nullable
    public static AircraftEntity launchCarrier(AbstractDeepOceanEntity carrier, int aircraftIndex) {
        Objects.requireNonNull(carrier, "carrier");
        Level level = carrier.level();
        if (level.isClientSide() || carrier.getMaxAircraft() <= 0
                || aircraftIndex < 0 || aircraftIndex >= carrier.getMaxAircraft()) {
            return null;
        }

        String itemId = carrierAircraftItemId(carrier, aircraftIndex, carrier.getMaxAircraft());
        LivingEntity target = carrier.getTarget();
        // 目标在本 tick 已死但 selector 未清理时，target 仍是死引用——置 null 让飞机以无目标起飞。
        if (target != null && !target.isAlive()) target = null;
        Vec3 spawnPosition = carrierSpawnPosition(carrier, target);
        AircraftEntity aircraft = createAutonomous(level, spawnPosition, itemId, target, carrier);
        level.addFreshEntity(aircraft);
        return aircraft;
    }

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
        AircraftState state = AircraftDefinitionService.stateOf(launchStack);

        // 自主飞机按定义满油起飞；定义必须存在，缺失视为数据错误。
        AircraftDefinition definition = AircraftDefinitionService.resolve(launchStack);
        if (definition == null) {
            throw new IllegalArgumentException("aircraft item has no definition: " + itemId);
        }
        launchStack.set(ModDataComponents.AIRCRAFT_STATE.get(),
                withFullFuel(state, AircraftStatsService.resolve(definition)));

        // Keep createAutonomous on AircraftEntity as the low-level field mapper;
        // this service owns the stable ID, definition and context preparation.
        return AircraftEntity.createAutonomous(level, spawnPosition, launchStack, target, aircraftOwner);
    }

    /**
     * 深海航母编成（《深海/01》第六章 2026-10-19）：
     * 轻母 1 战 1 雷；高级航母 2 战 2 轰 1 雷；装甲航母 1 战 2 轰 1 雷；
     * 大凤 3 战 2 轰 2 雷；信浓 1 战 3 轰 1 雷；瑞鹤 2 战 3 轰 3 雷；
     * 旗舰 1 战 1 轰（甲板容量大于 2 时循环）。
     * 低级编队用 1 型，高级编队用 2 型。
     */
    private static String carrierAircraftItemId(AbstractDeepOceanEntity carrier, int aircraftIndex, int maxAircraft) {
        final String f1 = "piranport:deep_sea_fighter_1";
        final String f2 = "piranport:deep_sea_fighter_2";
        final String b1 = "piranport:deep_sea_bomber_1";
        final String b2 = "piranport:deep_sea_bomber_2";
        final String t1 = "piranport:deep_sea_torpedo_bomber_1";
        final String t2 = "piranport:deep_sea_torpedo_bomber_2";

        if (carrier instanceof DeepOceanLightCarrierEntity) {
            return aircraftIndex % 2 == 0 ? f1 : t1;
        }
        if (carrier instanceof DeepOceanArmoredCarrierEntity) {
            return switch (aircraftIndex % 4) {
                case 0 -> f2;
                case 1, 2 -> b2;
                default -> t2;
            };
        }
        if (carrier instanceof DeepOceanTaihouEntity) {
            return switch (aircraftIndex % 7) {
                case 0, 1, 2 -> f2;
                case 3, 4 -> b2;
                default -> t2;
            };
        }
        if (carrier instanceof DeepOceanShinanoEntity) {
            return switch (aircraftIndex % 5) {
                case 0 -> f2;
                case 1, 2, 3 -> b2;
                default -> t2;
            };
        }
        if (carrier instanceof DeepOceanZuikakuEntity) {
            return switch (aircraftIndex % 8) {
                case 0, 1 -> f2;
                case 2, 3, 4 -> b2;
                default -> t2;
            };
        }
        if (carrier instanceof DeepOceanBossEntity) {
            return aircraftIndex % 2 == 0 ? f2 : b2;
        }
        // 普通航母（高级）：2 战 2 轰 1 雷。
        return switch (aircraftIndex % 5) {
            case 0, 1 -> f2;
            case 2, 3 -> b2;
            default -> t2;
        };
    }

    private static Vec3 carrierSpawnPosition(AbstractDeepOceanEntity carrier, @Nullable LivingEntity target) {
        if (target != null && target.isAlive()) {
            double dx = target.getX() - carrier.getX();
            double dz = target.getZ() - carrier.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0.01) {
                return new Vec3(carrier.getX() + dx / distance * 1.5,
                        carrier.getY() + 3.0,
                        carrier.getZ() + dz / distance * 1.5);
            }
        }
        return new Vec3(carrier.getX(), carrier.getY() + 3.0, carrier.getZ() + 1.5);
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

    private static AircraftState withFullFuel(AircraftState state, ResolvedAircraftStats stats) {
        return state.withCurrentFuel(stats.fuelCapacity());
    }
}
