package com.piranport.npc.deepocean;

import com.piranport.npc.ai.FleetGroup;
import com.piranport.npc.ai.FleetGroupManager;
import com.piranport.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Base class for all Deep Ocean enemy entities.
 * Provides water-walking, fleet group membership, sinking death animation, and shared attributes.
 */
public abstract class AbstractDeepOceanEntity extends Monster {

    // --- Synched Data ---
    private static final EntityDataAccessor<Integer> DATA_FLEET_STATE =
            SynchedEntityData.defineId(AbstractDeepOceanEntity.class, EntityDataSerializers.INT);

    // --- Fleet Group ---
    @Nullable
    private UUID fleetGroupId;

    // --- Death animation ---
    private int sinkingTicks = 0;
    private static final int SINKING_DURATION = 40; // 2 seconds
    private boolean isSinking = false;

    protected AbstractDeepOceanEntity(EntityType<? extends AbstractDeepOceanEntity> type, Level level) {
        super(type, level);
    }

    // --- Attributes ---

    public static AttributeSupplier.Builder createDeepOceanAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ARMOR, 8.0);
    }

    // --- Synched Data ---

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLEET_STATE, FleetGroup.State.IDLE.ordinal());
    }

    // --- Fleet Group ---

    @Nullable
    public UUID getFleetGroupId() {
        return fleetGroupId;
    }

    public void setFleetGroupId(@Nullable UUID id) {
        this.fleetGroupId = id;
    }

    /**
     * Get fleet state from synched data (for client rendering).
     */
    public FleetGroup.State getFleetState() {
        int ordinal = entityData.get(DATA_FLEET_STATE);
        FleetGroup.State[] states = FleetGroup.State.values();
        return ordinal >= 0 && ordinal < states.length ? states[ordinal] : FleetGroup.State.IDLE;
    }

    /**
     * Set fleet state (server only, syncs to client).
     */
    public void setFleetState(FleetGroup.State state) {
        entityData.set(DATA_FLEET_STATE, state.ordinal());
    }

    @Nullable
    public FleetGroup getFleetGroup() {
        if (fleetGroupId == null || level().isClientSide()) return null;
        FleetGroupManager mgr = FleetGroupManager.get((ServerLevel) level());
        return mgr.getGroup(fleetGroupId);
    }

    // --- Water Walking ---

    @Override
    public void tick() {
        // Sinking death animation — skip AI and water-walking, but keep baseTick
        if (isSinking) {
            sinkingTicks++;
            if (level().isClientSide()) {
                for (int i = 0; i < 3; i++) {
                    level().addParticle(ParticleTypes.BUBBLE,
                            getX() + (random.nextDouble() - 0.5) * getBbWidth(),
                            getY() + random.nextDouble() * getBbHeight(),
                            getZ() + (random.nextDouble() - 0.5) * getBbWidth(),
                            0, 0.05, 0);
                }
            }
            setDeltaMovement(0, -0.05, 0);
            super.tick();
            if (sinkingTicks >= SINKING_DURATION) {
                isSinking = false;
                remove(RemovalReason.KILLED);
            }
            return;
        }

        super.tick();

        if (!level().isClientSide()) {
            // Float on water surface (same pattern as LowTierDestroyerEntity)
            if (isInWater() && !isEyeInFluid(FluidTags.WATER)) {
                Vec3 vel = getDeltaMovement();
                if (vel.y < 0) {
                    setDeltaMovement(vel.x, 0.0, vel.z);
                }
                resetFallDistance();
            } else if (isInWater()) {
                // Submerged — float up unless this is a submarine
                if (!canSubmerge()) {
                    Vec3 vel = getDeltaMovement();
                    setDeltaMovement(vel.x, Math.max(vel.y, 0.04), vel.z);
                }
            }

            // Sync fleet state
            if (tickCount % 10 == 0) {
                syncFleetState();
            }

            // Fleet state particles (ALERT/COMBAT) — throttled, server-spawned so they sync to all clients
            if (tickCount % 20 == 0 && level() instanceof ServerLevel sl) {
                FleetGroup.State state = getFleetState();
                if (state == FleetGroup.State.ALERT) {
                    sl.sendParticles(ParticleTypes.WAX_ON,
                            getX(), getY() + getBbHeight() + 0.5, getZ(),
                            1, 0, 0.02, 0, 0);
                } else if (state == FleetGroup.State.COMBAT) {
                    sl.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                            getX(), getY() + getBbHeight() + 0.5, getZ(),
                            1, 0, 0.02, 0, 0);
                }
            }
        }

        // Client splash particles (ship wake)
        if (level().isClientSide() && isAlive() && isInWater() && tickCount % 10 == 0) {
            level().addParticle(ParticleTypes.SPLASH,
                    getX() + (random.nextDouble() - 0.5) * 0.8,
                    getY() + 0.1,
                    getZ() + (random.nextDouble() - 0.5) * 0.8,
                    0, 0.05, 0);
        }
    }

    /**
     * Override in submarine to allow staying underwater.
     */
    protected boolean canSubmerge() {
        return false;
    }

    private void syncFleetState() {
        FleetGroup group = getFleetGroup();
        if (group != null) {
            setFleetState(group.getState());
        }
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide()) {
            syncFleetState();
        }
    }

    // --- Death with sinking animation ---

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isSinking) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (this.isRemoved()) return;
        if (!level().isClientSide() && !isSinking) {
            isSinking = true;
            sinkingTicks = 0;
            // Remove from fleet group
            if (fleetGroupId != null) {
                FleetGroupManager mgr = FleetGroupManager.get((ServerLevel) level());
                mgr.removeMember(fleetGroupId, getUUID());
            }
            // Still call super to trigger drops/xp
            super.die(source);
        } else {
            super.die(source);
        }
    }

    // --- Combat helpers ---

    /**
     * Called when this entity acquires a target — notify fleet group.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity prev = getTarget();
        super.setTarget(target);
        if (level().isClientSide() || fleetGroupId == null) return;
        FleetGroupManager mgr = FleetGroupManager.get((ServerLevel) level());
        // Notify fleet group when we first acquire a target
        if (target != null && prev == null) {
            mgr.alertGroup(fleetGroupId, target, getUUID());
        } else if (target == null && prev != null) {
            // If this entity was the group's discoverer, clear the shared target so other members go IDLE
            FleetGroup group = mgr.getGroup(fleetGroupId);
            if (group != null && prev.getUUID().equals(group.getSharedTargetUuid())) {
                mgr.clearGroupTarget(fleetGroupId);
            }
        }
    }

    /**
     * Drop ship-type-specific resources on death.
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (this instanceof DeepOceanSupplyEntity) {
            spawnAtLocation(new ItemStack(ModItems.FUEL.get(), 2 + random.nextInt(3)));
            spawnAtLocation(new ItemStack(Items.IRON_INGOT, 1 + random.nextInt(2)));
        } else if (this instanceof DeepOceanDestroyerEntity) {
            spawnAtLocation(new ItemStack(Items.IRON_INGOT, 1 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(Items.GUNPOWDER, 1 + random.nextInt(2)));
        } else if (this instanceof DeepOceanLightCruiserEntity) {
            spawnAtLocation(new ItemStack(Items.IRON_INGOT, 2 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(Items.GUNPOWDER, 2 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(ModItems.FUEL.get(), 1));
        } else if (this instanceof DeepOceanHeavyCruiserEntity) {
            spawnAtLocation(new ItemStack(Items.IRON_INGOT, 3 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(Items.GUNPOWDER, 2 + random.nextInt(3)));
            spawnAtLocation(new ItemStack(ModItems.FUEL.get(), 1 + random.nextInt(2)));
        } else if (this instanceof DeepOceanBattleCruiserEntity) {
            spawnAtLocation(new ItemStack(Items.IRON_INGOT, 4 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(Items.GUNPOWDER, 3 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(ModItems.FUEL.get(), 2));
            if (random.nextFloat() < 0.3f) {
                spawnAtLocation(new ItemStack(ModItems.CHAOS_SHARD_ALPHA.get(), 1));
            }
        } else if (this instanceof DeepOceanBattleshipEntity) {
            spawnAtLocation(new ItemStack(Items.IRON_INGOT, 4 + random.nextInt(3)));
            spawnAtLocation(new ItemStack(Items.GUNPOWDER, 3 + random.nextInt(3)));
            spawnAtLocation(new ItemStack(ModItems.FUEL.get(), 2 + random.nextInt(2)));
            if (random.nextFloat() < 0.4f) {
                spawnAtLocation(new ItemStack(ModItems.CHAOS_SHARD_BETA.get(), 1));
            }
        } else if (this instanceof DeepOceanLightCarrierEntity) {
            spawnAtLocation(new ItemStack(Items.IRON_INGOT, 3 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(ModItems.FUEL.get(), 3 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(ModItems.AVIATION_FUEL.get(), 2 + random.nextInt(2)));
            if (random.nextFloat() < 0.3f) {
                spawnAtLocation(new ItemStack(ModItems.CHAOS_SHARD_GAMMA.get(), 1));
            }
        } else if (this instanceof DeepOceanCarrierEntity) {
            spawnAtLocation(new ItemStack(Items.IRON_INGOT, 5 + random.nextInt(3)));
            spawnAtLocation(new ItemStack(ModItems.FUEL.get(), 4 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(ModItems.AVIATION_FUEL.get(), 3 + random.nextInt(3)));
            if (random.nextFloat() < 0.5f) {
                spawnAtLocation(new ItemStack(ModItems.CHAOS_SHARD_DELTA.get(), 1));
            }
        } else if (this instanceof DeepOceanSubmarineEntity) {
            spawnAtLocation(new ItemStack(Items.IRON_INGOT, 2 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(ModItems.FUEL.get(), 1 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(ModItems.CHAOS_SHARD_EPSILON.get(), 1));
        }
    }

    /**
     * Get the orbit distance for this ship type. Override in subclasses.
     */
    public double getOrbitDistance() {
        return 16.0;
    }

    /**
     * Get the fire interval in ticks. Override in subclasses.
     */
    public int getFireInterval() {
        return 80;
    }

    /**
     * Get shell damage. Override in subclasses.
     */
    public float getShellDamage() {
        return 5.0f;
    }

    /**
     * Get explosion power. Override in subclasses.
     */
    public float getExplosionPower() {
        return 1.5f;
    }

    /**
     * How many shots between tracking rounds (min). Override in subclasses.
     */
    public int getTrackingIntervalMin() {
        return 3;
    }

    /**
     * How many shots between tracking rounds (max). Override in subclasses.
     */
    public int getTrackingIntervalMax() {
        return 6;
    }

    /**
     * Whether this entity can use torpedoes. Override in subclasses.
     */
    public boolean canUseTorpedoes() {
        return false;
    }

    /**
     * Whether this entity can launch aircraft. Override in subclasses.
     */
    public boolean canLaunchAircraft() {
        return false;
    }

    /**
     * Torpedo damage. Override in subclasses.
     */
    public float getTorpedoDamage() {
        return 8.0f;
    }

    /**
     * Max aircraft capacity for carriers. Override in subclasses.
     */
    public int getMaxAircraft() {
        return 0;
    }

    // --- Glowing & Persistence ---

    @Override
    public boolean isCurrentlyGlowing() {
        // Submarines should not glow when submerged (breaks stealth)
        if (canSubmerge() && isEyeInFluid(FluidTags.WATER)) {
            return false;
        }
        return true; // Always visible without custom model
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return false; // Don't despawn
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    // --- Persistence ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (fleetGroupId != null) {
            tag.putUUID("FleetGroupId", fleetGroupId);
        }
        tag.putBoolean("Sinking", isSinking);
        tag.putInt("SinkingTicks", sinkingTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("FleetGroupId")) {
            fleetGroupId = tag.getUUID("FleetGroupId");
        }
        isSinking = tag.getBoolean("Sinking");
        sinkingTicks = tag.getInt("SinkingTicks");
    }
}
