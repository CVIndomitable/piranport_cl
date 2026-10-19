package com.piranport.entity;

import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import com.piranport.aviation.combat.AircraftCombatService;
import com.piranport.aviation.combat.TargetResolution;
import com.piranport.combat.CombatTargeting;
import com.piranport.component.AircraftAttackMode;
import com.piranport.config.ModEquipmentConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

/**
 * 飞机服务端飞行主循环。
 *
 * <p>玩家飞机与自主飞机共用同一套燃料、留空、脱困、距离、状态机与移动约束；
 * 差异只体现在三处：
 * <ul>
 *   <li>目标来源：玩家走火控锁定，自主飞机走自主索敌；</li>
 *   <li>返航终点：玩家飞回所属者并返还物品，自主飞机直接销毁；</li>
 *   <li>侦察：只有玩家飞机有 RECON_ACTIVE 状态。</li>
 * </ul>
 */
final class AircraftFlight {
    private AircraftFlight() {}

    static void tickServer(AircraftEntity craft, @Nullable Player owner) {
        AircraftEntity.FlightState state = craft.getFlightState();
        if (state == AircraftEntity.FlightState.REMOVED) {
            craft.discard();
            return;
        }

        if (owner != null && state != craft.lastKnownState) {
            craft.handleStateTransition(craft.lastKnownState, state, owner);
            craft.lastKnownState = state;
        }

        craft.airtimeTicks++;
        craft.stateTicks++;

        // 燃料消耗：仅在主动飞行阶段
        if (state == AircraftEntity.FlightState.CRUISING
                || state == AircraftEntity.FlightState.ATTACKING
                || state == AircraftEntity.FlightState.RECON_ACTIVE) {
            if (craft.getFuelCapacity() > 0 && craft.hasFuel()
                    && craft.airtimeTicks % ModEquipmentConfig.AIRCRAFT_FUEL_BURN_INTERVAL.get() == 0) {
                craft.consumeFuel();
            }
            if (!craft.hasFuel()) {
                if (craft.isAutonomous()) {
                    craft.discard();
                    return;
                }
                craft.markForcedReturn();
                craft.startReturning("fuel_empty");
                if (owner instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "message.piranport.aircraft_no_fuel", craft.getDisplayName()), true);
                }
                return;
            }
        }

        // 防御：最大留空时间
        if (craft.airtimeTicks >= ModEquipmentConfig.AIRCRAFT_MAX_AIRTIME.get()) {
            if (craft.isAutonomous()) {
                craft.discard();
                return;
            }
            if (state == AircraftEntity.FlightState.RETURNING) {
                craft.recallAndRemove();
                return;
            }
            craft.markForcedReturn();
            craft.startReturning("max_airtime");
            return;
        }

        // 防御：脱困检测（侦察悬停时不判）
        if (state != AircraftEntity.FlightState.RECON_ACTIVE
                && craft.stateTicks % ModEquipmentConfig.AIRCRAFT_STUCK_CHECK_INTERVAL.get() == 0) {
            double moved = craft.position().distanceTo(craft.stuckCheckPos);
            if (moved < ModEquipmentConfig.AIRCRAFT_STUCK_THRESHOLD.get()) {
                craft.stuckTicks += ModEquipmentConfig.AIRCRAFT_STUCK_CHECK_INTERVAL.get();
                if (craft.stuckTicks >= ModEquipmentConfig.AIRCRAFT_STUCK_CHECK_INTERVAL.get() * 2) {
                    if (craft.isAutonomous()) {
                        craft.discard();
                        return;
                    }
                    craft.markForcedReturn();
                    craft.recallAndRemove();
                    return;
                }
            } else {
                craft.stuckTicks = 0;
            }
            craft.stuckCheckPos = craft.position();
        }

        if (!tickDistanceLimit(craft, owner, state)) {
            return;
        }

        if (craft.attackCooldown > 0) craft.attackCooldown--;

        switch (state) {
            case LAUNCHING -> tickLaunching(craft, owner);
            case CRUISING -> tickCruising(craft, owner);
            case ATTACKING -> tickAttacking(craft, owner);
            case RETURNING -> tickReturning(craft, owner);
            case RECON_ACTIVE -> {
                if (owner != null) AircraftAswRecon.tickReconActive(craft, owner);
            }
            case REMOVED -> {
                return;
            }
        }

        // 反潜声呐扫描：仅玩家所属的反潜机，在巡航/攻击时每 20 tick 一次
        if (owner != null && craft.hasSonar()
                && (state == AircraftEntity.FlightState.CRUISING
                || state == AircraftEntity.FlightState.ATTACKING)
                && craft.tickCount % 20 == 0) {
            AircraftAswRecon.tickAswSonar(craft, owner);
        }

        applyTurnRadiusConstraint(craft, state);

        Vec3 velocity = craft.getDeltaMovement();
        double horizontalDistance = velocity.horizontalDistance();
        if (horizontalDistance > 0.001) {
            craft.setYRot((float) (Math.atan2(-velocity.x, velocity.z) * (180.0 / Math.PI)));
            craft.setXRot((float) (Math.atan2(-velocity.y, horizontalDistance) * (180.0 / Math.PI)));
        }
        craft.setPos(craft.getX() + velocity.x, craft.getY() + velocity.y, craft.getZ() + velocity.z);
    }

    // ===== 状态实现 =====

    private static void tickLaunching(AircraftEntity craft, @Nullable Player owner) {
        double phase = craft.phaseSpeedCoefficient();
        Vec3 anchor = anchor(craft, owner);
        double targetY = anchor.y + ModEquipmentConfig.AIRCRAFT_CRUISE_ALTITUDE.get();
        double difference = targetY - craft.getY();
        double rise = Math.min(craft.panelSpeed() * phase, Math.abs(difference));
        double forwardSpeed = craft.panelSpeed() * phase;
        craft.setDeltaMovement(
                Math.cos(craft.orbitAngle) * forwardSpeed,
                difference > 0 ? rise : -rise,
                Math.sin(craft.orbitAngle) * forwardSpeed);
        if (craft.stateTicks >= ModEquipmentConfig.AIRCRAFT_LAUNCH_TICKS.get()
                || Math.abs(difference) < ModEquipmentConfig.AIRCRAFT_LAUNCH_COMPLETE_DY.get()) {
            craft.setState(owner != null && craft.isReconAircraft()
                    ? AircraftEntity.FlightState.RECON_ACTIVE
                    : AircraftEntity.FlightState.CRUISING);
        }
    }

    private static void tickCruising(AircraftEntity craft, @Nullable Player owner) {
        if (owner != null) {
            if (tryEngagePlayerTargets(craft, owner)) return;
        } else if (tryEngageAutonomousTargets(craft)) {
            return;
        }

        // FOLLOW：围绕玩家的侦察机盘旋；没有侦察机则退回玩家/母舰
        if (owner != null && craft.attackMode == AircraftAttackMode.FOLLOW) {
            if (craft.tickCount - craft.reconCacheTick > 20
                    || craft.cachedReconAircraft != null && craft.cachedReconAircraft.isRemoved()) {
                craft.cachedReconAircraft = findOwnerReconAircraft(craft, owner);
                craft.reconCacheTick = craft.tickCount;
            }
            AircraftEntity reconAircraft = craft.cachedReconAircraft;
            if (reconAircraft != null) {
                orbit(craft, reconAircraft.position(), reconAircraft.getY());
                return;
            }
        }

        Vec3 anchor = anchor(craft, owner);
        orbit(craft, anchor, anchor.y + ModEquipmentConfig.AIRCRAFT_CRUISE_ALTITUDE.get());
    }

    private static void orbit(AircraftEntity craft, Vec3 center, double targetY) {
        double phase = craft.phaseSpeedCoefficient();
        craft.orbitAngle += craft.panelSpeed()
                * ModEquipmentConfig.AIRCRAFT_ORBIT_ANGULAR_RATE.get() * phase;
        double radius = ModEquipmentConfig.AIRCRAFT_ORBIT_RADIUS.get();
        Vec3 target = new Vec3(
                center.x + Math.cos(craft.orbitAngle) * radius,
                targetY,
                center.z + Math.sin(craft.orbitAngle) * radius);
        Vec3 toTarget = target.subtract(craft.position());
        double distance = toTarget.length();
        craft.setDeltaMovement(distance > 0.1
                ? toTarget.normalize().scale(Math.min(craft.panelSpeed() * phase, distance))
                : Vec3.ZERO);
    }

    private static void tickAttacking(AircraftEntity craft, @Nullable Player owner) {
        if (owner != null) {
            AircraftCombatService.tickAttacking(craft, owner);
            return;
        }

        if (craft.remainingAmmo <= 0) {
            craft.discard();
            return;
        }
        if (!craft.getResolvedDefinition().isGunFighter()) {
            LivingEntity target = nearestPlayer(craft);
            if (target == null) {
                craft.setState(AircraftEntity.FlightState.CRUISING);
                return;
            }
            craft.autonomousTarget = target;
        }
        AircraftCombatService.tickAutonomousAttacking(craft);
    }

    private static void tickReturning(AircraftEntity craft, @Nullable Player owner) {
        if (owner == null) {
            craft.discard();
            return;
        }
        // 侦察机返航票据随飞机移动，避免区块卸载导致返航冻结
        AircraftAswRecon.tickReturnLoading(craft);
        // 返航超时兜底：直接回收并返还物品，不判定战损
        if (craft.stateTicks >= ModEquipmentConfig.AIRCRAFT_RETURN_TIMEOUT_TICKS.get()) {
            craft.recallAndRemove();
            return;
        }
        double phase = craft.phaseSpeedCoefficient();
        Vec3 toOwner = owner.getEyePosition().subtract(craft.position());
        double distance = toOwner.length();
        if (distance < ModEquipmentConfig.AIRCRAFT_RETURN_ARRIVAL_DISTANCE.get()) {
            craft.recallAndRemove();
            return;
        }
        craft.setDeltaMovement(toOwner.normalize().scale(Math.min(craft.panelSpeed() * phase, distance)));
    }

    // ===== 索敌 =====

    private static boolean tryEngagePlayerTargets(AircraftEntity craft, Player owner) {
        boolean canAttack = craft.usesBulletAttack() || craft.requiresPayload();
        if (craft.isReconAircraft() || !canAttack) return false;

        boolean hasLocks = !FireControlManager.getTargets(owner.getUUID()).isEmpty();
        if (hasLocks) {
            boolean canEngage;
            if (craft.hasSonar()) {
                canEngage = hasAliveAswLockedTarget(craft, owner);
            } else if (craft.usesBulletAttack()) {
                // 战斗机/火箭机：空中锁定优先；火箭机在没有空中锁定时可接地面锁定
                canEngage = hasAliveAirLockedTarget(craft, owner)
                        || (craft.getResolvedDefinition().attackProfile()
                                == com.piranport.aviation.AircraftDefinition.AttackProfile.ROCKET
                            && hasAliveGroundLockedTarget(craft, owner));
            } else if (craft.isPayloadAttacker()) {
                canEngage = hasAliveGroundLockedTarget(craft, owner);
            } else {
                canEngage = false;
            }
            if (canEngage) {
                craft.hasEverHadFireControl = true;
                craft.setState(AircraftEntity.FlightState.ATTACKING);
                return true;
            }
            // 锁定目标全部死亡：清理后如果列表变空，允许回到一次性自动索敌。
            if (craft.level() instanceof ServerLevel serverLevel) {
                FireControlManager.removeDeadTargets(owner.getUUID(), uuid -> {
                    var entity = serverLevel.getEntities().get(uuid);
                    return entity == null || !entity.isAlive();
                });
            }
            hasLocks = !FireControlManager.getTargets(owner.getUUID()).isEmpty();
        }
        if (hasLocks) return false;

        // 无火控锁定：起飞后只自动搜索一次
        if (craft.hasEverHadFireControl || craft.autoSeekDone) return false;
        if (craft.autoSeekCooldown > 0) {
            craft.autoSeekCooldown--;
            return false;
        }
        craft.autoSeekDone = true;
        ServerLevel serverLevel = craft.level() instanceof ServerLevel level ? level : null;
        if (serverLevel == null) return false;
        AABB box = craft.getBoundingBox().inflate(ModEquipmentConfig.AIRCRAFT_AUTO_SEEK_RANGE.get());
        boolean hasNearbyTarget;
        if (craft.hasSonar()) {
            hasNearbyTarget = !serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                    entity -> entity.isAlive() && entity != owner
                            && AircraftAswRecon.isHostileAswTarget(owner, entity)).isEmpty();
        } else if (craft.usesBulletAttack()) {
            boolean hasHostile = !serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                    entity -> entity instanceof Monster
                            && CombatTargeting.isHostileTarget(owner, entity)).isEmpty();
            boolean hasEnemyAircraft = !serverLevel.getEntitiesOfClass(AircraftEntity.class, box,
                    entity -> CombatTargeting.isHostileTarget(owner, entity)).isEmpty();
            hasNearbyTarget = hasHostile || hasEnemyAircraft;
        } else {
            hasNearbyTarget = !serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                    entity -> entity.isAlive() && entity != owner
                            && entity instanceof Monster
                            && !TargetResolution.isAirborneTarget(entity)).isEmpty();
        }
        if (hasNearbyTarget) {
            craft.autoSeekCooldown = 0;
            craft.setState(AircraftEntity.FlightState.ATTACKING);
            return true;
        }
        return false;
    }

    private static boolean tryEngageAutonomousTargets(AircraftEntity craft) {
        if (!craft.usesBulletAttack() && !craft.requiresPayload()) return false;
        ServerLevel serverLevel = craft.level() instanceof ServerLevel level ? level : null;
        if (serverLevel == null) return false;
        boolean hasTarget;
        if (craft.getResolvedDefinition().isGunFighter()) {
            hasTarget = TargetResolution.resolveAutonomousFighterTarget(craft) != null;
        } else {
            hasTarget = nearestPlayer(craft) != null;
        }
        if (hasTarget) {
            craft.setState(AircraftEntity.FlightState.ATTACKING);
            return true;
        }
        return false;
    }

    @Nullable
    private static LivingEntity nearestPlayer(AircraftEntity craft) {
        ServerLevel serverLevel = craft.level() instanceof ServerLevel level ? level : null;
        if (serverLevel == null) return null;
        AABB box = craft.getBoundingBox().inflate(ModEquipmentConfig.AIRCRAFT_AUTONOMOUS_SEEK_RANGE.get());
        return serverLevel.getEntitiesOfClass(Player.class, box,
                        player -> player.isAlive() && !player.isSpectator())
                .stream()
                .min(Comparator.comparingDouble(craft::distanceTo))
                .orElse(null);
    }

    @Nullable
    private static AircraftEntity findOwnerReconAircraft(AircraftEntity craft, Player owner) {
        if (!(craft.level() instanceof ServerLevel serverLevel)) return null;
        UUID reconEntityId = ReconManager.getReconEntity(owner.getUUID());
        if (reconEntityId == null) return null;
        return serverLevel.getEntity(reconEntityId) instanceof AircraftEntity aircraft
                && aircraft.getFlightState() == AircraftEntity.FlightState.RECON_ACTIVE
                ? aircraft : null;
    }

    /** 敌方飞机是 AircraftEntity（不继承 LivingEntity），必须单独判定。 */
    private static boolean hasAliveAirLockedTarget(AircraftEntity craft, Player owner) {
        if (!(craft.level() instanceof ServerLevel serverLevel)) return false;
        for (UUID uuid : FireControlManager.getTargets(owner.getUUID())) {
            var entity = serverLevel.getEntity(uuid);
            if (entity instanceof AircraftEntity aircraft && aircraft.isAlive()) return true;
        }
        return false;
    }

    private static boolean hasAliveGroundLockedTarget(AircraftEntity craft, Player owner) {
        if (!(craft.level() instanceof ServerLevel serverLevel)) return false;
        for (UUID uuid : FireControlManager.getTargets(owner.getUUID())) {
            var entity = serverLevel.getEntity(uuid);
            if (entity instanceof LivingEntity living && living.isAlive()
                    && !TargetResolution.isAirborneTarget(living) && !living.isUnderWater()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAliveAswLockedTarget(AircraftEntity craft, Player owner) {
        if (!(craft.level() instanceof ServerLevel serverLevel)) return false;
        for (UUID uuid : FireControlManager.getTargets(owner.getUUID())) {
            var entity = serverLevel.getEntity(uuid);
            if (entity instanceof LivingEntity living && living.isAlive()
                    && AircraftAswRecon.isAswTarget(living)) {
                return true;
            }
        }
        return false;
    }

    // ===== 距离 / 移动约束 =====

    private static boolean tickDistanceLimit(AircraftEntity craft, @Nullable Player owner,
                                             AircraftEntity.FlightState state) {
        if (owner == null) {
            double limit = getDistanceLimit(craft, ModEquipmentConfig.AIRCRAFT_MIN_DISTANCE_FROM_OWNER.get()) * 2;
            if (craft.position().distanceTo(craft.homePosition) > limit) {
                craft.discard();
                return false;
            }
            return true;
        }

        double maxDistance = (state == AircraftEntity.FlightState.RECON_ACTIVE
                || craft.attackMode == AircraftAttackMode.FOLLOW)
                ? getDistanceLimit(craft, ModEquipmentConfig.AIRCRAFT_MIN_RECON_DISTANCE.get())
                : getDistanceLimit(craft, ModEquipmentConfig.AIRCRAFT_MIN_DISTANCE_FROM_OWNER.get());
        if (craft.distanceTo(owner) <= maxDistance) return true;

        if (state == AircraftEntity.FlightState.RECON_ACTIVE) {
            craft.markForcedReturn();
            craft.startReturning("recon_max_distance");
            return false;
        }
        Vec3 near = owner.position().add(
                (craft.level().random.nextDouble() - 0.5) * 4, 3,
                (craft.level().random.nextDouble() - 0.5) * 4);
        craft.setPos(near.x, near.y, near.z);
        craft.stuckTicks = 0;
        craft.stuckCheckPos = craft.position();
        craft.markForcedReturn();
        craft.startReturning("distance_limit");
        return false;
    }

    private static double getDistanceLimit(AircraftEntity craft, double minimum) {
        int simulationDistance = 10;
        if (craft.level() instanceof ServerLevel serverLevel && serverLevel.getServer() != null) {
            simulationDistance = serverLevel.getServer().getPlayerList().getSimulationDistance();
        }
        return Math.max(minimum, (simulationDistance - 1) * 16.0);
    }

    private static void applyTurnRadiusConstraint(AircraftEntity craft,
                                                  AircraftEntity.FlightState state) {
        Vec3 rawVelocity = craft.getDeltaMovement();

        // 禁止悬停：除起飞阶段外保持最低水平速度
        if (state != AircraftEntity.FlightState.LAUNCHING && craft.lastHorizontalDir != null) {
            double rawHorizontalSpeed = rawVelocity.horizontalDistance();
            double minSpeed = craft.panelSpeed()
                    * ModEquipmentConfig.AIRCRAFT_MIN_SPEED_MULTIPLIER.get()
                    * craft.phaseSpeedCoefficient();
            if (rawHorizontalSpeed < minSpeed) {
                rawVelocity = new Vec3(craft.lastHorizontalDir.x * minSpeed, rawVelocity.y,
                        craft.lastHorizontalDir.z * minSpeed);
                craft.setDeltaMovement(rawVelocity);
            }
        }

        Vec3 constrained = constrainTurnRadius(craft, craft.getDeltaMovement());
        craft.setDeltaMovement(constrained);

        double horizontalDistance = constrained.horizontalDistance();
        if (horizontalDistance > 0.001) {
            craft.lastHorizontalDir = new Vec3(
                    constrained.x / horizontalDistance, 0,
                    constrained.z / horizontalDistance);
        }
    }

    private static Vec3 constrainTurnRadius(AircraftEntity craft, Vec3 desiredVelocity) {
        double desiredHorizontalSpeed = desiredVelocity.horizontalDistance();
        if (desiredHorizontalSpeed < 0.001 || craft.lastHorizontalDir == null) {
            return desiredVelocity;
        }

        double maxAngle = desiredHorizontalSpeed / ModEquipmentConfig.AIRCRAFT_TURN_RADIUS.get();
        double inverseSpeed = 1.0 / desiredHorizontalSpeed;
        double desiredX = desiredVelocity.x * inverseSpeed;
        double desiredZ = desiredVelocity.z * inverseSpeed;

        double dot = craft.lastHorizontalDir.x * desiredX + craft.lastHorizontalDir.z * desiredZ;
        dot = Math.max(-1.0, Math.min(1.0, dot));
        double angleDifference = Math.acos(dot);
        if (angleDifference <= maxAngle) return desiredVelocity;

        double cross = craft.lastHorizontalDir.x * desiredZ - craft.lastHorizontalDir.z * desiredX;
        double currentYaw = Math.atan2(-craft.lastHorizontalDir.x, craft.lastHorizontalDir.z);
        currentYaw += cross >= 0 ? maxAngle : -maxAngle;
        return new Vec3(-Math.sin(currentYaw) * desiredHorizontalSpeed,
                desiredVelocity.y,
                Math.cos(currentYaw) * desiredHorizontalSpeed);
    }

    private static Vec3 anchor(AircraftEntity craft, @Nullable Player owner) {
        return owner != null ? owner.position() : craft.homePosition;
    }
}
