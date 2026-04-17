package com.piranport.npc.ai.goal;

import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Idle wandering on water surface within a configurable radius of spawn point.
 * Walk-stop-walk natural rhythm.
 */
public class IdleWanderGoal extends RandomStrollGoal {

    private final AbstractDeepOceanEntity deepOcean;
    private final double maxWanderRadius;

    public IdleWanderGoal(AbstractDeepOceanEntity mob, double speedModifier, double maxWanderRadius) {
        super(mob, speedModifier, 80); // interval=80 ticks for natural walk-stop rhythm
        this.deepOcean = mob;
        this.maxWanderRadius = maxWanderRadius;
    }

    @Override
    public boolean canUse() {
        // Only wander when no target (IDLE state)
        if (deepOcean.getTarget() != null) return false;
        return super.canUse();
    }

    @Override
    @Nullable
    protected Vec3 getPosition() {
        Vec3 pos = DefaultRandomPos.getPos(this.mob, 10, 7);
        if (pos == null) return null;

        if (maxWanderRadius > 0) {
            Vec3 home = deepOcean.hasRestriction()
                    ? Vec3.atCenterOf(deepOcean.getRestrictCenter())
                    : deepOcean.position();
            if (pos.distanceTo(home) > maxWanderRadius) {
                return null;
            }
        }
        return pos;
    }
}
