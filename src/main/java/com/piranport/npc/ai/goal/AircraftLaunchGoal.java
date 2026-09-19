package com.piranport.npc.ai.goal;

import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.network.AircraftLaunchPosePayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumSet;

/**
 * Carrier-specific goal: launches real {@link com.piranport.entity.AircraftEntity} instances
 * that autonomously seek and attack targets, then return to the carrier when fuel runs out.
 *
 * <p>Deck capacity is enforced via {@link AbstractDeepOceanEntity#spawnAircraft()}.</p>
 */
public class AircraftLaunchGoal extends Goal {

    private final AbstractDeepOceanEntity mob;
    private int readyCooldown = 0;

    private static final int READY_INTERVAL = 300; // 15 seconds between launches
    private static final int INITIAL_DELAY = 100;  // 5 seconds before first launch

    public AircraftLaunchGoal(AbstractDeepOceanEntity mob) {
        this.mob = mob;
        this.readyCooldown = INITIAL_DELAY;
        setFlags(EnumSet.noneOf(Flag.class));
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

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        launchAircraft(target);
        readyCooldown = READY_INTERVAL;
    }

    private void launchAircraft(LivingEntity target) {
        if (mob.level().isClientSide()) return;
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        // Send launch pose animation to nearby clients
        double radius = serverLevel.getServer().getPlayerList().getSimulationDistance() * 16.0;
        PacketDistributor.sendToPlayersNear(
                serverLevel,
                null,
                mob.getX(), mob.getY(), mob.getZ(),
                Math.max(48.0, radius),
                new AircraftLaunchPosePayload(mob.getId(), 0, 22));

        // Spawn the actual aircraft entity (deck capacity is enforced inside spawnAircraft)
        mob.spawnAircraft();
    }
}
