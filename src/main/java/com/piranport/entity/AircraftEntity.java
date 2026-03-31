package com.piranport.entity;

import com.piranport.aviation.FireControlManager;
import com.piranport.config.ModCommonConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import com.piranport.aviation.ReconManager;
import com.piranport.component.AircraftInfo;
import com.piranport.component.FlightGroupData;
import com.piranport.item.ShipCoreItem;
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

    // Phase 32 runtime fields
    private FlightState lastKnownState = FlightState.LAUNCHING;
    private int lastForcedChunkX = Integer.MIN_VALUE;
    private int lastForcedChunkZ = Integer.MIN_VALUE;
    private boolean appliedSlowness = false;

    // Phase 33: aircraft health (runtime only — not saved to NBT; aircraft resets on re-launch)
    private int aircraftHealth = 20;

    // Phase 34: set to true when defense mechanisms force a recall (stuck, timeout, distance)
    private boolean isForcedReturn = false;

    private static final int MAX_AIRTIME_TICKS = 12000;
    private static final double MAX_DIST_FROM_OWNER = 48.0;
    private static final double MAX_RECON_DIST = 200.0;  // Phase 32
    private static final int STUCK_CHECK_INTERVAL = 60;
    private static final double STUCK_THRESHOLD = 0.1;
    private static final int LAUNCH_DURATION = 30;
    private static final double CRUISE_ALTITUDE = 18.0;
    private static final double ORBIT_RADIUS = 8.0;
    private static final double RETURN_ARRIVAL_DIST = 3.0;

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
        entity.aircraftHealth = getMaxHealth(entity.aircraftType);
        entity.entityData.set(AIRCRAFT_TYPE_DATA, entity.aircraftType.ordinal());
        entity.entityData.set(OWNER_ID, Optional.of(owner.getUUID()));

        Vec3 look = owner.getLookAngle();
        entity.setPos(owner.getX() + look.x * 0.8, owner.getEyeY(), owner.getZ() + look.z * 0.8);
        entity.orbitAngle = Math.atan2(look.z, look.x);
        entity.stuckCheckPos = entity.position();
        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, FlightState.LAUNCHING.ordinal());
        builder.define(AIRCRAFT_TYPE_DATA, 0);
        builder.define(OWNER_ID, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();

        // Client: particles only
        if (level().isClientSide()) {
            if (getFlightState() == FlightState.CRUISING && tickCount % 8 == 0) {
                level().addParticle(ParticleTypes.CLOUD, getX(), getY(), getZ(), 0, 0.02, 0);
            }
            return;
        }

        FlightState state = getFlightState();
        if (state == FlightState.REMOVED) { discard(); return; }

        Player owner = getOwner();
        if (owner == null) {
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

        // Defense: 10-min airtime limit
        if (airtimeTicks >= MAX_AIRTIME_TICKS) {
            if (state == FlightState.RETURNING) { recallAndRemove(); return; }
            isForcedReturn = true;
            setState(FlightState.RETURNING);
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
                setState(FlightState.RETURNING);
            } else {
                Vec3 near = owner.position().add(
                        (random.nextDouble() - 0.5) * 4, 3, (random.nextDouble() - 0.5) * 4);
                setPos(near.x, near.y, near.z);
                stuckTicks = 0;
                stuckCheckPos = position();
                isForcedReturn = true;
                setState(FlightState.RETURNING);
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

        // Apply movement
        setPos(getX() + getDeltaMovement().x,
               getY() + getDeltaMovement().y,
               getZ() + getDeltaMovement().z);
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
        // Auto fuel resupply: draw from core if empty and config enabled
        if (currentFuel <= 0 && ModCommonConfig.AUTO_RESUPPLY_ENABLED.get()) {
            tryAutoResupplyFuel(owner);
        }

        // Transition to ATTACKING if owner has locked targets, aircraft has attack capability, and is not RECON
        if (aircraftType != AircraftInfo.AircraftType.RECON
                && (hasBullets || !payloadType.isEmpty())
                && !FireControlManager.getTargets(owner.getUUID()).isEmpty()) {
            setState(FlightState.ATTACKING);
            return;
        }

        // FOLLOW mode: orbit the owner's active recon aircraft, or fall back to player
        if (attackMode == FlightGroupData.AttackMode.FOLLOW) {
            AircraftEntity reconAircraft = findOwnerReconAircraft(owner);
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
            setState(FlightState.RETURNING);
            return;
        }

        // 子弹优先：使用战斗机AI
        if (hasBullets) {
            Entity target = resolveFighterTarget(owner);
            if (target == null) { setState(FlightState.CRUISING); return; }
            tickFighterAttack(owner, target);
            return;
        }

        // 无子弹时按挂载类型决定攻击行为
        switch (payloadType) {
            case "piranport:aerial_torpedo" -> {
                LivingEntity target = resolveTarget(owner);
                if (target == null) { setState(FlightState.CRUISING); return; }
                tickTorpedoBomberAttack(owner, target);
            }
            case "piranport:aerial_bomb" -> {
                LivingEntity target = resolveTarget(owner);
                if (target == null) { setState(FlightState.CRUISING); return; }
                if (bombingMode == AircraftInfo.BombingMode.LEVEL) {
                    tickLevelBomberAttack(owner, target);
                } else {
                    tickDiveBomberAttack(owner, target);
                }
            }
            default -> setState(FlightState.RETURNING); // 无挂载，不攻击
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
        if (ammoEnabled && remainingAmmo <= 0) { setState(FlightState.RETURNING); return; }

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
            level().addFreshEntity(bullet);
            attackCooldown = 5;

            if (ammoEnabled) {
                remainingAmmo--;
                if (remainingAmmo <= 0) {
                    if (ModCommonConfig.AUTO_RESUPPLY_ENABLED.get() && tryAutoResupplyAmmo(owner)) {
                        // Ammo restored (payload type must be set) — continue
                    } else {
                        setState(FlightState.RETURNING);
                    }
                }
            }
        }
    }

    /**
     * DIVE_BOMBER: climb to target+18 blocks, then dive.
     * On close contact (dist < 4), drop an AerialBombEntity and return.
     */
    private void tickDiveBomberAttack(Player owner, LivingEntity target) {
        if (hasFired) {
            if (ModCommonConfig.AUTO_RESUPPLY_ENABLED.get() && tryAutoResupplyAmmo(owner)) {
                setState(FlightState.CRUISING);
            } else {
                setState(FlightState.RETURNING);
            }
            return;
        }

        double climbY = target.getY() + 18.0;

        if (getY() < climbY - 1.0 && stateTicks < 80) {
            Vec3 toClimb = new Vec3(target.getX() - getX(), climbY - getY(), target.getZ() - getZ());
            double dist = toClimb.length();
            setDeltaMovement(toClimb.normalize().scale(Math.min(panelSpeed * 0.4, dist)));
        } else {
            Vec3 toTarget = target.getEyePosition().subtract(position());
            double dist = toTarget.length();
            if (dist < 4.0) {
                AerialBombEntity bomb = new AerialBombEntity(level(), panelDamage * 1.5f, 2.5f);
                bomb.setPos(getX(), getY(), getZ());
                bomb.setDeltaMovement(getDeltaMovement().x * 0.2, -0.3, getDeltaMovement().z * 0.2);
                bomb.setOwner(owner);
                level().addFreshEntity(bomb);
                hasFired = true;
                setState(FlightState.RETURNING);
                return;
            }
            setDeltaMovement(toTarget.normalize().scale(Math.min(panelSpeed * 0.6, dist)));
        }
    }

    /**
     * TORPEDO_BOMBER: fly low, fire torpedo when 20-30 blocks from target.
     * Fires early so the torpedo travels forward to hit.
     */
    private void tickTorpedoBomberAttack(Player owner, LivingEntity target) {
        if (hasFired) {
            if (ModCommonConfig.AUTO_RESUPPLY_ENABLED.get() && tryAutoResupplyAmmo(owner)) {
                setState(FlightState.CRUISING);
            } else {
                setState(FlightState.RETURNING);
            }
            return;
        }

        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        if (horizDist >= 20 && horizDist <= 30) {
            TorpedoEntity torpedo = new TorpedoEntity(ModEntityTypes.TORPEDO_ENTITY.get(), level());
            Vec3 dir = new Vec3(dx, 0, dz).normalize();
            torpedo.setPos(getX(), getY(), getZ());
            torpedo.setDeltaMovement(dir.x * 0.7, 0, dir.z * 0.7);
            torpedo.setOwner(owner);
            level().addFreshEntity(torpedo);
            hasFired = true;
            setState(FlightState.RETURNING);
            return;
        }

        if (horizDist < 16) {
            // Overshot — abort
            hasFired = true;
            setState(FlightState.RETURNING);
            return;
        }

        // Approach at low altitude (target.y + 2)
        double targetY = target.getY() + 2.0;
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
                setState(FlightState.RETURNING);
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
     * RECON_ACTIVE: read movement input from ReconManager and apply to entity.
     * Maintains chunk forcing around current position.
     */
    private void tickReconActive(Player owner) {
        // Maintain forced chunks around current position
        int cx = getBlockX() >> 4;
        int cz = getBlockZ() >> 4;
        if (cx != lastForcedChunkX || cz != lastForcedChunkZ) {
            if (lastForcedChunkX != Integer.MIN_VALUE) {
                releaseChunks(lastForcedChunkX, lastForcedChunkZ);
            }
            forceChunks(cx, cz);
            lastForcedChunkX = cx;
            lastForcedChunkZ = cz;
        }

        // Apply pending movement input from client
        float[] input = ReconManager.consumeInput(owner.getUUID());
        if (input != null && (input[0] != 0 || input[1] != 0 || input[2] != 0)) {
            double speed = panelSpeed * 0.5;
            setDeltaMovement(input[0] * speed, input[1] * speed, input[2] * speed);
        } else {
            // No input — decelerate smoothly
            setDeltaMovement(getDeltaMovement().scale(0.7));
        }
    }

    // ===== Chunk forcing (Phase 32) =====

    private void forceChunks(int cx, int cz) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                sl.setChunkForced(cx + dx, cz + dz, true);
            }
        }
    }

    private void releaseChunks(int cx, int cz) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                sl.setChunkForced(cx + dx, cz + dz, false);
            }
        }
    }

    private void releaseAllForcedChunks() {
        if (lastForcedChunkX != Integer.MIN_VALUE) {
            releaseChunks(lastForcedChunkX, lastForcedChunkZ);
            lastForcedChunkX = Integer.MIN_VALUE;
            lastForcedChunkZ = Integer.MIN_VALUE;
        }
    }

    // ===== Target resolution =====

    /**
     * Resolves the attack target from the fire control lock list.
     */
    @Nullable
    private LivingEntity resolveTarget(Player owner) {
        if (!(level() instanceof ServerLevel sl)) return null;

        List<UUID> locks = FireControlManager.getTargets(owner.getUUID());
        if (!locks.isEmpty()) {
            if (attackMode == FlightGroupData.AttackMode.SPREAD) {
                return locks.stream()
                        .map(uuid -> sl.getEntity(uuid))
                        .filter(e -> e instanceof LivingEntity le && le.isAlive())
                        .map(e -> (LivingEntity) e)
                        .min(Comparator.comparingDouble(e -> distanceTo(e)))
                        .orElse(null);
            } else {
                Entity e = sl.getEntity(locks.get(0));
                if (e instanceof LivingEntity le && le.isAlive()) return le;
            }
        }

        // Auto-seek: nearest hostile within 32 blocks
        AABB box = getBoundingBox().inflate(32.0);
        return sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner && e instanceof Monster)
                .stream()
                .min(Comparator.comparingDouble(e -> distanceTo(e)))
                .orElse(null);
    }

    private void tickReturning(Player owner) {
        Vec3 toOwner = owner.getEyePosition().subtract(position());
        double dist = toOwner.length();
        if (dist < RETURN_ARRIVAL_DIST) { recallAndRemove(); return; }
        setDeltaMovement(toOwner.normalize().scale(Math.min(panelSpeed * 0.4, dist)));
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
        if (!com.piranport.config.ModCommonConfig.SHIP_CORE_GUI_ENABLED.get()) {
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
            items.set(weaponSlotIndex, buildReturnStack());
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
        return ItemStack.EMPTY;
    }

    private ItemStack buildReturnStack() {
        ItemStack stack = switch (aircraftType) {
            case FIGHTER        -> new ItemStack(ModItems.FIGHTER_SQUADRON.get());
            case DIVE_BOMBER    -> new ItemStack(ModItems.DIVE_BOMBER_SQUADRON.get());
            case TORPEDO_BOMBER -> new ItemStack(ModItems.TORPEDO_BOMBER_SQUADRON.get());
            case LEVEL_BOMBER   -> new ItemStack(ModItems.LEVEL_BOMBER_SQUADRON.get());
            case RECON          -> new ItemStack(ModItems.RECON_SQUADRON.get());
        };
        AircraftInfo orig = stack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (orig != null) stack.set(ModDataComponents.AIRCRAFT_INFO.get(), orig.withCurrentFuel(0));
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
                held.shrink(1);
                remainingAmmo = ammoCapacity;
                hasFired = false;
                player.displayClientMessage(
                        Component.translatable("message.piranport.aircraft_resupplied", getDisplayName()), true);
                return InteractionResult.sidedSuccess(false);
            }
        }

        // Empty hand → recall
        if (held.isEmpty()) {
            recallAndRemove();
            return InteractionResult.sidedSuccess(false);
        }

        return InteractionResult.PASS;
    }

    /**
     * Attempt to draw one unit of the aircraft's payload item from the owner's
     * ship core ammo slots. Returns true if successful.
     */
    private boolean tryAutoResupplyAmmo(Player owner) {
        if (payloadType.isEmpty()) return false;
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
            if (!ammo.isEmpty() && BuiltInRegistries.ITEM.getKey(ammo.getItem()).toString().equals(payloadType)) {
                ammo.shrink(1);
                coreStack.set(ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.fromItems(items));
                remainingAmmo = ammoCapacity;
                hasFired = false;
                return true;
            }
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

        // Friendly fire — ignore damage from the same player's attacks
        Entity attacker = source.getEntity();
        if (attacker instanceof Player p && ownerUUID != null && ownerUUID.equals(p.getUUID())) {
            return false;
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
                owner.displayClientMessage(Component.translatable(
                        "message.piranport.aircraft_shot_down", buildReturnStack().getHoverName()), true);
            }
            com.piranport.debug.PiranPortDebug.event(
                    "Aircraft KILLED | type={} entityId={} attacker={}",
                    aircraftType.name(), getId(),
                    attacker != null ? attacker.getType().toShortString() : "unknown");
            discard();
        }
        return true;
    }

    /**
     * Phase 33: fighter target resolution with extended priority:
     * 1. Fire control locked targets (any entity type)
     * 2. Enemy aircraft (non-same-owner AircraftEntity, 32-block radius)
     * 3. Hostile mobs (32-block radius)
     */
    @Nullable
    private Entity resolveFighterTarget(Player owner) {
        if (!(level() instanceof ServerLevel sl)) return null;

        List<UUID> locks = FireControlManager.getTargets(owner.getUUID());
        if (!locks.isEmpty()) {
            if (attackMode == FlightGroupData.AttackMode.SPREAD) {
                return locks.stream()
                        .map(sl::getEntity)
                        .filter(e -> e != null && e.isAlive())
                        .min(Comparator.comparingDouble(this::distanceTo))
                        .orElse(null);
            } else {
                Entity e = sl.getEntity(locks.get(0));
                if (e != null && e.isAlive()) return e;
            }
        }

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
        }
        entityData.set(STATE, newState.ordinal());
        stateTicks = 0;
    }

    public FlightState getFlightState() {
        int ordinal = entityData.get(STATE);
        FlightState[] values = FlightState.values();
        if (ordinal < 0 || ordinal >= values.length) return FlightState.REMOVED;
        return values[ordinal];
    }

    public AircraftInfo.AircraftType getAircraftType() {
        int ordinal = entityData.get(AIRCRAFT_TYPE_DATA);
        AircraftInfo.AircraftType[] values = AircraftInfo.AircraftType.values();
        if (ordinal < 0 || ordinal >= values.length) return AircraftInfo.AircraftType.FIGHTER;
        return values[ordinal];
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
        if (level().isClientSide()) {
            return AircraftGlowHelper.shouldGlow(this);
        }
        return super.isCurrentlyGlowing();
    }

    /** Isolates client-only class references to avoid NoClassDefFoundError on dedicated servers. */
    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static class AircraftGlowHelper {
        static boolean shouldGlow(AircraftEntity aircraft) {
            if (!com.piranport.ClientTickHandler.isHighlightEnabled()) return false;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            return mc != null && mc.player != null && aircraft.isOwnedByPlayer(mc.player);
        }
    }

    @Override
    public boolean isPickable() { return true; }

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
        entityData.set(STATE, tag.getInt("FlightState"));
        entityData.set(AIRCRAFT_TYPE_DATA, aircraftType.ordinal());
        if (ownerUUID != null) entityData.set(OWNER_ID, Optional.of(ownerUUID));
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
        tag.putInt("FlightState", entityData.get(STATE));
    }
}
