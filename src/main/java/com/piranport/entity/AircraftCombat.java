package com.piranport.entity;

import com.piranport.component.AircraftInfo;
import com.piranport.component.AircraftAttackMode;
import com.piranport.config.ModCommonConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * 飞机战斗系统 — 从 AircraftEntity 提取的静态辅助方法。
 *
 * <p>包括攻击分发、各类型攻击战术（战斗机/火箭机/俯冲轰炸/鱼雷/水平轰炸）、
 * 目标解析和自主攻击。
 *
 * <p><b>线程模型</b>: 服务端主线程。
 */
public class AircraftCombat {

    private AircraftCombat() {}

    // ====================================================================
    // 攻击主分发
    // ====================================================================

    /** @see AircraftEntity#tickAttacking(Player) */
    public static void tickAttacking(AircraftEntity craft, Player owner) {
        if (craft.autonomous && craft.autonomousTarget != null) {
            tickAutonomousAttacking(craft);
            return;
        }

        if (craft.aircraftType == AircraftInfo.AircraftType.RECON) {
            craft.startReturning("recon_no_attack");
            return;
        }

        boolean canFallback = craft.stateTicks >= 20;

        if (craft.aircraftType == AircraftInfo.AircraftType.ROCKET_FIGHTER) {
            if (!craft.hasFired && craft.remainingAmmo > 0) {
                LivingEntity groundTarget = resolveTarget(craft, owner);
                if (groundTarget != null) {
                    tickRocketFighterMissileRun(craft, owner, groundTarget);
                    return;
                }
            }
            Entity airTarget = resolveFighterTarget(craft, owner);
            if (airTarget == null) {
                if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
                return;
            }
            tickFighterAttack(craft, owner, airTarget);
            return;
        }

        if (craft.hasBullets) {
            Entity target = resolveFighterTarget(craft, owner);
            if (target == null) {
                if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
                return;
            }
            tickFighterAttack(craft, owner, target);
            return;
        }

        switch (craft.payloadType) {
            case "piranport:aerial_torpedo" -> {
                LivingEntity target = resolveTarget(craft, owner);
                if (target == null) {
                    if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
                    return;
                }
                tickTorpedoBomberAttack(craft, owner, target);
            }
            case "piranport:aerial_bomb" -> {
                LivingEntity target = resolveTarget(craft, owner);
                if (target == null) {
                    if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
                    return;
                }
                if (craft.bombingMode == AircraftInfo.BombingMode.LEVEL) {
                    tickLevelBomberAttack(craft, owner, target);
                } else {
                    tickDiveBomberAttack(craft, owner, target);
                }
            }
            case "piranport:depth_charge" -> {
                LivingEntity target = AircraftAswRecon.resolveASWTarget(craft, owner);
                if (target == null) {
                    if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
                    return;
                }
                AircraftAswRecon.tickASWAttack(craft, owner, target);
            }
            default -> craft.startReturning("no_payload");
        }
    }

    /** @see AircraftEntity#tickAutonomousAttacking() */
    public static void tickAutonomousAttacking(AircraftEntity craft) {
        if (craft.autonomousTarget == null || !craft.autonomousTarget.isAlive() || craft.autonomousTarget.isRemoved()) {
            craft.startReturning("target_lost");
            return;
        }

        if (craft.aircraftType == AircraftInfo.AircraftType.ROCKET_FIGHTER) {
            if (!craft.hasFired && craft.remainingAmmo > 0) {
                tickRocketFighterMissileRun(craft, null, craft.autonomousTarget);
                return;
            }
            tickFighterAttack(craft, null, craft.autonomousTarget);
            return;
        }

        if (craft.hasBullets) {
            tickFighterAttack(craft, null, craft.autonomousTarget);
            return;
        }

        switch (craft.payloadType) {
            case "piranport:aerial_torpedo" -> tickTorpedoBomberAttack(craft, null, craft.autonomousTarget);
            case "piranport:aerial_bomb" -> {
                if (craft.bombingMode == AircraftInfo.BombingMode.LEVEL) {
                    tickLevelBomberAttack(craft, null, craft.autonomousTarget);
                } else {
                    tickDiveBomberAttack(craft, null, craft.autonomousTarget);
                }
            }
            case "piranport:depth_charge" -> AircraftAswRecon.tickASWAttack(craft, null, craft.autonomousTarget);
            default -> craft.startReturning("no_payload");
        }
    }

