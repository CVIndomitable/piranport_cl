package com.piranport.compat.maid.brain;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import com.piranport.compat.maid.combat.MaidWeaponFirer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Distance-aware movement for maid rigging combat.
 * Chase when far, hold position at optimal range, retreat when enemy closes in.
 */
public class RiggingMovementTask extends Behavior<EntityMaid> {
    private static final float CHASE_DISTANCE = 20f;
    private static final float RETREAT_DISTANCE = 10f;
    private static final double MOVE_SPEED = 0.6;
    private static final double RETREAT_AMOUNT = 5.0;

    public RiggingMovementTask() {
        super(ImmutableMap.of(
                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT
        ), 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        return MaidWeaponFirer.isOffensiveWeapon(maid.getMainHandItem());
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        if (!MaidWeaponFirer.isOffensiveWeapon(maid.getMainHandItem())) return false;
        Optional<LivingEntity> target = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        return target.isPresent() && target.get().isAlive() && !target.get().isRemoved();
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        Optional<LivingEntity> target = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (target.isEmpty()) return;
        LivingEntity t = target.get();
        double dist = maid.distanceTo(t);

        if (dist > CHASE_DISTANCE) {
            maid.getNavigation().moveTo(t, MOVE_SPEED);
        } else if (dist < RETREAT_DISTANCE) {
            Vec3 away = maid.position().subtract(t.position()).normalize();
            Vec3 retreatPos = maid.position().add(away.x * RETREAT_AMOUNT, 0, away.z * RETREAT_AMOUNT);
            maid.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, MOVE_SPEED);
        } else {
            maid.getNavigation().stop();
        }
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
        maid.getNavigation().stop();
    }
}
