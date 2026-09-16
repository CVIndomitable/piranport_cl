package com.piranport.npc.ai.goal;

import com.piranport.npc.ai.FleetGroup;
import com.piranport.npc.ai.FleetGroupManager;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.npc.deepocean.DeepOceanSubmarineEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * 编队走位 Goal。
 * <p>
 * 领舰（队列索引 0）：沿用环绕目标的 chase/orbit/retreat 三段式移动（复用 OrbitTargetGoal 相位逻辑）。
 * 后续舰船：按队形偏移跟随领舰；到达队形位置后保持，不额外环绕目标。
 * 潜艇：不参与队形排列，此 Goal 跳过（潜艇保留独立 AI）。
 * <p>
 * 策划依据：深海/01 §3（编队队形四式、领舰递补、后续舰船跟随前船航线）。
 */
public class FollowLeaderGoal extends Goal {

    private final AbstractDeepOceanEntity mob;
    private final double surfaceSpeed;

    private double orbitAngle;
    private int transitionTicks = 0;
    private static final int TRANSITION_DURATION = 20;
    private Vec3 prevMovement = Vec3.ZERO;

    private enum Phase { CHASE, ORBIT, RETREAT }
    private Phase currentPhase = Phase.CHASE;

    public FollowLeaderGoal(AbstractDeepOceanEntity mob, double surfaceSpeed) {
        this.mob = mob;
        this.surfaceSpeed = surfaceSpeed;
        this.orbitAngle = mob.getRandom().nextDouble() * Math.PI * 2;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (!mob.isAlive()) return false;
        if (mob.level().isClientSide()) return false;

        FleetGroup group = mob.getFleetGroup();
        if (group == null) return false;

        // 潜艇保持独立行动，不参与队形排列
        if (mob instanceof DeepOceanSubmarineEntity) return false;

        UUID leaderUuid = group.getLeaderUuid();
        if (leaderUuid == null) return false;

        if (leaderUuid.equals(mob.getUUID())) {
            // 领舰：环绕目标
            return true;
        } else {
            // 后续舰船：跟随领舰（领舰必须存活）
            Entity leader = ((ServerLevel) mob.level()).getEntity(leaderUuid);
            return leader != null && leader.isAlive();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        currentPhase = Phase.CHASE;
        transitionTicks = 0;
    }

    @Override
    public void tick() {
        FleetGroup group = mob.getFleetGroup();
        if (group == null) return;

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        UUID leaderUuid = group.getLeaderUuid();
        if (leaderUuid == null) return;

        if (leaderUuid.equals(mob.getUUID())) {
            tickAsLeader(target);
        } else {
            ServerLevel sl = (ServerLevel) mob.level();
            Entity leader = sl.getEntity(leaderUuid);
            if (leader == null || !leader.isAlive()) return;
            tickAsFollower(leader, target, group);
        }
    }

    /** 领舰：环绕目标移动（复用 OrbitTargetGoal 三段式相位逻辑） */
    private void tickAsLeader(LivingEntity target) {
        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);

        Vec3 toTarget = target.position().subtract(mob.position());
        double hDist = toTarget.horizontalDistance();
        double orbitDist = mob.getOrbitDistance();

        Phase newPhase;
        if (hDist > orbitDist * 1.2) {
            newPhase = Phase.CHASE;
        } else if (hDist < orbitDist * 0.7) {
            newPhase = Phase.RETREAT;
        } else {
            newPhase = Phase.ORBIT;
        }

        if (newPhase != currentPhase) {
            prevMovement = mob.getDeltaMovement();
            transitionTicks = TRANSITION_DURATION;
            currentPhase = newPhase;
        }

        Vec3 rawMovement;
        switch (currentPhase) {
            case CHASE -> {
                Vec3 dir = toTarget.normalize();
                rawMovement = new Vec3(dir.x * surfaceSpeed, 0, dir.z * surfaceSpeed);
            }
            case RETREAT -> {
                Vec3 dir = toTarget.normalize();
                rawMovement = new Vec3(-dir.x * surfaceSpeed * 0.8, 0, -dir.z * surfaceSpeed * 0.8);
            }
            case ORBIT -> {
                orbitAngle += 0.02;
                double ox = target.getX() + Math.cos(orbitAngle) * orbitDist;
                double oz = target.getZ() + Math.sin(orbitAngle) * orbitDist;
                Vec3 toOrbit = new Vec3(ox - mob.getX(), 0, oz - mob.getZ());
                double oDist = toOrbit.horizontalDistance();
                if (oDist > 0.1) {
                    rawMovement = new Vec3(toOrbit.x / oDist * surfaceSpeed, 0,
                            toOrbit.z / oDist * surfaceSpeed);
                } else {
                    rawMovement = Vec3.ZERO;
                }
            }
            default -> rawMovement = Vec3.ZERO;
        }

        Vec3 finalMovement;
        if (transitionTicks > 0) {
            double t = 1.0 - (double) transitionTicks / TRANSITION_DURATION;
            finalMovement = prevMovement.lerp(rawMovement, t);
            transitionTicks--;
        } else {
            finalMovement = rawMovement;
        }

        Vec3 current = mob.getDeltaMovement();
        mob.setDeltaMovement(finalMovement.x, current.y, finalMovement.z);
    }

    /** 后续舰船：按队形偏移跟随领舰 */
    private void tickAsFollower(Entity leader, LivingEntity target, FleetGroup group) {
        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);

        // 获取自己在队列中的索引
        int memberIndex = -1;
        List<UUID> members = group.getMembers();
        UUID myUuid = mob.getUUID();
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).equals(myUuid)) {
                memberIndex = i;
                break;
            }
        }
        if (memberIndex < 0) return;

        Vec3 leaderPos = leader.position();
        Vec3 targetPos = target.position();
        Vec3 offset = group.getFormationOffset(memberIndex, members.size(), leaderPos, targetPos);
        Vec3 dest = leaderPos.add(offset);

        // 平滑移向队形位置
        Vec3 toDest = dest.subtract(mob.position());
        toDest = new Vec3(toDest.x, 0, toDest.z);
        double dist = toDest.horizontalDistance();

        Vec3 movement;
        if (dist < 0.5) {
            // 已到达队形位置，保持静止（水平方向）
            movement = new Vec3(0, mob.getDeltaMovement().y, 0);
        } else {
            Vec3 dir = toDest.normalize();
            movement = new Vec3(dir.x * surfaceSpeed, 0, dir.z * surfaceSpeed);
        }

        Vec3 current = mob.getDeltaMovement();
        mob.setDeltaMovement(movement.x, current.y, movement.z);
    }
}
