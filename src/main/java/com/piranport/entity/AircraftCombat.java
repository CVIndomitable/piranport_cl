package com.piranport.entity;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.AircraftDefinitionService;
import com.piranport.combat.LevelBombLead;
import com.piranport.component.AircraftInfo;
import com.piranport.component.AircraftAttackMode;
import com.piranport.config.ModCommonConfig;
import com.piranport.config.ModEquipmentConfig;
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

        boolean canFallback = craft.stateTicks >= 20;
        switch (attackProfile(craft)) {
            case RECON -> craft.startReturning("recon_no_attack");
            case ROCKET -> {
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
            }
            case GUN -> {
                Entity target = resolveFighterTarget(craft, owner);
                if (target == null) {
                    if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
                    return;
                }
                tickFighterAttack(craft, owner, target);
            }
            case TORPEDO -> {
                LivingEntity target = resolveTarget(craft, owner);
                if (target == null) {
                    if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
                    return;
                }
                tickTorpedoBomberAttack(craft, owner, target);
            }
            case DIVE_BOMB -> {
                LivingEntity target = resolveTarget(craft, owner);
                if (target == null) {
                    if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
                    return;
                }
                tickDiveBomberAttack(craft, owner, target);
            }
            case LEVEL_BOMB -> {
                LivingEntity target = resolveTarget(craft, owner);
                if (target == null) {
                    if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
                    return;
                }
                tickLevelBomberAttack(craft, owner, target);
            }
            case DEPTH_CHARGE -> {
                LivingEntity target = AircraftAswRecon.resolveASWTarget(craft, owner);
                if (target == null) {
                    if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
                    return;
                }
                AircraftAswRecon.tickASWAttack(craft, owner, target);
            }
            case NONE -> craft.startReturning("no_payload");
        }
    }

    /** @see AircraftEntity#tickAutonomousAttacking() */
    public static void tickAutonomousAttacking(AircraftEntity craft) {
        if (craft.autonomousTarget == null || !craft.autonomousTarget.isAlive() || craft.autonomousTarget.isRemoved()) {
            craft.startReturning("target_lost");
            return;
        }

        switch (attackProfile(craft)) {
            case ROCKET -> {
                if (!craft.hasFired && craft.remainingAmmo > 0) {
                    tickRocketFighterMissileRun(craft, null, craft.autonomousTarget);
                } else {
                    tickFighterAttack(craft, null, craft.autonomousTarget);
                }
            }
            case GUN -> tickFighterAttack(craft, null, craft.autonomousTarget);
            case TORPEDO -> tickTorpedoBomberAttack(craft, null, craft.autonomousTarget);
            case DIVE_BOMB -> tickDiveBomberAttack(craft, null, craft.autonomousTarget);
            case LEVEL_BOMB -> tickLevelBomberAttack(craft, null, craft.autonomousTarget);
            case DEPTH_CHARGE -> AircraftAswRecon.tickASWAttack(craft, null, craft.autonomousTarget);
            case NONE, RECON -> craft.startReturning("no_payload");
        }
    }

    /** Resolve the immutable definition profile; legacy entities fall back to their old enum type. */
    private static AircraftDefinition.AttackProfile attackProfile(AircraftEntity craft) {
        AircraftDefinition definition = AircraftDefinitionService.find(craft.getAircraftDefinitionId());
        if (definition != null) return definition.attackProfile();
        return switch (craft.aircraftType) {
            case FIGHTER -> AircraftDefinition.AttackProfile.GUN;
            case ROCKET_FIGHTER -> AircraftDefinition.AttackProfile.ROCKET;
            case DIVE_BOMBER -> AircraftDefinition.AttackProfile.DIVE_BOMB;
            case LEVEL_BOMBER -> AircraftDefinition.AttackProfile.LEVEL_BOMB;
            case TORPEDO_BOMBER -> AircraftDefinition.AttackProfile.TORPEDO;
            case ASW -> AircraftDefinition.AttackProfile.DEPTH_CHARGE;
            case RECON -> AircraftDefinition.AttackProfile.RECON;
        };
    }

    // ====================================================================
    // 各类型攻击
    // ====================================================================

    /** @see AircraftEntity#tickFighterAttack(Player, Entity) */
    public static void tickFighterAttack(AircraftEntity craft, @Nullable Player owner, Entity target) {
        // 战斗机机枪弹药固定无限，不再由配置改变行为。
        boolean ammoEnabled = false;
        if (ammoEnabled && craft.remainingAmmo <= 0) { craft.startReturning("fighter_ammo_depleted"); return; }

        Vec3 toTarget = target.getEyePosition().subtract(craft.position());
        double dist = toTarget.length();
        if (dist < 0.01) return; // 零距离保护：防止 normalize 产生 NaN
        double preferredDist = 11.0;

        if (dist > preferredDist + 3) {
            craft.setDeltaMovement(toTarget.normalize().scale(Math.min(craft.getPanelSpeed() * 0.5
                    * craft.phaseSpeedCoefficient(), dist)));
        } else if (dist < preferredDist - 3) {
            craft.setDeltaMovement(toTarget.normalize().scale(-craft.getPanelSpeed() * 0.2
                    * craft.phaseSpeedCoefficient()));
        } else {
            craft.setDeltaMovement(craft.getDeltaMovement().scale(0.8));
        }

        if (craft.attackCooldown <= 0 && dist < 24.0) {
            BulletEntity bullet = new BulletEntity(craft.level(), craft.getPanelDamage() / 8f);
            Vec3 dir = toTarget.normalize();
            bullet.moveTo(craft.getX(), craft.getY() + 0.3, craft.getZ(), bullet.getYRot(), bullet.getXRot());
            bullet.setDeltaMovement(dir.scale(2.5));
            bullet.setOwner(owner);
            bullet.setSourceAircraftName(craft.getDisplayName());
            bullet.setSourceAircraft(craft);
            craft.level().addFreshEntity(bullet);
            craft.attackCooldown = craft.attackCooldownDuration();

            if (ammoEnabled) {
                craft.remainingAmmo--;
                if (craft.remainingAmmo <= 0) {
                    if (owner != null && craft.tryAutoResupplyAmmo(owner)) {
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
            float rocketDamage = craft.getPanelDamage() * 1.2f;
            int toFire = craft.computeSalvoSize(target, rocketDamage);
            com.piranport.debug.PiranPortDebug.event(
                    "Aircraft ROCKET_SALVO | entityId={} capacity={} remaining={} firing={} targetHP={}",
                    craft.getId(), craft.ammoCapacity, craft.remainingAmmo, toFire, target.getHealth());
            float initSpeed = MissileEntity.MissileType.ROCKET.initialSpeed();
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
                craft.attackCooldown = craft.attackCooldownDuration();
            }
            return;
        }

        Vec3 toTarget = new Vec3(dx, desiredY - craft.getY(), dz);
        double dist = toTarget.length();
        if (dist > 0.1) {
            craft.setDeltaMovement(toTarget.normalize().scale(Math.min(craft.getPanelSpeed() * 0.5
                    * craft.phaseSpeedCoefficient(), dist)));
        }
    }

    /** @see AircraftEntity#tickDiveBomberAttack(Player, LivingEntity) */
    public static void tickDiveBomberAttack(AircraftEntity craft, @Nullable Player owner, LivingEntity target) {
        if (craft.hasFired) {
            if (owner != null && craft.tryAutoResupplyAmmo(owner)) {
                craft.setState(AircraftEntity.FlightState.CRUISING);
            } else {
                craft.startReturning("dive_bomber_done");
            }
            return;
        }

        double climbY = target.getY() + 18.0;
        double heightDiff = Math.max(0, climbY - craft.getY());
        double phaseSpeed = craft.phaseSpeedCoefficient();
        int climbTimeout = Math.max(80, (int)Math.ceil(heightDiff
                / Math.max(craft.getPanelSpeed() * 0.4 * phaseSpeed, 0.1)));
        if (!craft.diveCommitted && craft.getY() < climbY - 1.0 && craft.stateTicks < climbTimeout) {
            Vec3 toClimb = new Vec3(target.getX() - craft.getX(), climbY - craft.getY(), target.getZ() - craft.getZ());
            double dist = toClimb.length();
            craft.setDeltaMovement(toClimb.normalize().scale(Math.min(craft.getPanelSpeed() * 0.4
                    * phaseSpeed, dist)));
            return;
        }

        if (!craft.diveCommitted) {
            craft.diveCommitted = true;
            Vec3 targetPos = target.getEyePosition();
            double estimatedDist = craft.position().distanceTo(targetPos);
            double diveSpeed = Math.max(craft.getPanelSpeed() * 0.6 * phaseSpeed, 0.1);
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
                float bombPower = craft.getPanelDamage();
                int bombCount = Math.max(1, craft.remainingAmmo);
                for (int i = 0; i < bombCount; i++) {
                    AerialBombEntity bomb = new AerialBombEntity(level, bombPower / bombCount, 4.0f);
                    double spreadX = (level.random.nextDouble() - 0.5) * 0.5;
                    double spreadZ = (level.random.nextDouble() - 0.5) * 0.5;
                    bomb.moveTo(craft.getX() + spreadX, craft.getY(), craft.getZ() + spreadZ, 0, 0);
                    // Preserve the aircraft's horizontal momentum.  Dropping with
                    // a zero horizontal velocity made every dive bomb fall behind
                    // the moving attack run and systematically miss ships at sea.
                    Vec3 aircraftVelocity = craft.getDeltaMovement();
                    bomb.setDeltaMovement(aircraftVelocity.x * 0.85,
                            Math.min(-0.20, aircraftVelocity.y),
                            aircraftVelocity.z * 0.85);
                    bomb.setOwner(owner);
                    level.addFreshEntity(bomb);
                }
                craft.remainingAmmo = 0;
                craft.hasFired = true;
            } else {
                double speed = Math.min(craft.getPanelSpeed() * 0.7 * phaseSpeed, diveDist);
                craft.setDeltaMovement(toDive.normalize().scale(speed));
            }
        }
    }

    /** @see AircraftEntity#tickTorpedoBomberAttack(Player, LivingEntity) */
    public static void tickTorpedoBomberAttack(AircraftEntity craft, @Nullable Player owner, LivingEntity target) {
        if (craft.remainingAmmo <= 0) {
            if (owner != null && craft.tryAutoResupplyAmmo(owner)) {
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
            craft.setDeltaMovement(descend.scale(Math.min(craft.getPanelSpeed() * 0.4
                    * craft.phaseSpeedCoefficient(), 1.0)));
            return;
        }

        if (horizDist < 6.0 && craft.attackCooldown <= 0) {
            Vec3 dir = new Vec3(dx, 0, dz).normalize();
            int toFire = craft.computeSalvoSize(target, craft.getPanelDamage());
            com.piranport.debug.PiranPortDebug.event(
                    "Aircraft TORPEDO_SALVO | entityId={} capacity={} remaining={} firing={} targetHP={}",
                    craft.getId(), craft.ammoCapacity, craft.remainingAmmo, toFire, target.getHealth());
            for (int i = 0; i < toFire; i++) {
                TorpedoEntity torpedo = new TorpedoEntity(com.piranport.registry.ModEntityTypes.TORPEDO_ENTITY.get(), craft.level());
                double offsetX = -dir.z * (i - (toFire - 1) / 2.0) * 1.2;
                double offsetZ = dir.x * (i - (toFire - 1) / 2.0) * 1.2;
                torpedo.moveTo(craft.getX() + offsetX, craft.getY() - 1.0, craft.getZ() + offsetZ, 0, 0);
                // 注意：空投鱼雷不绑定具体 TorpedoItem 型号，初速固定 0.8，
                // 因此不受调试终端的型号航速覆盖影响（玩家发射路径读 TorpedoItem.getSpeed()，
                // 这里没有型号可查）。这是设计如此，不是漏改 —— 若要让它也受覆盖，
                // 需要先给飞机挂弹定义「所投型号」再按型号取 getSpeed()。
                Vec3 vel = dir.scale(0.8).add(0, -0.1, 0);
                torpedo.setDeltaMovement(vel);
                torpedo.setOwner(owner);
                craft.level().addFreshEntity(torpedo);
            }
            craft.remainingAmmo -= toFire;
            craft.attackCooldown = craft.attackCooldownDuration();
            if (craft.remainingAmmo <= 0) {
                craft.startReturning("torpedo_launched");
            }
            return;
        }

        Vec3 toTarget = new Vec3(dx, 0, dz).normalize().scale(Math.min(craft.getPanelSpeed() * 0.5
                * craft.phaseSpeedCoefficient(), horizDist));
        double yAdjust = (target.getY() + approachAlt - craft.getY()) * 0.1;
        craft.setDeltaMovement(toTarget.x, yAdjust, toTarget.z);
    }

    // ==== 水平轰炸机投弹参数 ====
    // 投弹高度相对目标的高度差（+32，飞行手感维持不变，勿改）
    private static final double LEVEL_BOMB_ALTITUDE_OFFSET = 32.0;
    // 投弹初速的垂直分量（恢复改动前的 -0.3）：航弹保留近垂直下坠的手感。
    // 水平分量不在此写死——它 = 载机当前水平速度 × 终端参数倍数，见 LEVEL_BOMBER_HORIZONTAL_VELOCITY_MULTIPLIER。
    private static final double LEVEL_BOMB_VERTICAL_VELOCITY = -0.3;
    // 投弹提前判据的最小窗口（半格）：倍数为 0（无前抛）时提前距离也算得 0，
    // 判据会退化成「必须恰好 0 格」而永远不成立，只能等 200 tick 超时把弹丢到目标身后。
    // 半格窗口保证这种极端设置下也能在目标正上方附近投弹；正常倍数下提前距离远大于此，不影响。
    private static final double LEVEL_BOMB_MIN_RELEASE_DISTANCE = 0.5;

    /** @see AircraftEntity#tickLevelBomberAttack(Player, LivingEntity) */
    public static void tickLevelBomberAttack(AircraftEntity craft, @Nullable Player owner, LivingEntity target) {
        if (craft.remainingAmmo <= 0) {
            if (owner != null && craft.tryAutoResupplyAmmo(owner)) {
                craft.setState(AircraftEntity.FlightState.CRUISING);
            } else {
                craft.startReturning("level_bomber_ammo_depleted");
            }
            return;
        }

        double dx = target.getX() - craft.getX();
        double dz = target.getZ() - craft.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        double bombAlt = target.getY() + LEVEL_BOMB_ALTITUDE_OFFSET;

        // Phase 1: Approach bombing altitude
        if (craft.getY() < bombAlt - 2.0) {
            Vec3 toAlt = new Vec3(dx, bombAlt - craft.getY(), dz);
            craft.setDeltaMovement(toAlt.normalize().scale(Math.min(craft.getPanelSpeed() * 0.4
                    * craft.phaseSpeedCoefficient(), toAlt.length())));
            return;
        }

        // Phase 2: Lock run direction and execute bomb run
        if (craft.levelRunDirection == null) {
            craft.levelRunDirection = new Vec3(dx, 0, dz).normalize();
            craft.levelRunTicks = 0;
        }

        double runSpeed = craft.getPanelSpeed() * 0.6 * craft.phaseSpeedCoefficient();
        Vec3 runDir = craft.levelRunDirection;
        Vec3 runVel = new Vec3(runDir.x * runSpeed, 0, runDir.z * runSpeed);
        craft.setDeltaMovement(runVel);

        craft.levelRunTicks++;

        // 投弹判据：载机到目标的水平距离 ≤ 提前距离时投弹。
        // 提前距离 = 航弹从投弹高度落到目标高度期间自身的水平前进量（保留前抛的直接后果），
        // 由 LevelBombLead 按 ThrowableProjectile#tick 的逐行物理积分算出。删掉了旧的固定 <3 格判据。
        if (!craft.levelBombDropped) {
            double craftHorizontalSpeed = Math.sqrt(craft.getDeltaMovement().x * craft.getDeltaMovement().x
                    + craft.getDeltaMovement().z * craft.getDeltaMovement().z);
            double horizontalMultiplier = ModEquipmentConfig.LEVEL_BOMBER_HORIZONTAL_VELOCITY_MULTIPLIER.get();
            // 起始高度与 bomb.moveTo 的 y 一致（craft.getY() - 0.5），目标高度取其脚底。
            double dropHeight = (craft.getY() - 0.5) - target.getY();
            // 重力读实体自身（AerialBombEntity.GRAVITY），不写死数字，避免与实体/落点标记漂移。
            double leadDistance = LevelBombLead.releaseDistance(dropHeight, AerialBombEntity.GRAVITY,
                    craftHorizontalSpeed * horizontalMultiplier, LEVEL_BOMB_VERTICAL_VELOCITY);

            if (horizDist <= Math.max(leadDistance, LEVEL_BOMB_MIN_RELEASE_DISTANCE) || craft.levelRunTicks > 200) {
                float bombPower = craft.getPanelDamage();
                int bombCount = Math.max(1, craft.remainingAmmo);
                for (int i = 0; i < bombCount; i++) {
                    AerialBombEntity bomb = new AerialBombEntity(craft.level(), bombPower / bombCount, 4.0f);
                    // 保留 ±0.5 格随机散布：提前量把弹着点拉回目标，但不必弹弹正中。
                    double spreadX = (craft.level().random.nextDouble() - 0.5) * 1.0;
                    double spreadZ = (craft.level().random.nextDouble() - 0.5) * 1.0;
                    bomb.moveTo(craft.getX() + spreadX, craft.getY() - 0.5, craft.getZ() + spreadZ, 0, 0);
                    // 保留前抛：航弹继承载机水平速度 × 倍数（终端可调，默认 0.5）。
                    bomb.setDeltaMovement(craft.getDeltaMovement().x * horizontalMultiplier,
                            LEVEL_BOMB_VERTICAL_VELOCITY,
                            craft.getDeltaMovement().z * horizontalMultiplier);
                    bomb.setOwner(owner);
                    craft.level().addFreshEntity(bomb);
                }
                craft.remainingAmmo = 0;
                craft.levelBombDropped = true;
                craft.levelDropPoint = craft.position();
            }
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
        // 自动索敌必须加敌对过滤：否则鱼、友军、玩家都会被无差别自动开打。
        // 手动火控锁定分支在上方，保持原样不限制（尊重玩家意图）。
        net.minecraft.world.phys.AABB box = craft.getBoundingBox().inflate(48.0);
        return sl.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e.isAlive() && e != owner
                                && com.piranport.combat.CombatTargeting.isHostileTarget(owner, e)
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

        // Auto-seek: only enemy aircraft (机枪只能对空)
        net.minecraft.world.phys.AABB box = craft.getBoundingBox().inflate(48.0);
        // 搜索敌方飞机
        AircraftEntity airTarget = sl.getEntitiesOfClass(AircraftEntity.class, box,
                        e -> e.isAlive() && e != craft
                                && e.getOwnerUUID() != null
                                && !e.getOwnerUUID().equals(owner.getUUID()))
                .stream()
                .min(java.util.Comparator.comparingDouble(craft::distanceToSqr))
                .orElse(null);
        return airTarget;
    }
}