    // ====================================================================
    // 各类型攻击
    // ====================================================================

    /** @see AircraftEntity#tickFighterAttack(Player, Entity) */
    public static void tickFighterAttack(AircraftEntity craft, @Nullable Player owner, Entity target) {
        boolean ammoEnabled = ModCommonConfig.FIGHTER_AMMO_ENABLED.get()
                && craft.aircraftType != AircraftInfo.AircraftType.ROCKET_FIGHTER;
        if (ammoEnabled && craft.remainingAmmo <= 0) { craft.startReturning("fighter_ammo_depleted"); return; }

        Vec3 toTarget = target.getEyePosition().subtract(craft.position());
        double dist = toTarget.length();
        if (dist < 0.01) return; // 零距离保护：防止 normalize 产生 NaN
        double preferredDist = 11.0;

        if (dist > preferredDist + 3) {
            craft.setDeltaMovement(toTarget.normalize().scale(Math.min(craft.panelSpeed * 0.5, dist)));
        } else if (dist < preferredDist - 3) {
            craft.setDeltaMovement(toTarget.normalize().scale(-craft.panelSpeed * 0.2));
        } else {
            craft.setDeltaMovement(craft.getDeltaMovement().scale(0.8));
        }

        if (craft.attackCooldown <= 0 && dist < 24.0) {
            BulletEntity bullet = new BulletEntity(craft.level(), craft.panelDamage / 8f);
            Vec3 dir = toTarget.normalize();
            bullet.moveTo(craft.getX(), craft.getY() + 0.3, craft.getZ(), bullet.getYRot(), bullet.getXRot());
            bullet.setDeltaMovement(dir.scale(2.5));
            bullet.setOwner(owner);
            bullet.setSourceAircraftName(craft.getDisplayName());
            bullet.setSourceAircraft(craft);
            craft.level().addFreshEntity(bullet);
            craft.attackCooldown = 5;

            if (ammoEnabled) {
                craft.remainingAmmo--;
                if (craft.remainingAmmo <= 0) {
                    if (owner != null && ModCommonConfig.AUTO_RESUPPLY_ENABLED.get() && craft.tryAutoResupplyAmmo(owner)) {
                    } else {
                        craft.startReturning("fighter_ammo_depleted");
                    }
                }
            }
        }
    }

    /** @see AircraftEntity#tickRocketFighterMissileRun(Player, LivingEntity) */
    public static void tickRocketFighterMissileRun(AircraftEntity craft, @Nullable Player owner, LivingEntity target) {
        if (craft.stateTicks > 200) {
            craft.hasFired = true;
            craft.remainingAmmo = 0;
            return;
        }

        double dx = target.getX() - craft.getX();
        double dz = target.getZ() - craft.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        double desiredY = target.getY() + 8.0;
        double altDelta = craft.getY() - desiredY;

        if (horizDist >= 12 && horizDist <= 22 && altDelta >= -2.0 && altDelta <= 8.0 && craft.attackCooldown <= 0) {
            Vec3 dir = new Vec3(dx, target.getEyeY() + 0.5 - craft.getY(), dz).normalize();
            float rocketDamage = craft.panelDamage * 1.2f;
            int toFire = craft.computeSalvoSize(target, rocketDamage);
            com.piranport.debug.PiranPortDebug.event(
                    "Aircraft ROCKET_SALVO | entityId={} capacity={} remaining={} firing={} targetHP={}",
                    craft.getId(), craft.ammoCapacity, craft.remainingAmmo, toFire, target.getHealth());
            float initSpeed = MissileEntity.MissileType.ROCKET.initialSpeed;
            for (int i = 0; i < toFire; i++) {
                double spread = (i - (toFire - 1) / 2.0) * 0.07;
                double cos = Math.cos(spread);
                double sin = Math.sin(spread);
                Vec3 fanDir = new Vec3(
                        dir.x * cos - dir.z * sin,
                        dir.y,
                        dir.x * sin + dir.z * cos
                ).normalize();
                MissileEntity rocket = new MissileEntity(craft.level(),
                        MissileEntity.MissileType.ROCKET,
                        rocketDamage, 0f, 2.0f,
                        "piranport:rocket_ammo");
                rocket.moveTo(craft.getX(), craft.getY() + 0.2, craft.getZ(), rocket.getYRot(), rocket.getXRot());
                rocket.setDeltaMovement(fanDir.x * initSpeed, fanDir.y * initSpeed, fanDir.z * initSpeed);
                rocket.setOwner(owner);
                craft.level().addFreshEntity(rocket);
            }
            craft.remainingAmmo -= toFire;
            if (craft.remainingAmmo <= 0) {
                craft.hasFired = true;
            } else {
                craft.attackCooldown = 40;
            }
            return;
        }

        Vec3 toTarget = new Vec3(dx, desiredY - craft.getY(), dz);
        double dist = toTarget.length();
        if (dist > 0.1) {
            craft.setDeltaMovement(toTarget.normalize().scale(Math.min(craft.panelSpeed * 0.5, dist)));
        }
    }

