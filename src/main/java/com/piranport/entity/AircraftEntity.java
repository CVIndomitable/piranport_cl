package com.piranport.entity;

import com.piranport.aviation.FireControlManager;
import com.piranport.component.AircraftInfo;
import com.piranport.component.FlightGroupData;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AircraftEntity extends Entity {

    public enum FlightState {
        LAUNCHING, CRUISING, ATTACKING, RETURNING, REMOVED
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

    // Runtime (not saved)
    private int airtimeTicks = 0;
    private int stuckTicks = 0;
    private Vec3 stuckCheckPos = Vec3.ZERO;
    private double orbitAngle = 0;
    private int stateTicks = 0;
    private int attackCooldown = 0;
    private boolean hasFired = false;

    private static final int MAX_AIRTIME_TICKS = 12000;
    private static final double MAX_DIST_FROM_OWNER = 48.0;
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
                                        int coreInventorySlot) {
        AircraftEntity entity = new AircraftEntity(ModEntityTypes.AIRCRAFT_ENTITY.get(), level);
        entity.ownerUUID = owner.getUUID();
        entity.weaponSlotIndex = weaponSlotIndex;
        entity.coreInventorySlot = coreInventorySlot;
        entity.attackMode = attackMode;

        AircraftInfo info = aircraftStack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (info != null) {
            entity.aircraftType = info.aircraftType();
            entity.panelDamage = info.panelDamage();
            entity.panelSpeed = info.panelSpeed();
            entity.remainingAmmo = info.ammoCapacity();
        }
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

        airtimeTicks++;
        stateTicks++;

        // Defense: 10-min airtime limit
        if (airtimeTicks >= MAX_AIRTIME_TICKS) {
            if (state == FlightState.RETURNING) { recallAndRemove(); return; }
            setState(FlightState.RETURNING);
        }

        // Defense: stuck detection (every 60 ticks)
        if (stateTicks % STUCK_CHECK_INTERVAL == 0) {
            double moved = position().distanceTo(stuckCheckPos);
            if (moved < STUCK_THRESHOLD) {
                stuckTicks += STUCK_CHECK_INTERVAL;
                if (stuckTicks >= STUCK_CHECK_INTERVAL) { recallAndRemove(); return; }
            } else {
                stuckTicks = 0;
            }
            stuckCheckPos = position();
        }

        Player owner = getOwner();
        if (owner == null) { discard(); return; }

        // Defense: distance limit
        if (distanceTo(owner) > MAX_DIST_FROM_OWNER) {
            Vec3 near = owner.position().add(
                    (random.nextDouble() - 0.5) * 4, 3, (random.nextDouble() - 0.5) * 4);
            setPos(near.x, near.y, near.z);
            stuckTicks = 0;
            stuckCheckPos = position();
            setState(FlightState.RETURNING);
        }

        if (attackCooldown > 0) attackCooldown--;

        switch (state) {
            case LAUNCHING  -> tickLaunching(owner);
            case CRUISING   -> tickCruising(owner);
            case ATTACKING  -> tickAttacking(owner);
            case RETURNING  -> tickReturning(owner);
        }

        // Apply movement
        setPos(getX() + getDeltaMovement().x,
               getY() + getDeltaMovement().y,
               getZ() + getDeltaMovement().z);
    }

    private void tickLaunching(Player owner) {
        double targetY = owner.getY() + CRUISE_ALTITUDE;
        double dy = targetY - getY();
        double rise = Math.min(panelSpeed * 0.3, Math.abs(dy));
        setDeltaMovement(getDeltaMovement().x * 0.5, dy > 0 ? rise : -rise, getDeltaMovement().z * 0.5);
        if (stateTicks >= LAUNCH_DURATION || Math.abs(dy) < 1.5) {
            setState(FlightState.CRUISING);
        }
    }

    private void tickCruising(Player owner) {
        // Transition to ATTACKING if owner has locked targets
        if (!FireControlManager.getTargets(owner.getUUID()).isEmpty()) {
            setState(FlightState.ATTACKING);
            return;
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

    // ===== ATTACKING dispatch =====

    private void tickAttacking(Player owner) {
        LivingEntity target = resolveTarget(owner);
        if (target == null) {
            setState(FlightState.CRUISING);
            return;
        }
        switch (aircraftType) {
            case FIGHTER        -> tickFighterAttack(owner, target);
            case DIVE_BOMBER    -> tickDiveBomberAttack(owner, target);
            case TORPEDO_BOMBER -> tickTorpedoBomberAttack(owner, target);
            case LEVEL_BOMBER   -> tickLevelBomberAttack(owner, target);
        }
    }

    /**
     * FIGHTER: hover at ~11 blocks and fire bullets. 64 rounds total.
     */
    private void tickFighterAttack(Player owner, LivingEntity target) {
        if (remainingAmmo <= 0) { setState(FlightState.RETURNING); return; }

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
            remainingAmmo--;
            attackCooldown = 5;

            if (remainingAmmo <= 0) {
                setState(FlightState.RETURNING);
            }
        }
    }

    /**
     * DIVE_BOMBER: climb to target+18 blocks, then dive.
     * On close contact (dist < 4), drop an AerialBombEntity and return.
     */
    private void tickDiveBomberAttack(Player owner, LivingEntity target) {
        if (hasFired) { setState(FlightState.RETURNING); return; }

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
        if (hasFired) { setState(FlightState.RETURNING); return; }

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
        if (remainingAmmo <= 0) { setState(FlightState.RETURNING); return; }

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
        if (!level().isClientSide() && ownerUUID != null && level() instanceof ServerLevel sl) {
            Player owner = sl.getServer().getPlayerList().getPlayer(ownerUUID);
            if (owner != null) returnItemToOwner(owner);
        }
        discard();
    }

    private void returnItemToOwner(Player player) {
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
        };
        AircraftInfo orig = stack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (orig != null) stack.set(ModDataComponents.AIRCRAFT_INFO.get(), orig.withCurrentFuel(0));
        return stack;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide() && ownerUUID != null && ownerUUID.equals(player.getUUID())) {
            recallAndRemove();
            return InteractionResult.sidedSuccess(false);
        }
        return InteractionResult.PASS;
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
        return FlightState.values()[entityData.get(STATE)];
    }

    public AircraftInfo.AircraftType getAircraftType() {
        return AircraftInfo.AircraftType.values()[entityData.get(AIRCRAFT_TYPE_DATA)];
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
    public boolean isPickable() { return true; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID");
        weaponSlotIndex = tag.getInt("WeaponSlot");
        coreInventorySlot = tag.getInt("CoreSlot");
        try { aircraftType = AircraftInfo.AircraftType.valueOf(tag.getString("AircraftType")); }
        catch (Exception e) { aircraftType = AircraftInfo.AircraftType.FIGHTER; }
        try { attackMode = FlightGroupData.AttackMode.valueOf(tag.getString("AttackMode")); }
        catch (Exception e) { attackMode = FlightGroupData.AttackMode.FOCUS; }
        panelDamage = tag.getFloat("PanelDamage");
        panelSpeed  = tag.getFloat("PanelSpeed");
        remainingAmmo = tag.getInt("RemainingAmmo");
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
        tag.putFloat("PanelSpeed", panelSpeed);
        tag.putInt("RemainingAmmo", remainingAmmo);
        tag.putInt("FlightState", entityData.get(STATE));
    }
}
