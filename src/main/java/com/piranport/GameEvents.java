package com.piranport;

import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import net.minecraft.network.chat.Component;
import com.piranport.combat.TransformationManager;
import com.piranport.component.FuelData;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
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
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class GameEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // Kirin Headband: permanent invisibility when worn on head (server only)
        if (!player.level().isClientSide()) {
            if (player.getItemBySlot(EquipmentSlot.HEAD).getItem()
                    instanceof com.piranport.item.KirinHeadbandItem) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.INVISIBILITY, 25, 0, false, false, true));
            }
        }

        // Football Superstar Set: experience boost when any piece is worn
        if (!player.level().isClientSide() && player.tickCount % 20 == 0) {
            boolean hasFootball = false;
            for (ItemStack armor : player.getArmorSlots()) {
                if (armor.getItem() instanceof com.piranport.item.FootballArmorItem) {
                    hasFootball = true;
                    break;
                }
            }
            if (hasFootball) {
                player.addEffect(new MobEffectInstance(
                        com.piranport.registry.ModMobEffects.EXPERIENCE_BOOST, 25, 0, false, false, true));
            }
        }

        // No-GUI mode: detect inventory weapon-load changes and recalculate attributes.
        // The scan is O(36) per tick but attribute recalculation only happens when the value changes.
        if (!player.level().isClientSide()
                && !com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
            tickInventoryLoadCheck(player);
        }

        if (!TransformationManager.isPlayerTransformed(player)) {
            // Not transformed — clear distance tracking
            if (!player.level().isClientSide()) {
                lastPlayerPos.remove(player.getUUID());
                accumulatedDistance.remove(player.getUUID());
            }
            return;
        }

        // Fuel consumption based on distance moved (server only)
        if (!player.level().isClientSide()) {
            tickFuelConsumption(player);
            // Re-check — fuel depletion may have caused untransform
            if (!TransformationManager.isPlayerTransformed(player)) return;
        }

        // Check core type for submarine-specific behavior
        ItemStack transformedCore = TransformationManager.findTransformedCore(player);
        boolean isSubmarine = transformedCore.getItem() instanceof ShipCoreItem sci
                && sci.getShipType() == ShipCoreItem.ShipType.SUBMARINE;

        // 水面行走：脚踩水但眼睛未入水时，取消下沉速度（潜艇核心不适用）
        // 水中摩擦力比陆地低，保留滑行惯性，模拟舰船水面移动手感
        if (!isSubmarine && player.isInWater() && !player.isEyeInFluid(FluidTags.WATER)) {
            Vec3 vel = player.getDeltaMovement();
            if (vel.y < 0) {
                player.setDeltaMovement(vel.x, 0.0, vel.z);
            }
            player.resetFallDistance();
        }

        // 潜艇核心：无限水下呼吸 + 水下隐身
        if (isSubmarine && !player.level().isClientSide()) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0, false, false, true));
            if (player.isEyeInFluid(FluidTags.WATER)) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
            }
        }

        // 声纳效果：装备声纳时，24格内敌对生物持续获得发光效果（每20tick刷新一次）
        if (!player.level().isClientSide() && player.tickCount % 20 == 0) {
            if (TransformationManager.hasSonarEquipped(player, transformedCore)) {
                List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(24.0),
                        e -> e.isAlive() && e != player && !(e instanceof Player));
                for (LivingEntity entity : nearby) {
                    entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
                }
            }
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
                // Check fuel — don't auto-transform with empty tank
                FuelData fuel = offhand.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                        new FuelData(0, ((ShipCoreItem) offhand.getItem()).getShipType().fuelCapacity));
                if (fuel.isEmpty()) {
                    // Show message only once (not every tick)
                    if (!lastWeaponLoad.containsKey(player.getUUID())) {
                        lastWeaponLoad.put(player.getUUID(), -999);
                        player.displayClientMessage(
                                Component.translatable("message.piranport.no_fuel"), true);
                    }
                    return;
                }
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

            // Core already transformed — recalculate attributes only when load/engine changes
            int weaponLoad = TransformationManager.getInventoryWeaponLoad(inv);
            int armorLoad  = TransformationManager.getCoreArmorLoad(offhand);
            double engineBonus = TransformationManager.getCoreEngineSpeedBonus(offhand);
            int maxLoad    = ((ShipCoreItem) offhand.getItem()).getShipType().maxLoad;
            int cacheKey   = java.util.Objects.hash(weaponLoad, armorLoad, maxLoad, engineBonus);
            Integer cached = lastWeaponLoad.get(player.getUUID());
            if (cached == null || cached != cacheKey) {
                lastWeaponLoad.put(player.getUUID(), cacheKey);
                TransformationManager.applyTransformationAttributes(player, offhand);
            } else {
                // Reapply overweight debuffs every 40 ticks to keep them active
                int totalLoad = weaponLoad + armorLoad;
                if (totalLoad > maxLoad && player.tickCount % 40 == 0) {
                    TransformationManager.applyOverweightPenalty(player, totalLoad, maxLoad);
                }
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
                TransformationManager.removeOverweightPenalty(player);
                player.removeEffect(com.piranport.registry.ModMobEffects.FLAMMABLE);
                player.removeEffect(MobEffects.WATER_BREATHING);
                recallAircraftForPlayer(player);
                player.displayClientMessage(
                        Component.translatable("message.piranport.untransformed"), true);
            }
        }
    }

    /**
     * Auto-launch fighters when hostile flying mobs are nearby.
     * Locks the nearest hostile flying mob as a fire control target and launches a fighter.
     * "Hostile flying mob" = Enemy that is a FlyingMob, Phantom, or Vex.
     * If ammo/fuel is exhausted the fighter returns, refuels, and re-launches on the next check.
     */
    private static void tickAutoLaunchFighters(Player player, ItemStack coreStack, int coreSlot) {
        if (!coreStack.getOrDefault(ModDataComponents.SHIP_AUTO_LAUNCH.get(), false)) return;

        // Find hostile flying mobs within 64 blocks (for fighter auto-launch)
        // Phantom extends FlyingMob (not Monster), so we check Enemy interface which all hostile mobs implement.
        List<LivingEntity> flyingHostiles = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(64.0),
                e -> e.isAlive() && e instanceof Enemy
                        && (e instanceof FlyingMob || e instanceof Phantom || e instanceof Vex)
        );

        if (!flyingHostiles.isEmpty()) {
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

        // Anti-air missiles: check for flying hostile mobs within 32 blocks
        // Only trigger when airborne targets exist (to avoid wasting ammo on ground mobs).
        // Phantom is not Monster — use Enemy interface so phantoms count as airborne hostiles.
        boolean hasAirborneHostile = !player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(32.0),
                e -> e.isAlive() && e.isPickable() && e instanceof Enemy
                        && !e.onGround() && !e.isUnderWater()
        ).isEmpty();
        if (hasAirborneHostile) {
            ShipCoreItem.tryAutoFireAntiAirMissile(player.level(), player, coreStack, coreSlot);
        }
    }

    // Cache of last-known weapon load per player (for no-GUI inventory mode).
    // Only recalculate attributes when the value actually changes.
    private static final java.util.Map<UUID, Integer> lastWeaponLoad =
            new java.util.HashMap<>();

    // Fuel consumption: track distance moved per player (server-side only).
    private static final java.util.Map<UUID, Vec3> lastPlayerPos =
            new java.util.HashMap<>();
    private static final java.util.Map<UUID, Double> accumulatedDistance =
            new java.util.HashMap<>();

    /**
     * Consume fuel based on distance moved while transformed.
     * Each ShipType defines distancePerFuel (blocks per 1b consumed).
     * When fuel reaches 0, auto-untransform and recall aircraft.
     */
    private static void tickFuelConsumption(Player player) {
        UUID uuid = player.getUUID();
        Vec3 currentPos = player.position();
        Vec3 lastPos = lastPlayerPos.put(uuid, currentPos);

        if (lastPos == null) return; // first tick — no distance yet

        double dist = currentPos.distanceTo(lastPos);
        if (dist > 10.0 || dist < 0.001) return; // ignore teleports and standing still

        ItemStack core = TransformationManager.findTransformedCore(player);
        if (!(core.getItem() instanceof ShipCoreItem sci)) return;

        double threshold = sci.getShipType().distancePerFuel;
        double acc = accumulatedDistance.getOrDefault(uuid, 0.0) + dist;

        FuelData fuel = core.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                new FuelData(0, sci.getShipType().fuelCapacity));

        while (acc >= threshold && fuel.currentFuel() > 0) {
            acc -= threshold;
            fuel = fuel.withCurrentFuel(fuel.currentFuel() - 1);
        }
        core.set(ModDataComponents.SHIP_CORE_FUEL.get(), fuel);

        if (fuel.isEmpty()) {
            // Fuel depleted — auto-untransform
            TransformationManager.setTransformed(core, false);
            TransformationManager.removeTransformationAttributes(player);
            TransformationManager.removeOverweightPenalty(player);
            player.removeEffect(com.piranport.registry.ModMobEffects.FLAMMABLE);
            recallAircraftForPlayer(player);
            lastWeaponLoad.remove(uuid);
            accumulatedDistance.remove(uuid);
            lastPlayerPos.remove(uuid);
            player.displayClientMessage(
                    Component.translatable("message.piranport.fuel_depleted"), true);
            return;
        }

        accumulatedDistance.put(uuid, acc);
    }

    /**
     * Elite Damage Control Squad: prevent lethal damage if the player has one in inventory.
     * Consumes the item, sets health to 1, and grants Resistance II (30s),
     * Fire Resistance (30s), Regeneration II (6s). Plays totem animation.
     */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onEliteDamageControl(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Inventory inv = player.getInventory();
        int foundSlot = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(com.piranport.registry.ModItems.ELITE_DAMAGE_CONTROL.get())) {
                foundSlot = i;
                break;
            }
        }
        if (foundSlot < 0) return;

        // Consume the item
        inv.getItem(foundSlot).shrink(1);

        // Cancel death
        event.setCanceled(true);

        // Restore to 1 HP
        player.setHealth(1.0f);

        // Apply buffs: Resistance II 30s, Fire Resistance 30s, Regeneration II 6s
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));

        // Totem-style animation
        player.level().broadcastEntityEvent(player, (byte) 35);
    }

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

    /** When a player logs in, give guidebook and clean up stale recon slowness. */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joiner)) return;

        // Safety net: remove residual recon slowness (amplifier 9) left by server crash
        var slowness = joiner.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slowness != null && slowness.getAmplifier() >= 9) {
            joiner.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }

        // Give guidebook on first join
        if (ModCommonConfig.GIVE_GUIDEBOOK_ON_FIRST_JOIN.get()) {
            net.minecraft.nbt.CompoundTag persisted = joiner.getPersistentData();
            String tag = "piranport:received_guidebook";
            if (!persisted.getBoolean(tag)) {
                persisted.putBoolean(tag, true);
                ItemStack guidebook = new ItemStack(
                        com.piranport.registry.ModItems.GUIDEBOOK.get());
                if (!joiner.getInventory().add(guidebook)) {
                    joiner.drop(guidebook, false);
                }
            }
        }
    }

    /** When a player logs out, recall all their airborne aircraft and clear cached load. */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        lastWeaponLoad.remove(player.getUUID());
        lastPlayerPos.remove(player.getUUID());
        accumulatedDistance.remove(player.getUUID());
        com.piranport.combat.HitNotifier.onPlayerLogout(player.getUUID());
        com.piranport.network.RecallAllAircraftPayload.onPlayerDisconnect(player.getUUID());
        recallAircraftForPlayer(player);
    }

    private static void recallAircraftForPlayer(Player player) {
        if (player.level().isClientSide()) return;
        UUID ownerUUID = player.getUUID();
        // O(k) lookup via the owner-keyed index instead of a per-dim full entity scan.
        for (AircraftEntity aircraft : com.piranport.aviation.AircraftIndex.snapshot(ownerUUID)) {
            aircraft.recallAndRemove();
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

    /** 经验提升Buff: 怪物掉落经验 +50% */
    @SubscribeEvent
    public static void onXpDrop(net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent event) {
        Player attacker = event.getAttackingPlayer();
        if (attacker != null && attacker.hasEffect(com.piranport.registry.ModMobEffects.EXPERIENCE_BOOST)) {
            int original = event.getDroppedExperience();
            event.setDroppedExperience((int) (original * 1.5));
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        com.piranport.debug.PiranPortCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FireControlManager.clearAll();
        ReconManager.clearAll();
        com.piranport.aviation.AircraftIndex.clearAll();
        lastWeaponLoad.clear();
        lastPlayerPos.clear();
        accumulatedDistance.clear();
    }
}
