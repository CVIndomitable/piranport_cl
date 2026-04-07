package com.piranport.npc.ai.goal;

import com.piranport.entity.AircraftEntity;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Carrier-specific goal: launches aircraft to attack the target.
 * Respects deck capacity and readying interval.
 */
public class AircraftLaunchGoal extends Goal {

    private final AbstractDeepOceanEntity mob;
    private int readyCooldown = 0;
    private int activeAircraft = 0;

    /** Interval between launches (ticks). */
    private static final int READY_INTERVAL = 300; // 15 seconds
    /** Initial delay before first launch. */
    private static final int INITIAL_DELAY = 100;

    public AircraftLaunchGoal(AbstractDeepOceanEntity mob) {
        this.mob = mob;
        this.readyCooldown = INITIAL_DELAY;
        setFlags(EnumSet.noneOf(Flag.class)); // Does not block movement
    }

    @Override
    public boolean canUse() {
        if (!mob.canLaunchAircraft()) return false;
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (readyCooldown > 0) {
            readyCooldown--;
            return;
        }

        if (activeAircraft >= mob.getMaxAircraft()) return;

        LivingEntity target = mob.getTarget();
        if (target == null) return;

        launchAircraft(target);
        readyCooldown = READY_INTERVAL;
    }

    private void launchAircraft(LivingEntity target) {
        if (mob.level().isClientSide()) return;

        // AircraftEntity is designed for player use with a factory method.
        // For deep ocean carriers, we spawn the aircraft as a hostile entity
        // and let it fly toward the target. Full integration will come in a later phase.
        AircraftEntity aircraft = new AircraftEntity(ModEntityTypes.AIRCRAFT_ENTITY.get(), mob.level());
        aircraft.setPos(mob.getX(), mob.getY() + 1.5, mob.getZ());
        // Give it initial velocity toward target
        net.minecraft.world.phys.Vec3 dir = target.position().subtract(mob.position()).normalize();
        aircraft.setDeltaMovement(dir.x * 0.5, 0.3, dir.z * 0.5);

        mob.level().addFreshEntity(aircraft);
        activeAircraft++;
    }

    /**
     * Called when an aircraft launched by this carrier is destroyed.
     */
    public void onAircraftLost() {
        if (activeAircraft > 0) activeAircraft--;
    }
}