    /** @see AircraftEntity#tickDiveBomberAttack(Player, LivingEntity) */
    public static void tickDiveBomberAttack(AircraftEntity craft, @Nullable Player owner, LivingEntity target) {
        if (craft.hasFired) {
            if (owner != null && ModCommonConfig.AUTO_RESUPPLY_ENABLED.get() && craft.tryAutoResupplyAmmo(owner)) {
                craft.setState(AircraftEntity.FlightState.CRUISING);
            } else {
                craft.startReturning("dive_bomber_done");
            }
            return;
        }

        double climbY = target.getY() + 18.0;
        double heightDiff = Math.max(0, climbY - craft.getY());
        int climbTimeout = Math.max(80, (int)Math.ceil(heightDiff / Math.max(craft.panelSpeed * 0.4, 0.1)));
        if (!craft.diveCommitted && craft.getY() < climbY - 1.0 && craft.stateTicks < climbTimeout) {
            Vec3 toClimb = new Vec3(target.getX() - craft.getX(), climbY - craft.getY(), target.getZ() - craft.getZ());
            double dist = toClimb.length();
            craft.setDeltaMovement(toClimb.normalize().scale(Math.min(craft.panelSpeed * 0.4, dist)));
            return;
        }

        if (!craft.diveCommitted) {
            craft.diveCommitted = true;
            Vec3 targetPos = target.getEyePosition();
            double estimatedDist = craft.position().distanceTo(targetPos);
            double diveSpeed = Math.max(craft.panelSpeed * 0.6, 0.1);
            double estimatedTicks = estimatedDist / diveSpeed;
            Vec3 targetVel = target.getDeltaMovement();
            craft.diveTarget = targetPos.add(targetVel.scale(estimatedTicks));
        }

        if (craft.diveTarget != null) {
            Vec3 toDive = craft.diveTarget.subtract(craft.position());
            double diveDist = toDive.length();
            if (diveDist < 2.0) {
                // Drop the bomb
                net.minecraft.world.level.Level level = craft.level();
                float bombPower = craft.panelDamage;
                int bombCount = Math.max(1, craft.remainingAmmo);
                for (int i = 0; i < bombCount; i++) {
                    AerialBombEntity bomb = new AerialBombEntity(level, bombPower / bombCount, 4.0f);
                    double spreadX = (level.random.nextDouble() - 0.5) * 0.5;
                    double spreadZ = (level.random.nextDouble() - 0.5) * 0.5;
                    bomb.moveTo(craft.getX() + spreadX, craft.getY(), craft.getZ() + spreadZ, 0, 0);
                    bomb.setDeltaMovement(0, -0.5, 0);
                    bomb.setOwner(owner);
                    level.addFreshEntity(bomb);
                }
                craft.remainingAmmo = 0;
                craft.hasFired = true;
            } else {
                double speed = Math.min(craft.panelSpeed * 0.7, diveDist);
                craft.setDeltaMovement(toDive.normalize().scale(speed));
            }
        }
    }

