package com.piranport.npc.ai.goal;

import com.piranport.entity.DeepOceanProjectileEntity;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Carrier-specific goal: simulates air strikes against the target using parabolic projectiles.
 * Respects deck capacity and readying interval.
 */
public class AircraftLaunchGoal extends Goal {

    private final AbstractDeepOceanEntity mob;
    private int readyCooldown = 0;

    private static final int READY_INTERVAL = 300; // 15 seconds
    private static final int INITIAL_DELAY = 100;
    private static final float STRIKE_DAMAGE = 12.0f;
    private static final float STRIKE_EXPLOSION = 2.0f;
    private static final int SALVO_SIZE = 3;

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
        if (target == null) return;

        launchAirStrike(target);
        readyCooldown = READY_INTERVAL;
    }

    private void launchAirStrike(LivingEntity target) {
        if (mob.level().isClientSide()) return;

        Vec3 launchPos = mob.position().add(0, 2.0, 0);
        Vec3 toTarget = target.position().subtract(launchPos);
        double dist = toTarget.horizontalDistance();

        for (int i = 0; i < SALVO_SIZE; i++) {
            DeepOceanProjectileEntity proj = new DeepOceanProjectileEntity(
                    mob.level(), mob, STRIKE_DAMAGE, STRIKE_EXPLOSION,
                    DeepOceanProjectileEntity.BallisticType.PARABOLIC);
            proj.setPos(launchPos.x, launchPos.y, launchPos.z);

            double speed = 1.2;
            double arcHeight = Math.min(dist * 0.15, 8.0);
            Vec3 dir = toTarget.normalize();
            double spread = (i - 1) * 0.1;
            proj.setDeltaMovement(
                    dir.x * speed + spread,
                    arcHeight * 0.3 + 0.5,
                    dir.z * speed + spread);

            mob.level().addFreshEntity(proj);
        }
    }
}
