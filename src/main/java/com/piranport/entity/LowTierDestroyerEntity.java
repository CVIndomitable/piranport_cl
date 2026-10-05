package com.piranport.entity;

import com.piranport.combat.cannon.CannonAim;
import com.piranport.combat.cannon.fire.CannonFireRequest;
import com.piranport.combat.cannon.fire.CannonFireService;
import com.piranport.combat.cannon.fire.CannonProjectileFactory;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.npc.ai.NpcCombatTuning;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Low-tier enemy destroyer NPC for the tutorial dungeon.
 * Hostile, orbits at 15 blocks, fires small-caliber HE shells with occasional tracking rounds.
 * Floats on water surface. Shares alert state with nearby destroyers.
 */
public class LowTierDestroyerEntity extends Monster {

    // --- Tuning ---
    // 手感数值改走调试终端（global.low_tier_destroyer.*，见 ModEquipmentConfig）。
    // 弹速/散布复用既有的 global.shell.*（NpcCombatTuning），与其它 NPC 舰船同一份手感来源。

    // --- State ---

    private int fireCooldown = 0;
    private int shotsFired = 0;
    /** Fire a tracking shot when shotsFired reaches this value. */
    private int nextTrackingShotAt;
    /** Current orbit angle around target (radians). */
    private double orbitAngle;

    public LowTierDestroyerEntity(EntityType<? extends LowTierDestroyerEntity> type, Level level) {
        super(type, level);
        this.nextTrackingShotAt = 2 + random.nextInt(4); // first tracking at shot 2-5
        this.orbitAngle = random.nextDouble() * Math.PI * 2;
    }

