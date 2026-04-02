package com.piranport;

import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import net.minecraft.network.chat.Component;
import com.piranport.combat.TransformationManager;
import com.piranport.config.ModCommonConfig;
import com.piranport.entity.AircraftEntity;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.neoforged.neoforge.common.util.TriState;
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

        if (!TransformationManager.isPlayerTransformed(player)) return;

        // 水面行走：脚踩水但眼睛未入水时，取消下沉速度
        if (player.isInWater() && !player.isEyeInFluid(FluidTags.WATER)) {
            Vec3 vel = player.getDeltaMovement();
            if (vel.y < 0) {
                player.setDeltaMovement(vel.x, 0.0, vel.z);
            }
            player.resetFallDistance();
        }

        // Safety net: remove recon slowness if no active recon (server only, every 20 ticks)
        if (!player.level().isClientSide() && player.tickCount % 20 == 0) {
            MobEffectInstance slowness = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (slowness != null && slowness.getAmplifier() >= 9
                    && !ReconManager.isInRecon(player.getUUID())) {
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
        }

        // 战斗机自动升空 — server only, checked every 40 ticks (2 s)
        if (!player.level().isClientSide() && player.tickCount % 40 == 0) {
            // Find the actual transformed core and its slot for auto-launch
            ItemStack autoLaunchCore = ItemStack.EMPTY;
            int autoLaunchSlot = -1;
            ItemStack mh = player.getMainHandItem();
            if (mh.getItem() instanceof ShipCoreItem
                    && TransformationManager.isTransformed(mh)) {
                autoLaunchCore = mh;
                autoLaunchSlot = player.getInventory().selected;
            } else {
                Inventory inv2 = player.getInventory();
                for (int i = 0; i < inv2.items.size(); i++) {
                    ItemStack s = inv2.items.get(i);
                    if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                        autoLaunchCore = s;
                        autoLaunchSlot = i;
                        break;
                    }
                }
                if (autoLaunchCore.isEmpty()) {
                    ItemStack offh = inv2.offhand.get(0);
                    if (offh.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(offh)) {
                        autoLaunchCore = offh;
                        autoLaunchSlot = 40;
                    }
                }
            }
            if (!autoLaunchCore.isEmpty()) {
                tickAutoLaunchFighters(player, autoLaunchCore, autoLaunchSlot);
            }
        }
    }

    /**
     * No-GUI mode: transformation is driven entirely by the offhand slot.
     * - ShipCoreItem enters offhand  → auto-transform (apply attributes, refill fuel, message).
     * - ShipCoreItem leaves offhand  → auto-un-transform (remove attributes, recall aircraft, message).
     * - Core stays in offhand        → recalculate attributes only when weapon load changes.
     */
    private static void tickInventoryLoadCheck(Player player) {
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        ItemStack offhand = inv.offhand.get(0);
        boolean coreInOffhand = offhand.getItem() instanceof ShipCoreItem;

        if (coreInOffhand) {
            // Auto-transform when core is placed in offhand
            if (!TransformationManager.isTransformed(offhand)) {
                TransformationManager.setTransformed(offhand, true);
                TransformationManager.applyTransformationAttributes(player, offhand);
                ShipCoreItem.refillAircraftFuel(player, offhand);
                player.displayClientMessage(
                        Component.translatable("message.piranport.transformed"), true);
                // Spawn green plant-growth particles (bone meal effect) around the player
                if (player.level() instanceof ServerLevel sl) {
                    double px = player.getX();
                    double py = player.getY() + 0.5;
                    double pz = player.getZ();
                    for (int i = 0; i < 30; i++) {
                        double ox = (player.getRandom().nextDouble() - 0.5) * 1.5;
                        double oy = player.getRandom().nextDouble() * 2.0;
                        double oz = (player.getRandom().nextDouble() - 0.5) * 1.5;
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                px + ox, py + oy, pz + oz,
                                1, 0, 0, 0, 0);
                    }
                }
                lastWeaponLoad.put(player.getUUID(), -1); // force attribute recalc next tick
                return;
            }

            // Core already transformed — recalculate attributes only when load changes
            int weaponLoad = TransformationManager.getInventoryWeaponLoad(inv);
            int armorLoad  = TransformationManager.getCoreArmorLoad(offhand);
            int maxLoad    = ((ShipCoreItem) offhand.getItem()).getShipType().maxLoad;
            int cacheKey   = (weaponLoad + armorLoad) * 1000 + maxLoad;
            Integer cached = lastWeaponLoad.get(player.getUUID());
            if (cached == null || cached != cacheKey) {
                lastWeaponLoad.put(player.getUUID(), cacheKey);
                TransformationManager.applyTransformationAttributes(player, offhand);
            }
        } else {
            // No core in offhand — clear isTransformed flag on any core still in inventory
            for (ItemStack stack : inv.items) {
                if (stack.getItem() instanceof ShipCoreItem
                        && TransformationManager.isTransformed(stack)) {
                    TransformationManager.setTransformed(stack, false);
                }
            }
            // Auto-un-transform if we were previously transformed
            if (lastWeaponLoad.remove(player.getUUID()) != null) {
                TransformationManager.removeTransformationAttributes(player);
                player.removeEffect(com.piranport.registry.ModMobEffects.FLAMMABLE);
                recallAircraftForPlayer(player);
                player.displayClientMessage(
                        Component.translatable("message.piranport.untransformed"), true);
            }
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

    /** When a player changes dimension, recall all aircraft and clear fire control state. */
    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
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
        AABB searchBox = AABB.ofSize(player.position(), 600, 600, 600);
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
