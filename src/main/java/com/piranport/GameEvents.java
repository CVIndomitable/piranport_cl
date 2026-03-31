package com.piranport;

import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import com.piranport.combat.TransformationManager;
import com.piranport.config.ModCommonConfig;
import com.piranport.entity.AircraftEntity;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.common.util.TriState;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class GameEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // No-GUI mode: detect inventory weapon-load changes and recalculate attributes.
        // The scan is O(36) per tick but attribute recalculation only happens when the value changes.
        if (!player.level().isClientSide()
                && !com.piranport.config.ModCommonConfig.SHIP_CORE_GUI_ENABLED.get()) {
            tickInventoryLoadCheck(player);
        }

        // Find a transformed ship core — main hand first, then full inventory in no-GUI mode
        ItemStack mainHand = player.getMainHandItem();
        boolean transformed = mainHand.getItem() instanceof ShipCoreItem
                && TransformationManager.isTransformed(mainHand);
        if (!transformed && !com.piranport.config.ModCommonConfig.SHIP_CORE_GUI_ENABLED.get()) {
            net.minecraft.world.entity.player.Inventory inv = player.getInventory();
            for (ItemStack stack : inv.items) {
                if (stack.getItem() instanceof ShipCoreItem
                        && TransformationManager.isTransformed(stack)) {
                    transformed = true;
                    break;
                }
            }
            if (!transformed) {
                ItemStack offhand = inv.offhand.get(0);
                if (offhand.getItem() instanceof ShipCoreItem
                        && TransformationManager.isTransformed(offhand)) {
                    transformed = true;
                }
            }
        }
        if (!transformed) return;

        // 水面行走：脚踩水但眼睛未入水时，取消下沉速度
        if (player.isInWater() && !player.isEyeInFluid(FluidTags.WATER)) {
            Vec3 vel = player.getDeltaMovement();
            if (vel.y < 0) {
                player.setDeltaMovement(vel.x, 0.0, vel.z);
            }
            player.resetFallDistance();
        }

        // 战斗机自动升空 — server only, checked every 40 ticks (2 s)
        if (!player.level().isClientSide() && player.tickCount % 40 == 0) {
            tickAutoLaunchFighters(player, mainHand, player.getInventory().selected);
        }
    }

    /**
     * Checks whether the inventory weapon load (or best core capacity) has changed since last tick.
     * Only calls applyTransformationAttributes when the value actually differs — avoiding the cost
     * of re-applying attribute modifiers on every tick.
     */
    private static void tickInventoryLoadCheck(Player player) {
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();

        // Find a transformed ShipCoreItem anywhere in the inventory
        ItemStack transformedCore = null;
        for (ItemStack stack : inv.items) {
            if (stack.getItem() instanceof ShipCoreItem
                    && TransformationManager.isTransformed(stack)) {
                transformedCore = stack;
                break;
            }
        }
        if (transformedCore == null) {
            ItemStack offhand = inv.offhand.get(0);
            if (offhand.getItem() instanceof ShipCoreItem
                    && TransformationManager.isTransformed(offhand)) {
                transformedCore = offhand;
            }
        }

        if (transformedCore == null) {
            lastWeaponLoad.remove(player.getUUID());
            return;
        }

        // Cache key encodes both weapon load and the best-core capacity so that picking up
        // a larger ship core also triggers a recalculation even without weapon changes.
        int weaponLoad = TransformationManager.getInventoryWeaponLoad(inv);
        int maxLoad = 0;
        for (ItemStack stack : inv.items) {
            if (stack.getItem() instanceof ShipCoreItem sci) {
                maxLoad = Math.max(maxLoad, sci.getShipType().maxLoad);
            }
        }
        if (inv.offhand.get(0).getItem() instanceof ShipCoreItem sci) {
            maxLoad = Math.max(maxLoad, sci.getShipType().maxLoad);
        }

        int cacheKey = weaponLoad * 1000 + maxLoad;
        Integer cached = lastWeaponLoad.get(player.getUUID());
        if (cached == null || cached != cacheKey) {
            lastWeaponLoad.put(player.getUUID(), cacheKey);
            TransformationManager.applyTransformationAttributes(player, transformedCore);
        }
    }

    /**
     * Auto-launch fighters when hostile flying mobs are nearby.
     * Locks the nearest hostile flying mob as a fire control target and launches a fighter.
     * "Hostile flying mob" = Monster that is a FlyingMob, Phantom, or Vex.
     * If ammo/fuel is exhausted the fighter returns, refuels, and re-launches on the next check.
     */
    private static void tickAutoLaunchFighters(Player player, ItemStack coreStack, int coreSlot) {
        if (!coreStack.getOrDefault(ModDataComponents.SHIP_AUTO_LAUNCH.get(), false)) return;

        // Find hostile flying mobs within 64 blocks
        List<LivingEntity> flyingHostiles = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(64.0),
                e -> e.isAlive() && e instanceof Monster
                        && (e instanceof FlyingMob || e instanceof Phantom || e instanceof Vex)
        );
        if (flyingHostiles.isEmpty()) return;

        // Lock the nearest target only when there are no currently-alive fire control targets
        if (player.level() instanceof ServerLevel sl) {
            List<UUID> currentLocks = FireControlManager.getTargets(player.getUUID());
            boolean hasActiveLock = currentLocks.stream().anyMatch(uuid -> {
                Entity e = sl.getEntity(uuid);
                return e != null && e.isAlive();
            });

            if (!hasActiveLock) {
                LivingEntity nearest = flyingHostiles.stream()
                        .min(Comparator.comparingDouble(player::distanceTo))
                        .orElse(null);
                if (nearest != null) {
                    FireControlManager.lock(player.getUUID(), nearest.getUUID());
                }
            }
        }

        ShipCoreItem.tryAutoLaunchFighter(player.level(), player, coreStack, coreSlot);
    }

    // Cache of last-known weapon load per player (for no-GUI inventory mode).
    // Only recalculate attributes when the value actually changes.
    private static final java.util.Map<UUID, Integer> lastWeaponLoad =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** When a player dies, recall all their airborne aircraft. */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        recallAircraftForPlayer(player);
    }

    /** When a player logs out, recall all their airborne aircraft and clear cached load. */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        lastWeaponLoad.remove(player.getUUID());
        recallAircraftForPlayer(player);
    }

    private static void recallAircraftForPlayer(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        UUID ownerUUID = player.getUUID();
        AABB searchBox = AABB.ofSize(player.position(), 200, 200, 200);
        List<AircraftEntity> aircraft = serverLevel.getEntitiesOfClass(
                AircraftEntity.class, searchBox,
                a -> ownerUUID.equals(a.getOwnerUUID()));
        for (AircraftEntity a : aircraft) {
            a.recallAndRemove();
        }
        FireControlManager.clearTargets(ownerUUID);
        ReconManager.endRecon(ownerUUID);
    }

    /**
     * Redirect weapon item pickups to main inventory (slots 9–35) instead of hotbar.
     * Enabled by the weaponPickupToInventory config flag.
     * Falls back to vanilla (hotbar-first) when main inventory is full.
     */
    @SubscribeEvent
    public static void onWeaponPickup(ItemEntityPickupEvent.Pre event) {
        if (!ModCommonConfig.WEAPON_PICKUP_TO_INVENTORY.get()) return;
        Player player = event.getPlayer();
        ItemEntity itemEntity = event.getItemEntity();
        ItemStack stack = itemEntity.getItem();
        if (TransformationManager.getItemLoad(stack) <= 0) return;

        Inventory inv = player.getInventory();
        int total = stack.getCount();
        int taken = 0;

        // Phase 1: merge with existing partial stacks in main inventory (slots 9–35)
        for (int i = 9; i < 36 && taken < total; i++) {
            ItemStack existing = inv.getItem(i);
            if (ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int add = Math.min(space, total - taken);
                existing.grow(add);
                taken += add;
            }
        }

        // Phase 2: place remainder in empty main inventory slots (slots 9–35)
        for (int i = 9; i < 36 && taken < total; i++) {
            if (inv.getItem(i).isEmpty()) {
                int add = Math.min(stack.getMaxStackSize(), total - taken);
                inv.setItem(i, stack.copyWithCount(add));
                taken += add;
            }
        }

        if (taken == 0) return; // main inventory full, fall back to vanilla (hotbar)

        // Award pickup stats
        if (player instanceof ServerPlayer sp) {
            sp.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), taken);
        }

        // Shrink / discard the item entity
        stack.shrink(taken);
        if (stack.isEmpty()) {
            itemEntity.discard();
        }

        // Prevent vanilla from adding to hotbar
        event.setCanPickup(TriState.FALSE);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FireControlManager.clearAll();
        ReconManager.clearAll();
        lastWeaponLoad.clear();
    }
}
