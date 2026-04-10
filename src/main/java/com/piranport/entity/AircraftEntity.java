package com.piranport.entity;

import com.piranport.aviation.FireControlManager;
import com.piranport.config.ModCommonConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import com.piranport.aviation.ReconManager;
import com.piranport.component.AircraftInfo;
import com.piranport.component.FlightGroupData;
import com.piranport.item.ShipCoreItem;
import com.piranport.network.AswSonarSyncPayload;
import com.piranport.network.ReconEndPayload;
import com.piranport.network.ReconStartPayload;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AircraftEntity extends Entity {

    public enum FlightState {
        LAUNCHING, CRUISING, ATTACKING, RETURNING, REMOVED,
        RECON_ACTIVE  // Phase 32 — ordinal 5, must remain last
    }

    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AIRCRAFT_TYPE_DATA =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    // Saved to NBT
    @Nullable private UUID ownerUUID;
    private int weaponSlotIndex;
    private int coreInventorySlot = 0;
    private AircraftInfo.AircraftType aircraftType = AircraftInfo.AircraftType.FIGHTER;
    private FlightGroupData.AttackMode attackMode = FlightGroupData.AttackMode.FOCUS;
    private float panelDamage;
    private float panelSpeed;
    private int remainingAmmo = 0;
    private int ammoCapacity = 0;
    private int currentFuel = 0;
    private int fuelCapacity = 1;
    private boolean hasBullets = false;
    private String payloadType = "";
    private AircraftInfo.BombingMode bombingMode = AircraftInfo.BombingMode.DIVE;

    // Runtime (not saved)
    private int airtimeTicks = 0;
    private int stuckTicks = 0;
    private Vec3 stuckCheckPos = Vec3.ZERO;
    private double orbitAngle = 0;
    private int stateTicks = 0;
    private int attackCooldown = 0;
    private boolean hasFired = false;
    private boolean diveCommitted = false;  // true once dive bomber locks its drop point
    @Nullable private Vec3 diveTarget = null; // predicted bomb drop point (fixed after commit)
    private int autoSeekCooldown = 0; // P3 #30: throttle AABB queries
    private boolean autoSeekDone = false;    // true after first auto-seek scan (one-shot)
    private boolean hasEverHadFireControl = false; // true if FC target was ever assigned
    private transient net.minecraft.world.item.Item cachedPayloadItem; // P3 #31: cached Item reference

    // Phase 32 runtime fields
    private FlightState lastKnownState = FlightState.LAUNCHING;
    private int lastForcedChunkX = Integer.MIN_VALUE;
    private int lastForcedChunkZ = Integer.MIN_VALUE;
    private boolean appliedSlowness = false;

    // Recon chunk loading: view-distance-based force loading + chunk sending
    private final java.util.Set<Long> reconForcedChunks = new java.util.HashSet<>();
    private final java.util.Set<Long> reconPendingSend = new java.util.HashSet<>();
    private static final int RECON_CHUNK_SEND_RATE = 16; // max chunks to send per tick

    // Phase 33: aircraft health (persisted to NBT)
    private int aircraftHealth = 20;

    // P3 #6: cached recon aircraft reference for FOLLOW mode (refreshed every 20 ticks)
    @Nullable private AircraftEntity cachedReconAircraft;
    private int reconCacheTick = 0;

    // Client-side position interpolation (prevents camera stutter from raw setPos jumps)
    private int clientLerpSteps;
    private double clientLerpX, clientLerpY, clientLerpZ;
    private float clientLerpYRot, clientLerpXRot;

    // P2 #29: preserve original ItemStack across launch-return cycle
    private ItemStack originalStack = ItemStack.EMPTY;

    // Phase 34: set to true when defense mechanisms force a recall (stuck, timeout, distance)
    private boolean isForcedReturn = false;

    // Autonomous mode: aircraft has no player owner (e.g. launched by floating target)
    private boolean autonomous = false;
    private Vec3 homePosition = Vec3.ZERO;

    private static final int MAX_AIRTIME_TICKS = 12000;
    private static final double MAX_DIST_FROM_OWNER = 48.0;
    private static final double MAX_RECON_DIST = 200.0;  // Phase 32
    private static final int STUCK_CHECK_INTERVAL = 60;
    private static final double STUCK_THRESHOLD = 0.1;
    private static final int LAUNCH_DURATION = 30;
    private static final double CRUISE_ALTITUDE = 18.0;
    private static final double ORBIT_RADIUS = 8.0;
    private static final double RETURN_ARRIVAL_DIST = 3.0;
    private static final int FUEL_BURN_INTERVAL = 4; // burn 1 fuel every 4 ticks

    public AircraftEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Factory — call on server only. */
    public static AircraftEntity create(Level level, Player owner, int weaponSlotIndex,
                                        ItemStack aircraftStack,
                                        FlightGroupData.AttackMode attackMode,
                                        int coreInventorySlot,
                                        boolean hasBullets,
                                        String payloadType) {
        AircraftEntity entity = new AircraftEntity(ModEntityTypes.AIRCRAFT_ENTITY.get(), level);
        entity.ownerUUID = owner.getUUID();
        entity.weaponSlotIndex = weaponSlotIndex;
        entity.coreInventorySlot = coreInventorySlot;
        entity.attackMode = attackMode;
        entity.hasBullets = hasBullets;
        entity.payloadType = payloadType;

        AircraftInfo info = aircraftStack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (info != null) {
            entity.aircraftType = info.aircraftType();
            entity.panelDamage = info.panelDamage();
            entity.panelSpeed = info.panelSpeed();
            entity.ammoCapacity = info.ammoCapacity();
            entity.remainingAmmo = info.ammoCapacity();
            entity.fuelCapacity = info.fuelCapacity();
            entity.currentFuel = info.currentFuel();
            entity.bombingMode = info.bombingMode();
        }
        // Default payload based on aircraft type when not explicitly configured
        if (entity.payloadType.isEmpty()) {
            switch (entity.aircraftType) {
                case TORPEDO_BOMBER -> entity.payloadType = "piranport:aerial_torpedo";
                case DIVE_BOMBER, LEVEL_BOMBER -> entity.payloadType = "piranport:aerial_bomb";
                case ASW -> entity.payloadType = "piranport:depth_charge";
                default -> { /* FIGHTER / RECON keep empty payload */ }
            }
            if (!entity.payloadType.isEmpty()) {
                entity.hasBullets = false;
            }
        }
        entity.aircraftHealth = getMaxHealth(entity.aircraftType);
        entity.originalStack = aircraftStack.copy();
        entity.entityData.set(AIRCRAFT_TYPE_DATA, entity.aircraftType.ordinal());
        entity.entityData.set(OWNER_ID, Optional.of(owner.getUUID()));

        Vec3 look = owner.getLookAngle();
        entity.setPos(owner.getX() + look.x * 0.8, owner.getEyeY(), owner.getZ() + look.z * 0.8);
        entity.orbitAngle = Math.atan2(look.z, look.x);
        entity.stuckCheckPos = entity.position();
        return entity;
    }

    /**
     * Factory for autonomous aircraft (no player owner).
     * Used by debug commands to spawn aircraft from non-player entities (e.g. floating targets).
     */
    public static AircraftEntity createAutonomous(Level level, Vec3 spawnPos, ItemStack aircraftStack) {
        AircraftEntity entity = new AircraftEntity(ModEntityTypes.AIRCRAFT_ENTITY.get(), level);
        entity.autonomous = true;
        entity.homePosition = spawnPos;

        AircraftInfo info = aircraftStack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (info != null) {
            entity.aircraftType = info.aircraftType();
            entity.panelDamage = info.panelDamage();
            entity.panelSpeed = info.panelSpeed();
            entity.ammoCapacity = info.ammoCapacity();
            entity.remainingAmmo = info.ammoCapacity();
            entity.fuelCapacity = info.fuelCapacity();
            entity.currentFuel = info.fuelCapacity();
            entity.bombingMode = info.bombingMode();
        }
        entity.payloadType = "piranport:aerial_bomb";
        entity.aircraftHealth = getMaxHealth(entity.aircraftType);
        entity.originalStack = aircraftStack.copy();
        entity.entityData.set(AIRCRAFT_TYPE_DATA, entity.aircraftType.ordinal());

        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        entity.orbitAngle = level.random.nextDouble() * Math.PI * 2;
        entity.stuckCheckPos = entity.position();
        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, FlightState.LAUNCHING.ordinal());
        builder.define(AIRCRAFT_TYPE_DATA, 0);
        builder.define(OWNER_ID, Optional.empty());
    }

    /**
     * Override lerpTo to:
     * 1. Smooth position updates on the client via lerp steps (Entity base class sets
     *    position directly in lerpTo, which causes camera stutter since xo==x after
     *    setOldPosAndRot → no partial-tick interpolation).
     * 2. In recon mode, ignore server rotation (client controls camera via mouse).
     *    Uses ClientReconData instead of getFlightState() to avoid race with entity
     *    data sync — the ReconStartPayload arrives before the STATE data packet.
     */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (level().isClientSide()) {
            // Store lerp target; position will be interpolated in tick()
            this.clientLerpSteps = steps + 2;
            this.clientLerpX = x;
            this.clientLerpY = y;
            this.clientLerpZ = z;
            // In recon mode, keep client-controlled rotation
            if (com.piranport.aviation.ClientReconData.isInReconMode()
                    && com.piranport.aviation.ClientReconData.getReconEntityId() == getId()) {
                return;
            }
            // Non-recon: store target rotation for smooth lerping in tick()
            this.clientLerpYRot = yRot;
            this.clientLerpXRot = xRot;
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, steps);
    }

    @Override public double lerpTargetX() { return clientLerpSteps > 0 ? clientLerpX : getX(); }
    @Override public double lerpTargetY() { return clientLerpSteps > 0 ? clientLerpY : getY(); }
    @Override public double lerpTargetZ() { return clientLerpSteps > 0 ? clientLerpZ : getZ(); }

    @Override
    public void tick() {
        super.tick();

        // Client: position + rotation interpolation + particles
        if (level().isClientSide()) {
            // Process position and rotation lerp steps for smooth rendering
            if (clientLerpSteps > 0) {
                double d = 1.0 / (double) clientLerpSteps;
                double nx = getX() + (clientLerpX - getX()) * d;
                double ny = getY() + (clientLerpY - getY()) * d;
                double nz = getZ() + (clientLerpZ - getZ()) * d;
                setPos(nx, ny, nz);
                // Smooth rotation lerp (shortest-path yaw wrapping)
                float yDiff = clientLerpYRot - getYRot();
                while (yDiff > 180f) yDiff -= 360f;
                while (yDiff < -180f) yDiff += 360f;
                setYRot(getYRot() + yDiff * (float) d);
                setXRot(getXRot() + (clientLerpXRot - getXRot()) * (float) d);
                clientLerpSteps--;
            }
            if (getFlightState() == FlightState.CRUISING && tickCount % 8 == 0) {
                level().addParticle(ParticleTypes.CLOUD, getX(), getY(), getZ(), 0, 0.02, 0);
            }
            return;
        }

        FlightState state = getFlightState();
        if (state == FlightState.REMOVED) { discard(); return; }

        Player owner = getOwner();
        if (owner == null) {
            if (autonomous) {
                tickAutonomousServer();
                return;
            }
            // Clean up recon state if owner goes offline
            if (ownerUUID != null && state == FlightState.RECON_ACTIVE) {
                ReconManager.endRecon(ownerUUID);
                releaseAllForcedChunks();
            }
            discard();
            return;
        }

        // Detect state transitions and fire enter/exit hooks
        if (state != lastKnownState) {
            handleStateTransition(lastKnownState, state, owner);
            lastKnownState = state;
        }

        airtimeTicks++;
        stateTicks++;

        // Fuel consumption: burn 1 fuel per FUEL_BURN_INTERVAL ticks during active flight
        if (state == FlightState.CRUISING || state == FlightState.ATTACKING || state == FlightState.RECON_ACTIVE) {
            if (currentFuel > 0 && airtimeTicks % FUEL_BURN_INTERVAL == 0) {
                currentFuel--;
            }
            if (currentFuel <= 0) {
                isForcedReturn = true;
                startReturning("fuel_empty");
                if (owner instanceof ServerPlayer sp) {
                    sp.displayClientMessage(
                            Component.translatable("message.piranport.aircraft_no_fuel", getDisplayName()), true);
                }
                return;
            }
        }

        // Defense: 10-min airtime limit
        if (airtimeTicks >= MAX_AIRTIME_TICKS) {
            if (state == FlightState.RETURNING) { recallAndRemove(); return; }
            isForcedReturn = true;
            startReturning("max_airtime");
            return;
        }

        // Defense: stuck detection (disabled in RECON_ACTIVE — player may hover stationary)
        if (state != FlightState.RECON_ACTIVE && stateTicks % STUCK_CHECK_INTERVAL == 0) {
            double moved = position().distanceTo(stuckCheckPos);
            if (moved < STUCK_THRESHOLD) {
                stuckTicks += STUCK_CHECK_INTERVAL;
                if (stuckTicks >= STUCK_CHECK_INTERVAL * 2) { isForcedReturn = true; recallAndRemove(); return; }
            } else {
                stuckTicks = 0;
            }
            stuckCheckPos = position();
        }

        // Defense: distance limit
        // FOLLOW mode uses extended range (recon plane may be 200 blocks away)
        double maxDist = (state == FlightState.RECON_ACTIVE
                || attackMode == FlightGroupData.AttackMode.FOLLOW)
                ? MAX_RECON_DIST : MAX_DIST_FROM_OWNER;
        if (distanceTo(owner) > maxDist) {
            if (state == FlightState.RECON_ACTIVE) {
                // Recon exceeded 200-block range — end recon and return
                isForcedReturn = true;
                startReturning("recon_max_distance");
            } else {
                Vec3 near = owner.position().add(
                        (random.nextDouble() - 0.5) * 4, 3, (random.nextDouble() - 0.5) * 4);
                setPos(near.x, near.y, near.z);
                stuckTicks = 0;
                stuckCheckPos = position();
                isForcedReturn = true;
                startReturning("distance_limit");
            }
            return;
        }

        if (attackCooldown > 0) attackCooldown--;

        switch (state) {
            case LAUNCHING      -> tickLaunching(owner);
            case CRUISING       -> tickCruising(owner);
            case ATTACKING      -> tickAttacking(owner);
            case RETURNING      -> tickReturning(owner);
            case RECON_ACTIVE   -> tickReconActive(owner);
        }

        // ASW sonar scan: every 20 ticks while cruising or attacking
        if (aircraftType == AircraftInfo.AircraftType.ASW
                && (state == FlightState.CRUISING || state == FlightState.ATTACKING)
                && tickCount % 20 == 0) {
            tickAswSonar(owner);
        }

        // Update rotation to face movement direction (nose forward)
        Vec3 vel = getDeltaMovement();
        double hDist = vel.horizontalDistance();
        if (hDist > 0.001) {
            float targetYaw = (float) (Math.atan2(-vel.x, vel.z) * (180.0 / Math.PI));
            float targetPitch = (float) (Math.atan2(vel.y, hDist) * (180.0 / Math.PI));
            setYRot(targetYaw);
            setXRot(targetPitch);
        }

        // Apply movement
        setPos(getX() + vel.x, getY() + vel.y, getZ() + vel.z);
    }

    // ===== State transition hooks =====

    private void handleStateTransition(FlightState from, FlightState to, Player owner) {
        if (to == FlightState.RECON_ACTIVE) {
            ReconManager.startRecon(owner.getUUID(), getUUID());
            // Lock player body with max slowness
            owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    Integer.MAX_VALUE, 9, false, false));
            appliedSlowness = true;
            // Notify client to switch camera
            if (owner instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, new ReconStartPayload(getId()));
            }
        } else if (from == FlightState.RECON_ACTIVE) {
            ReconManager.endRecon(owner.getUUID());
            if (appliedSlowness) {
                owner.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                appliedSlowness = false;
            }
            releaseAllForcedChunks();
            // Notify client to restore camera
            if (owner instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, new ReconEndPayload());
            }
        }
    }

    // ===== Tick methods =====

    private void tickLaunching(Player owner) {
        double targetY = owner.getY() + CRUISE_ALTITUDE;
        double dy = targetY - getY();
        double rise = Math.min(panelSpeed * 0.3, Math.abs(dy));
        setDeltaMovement(getDeltaMovement().x * 0.5, dy > 0 ? rise : -rise, getDeltaMovement().z * 0.5);
        if (stateTicks >= LAUNCH_DURATION || Math.abs(dy) < 1.5) {
            // RECON goes directly to RECON_ACTIVE; others go to CRUISING
            setState(aircraftType == AircraftInfo.AircraftType.RECON
                    ? FlightState.RECON_ACTIVE : FlightState.CRUISING);
        }
    }

    private void tickCruising(Player owner) {
        boolean isAsw = aircraftType == AircraftInfo.AircraftType.ASW;
        // Transition to ATTACKING if owner has locked targets with at least one alive entity
        if (aircraftType != AircraftInfo.AircraftType.RECON
                && (hasBullets || !payloadType.isEmpty())
                && !FireControlManager.getTargets(owner.getUUID()).isEmpty()) {
            // Verify at least one locked target is still alive before transitioning
            if (hasAliveLockedTarget(owner)) {
                // ASW: only transition if there's an alive ASW-eligible target
                if (isAsw) {
                    if (hasAliveAswLockedTarget(owner)) {
                        hasEverHadFireControl = true;
                        setState(FlightState.ATTACKING);
                        return;
                    }
                    // ASW: alive targets exist but none are aquatic — stay cruising
                } else {
                    // Attack aircraft (payload-only, no bullets) skip airborne targets
                    boolean isAttackAircraft = !hasBullets && !payloadType.isEmpty();
                    if (!isAttackAircraft || hasAliveGroundLockedTarget(owner)) {
                        hasEverHadFireControl = true;
                        setState(FlightState.ATTACKING);
                        return;
                    }
                    // Attack aircraft: alive targets exist but all airborne — stay cruising
                }
            } else {
                // Remove only the dead UUIDs, preserving any newly added targets
                if (owner.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    FireControlManager.removeDeadTargets(owner.getUUID(), targetUuid -> {
                        Entity e = sl.getEntities().get(targetUuid);
                        return e == null || !e.isAlive();
                    });
                }
            }
        }

        // Auto-seek for attack aircraft (bombers + torpedo bombers) without fire-control locks:
        // Only search ONCE after launch; if FC target died, just keep orbiting.
        if (!payloadType.isEmpty()
                && aircraftType != AircraftInfo.AircraftType.RECON
                && !hasEverHadFireControl && !autoSeekDone
                && FireControlManager.getTargets(owner.getUUID()).isEmpty()) {
            if (autoSeekCooldown > 0) {
                autoSeekCooldown--;
            } else {
                autoSeekDone = true;
                if (level() instanceof ServerLevel sl) {
                    AABB box = getBoundingBox().inflate(32.0);
                    boolean hasNearbyTarget;
                    if (isAsw) {
                        hasNearbyTarget = !sl.getEntitiesOfClass(LivingEntity.class, box,
                                e -> e.isAlive() && e != owner && isAswTarget(e)).isEmpty();
                    } else {
                        hasNearbyTarget = !sl.getEntitiesOfClass(LivingEntity.class, box,
                                e -> e.isAlive() && e != owner && e instanceof Monster && !isAirborneTarget(e)).isEmpty();
                    }
                    if (hasNearbyTarget) {
                        autoSeekCooldown = 0; // reset so resolveTarget() finds it immediately
                        setState(FlightState.ATTACKING);
                        return;
                    }
                }
            }
        }

        // FOLLOW mode: orbit the owner's active recon aircraft, or fall back to player
        if (attackMode == FlightGroupData.AttackMode.FOLLOW) {
            // Refresh cache every 20 ticks to avoid per-tick entity lookup
            if (tickCount - reconCacheTick > 20 || cachedReconAircraft != null && cachedReconAircraft.isRemoved()) {
                cachedReconAircraft = findOwnerReconAircraft(owner);
                reconCacheTick = tickCount;
            }
            AircraftEntity reconAircraft = cachedReconAircraft;
            if (reconAircraft != null) {
                orbitAngle += panelSpeed * 0.015;
                double tx = reconAircraft.getX() + Math.cos(orbitAngle) * ORBIT_RADIUS;
                double ty = reconAircraft.getY();  // match recon altitude
                double tz = reconAircraft.getZ() + Math.sin(orbitAngle) * ORBIT_RADIUS;
                Vec3 toTarget = new Vec3(tx - getX(), ty - getY(), tz - getZ());
                double dist = toTarget.length();
                if (dist > 0.1) {
                    setDeltaMovement(toTarget.normalize().scale(Math.min(panelSpeed * 0.35, dist)));
                } else {
                    setDeltaMovement(Vec3.ZERO);
                }
                return;
            }
            // No active recon — fall through to normal orbit around player
        }

        orbitAngle += panelSpeed * 0.015;
        double tx = owner.getX() + Math.cos(orbitAngle) * ORBIT_RADIUS;
        double ty = owner.getY() + CRUISE_ALTITUDE;
        double tz = owner.getZ() + Math.sin(orbitAngle) * ORBIT_RADIUS;
        Vec3 toTarget = new Vec3(tx - getX(), ty - getY(), tz - getZ());
        double dist = toTarget.length();
        if (dist > 0.1) {
            setDeltaMovement(toTarget.normalize().scale(Math.min(panelSpeed * 0.3, dist)));
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    /** Finds the owner's currently active RECON_ACTIVE aircraft, or null if none. */
    @Nullable
    private AircraftEntity findOwnerReconAircraft(Player owner) {
        if (!(level() instanceof ServerLevel sl)) return null;
        UUID reconEntityId = ReconManager.getReconEntity(owner.getUUID());
        if (reconEntityId == null) return null;
        Entity e = sl.getEntity(reconEntityId);
        if (e instanceof AircraftEntity ae && ae.getFlightState() == FlightState.RECON_ACTIVE) return ae;
        return null;
    }

    // ===== ATTACKING dispatch =====

    private void tickAttacking(Player owner) {
        if (aircraftType == AircraftInfo.AircraftType.RECON) {
            startReturning("recon_no_attack");
            return;
        }

        // Minimum 20 ticks in ATTACKING before allowing fallback to CRUISING
        boolean canFallback = stateTicks >= 20;

        // 子弹优先：使用战斗机AI
        if (hasBullets) {
            Entity target = resolveFighterTarget(owner);
            if (target == null) {
                if (canFallback) setState(FlightState.CRUISING);
                return;
            }
            tickFighterAttack(owner, target);
            return;
        }

        // 无子弹时按挂载类型决定攻击行为
        switch (payloadType) {
            case "piranport:aerial_torpedo" -> {
                LivingEntity target = resolveTarget(owner);
                if (target == null) {
                    if (canFallback) setState(FlightState.CRUISING);
                    return;
                }
                tickTorpedoBomberAttack(owner, target);
            }
            case "piranport:aerial_bomb" -> {
                LivingEntity target = resolveTarget(owner);
                if (target == null) {
                    if (canFallback) setState(FlightState.CRUISING);
                    return;
                }
                if (bombingMode == AircraftInfo.BombingMode.LEVEL) {
                    tickLevelBomberAttack(owner, target);
                } else {
                    tickDiveBomberAttack(owner, target);
                }
            }
            case "piranport:depth_charge" -> {
                LivingEntity target = resolveASWTarget(owner);
                if (target == null) {
                    if (canFallback) setState(FlightState.CRUISING);
                    return;
                }
                tickASWAttack(owner, target);
            }
            default -> startReturning("no_payload");
        }
    }

    /**
     * FIGHTER: hover at ~11 blocks and fire bullets.
     * When fighterAmmoEnabled=false (default), ammo is unlimited and the fighter
     * never returns due to depletion. When true, 64 rounds total (ammoCapacity).
     * Phase 33: target may be a LivingEntity or an enemy AircraftEntity.
     */
    private void tickFighterAttack(Player owner, Entity target) {
        boolean ammoEnabled = ModCommonConfig.FIGHTER_AMMO_ENABLED.get();
        if (ammoEnabled && remainingAmmo <= 0) { startReturning("fighter_ammo_depleted"); return; }

        Vec3 toTarget = target.getEyePosition().subtract(position());
        double dist = toTarget.length();
        double preferredDist = 11.0;

        if (dist > preferredDist + 3) {
            setDeltaMovement(toTarget.normalize().scale(Math.min(panelSpeed * 0.5, dist)));
        } else if (dist < preferredDist - 3) {
            setDeltaMovement(toTarget.normalize().scale(-panelSpeed * 0.2));
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.8));
        }

        if (attackCooldown <= 0 && dist < 24.0) {
            BulletEntity bullet = new BulletEntity(level(), panelDamage / 8f);
            Vec3 dir = toTarget.normalize();
            bullet.setPos(getX(), getY() + 0.3, getZ());
            bullet.setDeltaMovement(dir.scale(2.5));
            bullet.setOwner(owner);
            bullet.setSourceAircraftName(getDisplayName());
            level().addFreshEntity(bullet);
            attackCooldown = 5;

            if (ammoEnabled) {
                remainingAmmo--;
                if (remainingAmmo <= 0) {
                    if (ModCommonConfig.AUTO_RESUPPLY_ENABLED.get() && tryAutoResupplyAmmo(owner)) {
                        // Ammo restored (payload type must be set) — continue
                    } else {
                        startReturning("fighter_ammo_depleted");
                    }
                }
            }
        }
    }

    /**
     * DIVE_BOMBER: climb to target+18 blocks, then commit to a dive with lead prediction.
     * Once the dive is committed, the drop point is fixed — no more recalculation.
     */
    private void tickDiveBomberAttack(Player owner, LivingEntity target) {
        if (hasFired) {
            if (ModCommonConfig.AUTO_RESUPPLY_ENABLED.get() && tryAutoResupplyAmmo(owner)) {
                setState(FlightState.CRUISING);
            } else {
                startReturning("dive_bomber_done");
            }
            return;
        }

        double climbY = target.getY() + 18.0;

        // Phase 1: Climb — ascend above the target until altitude reached or timeout
        if (!diveCommitted && getY() < climbY - 1.0 && stateTicks < 80) {
            Vec3 toClimb = new Vec3(target.getX() - getX(), climbY - getY(), target.getZ() - getZ());
            double dist = toClimb.length();
            setDeltaMovement(toClimb.normalize().scale(Math.min(panelSpeed * 0.4, dist)));
            return;
        }

        // Phase 2: Commit — lock the predicted drop point once
        if (!diveCommitted) {
            diveCommitted = true;
            Vec3 targetPos = target.getEyePosition();
            double estimatedDist = position().distanceTo(targetPos);
            double diveSpeed = Math.max(panelSpeed * 0.6, 0.1);
            double estimatedTicks = estimatedDist / diveSpeed;
            Vec3 targetVel = target.getDeltaMovement();
            diveTarget = targetPos.add(targetVel.scale(estimatedTicks));
        }

        // Phase 3: Dive — fly toward the fixed predicted point
        Vec3 toTarget = diveTarget.subtract(position());
        double dist = toTarget.length();

        // Drop bomb when close enough or when aircraft has descended past the drop point
        if (dist < 4.0 || getY() < diveTarget.y()) {
            AerialBombEntity bomb = new AerialBombEntity(level(), panelDamage * 1.5f, 2.5f);
            bomb.setPos(getX(), getY(), getZ());
            bomb.setDeltaMovement(getDeltaMovement().x * 0.2, -0.3, getDeltaMovement().z * 0.2);
            bomb.setOwner(owner);
            bomb.setSourceAircraftName(getDisplayName());
            level().addFreshEntity(bomb);
            hasFired = true;
            startReturning("dive_bomb_dropped");
            return;
        }

        setDeltaMovement(toTarget.normalize().scale(Math.min(panelSpeed * 0.6, dist)));
    }

    /**
     * TORPEDO_BOMBER: fly low, fire torpedo when 20-30 blocks from target.
     * If too close, flies away first to set up a proper attack run.
     */
    private void tickTorpedoBomberAttack(Player owner, LivingEntity target) {
        if (hasFired) {
            if (ModCommonConfig.AUTO_RESUPPLY_ENABLED.get() && tryAutoResupplyAmmo(owner)) {
                setState(FlightState.CRUISING);
            } else {
                startReturning("torpedo_bomber_done");
            }
            return;
        }

        // Timeout — if attack run takes too long, abort
        if (stateTicks > 200) {
            hasFired = true;
            startReturning("torpedo_attack_timeout");
            return;
        }

        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        if (horizDist >= 20 && horizDist <= 30) {
            TorpedoEntity torpedo = new TorpedoEntity(ModEntityTypes.TORPEDO_ENTITY.get(), level());
            Vec3 dir = new Vec3(dx, 0, dz).normalize();
            torpedo.setPos(getX(), getY(), getZ());
            // 投下鱼雷：垂直入水后再启动巡航
            torpedo.setDeltaMovement(0, -0.6, 0);
            torpedo.setAirDrop(true, dir);
            torpedo.setOwner(owner);
            torpedo.setSourceAircraftName(getDisplayName());
            level().addFreshEntity(torpedo);
            remainingAmmo--;
            hasFired = true;
            startReturning("torpedo_launched");
            return;
        }

        double targetY = target.getY() + 2.0;

        if (horizDist < 20) {
            // Too close — fly away from target to set up attack run
            Vec3 awayDir = new Vec3(-dx, 0, -dz).normalize();
            double yDelta = (targetY - getY()) * 0.1;
            setDeltaMovement(awayDir.scale(panelSpeed * 0.4).add(0, yDelta, 0));
            return;
        }

        // horizDist > 30 — approach at low altitude (target.y + 2)
        Vec3 toTarget = new Vec3(dx, targetY - getY(), dz);
        double dist = toTarget.length();
        if (dist > 0.1) {
            setDeltaMovement(toTarget.normalize().scale(Math.min(panelSpeed * 0.4, dist)));
        }
    }

    /**
     * LEVEL_BOMBER: climb to target+32 blocks, fly over and drop bombs.
     * 8 rounds total; does not return until ammo depleted.
     */
    private void tickLevelBomberAttack(Player owner, LivingEntity target) {
        if (remainingAmmo <= 0) {
            if (ModCommonConfig.AUTO_RESUPPLY_ENABLED.get() && tryAutoResupplyAmmo(owner)) {
                // Ammo restored — continue attacking
            } else {
                startReturning("level_bomber_ammo_depleted");
                return;
            }
        }

        double bombAltitude = target.getY() + 32.0;

        if (getY() < bombAltitude - 1.5) {
            Vec3 toPoint = new Vec3(target.getX() - getX(), bombAltitude - getY(), target.getZ() - getZ());
            double dist = toPoint.length();
            setDeltaMovement(toPoint.normalize().scale(Math.min(panelSpeed * 0.4, dist)));
        } else {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double horizDist = Math.sqrt(dx * dx + dz * dz);

            if (horizDist < 3.0 && attackCooldown <= 0) {
                AerialBombEntity bomb = new AerialBombEntity(level(), panelDamage * 1.5f, 2.5f);
                bomb.setPos(getX(), getY(), getZ());
                bomb.setDeltaMovement(getDeltaMovement().x * 0.1, -0.1, getDeltaMovement().z * 0.1);
                bomb.setOwner(owner);
                bomb.setSourceAircraftName(getDisplayName());
                level().addFreshEntity(bomb);
                remainingAmmo--;
                attackCooldown = 40;
            }

            Vec3 horizontal = new Vec3(dx, 0, dz).normalize().scale(Math.min(panelSpeed * 0.4, horizDist));
            double yCorrect = (bombAltitude - getY()) * 0.15;
            setDeltaMovement(horizontal.x, yCorrect, horizontal.z);
        }
    }

    /**
     * ASW: fly at target+20 altitude, drop depth charges when over target.
     * Similar to level bomber but lower altitude and uses DepthChargeEntity.
     */
    private void tickASWAttack(Player owner, LivingEntity target) {
        if (remainingAmmo <= 0) {
            if (ModCommonConfig.AUTO_RESUPPLY_ENABLED.get() && tryAutoResupplyAmmo(owner)) {
                // Ammo restored — continue attacking
            } else {
                startReturning("asw_ammo_depleted");
                return;
            }
        }

        double bombAltitude = target.getY() + 20.0;

        if (getY() < bombAltitude - 1.5) {
            Vec3 toPoint = new Vec3(target.getX() - getX(), bombAltitude - getY(), target.getZ() - getZ());
            double dist = toPoint.length();
            setDeltaMovement(toPoint.normalize().scale(Math.min(panelSpeed * 0.4, dist)));
        } else {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double horizDist = Math.sqrt(dx * dx + dz * dz);

            if (horizDist < 4.0 && attackCooldown <= 0) {
                DepthChargeEntity dc = new DepthChargeEntity(level(), panelDamage * 1.5f, 3.0f);
                dc.setPos(getX(), getY(), getZ());
                dc.setDeltaMovement(getDeltaMovement().x * 0.1, -0.1, getDeltaMovement().z * 0.1);
                dc.setOwner(owner);
                level().addFreshEntity(dc);
                remainingAmmo--;
                attackCooldown = 30;
            }

            Vec3 horizontal = new Vec3(dx, 0, dz).normalize().scale(Math.min(panelSpeed * 0.4, horizDist));
            double yCorrect = (bombAltitude - getY()) * 0.15;
            setDeltaMovement(horizontal.x, yCorrect, horizontal.z);
        }
    }

    /**
     * ASW target resolution: only targets submarines and aquatic creatures.
     * 1. FC-locked targets (filtered to ASW-eligible)
     * 2. Auto-seek: underwater monsters and aquatic creatures within 32 blocks.
     */
    @Nullable
    private LivingEntity resolveASWTarget(Player owner) {
        if (!(level() instanceof ServerLevel sl)) return null;

        List<UUID> locks = FireControlManager.getTargets(owner.getUUID());
        if (!locks.isEmpty()) {
            if (attackMode == FlightGroupData.AttackMode.SPREAD) {
                return locks.stream()
                        .map(sl::getEntity)
                        .filter(e -> e instanceof LivingEntity le && le.isAlive() && isAswTarget(le))
                        .map(e -> (LivingEntity) e)
                        .min(Comparator.comparingDouble(this::distanceTo))
                        .orElse(null);
            } else {
                for (UUID uuid : locks) {
                    Entity e = sl.getEntity(uuid);
                    if (e instanceof LivingEntity le && le.isAlive() && isAswTarget(le)) return le;
                }
                return null;
            }
        }

        // Auto-seek: only once after launch
        if (hasEverHadFireControl || autoSeekDone) return null;
        if (autoSeekCooldown > 0) { autoSeekCooldown--; return null; }
        autoSeekDone = true;
        AABB box = getBoundingBox().inflate(32.0);
        return sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner && isAswTarget(e))
                .stream()
                .min(Comparator.comparingDouble(this::distanceTo))
                .orElse(null);
    }

    /** Returns true if the entity qualifies as an ASW target (submarine or aquatic creature). */
    private static boolean isAswTarget(Entity e) {
        if (e instanceof com.piranport.npc.deepocean.DeepOceanSubmarineEntity) return true;
        if (e instanceof WaterAnimal) return true;
        if (e instanceof Guardian) return true;
        // Any monster currently submerged in water
        if (e instanceof Monster && e.isUnderWater()) return true;
        return false;
    }

    /**
     * ASW sonar: scan 16-block radius for underwater entities and send detections to owner.
     * Called from main tick every 20 ticks while ASW aircraft is active (CRUISING or ATTACKING).
     */
    private void tickAswSonar(Player owner) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (!(owner instanceof ServerPlayer sp)) return;

        AABB sonarBox = getBoundingBox().inflate(16.0);
        List<Integer> detectedIds = new java.util.ArrayList<>();
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, sonarBox,
                le -> le.isAlive() && le != owner && (le.isInWater() || isAswTarget(le)))) {
            detectedIds.add(e.getId());
            if (detectedIds.size() >= 128) break;
        }
        PacketDistributor.sendToPlayer(sp, new AswSonarSyncPayload(getId(), detectedIds));
    }

    /**
     * RECON_ACTIVE: read movement input from ReconManager and apply to entity.
     * Maintains chunk forcing around current position (view-distance radius, like a player).
     */
    private void tickReconActive(Player owner) {
        // Maintain forced chunks around current position (view-distance radius)
        int cx = getBlockX() >> 4;
        int cz = getBlockZ() >> 4;
        if (cx != lastForcedChunkX || cz != lastForcedChunkZ) {
            updateReconChunkLoading(owner, cx, cz);
            lastForcedChunkX = cx;
            lastForcedChunkZ = cz;
        }
        // Send pending chunks to player each tick (rate limited)
        if (owner instanceof ServerPlayer sp) {
            sendPendingChunks(sp);
        }

        // Apply pending movement input from client (lerp for smooth acceleration/deceleration)
        float[] input = ReconManager.consumeInput(owner.getUUID());
        boolean hasInput = input != null && (input[0] != 0 || input[1] != 0 || input[2] != 0);
        double speed = panelSpeed * 0.5;
        Vec3 target;
        if (hasInput) {
            target = new Vec3(input[0] * speed, input[1] * speed, input[2] * speed);
        } else {
            // No hovering — drift forward at minimum speed along current yaw
            double minSpeed = speed * 0.15;
            float yawRad = (float) Math.toRadians(getYRot());
            target = new Vec3(-Math.sin(yawRad) * minSpeed, 0, Math.cos(yawRad) * minSpeed);
        }
        Vec3 current = getDeltaMovement();
        // Accelerate slowly (inertia), decelerate faster (responsiveness)
        double factor = hasInput ? 0.12 : 0.25;
        setDeltaMovement(current.lerp(target, factor));

        // Update maps in owner's inventory using aircraft position (throttled to every 20 ticks)
        if (!level().isClientSide && tickCount % 20 == 0) {
            for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
                ItemStack stack = owner.getInventory().getItem(i);
                if (stack.getItem() instanceof MapItem mapItem) {
                    MapItemSavedData mapData = MapItem.getSavedData(stack, level());
                    if (mapData != null && !mapData.locked) {
                        mapData.tickCarriedBy(owner, stack);
                        mapItem.update(level(), this, mapData);
                    }
                }
            }
        }
    }

    // ===== Chunk forcing (Phase 32) — view-distance-based for recon =====

    /**
     * Updates forced chunks around the recon aircraft to match the server view distance.
     * Diffs with the previous set to only force/release changed chunks.
     * New chunks are queued for sending to the player.
     */
    private void updateReconChunkLoading(Player owner, int cx, int cz) {
        if (!(level() instanceof ServerLevel sl)) return;
        int radius = Math.min(sl.getServer().getPlayerList().getViewDistance(), 5);

        java.util.Set<Long> desired = new java.util.HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                desired.add(net.minecraft.world.level.ChunkPos.asLong(cx + dx, cz + dz));
            }
        }

        // Release chunks no longer in range
        java.util.Iterator<Long> it = reconForcedChunks.iterator();
        while (it.hasNext()) {
            long key = it.next();
            if (!desired.contains(key)) {
                sl.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),
                        net.minecraft.world.level.ChunkPos.getZ(key), false);
                it.remove();
            }
        }

        // Force-load new chunks and queue them for sending
        for (long key : desired) {
            if (!reconForcedChunks.contains(key)) {
                int x = net.minecraft.world.level.ChunkPos.getX(key);
                int z = net.minecraft.world.level.ChunkPos.getZ(key);
                sl.setChunkForced(x, z, true);
                reconForcedChunks.add(key);
                reconPendingSend.add(key);
            }
        }
    }

    /**
     * Sends pending chunks to the player (rate-limited to avoid network spikes).
     * Chunks that haven't finished generating yet remain in the queue.
     * Note: uses vanilla ClientboundLevelChunkWithLightPacket directly because NeoForge's
     * PacketDistributor has no equivalent for sending raw chunk data to a specific player.
     */
    private void sendPendingChunks(ServerPlayer player) {
        if (reconPendingSend.isEmpty()) return;
        if (!(level() instanceof ServerLevel sl)) return;

        java.util.Iterator<Long> it = reconPendingSend.iterator();
        int sent = 0;
        while (it.hasNext() && sent < RECON_CHUNK_SEND_RATE) {
            long key = it.next();
            int x = net.minecraft.world.level.ChunkPos.getX(key);
            int z = net.minecraft.world.level.ChunkPos.getZ(key);
            net.minecraft.world.level.chunk.LevelChunk chunk = sl.getChunkSource().getChunkNow(x, z);
            if (chunk != null) {
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(
                        chunk, sl.getLightEngine(), null, null));
                it.remove();
                sent++;
            }
        }
    }

    private void releaseAllForcedChunks() {
        if (!(level() instanceof ServerLevel sl)) {
            reconForcedChunks.clear();
            reconPendingSend.clear();
            lastForcedChunkX = Integer.MIN_VALUE;
            lastForcedChunkZ = Integer.MIN_VALUE;
            return;
        }
        for (long key : reconForcedChunks) {
            sl.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),
                    net.minecraft.world.level.ChunkPos.getZ(key), false);
        }
        reconForcedChunks.clear();
        reconPendingSend.clear();
        lastForcedChunkX = Integer.MIN_VALUE;
        lastForcedChunkZ = Integer.MIN_VALUE;
    }

    // ===== Target resolution =====

    /**
     * Resolves the attack target from the fire control lock list.
     * Attack aircraft (bombers/torpedo bombers) skip airborne targets — only fighters engage air targets.
     */
    @Nullable
    private LivingEntity resolveTarget(Player owner) {
        if (!(level() instanceof ServerLevel sl)) return null;

        List<UUID> locks = FireControlManager.getTargets(owner.getUUID());
        if (!locks.isEmpty()) {
            if (attackMode == FlightGroupData.AttackMode.SPREAD) {
                return locks.stream()
                        .map(uuid -> sl.getEntity(uuid))
                        .filter(e -> e instanceof LivingEntity le && le.isAlive() && !isAirborneTarget(le))
                        .map(e -> (LivingEntity) e)
                        .min(Comparator.comparingDouble(e -> distanceTo(e)))
                        .orElse(null);
            } else {
                // FOCUS: find first non-airborne alive target in the lock list
                for (UUID uuid : locks) {
                    Entity e = sl.getEntity(uuid);
                    if (e instanceof LivingEntity le && le.isAlive() && !isAirborneTarget(le)) return le;
                }
                return null;
            }
        }

        // Auto-seek: only if never had FC target and first scan not yet done
        if (hasEverHadFireControl || autoSeekDone) return null;
        if (autoSeekCooldown > 0) { autoSeekCooldown--; return null; }
        autoSeekDone = true;
        AABB box = getBoundingBox().inflate(32.0);
        return sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner && e instanceof Monster && !isAirborneTarget(e))
                .stream()
                .min(Comparator.comparingDouble(e -> distanceTo(e)))
                .orElse(null);
    }

    /** Check if any locked target UUID still resolves to an alive entity. */
    private boolean hasAliveLockedTarget(Player owner) {
        if (!(level() instanceof ServerLevel sl)) return false;
        for (UUID uuid : FireControlManager.getTargets(owner.getUUID())) {
            Entity e = sl.getEntity(uuid);
            if (e instanceof LivingEntity le && le.isAlive()) return true;
        }
        return false;
    }

    /** Check if any locked target is alive AND on the ground/in water (not airborne). */
    private boolean hasAliveGroundLockedTarget(Player owner) {
        if (!(level() instanceof ServerLevel sl)) return false;
        for (UUID uuid : FireControlManager.getTargets(owner.getUUID())) {
            Entity e = sl.getEntity(uuid);
            if (e instanceof LivingEntity le && le.isAlive() && !isAirborneTarget(le)) return true;
        }
        return false;
    }

    /** Check if any locked target is alive AND is an ASW-eligible target. */
    private boolean hasAliveAswLockedTarget(Player owner) {
        if (!(level() instanceof ServerLevel sl)) return false;
        for (UUID uuid : FireControlManager.getTargets(owner.getUUID())) {
            Entity e = sl.getEntity(uuid);
            if (e instanceof LivingEntity le && le.isAlive() && isAswTarget(le)) return true;
        }
        return false;
    }

    /** An entity is considered airborne if it is not on the ground and not in water. */
    private static boolean isAirborneTarget(Entity e) {
        return !e.onGround() && !e.isInWater();
    }

    private void tickReturning(Player owner) {
        Vec3 toOwner = owner.getEyePosition().subtract(position());
        double dist = toOwner.length();
        if (dist < RETURN_ARRIVAL_DIST) { recallAndRemove(); return; }
        setDeltaMovement(toOwner.normalize().scale(Math.min(panelSpeed * 0.4, dist)));
    }

    // ===== Autonomous (no player owner) tick =====

    private void tickAutonomousServer() {
        FlightState state = getFlightState();
        if (state == FlightState.REMOVED) { discard(); return; }

        airtimeTicks++;
        stateTicks++;

        // Fuel consumption
        if (state == FlightState.CRUISING || state == FlightState.ATTACKING) {
            if (currentFuel > 0 && airtimeTicks % FUEL_BURN_INTERVAL == 0) currentFuel--;
            if (currentFuel <= 0) { discard(); return; }
        }
        if (airtimeTicks >= MAX_AIRTIME_TICKS) { discard(); return; }
        if (position().distanceTo(homePosition) > MAX_DIST_FROM_OWNER * 2) { discard(); return; }

        if (attackCooldown > 0) attackCooldown--;

        switch (state) {
            case LAUNCHING -> {
                double targetY = homePosition.y + CRUISE_ALTITUDE;
                double dy = targetY - getY();
                double rise = Math.min(panelSpeed * 0.3, Math.abs(dy));
                setDeltaMovement(getDeltaMovement().x * 0.5, dy > 0 ? rise : -rise, getDeltaMovement().z * 0.5);
                if (stateTicks >= LAUNCH_DURATION || Math.abs(dy) < 1.5) {
                    setState(FlightState.CRUISING);
                }
            }
            case CRUISING -> {
                // Auto-seek players
                if (level() instanceof ServerLevel sl && !payloadType.isEmpty()) {
                    AABB box = getBoundingBox().inflate(48.0);
                    boolean hasTarget = !sl.getEntitiesOfClass(Player.class, box,
                            e -> e.isAlive() && !e.isSpectator()).isEmpty();
                    if (hasTarget) {
                        setState(FlightState.ATTACKING);
                        return;
                    }
                }
                // Orbit home position
                orbitAngle += panelSpeed * 0.015;
                double tx = homePosition.x + Math.cos(orbitAngle) * ORBIT_RADIUS;
                double ty = homePosition.y + CRUISE_ALTITUDE;
                double tz = homePosition.z + Math.sin(orbitAngle) * ORBIT_RADIUS;
                Vec3 toTarget = new Vec3(tx - getX(), ty - getY(), tz - getZ());
                double dist = toTarget.length();
                if (dist > 0.1) {
                    setDeltaMovement(toTarget.normalize().scale(Math.min(panelSpeed * 0.3, dist)));
                } else {
                    setDeltaMovement(Vec3.ZERO);
                }
            }
            case ATTACKING -> {
                if (remainingAmmo <= 0) { discard(); return; }
                if (!(level() instanceof ServerLevel sl)) return;
                AABB box = getBoundingBox().inflate(48.0);
                LivingEntity target = sl.getEntitiesOfClass(Player.class, box,
                                e -> e.isAlive() && !e.isSpectator())
                        .stream().map(e -> (LivingEntity) e)
                        .min(Comparator.comparingDouble(this::distanceTo)).orElse(null);
                if (target == null) { setState(FlightState.CRUISING); return; }
                tickAutonomousLevelBomb(target);
            }
            case RETURNING -> discard();
            default -> {}
        }

        // Update rotation to face movement direction
        Vec3 vel = getDeltaMovement();
        double hDist = vel.horizontalDistance();
        if (hDist > 0.001) {
            setYRot((float) (Math.atan2(-vel.x, vel.z) * (180.0 / Math.PI)));
            setXRot((float) (Math.atan2(vel.y, hDist) * (180.0 / Math.PI)));
        }
    }

    private void tickAutonomousLevelBomb(LivingEntity target) {
        double bombAltitude = target.getY() + 32.0;
        if (getY() < bombAltitude - 1.5) {
            Vec3 toPoint = new Vec3(target.getX() - getX(), bombAltitude - getY(), target.getZ() - getZ());
            double dist = toPoint.length();
            setDeltaMovement(toPoint.normalize().scale(Math.min(panelSpeed * 0.4, dist)));
        } else {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            if (horizDist < 3.0 && attackCooldown <= 0) {
                AerialBombEntity bomb = new AerialBombEntity(level(), panelDamage * 1.5f, 2.5f);
                bomb.setPos(getX(), getY(), getZ());
                bomb.setDeltaMovement(getDeltaMovement().x * 0.1, -0.1, getDeltaMovement().z * 0.1);
                bomb.setSourceAircraftName(getDisplayName());
                if (getOwner() != null) bomb.setOwner(getOwner());
                level().addFreshEntity(bomb);
                remainingAmmo--;
                attackCooldown = 40;
            }
            Vec3 horizontal = new Vec3(dx, 0, dz).normalize().scale(Math.min(panelSpeed * 0.4, horizDist));
            double yCorrect = (bombAltitude - getY()) * 0.15;
            setDeltaMovement(horizontal.x, yCorrect, horizontal.z);
        }
    }

    /**
     * Begin returning to owner — enters RETURNING state so the aircraft visibly
     * flies back before being recalled.  Safe to call from any state; silently
     * ignored if already RETURNING or REMOVED.
     *
     * @param reason debug reason tag (logged when debug mode is active)
     */
    public void startReturning(String reason) {
        FlightState cur = getFlightState();
        if (cur == FlightState.RETURNING || cur == FlightState.REMOVED) return;
        com.piranport.debug.PiranPortDebug.event(
                "Aircraft RETURNING | type={} entityId={} reason={}",
                aircraftType.name(), getId(), reason);
        setState(FlightState.RETURNING);
    }

    /** Remove entity and return aircraft item to the owner's ship core weapon slot. */
    public void recallAndRemove() {
        com.piranport.debug.PiranPortDebug.event(
                "Aircraft RETURN | type={} entityId={} forced={}",
                aircraftType.name(), getId(), isForcedReturn);
        if (!level().isClientSide() && ownerUUID != null && level() instanceof ServerLevel sl) {
            Player owner = sl.getServer().getPlayerList().getPlayer(ownerUUID);
            if (owner != null) {
                // Clean up recon if active
                if (getFlightState() == FlightState.RECON_ACTIVE) {
                    handleStateTransition(FlightState.RECON_ACTIVE, FlightState.RETURNING, owner);
                }
                // Action bar notification
                Component aircraftName = buildReturnStack().getHoverName();
                String msgKey = isForcedReturn
                        ? "message.piranport.aircraft_lost"
                        : "message.piranport.aircraft_returned";
                owner.displayClientMessage(Component.translatable(msgKey, aircraftName), true);
                returnItemToOwner(owner);
            } else {
                // Owner offline — still clean up recon state and chunks
                ReconManager.endRecon(ownerUUID);
                releaseAllForcedChunks();
            }
        }
        discard();
    }

    private void returnItemToOwner(Player player) {
        if (!com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
            // Inventory mode: return aircraft directly to the inventory slot it was launched from
            ItemStack returnStack = buildReturnStack();
            if (weaponSlotIndex == 40) {
                if (player.getInventory().offhand.get(0).isEmpty()) {
                    player.getInventory().offhand.set(0, returnStack);
                } else {
                    player.getInventory().placeItemBackInInventory(returnStack);
                }
            } else if (weaponSlotIndex >= 0 && weaponSlotIndex < player.getInventory().items.size()) {
                if (player.getInventory().items.get(weaponSlotIndex).isEmpty()) {
                    player.getInventory().items.set(weaponSlotIndex, returnStack);
                } else {
                    player.getInventory().placeItemBackInInventory(returnStack);
                }
            } else {
                player.getInventory().placeItemBackInInventory(returnStack);
            }
            return;
        }

        ItemStack coreStack = findCoreStack(player);
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return;

        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(sci.getShipType().totalSlots(), ItemStack.EMPTY);
        contents.copyInto(items);

        if (weaponSlotIndex < sci.getShipType().weaponSlots) {
            ItemStack returnStack = buildReturnStack();
            if (items.get(weaponSlotIndex).isEmpty()) {
                items.set(weaponSlotIndex, returnStack);
            } else {
                // Slot occupied — try to put in player's inventory instead
                if (!player.getInventory().add(returnStack)) {
                    Block.popResource(player.level(), player.blockPosition(), returnStack);
                }
            }
            coreStack.set(ModDataComponents.SHIP_CORE_CONTENTS.get(),
                    ItemContainerContents.fromItems(items));
        }
    }

    private ItemStack findCoreStack(Player player) {
        int size = player.getInventory().getContainerSize();
        if (coreInventorySlot >= 0 && coreInventorySlot < size) {
            ItemStack s = player.getInventory().getItem(coreInventorySlot);
            if (s.getItem() instanceof ShipCoreItem) return s;
        }
        if (player.getMainHandItem().getItem() instanceof ShipCoreItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof ShipCoreItem) return player.getOffhandItem();
        // Fallback: scan entire inventory (no-GUI mode, core can be anywhere)
        for (ItemStack s : player.getInventory().items) {
            if (s.getItem() instanceof ShipCoreItem) return s;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack buildReturnStack() {
        // Restore original ItemStack if available, preserving custom names/enchants/components
        ItemStack stack;
        if (originalStack != null && !originalStack.isEmpty()) {
            stack = originalStack.copy();
        } else {
            stack = switch (aircraftType) {
                case FIGHTER        -> new ItemStack(ModItems.FIGHTER_SQUADRON.get());
                case DIVE_BOMBER    -> new ItemStack(ModItems.DIVE_BOMBER_SQUADRON.get());
                case TORPEDO_BOMBER -> new ItemStack(ModItems.SWORDFISH_TORPEDO.get());
                case LEVEL_BOMBER   -> new ItemStack(ModItems.B25_BOMBER.get());
                case ASW            -> new ItemStack(ModItems.SWORDFISH_ASW.get());
                case RECON          -> new ItemStack(ModItems.RECON_SQUADRON.get());
            };
        }
        // Intentionally reset fuel to 0: returning aircraft must be refueled before next sortie
        AircraftInfo info = stack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (info != null) stack.set(ModDataComponents.AIRCRAFT_INFO.get(), info.withCurrentFuel(0));
        return stack;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide() || ownerUUID == null || !ownerUUID.equals(player.getUUID()))
            return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // Manual fuel resupply: hold aviation_fuel → right-click
        if (held.is(ModItems.AVIATION_FUEL.get())) {
            if (currentFuel < fuelCapacity) {
                held.shrink(1);
                currentFuel = fuelCapacity;
                player.displayClientMessage(
                        Component.translatable("message.piranport.aircraft_refueled", getDisplayName()), true);
                return InteractionResult.sidedSuccess(false);
            }
            // Already full fuel — fall through to recall check
        }

        // Manual ammo resupply: hold the aircraft's payload item → right-click
        if (!held.isEmpty() && !payloadType.isEmpty()) {
            String heldId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
            if (payloadType.equals(heldId) && remainingAmmo < ammoCapacity) {
                int needed = ammoCapacity - remainingAmmo;
                int toLoad = Math.min(needed, held.getCount());
                held.shrink(toLoad);
                remainingAmmo += toLoad;
                hasFired = false;
                player.displayClientMessage(
                        Component.translatable("message.piranport.aircraft_resupplied", getDisplayName()), true);
                return InteractionResult.sidedSuccess(false);
            }
        }

        // Empty hand → fly back then recall
        if (held.isEmpty()) {
            startReturning("player_interact");
            return InteractionResult.sidedSuccess(false);
        }

        return InteractionResult.PASS;
    }

    /**
     * Attempt to draw one unit of the aircraft's payload item from the owner's
     * ship core ammo slots. Returns true if successful.
     */
    /** Lazily resolve the payload type string to an Item reference for fast comparison. */
    private net.minecraft.world.item.Item resolvePayloadItem() {
        if (cachedPayloadItem == null && !payloadType.isEmpty()) {
            cachedPayloadItem = BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.parse(payloadType));
        }
        return cachedPayloadItem;
    }

    private boolean tryAutoResupplyAmmo(Player owner) {
        if (payloadType.isEmpty()) return false;
        ItemStack coreStack = findCoreStack(owner);
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return false;

        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(sci.getShipType().totalSlots(), ItemStack.EMPTY);
        contents.copyInto(items);

        int needed = ammoCapacity - remainingAmmo;
        int loaded = 0;
        int ammoStart = sci.getShipType().weaponSlots;
        int ammoEnd = ammoStart + sci.getShipType().ammoSlots;
        for (int ai = ammoStart; ai < ammoEnd && needed > 0; ai++) {
            ItemStack ammo = items.get(ai);
            if (!ammo.isEmpty() && ammo.getItem() == resolvePayloadItem()) {
                int take = Math.min(needed, ammo.getCount());
                ammo.shrink(take);
                needed -= take;
                loaded += take;
            }
        }
        if (loaded > 0) {
            coreStack.set(ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.fromItems(items));
            remainingAmmo += loaded;
            hasFired = false;
            return true;
        }
        return false;
    }

    /**
     * Attempt to draw one aviation_fuel from the owner's ship core ammo slots.
     * Returns true if successful.
     */
    private boolean tryAutoResupplyFuel(Player owner) {
        ItemStack coreStack = findCoreStack(owner);
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return false;

        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(sci.getShipType().totalSlots(), ItemStack.EMPTY);
        contents.copyInto(items);

        int ammoStart = sci.getShipType().weaponSlots;
        int ammoEnd = ammoStart + sci.getShipType().ammoSlots;
        for (int ai = ammoStart; ai < ammoEnd; ai++) {
            ItemStack ammo = items.get(ai);
            if (ammo.is(ModItems.AVIATION_FUEL.get()) && ammo.getCount() > 0) {
                ammo.shrink(1);
                coreStack.set(ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.fromItems(items));
                currentFuel = fuelCapacity;
                return true;
            }
        }
        return false;
    }

    // ===== Phase 33: air combat =====

    private static int getMaxHealth(AircraftInfo.AircraftType type) {
        return switch (type) {
            case FIGHTER        -> 20;
            case DIVE_BOMBER    -> 15;
            case TORPEDO_BOMBER -> 15;
            case LEVEL_BOMBER   -> 12;
            case ASW            -> 12;
            case RECON          -> 10;
        };
    }

    /**
     * Aircraft takes damage from bullets and other sources.
     * Friendly fire (same ownerUUID) is ignored.
     * On death: explosion effect, discard WITHOUT returning item to owner.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide()) return false;
        FlightState state = getFlightState();
        if (state == FlightState.REMOVED) return false;

        // Friendly fire — ignore all damage from the same player's weapons/aircraft
        Entity attacker = source.getEntity();
        if (ownerUUID != null) {
            if (attacker instanceof Player p && ownerUUID.equals(p.getUUID())) return false;
            if (attacker instanceof AircraftEntity ac && ownerUUID.equals(ac.getOwnerUUID())) return false;
        }

        aircraftHealth -= (int) Math.ceil(amount);

        // Hit sound
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.IRON_GOLEM_HURT, SoundSource.HOSTILE,
                0.4f, 1.4f + random.nextFloat() * 0.2f);

        if (aircraftHealth <= 0) {
            // Explosion effect + destroy (no item return)
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0f, 0.9f + random.nextFloat() * 0.2f);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(),
                        1, 0.3, 0.3, 0.3, 0.0);
            }
            // Clean up recon state if needed + notify owner
            Player owner = getOwner();
            if (state == FlightState.RECON_ACTIVE && ownerUUID != null) {
                if (owner != null) handleStateTransition(FlightState.RECON_ACTIVE, FlightState.REMOVED, owner);
                else { ReconManager.endRecon(ownerUUID); releaseAllForcedChunks(); }
            }
            // Action bar shot-down notification
            if (owner != null) {
                Component aircraftName = buildReturnStack().getHoverName();
                if (attacker != null) {
                    owner.displayClientMessage(Component.translatable(
                            "message.piranport.aircraft_shot_down_by", aircraftName, attacker.getDisplayName()), true);
                } else {
                    owner.displayClientMessage(Component.translatable(
                            "message.piranport.aircraft_shot_down", aircraftName), true);
                }
            }
            com.piranport.debug.PiranPortDebug.event(
                    "Aircraft KILLED | type={} entityId={} attacker={}",
                    aircraftType.name(), getId(),
                    attacker != null ? attacker.getType().toShortString() : "unknown");
            // 掉落飞机物品（防爆+浮水）
            ItemStack dropStack = buildReturnStack();
            if (!dropStack.isEmpty()) {
                AircraftDropEntity drop = new AircraftDropEntity(
                        level(), getX(), getY(), getZ(), dropStack);
                level().addFreshEntity(drop);
            }
            discard();
        }
        return true;
    }

    /**
     * Phase 33: fighter target resolution with extended priority:
     * 1. Fire control locked targets (any entity type)
     * 2. Enemy aircraft (non-same-owner AircraftEntity, 32-block radius)
     * 3. Hostile mobs (32-block radius)
     *
     * When fighter-air-only mode is active, only FC-locked airborne targets are considered.
     */
    @Nullable
    private Entity resolveFighterTarget(Player owner) {
        if (!(level() instanceof ServerLevel sl)) return null;

        boolean airOnly = FireControlManager.isFighterAirOnly(owner.getUUID());

        List<UUID> locks = FireControlManager.getTargets(owner.getUUID());
        if (!locks.isEmpty()) {
            if (attackMode == FlightGroupData.AttackMode.SPREAD) {
                return locks.stream()
                        .map(sl::getEntity)
                        .filter(e -> e != null && e.isAlive()
                                && (!airOnly || e instanceof AircraftEntity || isAirborneTarget(e)))
                        .min(Comparator.comparingDouble(this::distanceTo))
                        .orElse(null);
            } else {
                for (UUID uuid : locks) {
                    Entity e = sl.getEntity(uuid);
                    if (e != null && e.isAlive()
                            && (!airOnly || e instanceof AircraftEntity || isAirborneTarget(e))) {
                        return e;
                    }
                }
                return null;
            }
        }

        // Air-only mode: no auto-seek at all
        if (airOnly) return null;

        // Auto-seek: only if never had FC target and first scan not yet done
        if (hasEverHadFireControl || autoSeekDone) return null;
        if (autoSeekCooldown > 0) { autoSeekCooldown--; return null; }
        autoSeekDone = true;
        AABB box = getBoundingBox().inflate(32.0);

        // Enemy aircraft (non-same-owner, non-self)
        List<AircraftEntity> enemyAircraft = sl.getEntitiesOfClass(AircraftEntity.class, box,
                ae -> ae.isAlive() && ae != this
                        && !(ownerUUID != null && ownerUUID.equals(ae.ownerUUID)));
        if (!enemyAircraft.isEmpty()) {
            return enemyAircraft.stream()
                    .min(Comparator.comparingDouble(this::distanceTo))
                    .orElse(null);
        }

        // Hostile mobs fallback
        return sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner && e instanceof Monster)
                .stream()
                .min(Comparator.comparingDouble(this::distanceTo))
                .orElse(null);
    }

    public void setState(FlightState newState) {
        if (newState == FlightState.ATTACKING) {
            hasFired = false;
            attackCooldown = 0;
            diveCommitted = false;
            diveTarget = null;
        }
        entityData.set(STATE, newState.ordinal());
        stateTicks = 0;
    }

    private static final FlightState[] FLIGHT_STATE_VALUES = FlightState.values();
    private static final AircraftInfo.AircraftType[] AIRCRAFT_TYPE_VALUES = AircraftInfo.AircraftType.values();

    public FlightState getFlightState() {
        int ordinal = entityData.get(STATE);
        if (ordinal < 0 || ordinal >= FLIGHT_STATE_VALUES.length) return FlightState.REMOVED;
        return FLIGHT_STATE_VALUES[ordinal];
    }

    public AircraftInfo.AircraftType getAircraftType() {
        int ordinal = entityData.get(AIRCRAFT_TYPE_DATA);
        if (ordinal < 0 || ordinal >= AIRCRAFT_TYPE_VALUES.length) return AircraftInfo.AircraftType.FIGHTER;
        return AIRCRAFT_TYPE_VALUES[ordinal];
    }

    /** Returns true if this aircraft is owned by the given player. Works client-side (synced). */
    public boolean isOwnedByPlayer(Player player) {
        Optional<UUID> id = entityData.get(OWNER_ID);
        return id.isPresent() && id.get().equals(player.getUUID());
    }

    @Nullable
    public UUID getOwnerUUID() { return ownerUUID; }

    @Nullable
    public Player getOwner() {
        if (ownerUUID == null) return null;
        if (level() instanceof ServerLevel sl) {
            return sl.getServer().getPlayerList().getPlayer(ownerUUID);
        }
        return null;
    }

    @Override
    public boolean isCurrentlyGlowing() {
        // Vanilla glow has highest priority
        if (super.isCurrentlyGlowing()) return true;
        if (level().isClientSide()) {
            return AircraftGlowHelper.shouldGlow(this);
        }
        return false;
    }

    @Override
    public int getTeamColor() {
        if (level().isClientSide()) {
            // Vanilla glow has highest priority for color
            if (super.isCurrentlyGlowing() && !AircraftGlowHelper.isFcTarget(this)) {
                return super.getTeamColor();
            }
            return AircraftGlowHelper.getGlowColor(this);
        }
        return super.getTeamColor();
    }

    /** Isolates client-only class references to avoid NoClassDefFoundError on dedicated servers. */
    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static class AircraftGlowHelper {
        private static final int FRIENDLY_BLUE = 0x3399FF;
        private static final int HOSTILE_RED = 0xFF3333;
        private static final int FC_TARGET_RED = 0xFF0000;
        private static final int ALLY_GREEN = 0x33FF33;

        static boolean shouldGlow(AircraftEntity aircraft) {
            // Fire control locked targets always glow
            if (isFcTarget(aircraft)) return true;
            // Highlight mode (Y key): glow any player-owned aircraft
            if (!com.piranport.ClientTickHandler.isHighlightEnabled()) return false;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.player == null) return false;
            return aircraft.entityData.get(OWNER_ID).isPresent();
        }

        static int getGlowColor(AircraftEntity aircraft) {
            // Fire control targets are always red
            if (isFcTarget(aircraft)) return FC_TARGET_RED;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.player == null) return 0xFFFFFF;
            if (aircraft.isOwnedByPlayer(mc.player)) {
                return FRIENDLY_BLUE;
            }
            if (aircraft.getFlightState() == FlightState.ATTACKING) {
                return HOSTILE_RED;
            }
            return ALLY_GREEN;
        }

        private static boolean isFcTarget(AircraftEntity aircraft) {
            return com.piranport.aviation.ClientFireControlData.getTargets().contains(aircraft.getUUID());
        }
    }

    @Override
    public boolean isPickable() { return true; }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide()) {
            // Safety net: release any forced chunks that weren't cleaned up
            releaseAllForcedChunks();
            // Clean up recon state
            if (ownerUUID != null) {
                ReconManager.endRecon(ownerUUID);
                // Remove slowness if we applied it
                if (appliedSlowness && level() instanceof ServerLevel sl) {
                    Player owner = sl.getServer().getPlayerList().getPlayer(ownerUUID);
                    if (owner != null) {
                        owner.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    }
                    appliedSlowness = false;
                }
            }
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID");
        weaponSlotIndex = tag.getInt("WeaponSlot");
        coreInventorySlot = tag.getInt("CoreSlot");
        try { aircraftType = AircraftInfo.AircraftType.valueOf(tag.getString("AircraftType")); }
        catch (IllegalArgumentException e) { aircraftType = AircraftInfo.AircraftType.FIGHTER; }
        try { attackMode = FlightGroupData.AttackMode.valueOf(tag.getString("AttackMode")); }
        catch (IllegalArgumentException e) { attackMode = FlightGroupData.AttackMode.FOCUS; }
        panelDamage = tag.getFloat("PanelDamage");
        panelSpeed  = tag.getFloat("PanelSpeed");
        remainingAmmo = tag.getInt("RemainingAmmo");
        ammoCapacity = tag.getInt("AmmoCapacity");
        currentFuel = tag.getInt("CurrentFuel");
        fuelCapacity = tag.getInt("FuelCapacity");
        if (fuelCapacity <= 0) fuelCapacity = 1; // guard against old saves
        hasBullets = tag.getBoolean("HasBullets");
        payloadType = tag.getString("PayloadType");
        try { bombingMode = AircraftInfo.BombingMode.valueOf(tag.getString("BombingMode")); }
        catch (IllegalArgumentException e) { bombingMode = AircraftInfo.BombingMode.DIVE; }
        lastForcedChunkX = tag.getInt("LastForcedChunkX");
        lastForcedChunkZ = tag.getInt("LastForcedChunkZ");
        if (!tag.contains("LastForcedChunkX")) lastForcedChunkX = Integer.MIN_VALUE;
        // Release any forced chunks from a previous session (recon interrupted by restart)
        if (tag.contains("ReconForcedChunks") && level() instanceof ServerLevel sl) {
            long[] saved = tag.getLongArray("ReconForcedChunks");
            for (long key : saved) {
                sl.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),
                        net.minecraft.world.level.ChunkPos.getZ(key), false);
            }
        }
        entityData.set(STATE, tag.getInt("FlightState"));
        entityData.set(AIRCRAFT_TYPE_DATA, aircraftType.ordinal());
        if (ownerUUID != null) entityData.set(OWNER_ID, Optional.of(ownerUUID));
        airtimeTicks = tag.getInt("AirtimeTicks");
        hasFired = tag.getBoolean("HasFired");
        aircraftHealth = tag.contains("AircraftHealth")
                ? tag.getInt("AircraftHealth")
                : getMaxHealth(aircraftType);
        if (tag.contains("OriginalStack")) {
            originalStack = ItemStack.parse(level().registryAccess(), tag.getCompound("OriginalStack"))
                    .orElse(ItemStack.EMPTY);
        }
        autonomous = tag.getBoolean("Autonomous");
        if (tag.contains("HomeX")) {
            homePosition = new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
        tag.putInt("WeaponSlot", weaponSlotIndex);
        tag.putInt("CoreSlot", coreInventorySlot);
        tag.putString("AircraftType", aircraftType.name());
        tag.putString("AttackMode", attackMode.name());
        tag.putFloat("PanelDamage", panelDamage);
        tag.putInt("AmmoCapacity", ammoCapacity);
        tag.putInt("CurrentFuel", currentFuel);
        tag.putInt("FuelCapacity", fuelCapacity);
        tag.putBoolean("HasBullets", hasBullets);
        tag.putString("PayloadType", payloadType);
        tag.putString("BombingMode", bombingMode.name());
        tag.putFloat("PanelSpeed", panelSpeed);
        tag.putInt("RemainingAmmo", remainingAmmo);
        tag.putInt("LastForcedChunkX", lastForcedChunkX);
        tag.putInt("LastForcedChunkZ", lastForcedChunkZ);
        // Persist forced chunks so they can be released after server restart
        if (!reconForcedChunks.isEmpty()) {
            long[] chunks = new long[reconForcedChunks.size()];
            int idx = 0;
            for (long key : reconForcedChunks) chunks[idx++] = key;
            tag.putLongArray("ReconForcedChunks", chunks);
        }
        tag.putInt("FlightState", entityData.get(STATE));
        tag.putInt("AirtimeTicks", airtimeTicks);
        tag.putBoolean("HasFired", hasFired);
        tag.putInt("AircraftHealth", aircraftHealth);
        if (originalStack != null && !originalStack.isEmpty()) {
            tag.put("OriginalStack", originalStack.save(level().registryAccess()));
        }
        tag.putBoolean("Autonomous", autonomous);
        if (autonomous) {
            tag.putDouble("HomeX", homePosition.x);
            tag.putDouble("HomeY", homePosition.y);
            tag.putDouble("HomeZ", homePosition.z);
        }
    }
}