    /** @see AircraftEntity#tickTorpedoBomberAttack(Player, LivingEntity) */
    public static void tickTorpedoBomberAttack(AircraftEntity craft, @Nullable Player owner, LivingEntity target) {
        if (craft.remainingAmmo <= 0) {
            if (owner != null && ModCommonConfig.AUTO_RESUPPLY_ENABLED.get() && craft.tryAutoResupplyAmmo(owner)) {
            } else {
                craft.startReturning("torpedo_bomber_done");
                return;
            }
        }

        double dx = target.getX() - craft.getX();
        double dz = target.getZ() - craft.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        double approachAlt = 12.0;
        double altitudeDiff = craft.getY() - (target.getY() + approachAlt);

        // Descend to attack altitude
        if (altitudeDiff > 2.0) {
            Vec3 descend = new Vec3(dx, target.getY() + approachAlt - craft.getY(), dz).normalize();
            craft.setDeltaMovement(descend.scale(Math.min(craft.panelSpeed * 0.4, 1.0)));
            return;
        }

        if (horizDist < 6.0 && craft.attackCooldown <= 0) {
            Vec3 dir = new Vec3(dx, 0, dz).normalize();
            int toFire = craft.computeSalvoSize(target, craft.panelDamage);
            com.piranport.debug.PiranPortDebug.event(
                    "Aircraft TORPEDO_SALVO | entityId={} capacity={} remaining={} firing={} targetHP={}",
                    craft.getId(), craft.ammoCapacity, craft.remainingAmmo, toFire, target.getHealth());
            for (int i = 0; i < toFire; i++) {
                TorpedoEntity torpedo = new TorpedoEntity(com.piranport.registry.ModEntityTypes.TORPEDO_ENTITY.get(), craft.level());
                double offsetX = -dir.z * (i - (toFire - 1) / 2.0) * 1.2;
                double offsetZ = dir.x * (i - (toFire - 1) / 2.0) * 1.2;
                torpedo.moveTo(craft.getX() + offsetX, craft.getY() - 1.0, craft.getZ() + offsetZ, 0, 0);
                Vec3 vel = dir.scale(0.8).add(0, -0.1, 0);
                torpedo.setDeltaMovement(vel);
                torpedo.setOwner(owner);
                craft.level().addFreshEntity(torpedo);
            }
            craft.remainingAmmo -= toFire;
            craft.attackCooldown = 40;
            if (craft.remainingAmmo <= 0) {
                craft.startReturning("torpedo_launched");
            }
            return;
        }

        Vec3 toTarget = new Vec3(dx, 0, dz).normalize().scale(Math.min(craft.panelSpeed * 0.5, horizDist));
        double yAdjust = (target.getY() + approachAlt - craft.getY()) * 0.1;
        craft.setDeltaMovement(toTarget.x, yAdjust, toTarget.z);
    }

    /** @see AircraftEntity#tickLevelBomberAttack(Player, LivingEntity) */
    public static void tickLevelBomberAttack(AircraftEntity craft, @Nullable Player owner, LivingEntity target) {
        if (craft.remainingAmmo <= 0) {
            if (owner != null && ModCommonConfig.AUTO_RESUPPLY_ENABLED.get() && craft.tryAutoResupplyAmmo(owner)) {
                craft.setState(AircraftEntity.FlightState.CRUISING);
            } else {
                craft.startReturning("level_bomber_ammo_depleted");
            }
            return;
        }

        double dx = target.getX() - craft.getX();
        double dz = target.getZ() - craft.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        double bombAlt = target.getY() + 30.0;

        // Phase 1: Approach bombing altitude
        if (craft.getY() < bombAlt - 2.0) {
            Vec3 toAlt = new Vec3(dx, bombAlt - craft.getY(), dz);
            craft.setDeltaMovement(toAlt.normalize().scale(Math.min(craft.panelSpeed * 0.4, toAlt.length())));
            return;
        }

        // Phase 2: Lock run direction and execute bomb run
        if (craft.levelRunDirection == null) {
            craft.levelRunDirection = new Vec3(dx, 0, dz).normalize();
            craft.levelRunTicks = 0;
        }

        double runSpeed = craft.panelSpeed * 0.6;
        Vec3 runDir = craft.levelRunDirection;
        Vec3 runVel = new Vec3(runDir.x * runSpeed, 0, runDir.z * runSpeed);
        craft.setDeltaMovement(runVel);

        craft.levelRunTicks++;

        // Drop bomb when within range, or timeout after 200 ticks
        if ((horizDist < 4.0 || craft.levelRunTicks > 200) && !craft.levelBombDropped) {
            float bombPower = craft.panelDamage;
            int bombCount = Math.max(1, craft.remainingAmmo);
            for (int i = 0; i < bombCount; i++) {
                AerialBombEntity bomb = new AerialBombEntity(craft.level(), bombPower / bombCount, 4.0f);
                double spreadX = (craft.level().random.nextDouble() - 0.5) * 1.0;
                double spreadZ = (craft.level().random.nextDouble() - 0.5) * 1.0;
                bomb.moveTo(craft.getX() + spreadX, craft.getY() - 0.5, craft.getZ() + spreadZ, 0, 0);
                bomb.setDeltaMovement(craft.getDeltaMovement().x * 0.5, -0.3, craft.getDeltaMovement().z * 0.5);
                bomb.setOwner(owner);
                craft.level().addFreshEntity(bomb);
            }
            craft.remainingAmmo = 0;
            craft.levelBombDropped = true;
            craft.levelDropPoint = craft.position();
        }

        // After dropping, fly for a few more ticks then return
        if (craft.levelBombDropped && craft.levelRunTicks > 220) {
            craft.startReturning("level_bomber_run_complete");
        }
    }