    // --- Attributes ---

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)          // 15 hearts — easy tutorial mob
                .add(Attributes.MOVEMENT_SPEED, 0.25)      // ~player walk speed
                .add(Attributes.FOLLOW_RANGE, ModEquipmentConfig.LOW_TIER_DESTROYER_DETECTION_RANGE.get())
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ARMOR, 4.0);
    }

    // --- AI ---

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new OrbitAndShootGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, LowTierDestroyerEntity.class));
    }

    // --- Tick ---

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()) {
            // Float on water surface
            if (isInWater()) {
                Vec3 vel = getDeltaMovement();
                setDeltaMovement(vel.x, Math.max(vel.y, 0.04), vel.z);
            }

            // Group alert: share target with nearby idle destroyers
            if (getTarget() != null && tickCount % 20 == 0) {
                alertNearbyDestroyers();
            }

            if (fireCooldown > 0) fireCooldown--;
        }

        // Client splash particles (ship wake)
        if (level().isClientSide() && tickCount % 10 == 0) {
            level().addParticle(ParticleTypes.SPLASH,
                    getX() + (random.nextDouble() - 0.5) * 0.8,
                    getY() + 0.1,
                    getZ() + (random.nextDouble() - 0.5) * 0.8,
                    0, 0.05, 0);
        }
    }

    // --- Group Alert ---

    private void alertNearbyDestroyers() {
        LivingEntity target = getTarget();
        if (target == null) return;
        AABB alertBox = getBoundingBox().inflate(ModEquipmentConfig.LOW_TIER_DESTROYER_ALERT_RANGE.get());
        List<LowTierDestroyerEntity> nearby = level().getEntitiesOfClass(
                LowTierDestroyerEntity.class, alertBox, d -> d != this && d.getTarget() == null);
        for (LowTierDestroyerEntity d : nearby) {
            d.setTarget(target);
        }
    }

    // --- Firing ---

    private void fireAtTarget(LivingEntity target) {
        if (level().isClientSide()) return;

        shotsFired++;
        boolean tracking = (shotsFired >= nextTrackingShotAt);
        if (tracking) {
            nextTrackingShotAt = shotsFired + 2 + random.nextInt(4);
        }

        // Aim: direction to target with arc compensation for gravity
        Vec3 aim = target.getEyePosition().subtract(getEyePosition());
        double hDist = aim.horizontalDistance();
        double arcY = hDist * 0.05; // rough upward compensation for parabolic drop
        float shellDamage = (float) (double) ModEquipmentConfig.LOW_TIER_DESTROYER_SHELL_DAMAGE.get();
        float explosionPower = (float) (double) ModEquipmentConfig.LOW_TIER_DESTROYER_EXPLOSION_POWER.get();
        float shellSpeed = NpcCombatTuning.shellSpeed();
        float shellInaccuracy = NpcCombatTuning.shellInaccuracy();
        ItemStack weapon = new ItemStack(ModItems.SINGLE_SMALL_GUN.get());
        ItemStack shellStack = new ItemStack(ModItems.SMALL_HE_SHELL.get());
        CannonFireRequest request = CannonFireService.request(
                level(), this, weapon, shellStack, shellDamage, explosionPower,
                shellSpeed, 0.015f, 9.8f, shellInaccuracy, shellInaccuracy,
                4, true, false,
                new CannonAim.DirectAim(target.getBoundingBox().getCenter()), getEyePosition());
        if (!CannonFireService.isValid(request)) return;
        var shell = CannonProjectileFactory.create(request);
        shell.shoot(aim.x, aim.y + arcY, aim.z, shellSpeed, shellInaccuracy);

        if (tracking) {
            shell.setTracking(target.getId());
        }

        level().addFreshEntity(shell);
    }

    // --- Rendering / Dungeon behaviour ---

    /** Always show glowing outline so the entity is visible without a custom model. */
    @Override
    public boolean isCurrentlyGlowing() {
        // Respect vanilla glow priority
        if (super.isCurrentlyGlowing()) return true;
        // Dungeon entities always glow by default
        return true;
    }

    /** Do not despawn — dungeon mobs must persist until killed. */
    @Override
    public boolean removeWhenFarAway(double distSq) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    // --- Persistence ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ShotsFired", shotsFired);
        tag.putInt("NextTrackingShot", nextTrackingShotAt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        shotsFired = tag.getInt("ShotsFired");
        nextTrackingShotAt = tag.getInt("NextTrackingShot");
        if (nextTrackingShotAt == 0) {
            nextTrackingShotAt = shotsFired + 2 + random.nextInt(4);
        }
    }

    // =====================================================================
    //  AI Goal — Orbit target at the terminal orbit_distance and fire periodically
    // =====================================================================

    private static class OrbitAndShootGoal extends Goal {
        private final LowTierDestroyerEntity mob;

        OrbitAndShootGoal(LowTierDestroyerEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive()) return;

            mob.getLookControl().setLookAt(target, 30.0f, 30.0f);

            double dist = mob.distanceTo(target);
            Vec3 toTarget = target.position().subtract(mob.position());
            double hDist = toTarget.horizontalDistance();

            double orbitDistance = ModEquipmentConfig.LOW_TIER_DESTROYER_ORBIT_DISTANCE.get();
            double surfaceSpeed = ModEquipmentConfig.LOW_TIER_DESTROYER_SURFACE_SPEED.get();

            Vec3 movement;
            if (hDist > orbitDistance + 3) {
                // Too far — approach
                Vec3 dir = toTarget.normalize();
                movement = new Vec3(dir.x * surfaceSpeed, 0, dir.z * surfaceSpeed);
            } else if (hDist < orbitDistance - 3) {
                // Too close — retreat
                Vec3 dir = toTarget.normalize();
                movement = new Vec3(-dir.x * surfaceSpeed * 0.8, 0, -dir.z * surfaceSpeed * 0.8);
            } else {
                // Orbit
                mob.orbitAngle += ModEquipmentConfig.LOW_TIER_DESTROYER_ORBIT_ANGULAR_SPEED.get();
                double ox = target.getX() + Math.cos(mob.orbitAngle) * orbitDistance;
                double oz = target.getZ() + Math.sin(mob.orbitAngle) * orbitDistance;
                Vec3 toOrbit = new Vec3(ox - mob.getX(), 0, oz - mob.getZ());
                double oDist = toOrbit.horizontalDistance();
                if (oDist > 0.1) {
                    movement = new Vec3(toOrbit.x / oDist * surfaceSpeed, 0,
                            toOrbit.z / oDist * surfaceSpeed);
                } else {
                    movement = Vec3.ZERO;
                }
            }

            // Apply horizontal movement, keep vertical from physics
            Vec3 current = mob.getDeltaMovement();
            mob.setDeltaMovement(movement.x, current.y, movement.z);

            // Fire when in range and cooldown ready
            if (dist <= ModEquipmentConfig.LOW_TIER_DESTROYER_DETECTION_RANGE.get() && mob.fireCooldown <= 0) {
                mob.fireAtTarget(target);
                mob.fireCooldown = ModEquipmentConfig.LOW_TIER_DESTROYER_FIRE_INTERVAL_TICKS.get();
            }
        }
    }
}
