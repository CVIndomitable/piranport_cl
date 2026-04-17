package com.piranport.npc.ai.goal;

import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Submarine-specific goal: submerge when in combat, gain invisibility.
 * Surface when ammo depleted or after max submerge time.
 */
public class SubmergeGoal extends Goal {

    private final AbstractDeepOceanEntity mob;
    private boolean submerged = false;
    private int submergeTicks = 0;

    /** Max time underwater before forced surface (ticks). */
    private static final int MAX_SUBMERGE_TIME = 600; // 30 seconds

    public SubmergeGoal(AbstractDeepOceanEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.isInWater();
    }

    @Override
    public boolean canContinueToUse() {
        if (!canUse()) return false;
        return submergeTicks < MAX_SUBMERGE_TIME;
    }

    @Override
    public void start() {
        submerged = true;
        submergeTicks = 0;
    }

    @Override
    public void stop() {
        submerged = false;
        submergeTicks = 0;
        // Remove invisibility when surfacing
        mob.removeEffect(MobEffects.INVISIBILITY);
    }

    @Override
    public void tick() {
        submergeTicks++;

        // Apply invisibility while submerged
        if (submergeTicks % 40 == 0) {
            mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
        }

        // Maintain depth
        Vec3 vel = mob.getDeltaMovement();
        if (mob.isInWater()) {
            if (!mob.isUnderWater()) {
                // Still at surface — push down
                mob.setDeltaMovement(vel.x, -0.1, vel.z);
            } else {
                // Maintain depth — gentle correction
                mob.setDeltaMovement(vel.x, vel.y * 0.5, vel.z);
            }
        }
    }

    public boolean isSubmerged() {
        return submerged;
    }
}