    // ====================================================================
    // 目标解析
    // ====================================================================

    /** @see AircraftEntity#resolveTarget(Player) */
    @Nullable
    public static LivingEntity resolveTarget(AircraftEntity craft, Player owner) {
        if (!(craft.level() instanceof net.minecraft.server.level.ServerLevel sl)) return null;

        java.util.List<java.util.UUID> locks = com.piranport.aviation.FireControlManager.getTargets(owner.getUUID());
        if (!locks.isEmpty()) {
            if (craft.attackMode == AircraftAttackMode.SPREAD) {
                return locks.stream()
                        .map(sl::getEntity)
                        .filter(e -> e instanceof LivingEntity le && le.isAlive() && !isAirborneTarget(e))
                        .map(e -> (LivingEntity) e)
                        .min(java.util.Comparator.comparingDouble(craft::distanceTo))
                        .orElse(null);
            } else {
                for (java.util.UUID uuid : locks) {
                    Entity e = sl.getEntity(uuid);
                    if (e instanceof LivingEntity le && le.isAlive() && !isAirborneTarget(e)) return le;
                }
                return null;
            }
        }

        // Auto-seek: scan for ground targets within 48 blocks
        if (craft.hasEverHadFireControl) return null;
        net.minecraft.world.phys.AABB box = craft.getBoundingBox().inflate(48.0);
        return sl.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e.isAlive() && e != owner
                                && !(e instanceof net.minecraft.world.entity.Mob mob && mob.isNoAi())
                                && !isAirborneTarget(e))
                .stream()
                .min(java.util.Comparator.comparingDouble(craft::distanceTo))
                .orElse(null);
    }

    /** @see AircraftEntity#isAirborneTarget(Entity) */
    public static boolean isAirborneTarget(Entity e) {
        return e instanceof AircraftEntity;
    }

    /** @see AircraftEntity#resolveFighterTarget(Player) */
    @Nullable
    public static Entity resolveFighterTarget(AircraftEntity craft, Player owner) {
        if (!(craft.level() instanceof net.minecraft.server.level.ServerLevel sl)) return null;

        java.util.List<java.util.UUID> locks = com.piranport.aviation.FireControlManager.getTargets(owner.getUUID());
        if (!locks.isEmpty()) {
            double closestDist = Double.MAX_VALUE;
            Entity closest = null;
            for (java.util.UUID uuid : locks) {
                Entity e = sl.getEntity(uuid);
                if (e != null && e.isAlive()) {
                    double d = craft.distanceToSqr(e);
                    if (d < closestDist) { closestDist = d; closest = e; }
                }
            }
            if (closest != null) return closest;
        }

        // Auto-seek: hostile mobs and enemy aircraft only (not all living entities)
        net.minecraft.world.phys.AABB box = craft.getBoundingBox().inflate(48.0);
        // Search for hostile mobs
        LivingEntity hostileTarget = sl.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e.isAlive() && e != owner
                                && e instanceof net.minecraft.world.entity.monster.Monster)
                .stream()
                .min(java.util.Comparator.comparingDouble(craft::distanceToSqr))
                .orElse(null);
        // Search for enemy aircraft
        AircraftEntity airTarget = sl.getEntitiesOfClass(AircraftEntity.class, box,
                        e -> e.isAlive() && e != craft
                                && e.getOwnerUUID() != null
                                && !e.getOwnerUUID().equals(owner.getUUID()))
                .stream()
                .min(java.util.Comparator.comparingDouble(craft::distanceToSqr))
                .orElse(null);
        // Return whichever is closer
        if (hostileTarget != null && airTarget != null) {
            return craft.distanceToSqr(hostileTarget) <= craft.distanceToSqr(airTarget)
                    ? hostileTarget : airTarget;
        }
        return hostileTarget != null ? hostileTarget : airTarget;
    }
}
