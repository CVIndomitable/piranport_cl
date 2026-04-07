package com.piranport.npc.ai.goal;

import com.piranport.npc.ai.FleetGroup;
import com.piranport.npc.ai.FleetGroupManager;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Monitors fleet group state and applies shared target to this entity.
 * Priority should be LOWER than direct hurt-by-target (so personal attacker takes priority).
 */
public class FleetAlertGoal extends Goal {

    private final AbstractDeepOceanEntity mob;

    public FleetAlertGoal(AbstractDeepOceanEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.noneOf(Flag.class)); // Does not block movement or look
    }

    @Override
    public boolean canUse() {
        // Only check when we have no target and belong to a fleet
        return mob.getTarget() == null && mob.getFleetGroupId() != null && !mob.level().isClientSide();
    }

    @Override
    public boolean canContinueToUse() {
        return false; // One-shot check per activation
    }

    @Override
    public void start() {
        FleetGroup group = mob.getFleetGroup();
        if (group == null) return;

        UUID targetUuid = group.getSharedTargetUuid();
        if (targetUuid == null) return;

        ServerLevel level = (ServerLevel) mob.level();
        Entity targetEntity = level.getEntity(targetUuid);
        if (targetEntity instanceof LivingEntity living && living.isAlive()) {
            mob.setTarget(living);
        }
    }
}
