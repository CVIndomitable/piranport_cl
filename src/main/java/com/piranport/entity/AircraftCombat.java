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
    // 各类型攻击算法（由 aviation.combat 注册表里的策略类逐个调用；
    // 主分发 switch 已下沉为 AttackStrategy 子类，本类不再做 profile 分发）
    // ====================================================================

    /** @see AircraftEntity#tickFighterAttack(Player, Entity) */
    public static void tickFighterAttack(AircraftEntity craft, @Nullable Player owner, Entity target) {
        // 战斗机机枪弹药固定无限，不再由配置改变行为。
        boolean ammoEnabled = false;
        if (ammoEnabled && craft.remainingAmmo <= 0) { craft.startReturning("fighter_ammo_depleted"); return; }

        Vec3 toTarget = target.getEyePosition().subtract(craft.position());
        double dist = toTarget.length();
        if (dist < 0.01) return; // 零距离保护：防止 normalize 产生 NaN
        double preferredDist = ModEquipmentConfig.FIGHTER_HOVER_DISTANCE.get();
        double hoverBand = ModEquipmentConfig.FIGHTER_HOVER_BAND.get();

        if (dist > preferredDist + hoverBand) {
            craft.setDeltaMovement(toTarget.normalize().scale(Math.min(craft.getPanelSpeed()
                    * ModEquipmentConfig.FIGHTER_PURSUIT_MULTIPLIER.get()
                    * craft.phaseSpeedCoefficient(), dist)));
        } else if (dist < preferredDist - hoverBand) {
            craft.setDeltaMovement(toTarget.normalize().scale(-craft.getPanelSpeed()
                    * ModEquipmentConfig.FIGHTER_RETREAT_MULTIPLIER.get()
                    * craft.phaseSpeedCoefficient()));
        } else {
            craft.setDeltaMovement(craft.getDeltaMovement().scale(ModEquipmentConfig.FIGHTER_HOVER_DAMPING.get()));
        }

        if (craft.attackCooldown <= 0 && dist < ModEquipmentConfig.FIGHTER_BULLET_RANGE.get()) {
            // 保留 float 除法（.floatValue()），保证默认 8.0 时与下沉前 8f 逐字节一致。
            BulletEntity bullet = new BulletEntity(craft.level(),
                    craft.getPanelDamage() / ModEquipmentConfig.FIGHTER_BULLET_DAMAGE_DIVISOR.get().floatValue());
            Vec3 dir = toTarget.normalize();
            bullet.moveTo(craft.getX(), craft.getY() + ModEquipmentConfig.FIGHTER_BULLET_SPAWN_Y_OFFSET.get(),
                    craft.getZ(), bullet.getYRot(), bullet.getXRot());
            bullet.setDeltaMovement(dir.scale(ModEquipmentConfig.FIGHTER_BULLET_SPEED.get()));
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
        if (craft.stateTicks > ModEquipmentConfig.ROCKET_FIGHTER_RUN_TICKS.get()) {
            craft.hasFired = true;
            craft.remainingAmmo = 0;
            return;
        }

        double dx = target.getX() - craft.getX();
        double dz = target.getZ() - craft.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        double desiredY = target.getY() + ModEquipmentConfig.ROCKET_FIGHTER_ATTACK_ALTITUDE_OFFSET.get();
        double altDelta = craft.getY() - desiredY;

        if (horizDist >= ModEquipmentConfig.ROCKET_FIGHTER_WINDOW_HORIZONTAL_MIN.get()
                && horizDist <= ModEquipmentConfig.ROCKET_FIGHTER_WINDOW_HORIZONTAL_MAX.get()
                && altDelta >= ModEquipmentConfig.ROCKET_FIGHTER_WINDOW_ALTITUDE_MIN.get()
                && altDelta <= ModEquipmentConfig.ROCKET_FIGHTER_WINDOW_ALTITUDE_MAX.get()
                && craft.attackCooldown <= 0) {
            Vec3 dir = new Vec3(dx, target.getEyeY() + ModEquipmentConfig.ROCKET_FIGHTER_AIM_Y_OFFSET.get()
                    - craft.getY(), dz).normalize();
            // 保留 float 乘法，保证默认 1.2 时与下沉前 1.2f 逐字节一致。
            float rocketDamage = craft.getPanelDamage()
                    * ModEquipmentConfig.ROCKET_FIGHTER_DAMAGE_MULTIPLIER.get().floatValue();
            int toFire = craft.computeSalvoSize(target, rocketDamage);
            com.piranport.debug.PiranPortDebug.event(
                    "Aircraft ROCKET_SALVO | entityId={} capacity={} remaining={} firing={} targetHP={}",
                    craft.getId(), craft.ammoCapacity, craft.remainingAmmo, toFire, target.getHealth());
            float initSpeed = MissileEntity.MissileType.ROCKET.initialSpeed();
            for (int i = 0; i < toFire; i++) {
                double spread = (i - (toFire - 1) / 2.0) * ModEquipmentConfig.ROCKET_FIGHTER_SPREAD_STEP.get();
                double cos = Math.cos(spread);
                double sin = Math.sin(spread);
                Vec3 fanDir = new Vec3(
                        dir.x * cos - dir.z * sin,
                        dir.y,
                        dir.x * sin + dir.z * cos
                ).normalize();
                MissileEntity rocket = new MissileEntity(craft.level(),
                        MissileEntity.MissileType.ROCKET,
                        rocketDamage, 0f,
                        ModEquipmentConfig.ROCKET_FIGHTER_EXPLOSION_POWER.get().floatValue(),
                        "piranport:rocket_ammo");
                rocket.moveTo(craft.getX(), craft.getY() + ModEquipmentConfig.ROCKET_FIGHTER_SPAWN_Y_OFFSET.get(),
                        craft.getZ(), rocket.getYRot(), rocket.getXRot());
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
            craft.setDeltaMovement(toTarget.normalize().scale(Math.min(craft.getPanelSpeed()
                    * ModEquipmentConfig.ROCKET_FIGHTER_PURSUIT_MULTIPLIER.get()
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

        double climbY = target.getY() + ModEquipmentConfig.DIVE_BOMBER_CLIMB_ALTITUDE_OFFSET.get();
        double heightDiff = Math.max(0, climbY - craft.getY());
        double phaseSpeed = craft.phaseSpeedCoefficient();
        int climbTimeout = Math.max(ModEquipmentConfig.DIVE_BOMBER_CLIMB_TIMEOUT_MIN_TICKS.get(),
                (int)Math.ceil(heightDiff / Math.max(craft.getPanelSpeed()
                        * ModEquipmentConfig.DIVE_BOMBER_CLIMB_SPEED_MULTIPLIER.get() * phaseSpeed, 0.1)));
        if (!craft.diveCommitted
                && craft.getY() < climbY - ModEquipmentConfig.DIVE_BOMBER_CLIMB_ARRIVAL_TOLERANCE.get()
                && craft.stateTicks < climbTimeout) {
            Vec3 toClimb = new Vec3(target.getX() - craft.getX(), climbY - craft.getY(), target.getZ() - craft.getZ());
            double dist = toClimb.length();
            craft.setDeltaMovement(toClimb.normalize().scale(Math.min(craft.getPanelSpeed()
                    * ModEquipmentConfig.DIVE_BOMBER_CLIMB_SPEED_MULTIPLIER.get() * phaseSpeed, dist)));
            return;
        }

        if (!craft.diveCommitted) {
            craft.diveCommitted = true;
            Vec3 targetPos = target.getEyePosition();
            double estimatedDist = craft.position().distanceTo(targetPos);
            double diveSpeed = Math.max(craft.getPanelSpeed()
                    * ModEquipmentConfig.DIVE_BOMBER_DIVE_SPEED_MULTIPLIER.get() * phaseSpeed, 0.1);
            double estimatedTicks = estimatedDist / diveSpeed;
            Vec3 targetVel = target.getDeltaMovement();
            craft.diveTarget = targetPos.add(targetVel.scale(estimatedTicks));
        }

        if (craft.diveTarget != null) {
            Vec3 toDive = craft.diveTarget.subtract(craft.position());
            double diveDist = toDive.length();
            if (diveDist < ModEquipmentConfig.DIVE_BOMBER_RELEASE_DISTANCE.get()) {
                // Drop the bomb
                net.minecraft.world.level.Level level = craft.level();
                float bombPower = craft.getPanelDamage();
                int bombCount = Math.max(1, craft.remainingAmmo);
                for (int i = 0; i < bombCount; i++) {
                    AerialBombEntity bomb = new AerialBombEntity(level, bombPower / bombCount,
                            ModEquipmentConfig.DIVE_BOMBER_EXPLOSION_POWER.get().floatValue());
                    double spreadX = (level.random.nextDouble() - 0.5) * ModEquipmentConfig.DIVE_BOMBER_BOMB_SPREAD.get();
                    double spreadZ = (level.random.nextDouble() - 0.5) * ModEquipmentConfig.DIVE_BOMBER_BOMB_SPREAD.get();
                    bomb.moveTo(craft.getX() + spreadX, craft.getY(), craft.getZ() + spreadZ, 0, 0);
                    // Preserve the aircraft's horizontal momentum.  Dropping with
                    // a zero horizontal velocity made every dive bomb fall behind
                    // the moving attack run and systematically miss ships at sea.
                    // 注意：俯冲投弹的水平动量倍数与水平轰炸的前抛倍数（level_bomber.horizontal_velocity_multiplier）
                    // 是两种不同机制，各自可调，不合并。
                    Vec3 aircraftVelocity = craft.getDeltaMovement();
                    bomb.setDeltaMovement(aircraftVelocity.x * ModEquipmentConfig.DIVE_BOMBER_HORIZONTAL_MOMENTUM.get(),
                            Math.min(ModEquipmentConfig.DIVE_BOMBER_MIN_VERTICAL_VELOCITY.get(), aircraftVelocity.y),
                            aircraftVelocity.z * ModEquipmentConfig.DIVE_BOMBER_HORIZONTAL_MOMENTUM.get());
                    bomb.setOwner(owner);
                    level.addFreshEntity(bomb);
                }
                craft.remainingAmmo = 0;
                craft.hasFired = true;
            } else {
                double speed = Math.min(craft.getPanelSpeed()
                        * ModEquipmentConfig.DIVE_BOMBER_DIVE_APPROACH_MULTIPLIER.get() * phaseSpeed, diveDist);
                craft.setDeltaMovement(toDive.normalize().scale(speed));
            }
        }
    }

    /**
     * 鱼雷机攻击：接近 → 低空贴水面平飞段 → 到投放点向下投雷。
     *
     * <p>阶段划分（用 {@code craft.torpedoRunStart} 区分）：
     * <ol>
     *   <li>接近段（torpedoRunStart == null）：保持 approach_altitude 高度飞向目标，
     *       水平距离 ≤ run_distance 时记录 torpedoRunStart 进入平飞段。</li>
     *   <li>平飞段：降到 target.Y + run_altitude 贴水面平飞，朝「预测撞击点」直飞。
     *       每 tick 做提前量解算：鱼雷入水后直线匀速（torpedo_speed），目标匀速直线外推，
     *       解一元一次 t ≈ 水平距离 / 雷速，预测点 P = T + u·t（u=目标水平速度，
     *       外推距离钳制在 max_lead 内；目标静止时 u=0，直接瞄当前位置）。</li>
     *   <li>投放：载机到「目标当前位置」的水平距离 ≤ release_distance 且高度已降到
     *       run_altitude 容差内（或平飞超时）时投雷。鱼雷走 airDrop 入水，
     *       入水方向 = 投放瞬间指向预测点 P 的固定方向，之后直线航行无制导。</li>
     * </ol>
     *
     * @see AircraftEntity#tickTorpedoBomberAttack(Player, LivingEntity)
     */
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
        double phaseSpeed = craft.phaseSpeedCoefficient();
        double torpedoSpeed = ModEquipmentConfig.TORPEDO_BOMBER_TORPEDO_SPEED.get();

        // ---- 提前量解算（平飞段与投放判定共用）----
        // 一元一次近似：t = 当前水平距离 / 雷速，P = T + u·t。
        // 鱼雷无制导，这就是投放时交给 TorpedoEntity 的固定入水方向。
        Vec3 targetVel = target.getDeltaMovement();
        Vec3 targetVelH = new Vec3(targetVel.x, 0, targetVel.z);
        double tHit = torpedoSpeed > 0.01 ? horizDist / torpedoSpeed : horizDist / 0.01;
        Vec3 lead = targetVelH.scale(tHit);
        double leadLen = lead.length();
        double maxLead = ModEquipmentConfig.TORPEDO_BOMBER_MAX_LEAD.get();
        if (leadLen > maxLead) lead = lead.scale(maxLead / leadLen);
        Vec3 predicted = new Vec3(target.getX(), 0, target.getZ()).add(lead);
        Vec3 toPredicted = new Vec3(predicted.x - craft.getX(), 0, predicted.z - craft.getZ());
        double distToPredicted = toPredicted.length();

        // ---- 阶段 1：接近段 ----
        // 在进入 run_distance 之前就开始下降：从 approach_altitude 平滑降到 run_altitude，
        // 这样平飞段一开始就是贴水面的直线鱼雷航线，而不是临到目标才俯冲。
        if (craft.torpedoRunStart == null) {
            craft.setTorpedoAimPoint(null);
            double runDistance = ModEquipmentConfig.TORPEDO_BOMBER_RUN_DISTANCE.get();
            double runAltitude = ModEquipmentConfig.TORPEDO_BOMBER_RUN_ALTITUDE.get();
            double tolerance = ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_TOLERANCE.get();
            double descentStart = Math.max(runDistance,
                    ModEquipmentConfig.TORPEDO_BOMBER_DESCENT_START_DISTANCE.get());
            double desiredAlt = target.getY() + (horizDist <= descentStart
                    ? runAltitude
                    : ModEquipmentConfig.TORPEDO_BOMBER_APPROACH_ALTITUDE.get());
            boolean atRunAltitude = craft.getY() - (target.getY() + runAltitude) <= tolerance;

            if (horizDist <= runDistance && atRunAltitude && craft.attackCooldown <= 0) {
                // 进入平飞段
                craft.torpedoRunStart = craft.position();
                craft.torpedoRunTicks = 0;
            } else if (horizDist <= runDistance && craft.attackCooldown > 0) {
                // 已到航线入口但武器还在冷却：在当前距离盘旋等待，避免继续前冲后贴着目标投雷。
                Vec3 radial = horizDist > 0.01 ? new Vec3(dx, 0, dz).normalize() : new Vec3(0, 0, 1);
                Vec3 tangent = new Vec3(-radial.z, 0, radial.x);
                double holdSpeed = Math.min(craft.getPanelSpeed() * 0.5 * phaseSpeed, 0.8);
                double radialError = horizDist - runDistance;
                double radialSpeed = Math.max(-holdSpeed * 0.5,
                        Math.min(holdSpeed * 0.5, radialError * 0.1));
                double yAdjust = (target.getY() + runAltitude - craft.getY())
                        * ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_ADJUST.get();
                craft.setDeltaMovement(
                        tangent.x * holdSpeed + radial.x * radialSpeed,
                        yAdjust,
                        tangent.z * holdSpeed + radial.z * radialSpeed);
                return;
            } else {
                double altitudeDiff = craft.getY() - desiredAlt;
                if (altitudeDiff > tolerance) {
                    // 受控下降：水平朝目标飞，垂直分量按高度差修正并钳制在浅角度内，
                    // 不再使用“垂直速度硬上限 1.0 格/tick”的俯冲式下降。
                    Vec3 toTarget = horizDist > 0.01
                            ? new Vec3(dx, 0, dz).normalize() : new Vec3(0, 0, 1);
                    double horizontalSpeed = Math.min(craft.getPanelSpeed()
                            * ModEquipmentConfig.TORPEDO_BOMBER_DESCEND_SPEED_MULTIPLIER.get()
                            * phaseSpeed, ModEquipmentConfig.TORPEDO_BOMBER_DESCEND_MAX_SPEED.get());
                    double maxVertical = Math.max(0.05, horizontalSpeed * 0.35);
                    double yAdjust = Math.max(-maxVertical, Math.min(maxVertical,
                            (desiredAlt - craft.getY())
                                    * ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_ADJUST.get()));
                    craft.setDeltaMovement(toTarget.x * horizontalSpeed, yAdjust,
                            toTarget.z * horizontalSpeed);
                    return;
                }
                Vec3 toTarget = new Vec3(dx, 0, dz).normalize().scale(Math.min(craft.getPanelSpeed()
                        * ModEquipmentConfig.TORPEDO_BOMBER_APPROACH_SPEED_MULTIPLIER.get()
                        * phaseSpeed, horizDist));
                double yAdjust = (desiredAlt - craft.getY())
                        * ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_ADJUST.get();
                craft.setDeltaMovement(toTarget.x, yAdjust, toTarget.z);
                return;
            }
        }

        // ---- 阶段 2：低空贴水面平飞段，朝预测撞击点直飞 ----
        // 预测点写到实体上，客户端 renderer 据此画预瞄线。
        craft.setTorpedoAimPoint(new Vec3(predicted.x, target.getY(), predicted.z));
        craft.torpedoRunTicks++;
        double runAlt = ModEquipmentConfig.TORPEDO_BOMBER_RUN_ALTITUDE.get();
        double desiredY = target.getY() + runAlt;
        double runAltDiff = craft.getY() - desiredY;
        boolean atRunAltitude = runAltDiff <= ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_TOLERANCE.get();
        boolean timeout = craft.torpedoRunTicks > ModEquipmentConfig.TORPEDO_BOMBER_RUN_TIMEOUT_TICKS.get();

        // 投放判据：到「目标当前位置」的水平距离 ≤ release_distance 且已到贴水面高度；超时强制投。
        // WHY 不用 distToPredicted：预测点会随目标速度前后/侧向偏移，目标横向机动时
        // 预测点可能离载机只有两三格，表现为“贴着目标投雷”。投雷距离必须和玩家看到
        // 的载机—目标距离同口径；预测点只负责决定鱼雷入水后的航向。
        if ((horizDist <= ModEquipmentConfig.TORPEDO_BOMBER_RELEASE_DISTANCE.get() && atRunAltitude
                || timeout)
                && craft.attackCooldown <= 0 && distToPredicted > 0.01) {
            Vec3 dir = toPredicted.normalize();
            int toFire = craft.computeSalvoSize(target, craft.getPanelDamage());
            com.piranport.debug.PiranPortDebug.event(
                    "Aircraft TORPEDO_SALVO | entityId={} capacity={} remaining={} firing={} targetHP={}",
                    craft.getId(), craft.ammoCapacity, craft.remainingAmmo, toFire, target.getHealth());
            for (int i = 0; i < toFire; i++) {
                TorpedoEntity torpedo = new TorpedoEntity(com.piranport.registry.ModEntityTypes.TORPEDO_ENTITY.get(), craft.level());
                double offsetX = -dir.z * (i - (toFire - 1) / 2.0) * ModEquipmentConfig.TORPEDO_BOMBER_SPREAD_OFFSET.get();
                double offsetZ = dir.x * (i - (toFire - 1) / 2.0) * ModEquipmentConfig.TORPEDO_BOMBER_SPREAD_OFFSET.get();
                torpedo.moveTo(craft.getX() + offsetX,
                        craft.getY() - ModEquipmentConfig.TORPEDO_BOMBER_DROP_HEIGHT_OFFSET.get(),
                        craft.getZ() + offsetZ, 0, 0);
                // 注意：空投鱼雷不绑定具体 TorpedoItem 型号，初速走终端参数
                // global.torpedo_bomber_combat.torpedo_speed（默认 0.8），
                // 因此不受调试终端的型号航速覆盖影响（玩家发射路径读 TorpedoItem.getSpeed()，
                // 这里没有型号可查）。这是设计如此，不是漏改 —— 若要让它也受型号覆盖，
                // 需要先给飞机挂弹定义「所投型号」再按型号取 getSpeed()。
                torpedo.setSpeed((float) torpedoSpeed);
                // 空投下落分支：入水后自动切巡航，方向固定为投放时解算好的 dir（指向预测撞击点），
                // 之后直线航行直到命中或航程耗尽 —— 无制导。
                torpedo.setAirDrop(true, dir);
                Vec3 vel = dir.scale(torpedoSpeed)
                        .add(0, ModEquipmentConfig.TORPEDO_BOMBER_TORPEDO_VERTICAL_SPEED.get(), 0);
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

        // 平飞移动：朝预测点直飞，高度向贴水面收敛
        double runSpeed = Math.min(craft.getPanelSpeed()
                * ModEquipmentConfig.TORPEDO_BOMBER_RUN_SPEED_MULTIPLIER.get()
                * phaseSpeed, Math.max(distToPredicted, 0.1));
        Vec3 runDir = distToPredicted > 0.01 ? toPredicted.normalize()
                : (horizDist > 0.01 ? new Vec3(dx, 0, dz).normalize() : new Vec3(0, 0, 1));
        double yAdjust = (desiredY - craft.getY())
                * ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_ADJUST.get();
        // 高度还差得远时允许继续下降，但钳制在浅角度内（≤约 14°）。
        // WHY：原先的垂直速度上限 1.0 格/tick 会让鱼雷机在目标附近俯冲，
        // 视觉上像俯冲轰炸机；鱼雷机应当是贴水面的平飞投雷。
        if (runAltDiff > ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_TOLERANCE.get()) {
            yAdjust -= Math.min(runAltDiff * 0.2, ModEquipmentConfig.TORPEDO_BOMBER_DESCEND_MAX_SPEED.get());
        }
        double maxVertical = Math.max(0.05, runSpeed * 0.25);
        yAdjust = Math.max(-maxVertical, Math.min(maxVertical, yAdjust));
        craft.setDeltaMovement(runDir.x * runSpeed, yAdjust, runDir.z * runSpeed);
    }

    // ==== 水平轰炸机投弹参数 ====
    // 投弹高度、投弹初速垂直分量、提前判据最小窗口、航线速度、散布等原先写死为常量，
    // 现已下沉到调试终端（global.level_bomber.*），默认值 = 下沉前写死值。
    // 水平分量 = 载机当前水平速度 × terminal 倍数，见 LEVEL_BOMBER_HORIZONTAL_VELOCITY_MULTIPLIER。

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
        double bombAlt = target.getY() + ModEquipmentConfig.LEVEL_BOMBER_ALTITUDE_OFFSET.get();

        // Phase 1: Approach bombing altitude
        if (craft.getY() < bombAlt - ModEquipmentConfig.LEVEL_BOMBER_ALTITUDE_TOLERANCE.get()) {
            Vec3 toAlt = new Vec3(dx, bombAlt - craft.getY(), dz);
            craft.setDeltaMovement(toAlt.normalize().scale(Math.min(craft.getPanelSpeed()
                    * ModEquipmentConfig.LEVEL_BOMBER_CLIMB_SPEED_MULTIPLIER.get()
                    * craft.phaseSpeedCoefficient(), toAlt.length())));
            return;
        }

        // Phase 2a: 绕背进场——移动目标要先绕到其运动方向后方，再顺着目标航向进入投弹航线，
        // 让提前量解算退化为「同向追尾」，弹着散布最小。静止目标直接跳过本相位。
        if (!craft.levelRearApproachDone) {
            Vec3 targetVelH = new Vec3(target.getDeltaMovement().x, 0, target.getDeltaMovement().z);
            if (targetVelH.lengthSqr() < 1.0e-4) {
                // 静止目标没有「后方」可言，直接进入原锁航线逻辑。
                craft.levelRearApproachDone = true;
            } else {
                Vec3 targetDir = targetVelH.normalize();
                double rearDist = ModEquipmentConfig.LEVEL_BOMBER_REAR_APPROACH_DISTANCE.get();
                Vec3 rearPoint = new Vec3(target.getX() - targetDir.x * rearDist,
                        bombAlt,
                        target.getZ() - targetDir.z * rearDist);
                Vec3 toRear = rearPoint.subtract(craft.position());
                // 到点容差取航线速度的三分之一并保底 2 格：容差太小会在目标附近绕圈，
                // 太大则「绕背」名存实亡。
                double runSpeedNow = craft.getPanelSpeed()
                        * ModEquipmentConfig.LEVEL_BOMBER_RUN_SPEED_MULTIPLIER.get()
                        * craft.phaseSpeedCoefficient();
                if (toRear.length() <= Math.max(runSpeedNow / 3.0, 2.0)) {
                    craft.levelRearApproachDone = true;
                } else {
                    craft.setDeltaMovement(toRear.normalize().scale(
                            Math.min(runSpeedNow, toRear.length())));
                    return;
                }
            }
        }

        // Phase 2b: Lock run direction and execute bomb run
        if (craft.levelRunDirection == null) {
            craft.levelRunDirection = new Vec3(dx, 0, dz).normalize();
            craft.levelRunTicks = 0;
            craft.levelAbortTicks = 0;
        }

        double runSpeed = craft.getPanelSpeed() * ModEquipmentConfig.LEVEL_BOMBER_RUN_SPEED_MULTIPLIER.get()
                * craft.phaseSpeedCoefficient();
        Vec3 runDir = craft.levelRunDirection;
        Vec3 runVel = new Vec3(runDir.x * runSpeed, 0, runDir.z * runSpeed);

        // 修正爬升终点侧偏：runDir 锁定后，载机可能仍在「过目标直线」的平行线上，
        // 纯 runDir 速度永远收敛不到目标垂线。加一个「垂直 runDir、指向目标」的修正分量，
        // 幅度取侧偏的一半并封顶 runSpeed 的 30%，既不抢直线轰炸手感，又把侧偏逐步清零。
        Vec3 toTargetHoriz = new Vec3(dx, 0, dz);
        Vec3 lateral = toTargetHoriz.subtract(runDir.scale(toTargetHoriz.dot(runDir)));
        double lateralDist = lateral.length();
        if (lateralDist > 0.01) {
            double correctionSpeed = Math.min(lateralDist * 0.5, runSpeed * 0.3);
            runVel = runVel.add(lateral.normalize().scale(correctionSpeed));
        }
        craft.setDeltaMovement(runVel);

        craft.levelRunTicks++;

        // 投弹判据：载机到目标的水平距离 ≤ 提前距离时投弹。
        // 提前距离 = 航弹从投弹高度落到目标高度期间自身的水平前进量（保留前抛的直接后果），
        // 由 LevelBombLead 按 ThrowableProjectile#tick 的逐行物理积分算出。删掉了旧的固定 <3 格判据。
        if (!craft.levelBombDropped) {
            // 沿 runDir 投影取水平速度，不能用 |deltaMovement|——侧偏修正分量会被算进
            // 「前抛速度」，让提前量按过大的速度算、落点跟着修正分量随机飘。
            Vec3 delta = craft.getDeltaMovement();
            double craftHorizontalSpeed = delta.x * runDir.x + delta.z * runDir.z;
            double horizontalMultiplier = ModEquipmentConfig.LEVEL_BOMBER_HORIZONTAL_VELOCITY_MULTIPLIER.get();
            double dropYOffset = ModEquipmentConfig.LEVEL_BOMBER_DROP_Y_OFFSET.get();
            double verticalVelocity = ModEquipmentConfig.LEVEL_BOMBER_VERTICAL_VELOCITY.get();
            // 起始高度与 bomb.moveTo 的 y 一致（craft.getY() - dropYOffset），目标高度取其脚底。
            double dropHeight = (craft.getY() - dropYOffset) - target.getY();
            // 重力读实体（AerialBombEntity.gravity()，终端参数 global.aerial_bomb.gravity），
            // 不写死数字，避免与实体物理/落点标记漂移。
            // C 方案：目标在动的场合，「该投的距离」不再是「到当前位置」，
            // 而是「航弹落地那 tick 目标会在哪里」。静止目标下 targetV=0，退化为原公式。
            // targetV 必须投影到 runDir 上取带符号标量（同向正、反向负），不能用 3D 距离
            // —— 3D 模长把「朝载机来」和「背离载机跑」混为一谈，方向错会系统性偏前/偏后。
            Vec3 targetVel = target.getDeltaMovement();
            double targetVRun = targetVel.x * runDir.x + targetVel.z * runDir.z;
            double leadDistance = LevelBombLead.releaseDistanceToMovingTarget(
                    dropHeight, AerialBombEntity.gravity(),
                    craftHorizontalSpeed * horizontalMultiplier, verticalVelocity,
                    targetVRun);

            // 投弹窗口除了距离，还要求目标仍在航线正前方锥形内：
            // levelRunDirection 锁定后若载机因爬升终点侧偏而不在过目标的直线上，
            // 纯距离判据会让投弹点偏到目标侧后。点积/模长 = cos(偏角)，≥0.9 ≈ 25°锥。
            Vec3 toTarget = new Vec3(dx, 0, dz);
            boolean inRunCone = horizDist < 1.0
                    || toTarget.normalize().dot(runDir) >= 0.9;

            // 锥外拉起：目标跑出 ±25° 锥后若继续直飞，载机会永远错过投弹窗口。
            // 连续若干 tick 不在锥内就放弃本航线：爬升拉起、清掉锁向，回到绕背/接近相位重新瞄准。
            if (!inRunCone) {
                craft.levelAbortTicks++;
            } else {
                craft.levelAbortTicks = 0;
            }
            if (craft.levelAbortTicks >= ModEquipmentConfig.LEVEL_BOMBER_ABORT_TICKS.get()) {
                craft.levelRunDirection = null;
                craft.levelBombDropped = false;
                craft.levelAbortTicks = 0;
                craft.levelRearApproachDone = false;
                craft.setDeltaMovement(new Vec3(runDir.x * runSpeed,
                        craft.getPanelSpeed() * ModEquipmentConfig.LEVEL_BOMBER_CLIMB_SPEED_MULTIPLIER.get()
                                * craft.phaseSpeedCoefficient(),
                        runDir.z * runSpeed));
                return;
            }

            if (inRunCone
                    && (horizDist <= Math.max(leadDistance, ModEquipmentConfig.LEVEL_BOMBER_MIN_RELEASE_DISTANCE.get())
                    || craft.levelRunTicks > ModEquipmentConfig.LEVEL_BOMBER_RUN_TIMEOUT_TICKS.get())) {
                float bombPower = craft.getPanelDamage();
                int bombCount = Math.max(1, craft.remainingAmmo);
                for (int i = 0; i < bombCount; i++) {
                    AerialBombEntity bomb = new AerialBombEntity(craft.level(), bombPower / bombCount,
                            ModEquipmentConfig.LEVEL_BOMBER_EXPLOSION_POWER.get().floatValue());
                    // 保留随机散布：提前量把弹着点拉回目标，但不必弹弹正中。
                    double spreadX = (craft.level().random.nextDouble() - 0.5)
                            * ModEquipmentConfig.LEVEL_BOMBER_BOMB_SPREAD.get();
                    double spreadZ = (craft.level().random.nextDouble() - 0.5)
                            * ModEquipmentConfig.LEVEL_BOMBER_BOMB_SPREAD.get();
                    bomb.moveTo(craft.getX() + spreadX, craft.getY() - dropYOffset, craft.getZ() + spreadZ, 0, 0);
                    // 前抛：航弹初速 = runDir × craftHorizontalSpeed（沿航线投影的速度）× 倍数。
                    // 不能直接用 craft.getDeltaMovement()——横向修正分量会让弹着点横向飘。
                    bomb.setDeltaMovement(runDir.x * craftHorizontalSpeed * horizontalMultiplier,
                            verticalVelocity,
                            runDir.z * craftHorizontalSpeed * horizontalMultiplier);
                    bomb.setOwner(owner);
                    craft.level().addFreshEntity(bomb);
                }
                craft.remainingAmmo = 0;
                craft.levelBombDropped = true;
                craft.levelDropPoint = craft.position();
            }
        }

        // After dropping, fly for a few more ticks then return
        if (craft.levelBombDropped
                && craft.levelRunTicks > ModEquipmentConfig.LEVEL_BOMBER_RETURN_TICKS.get()) {
            craft.startReturning("level_bomber_run_complete");
        }
    }

    // ====================================================================
    // 目标解析
    // ====================================================================

    /** @see AircraftEntity#isAirborneTarget(Entity) */
    public static boolean isAirborneTarget(Entity e) {
        return e instanceof AircraftEntity;
    }

    // ---- 公共索敌骨架：resolveLocked + autoSeek，两个入口仅谓词不同 ----

    /**
     * 火控锁定分支：遍历锁定 UUID 列表，取首个满足谓词的存活实体。
     * SPREAD 模式取最近的，FOCUS/FOLLOW 按列表顺序取首个。
     * 谓词只负责类型/高度过滤，不负责生死（这里统一判 isAlive）。
     */
    @Nullable
    private static <T extends Entity> T resolveLocked(AircraftEntity craft, Player owner,
                                                      java.util.function.Predicate<Entity> predicate,
                                                      Class<T> castTo) {
        if (!(craft.level() instanceof net.minecraft.server.level.ServerLevel sl)) return null;
        java.util.List<java.util.UUID> locks = com.piranport.aviation.FireControlManager.getTargets(owner.getUUID());
        if (locks.isEmpty()) return null;
        if (craft.attackMode == AircraftAttackMode.SPREAD) {
            return locks.stream()
                    .map(sl::getEntity)
                    .filter(e -> e != null && e.isAlive() && predicate.test(e))
                    .map(castTo::cast)
                    .min(java.util.Comparator.comparingDouble(craft::distanceTo))
                    .orElse(null);
        }
        for (java.util.UUID uuid : locks) {
            Entity e = sl.getEntity(uuid);
            if (e != null && e.isAlive() && predicate.test(e)) return castTo.cast(e);
        }
        return null;
    }

    /**
     * 自动索敌分支：以 craft 为中心 inflate(range) 扫 rangeClass，按谓词过滤后取最近。
     * 调用方负责传入「敌对过滤 + 高度/类型过滤」的合成谓词；
     * 已锁定过火控（hasEverHadFireControl）或已一次性搜过（autoSeekDone）时由调用方拦截。
     */
    @Nullable
    private static <T extends Entity> T autoSeek(AircraftEntity craft, Player owner,
                                                 Class<T> rangeClass,
                                                 java.util.function.Predicate<T> predicate,
                                                 double range) {
        if (!(craft.level() instanceof net.minecraft.server.level.ServerLevel sl)) return null;
        net.minecraft.world.phys.AABB box = craft.getBoundingBox().inflate(range);
        return sl.getEntitiesOfClass(rangeClass, box, predicate)
                .stream()
                .min(java.util.Comparator.comparingDouble(craft::distanceToSqr))
                .orElse(null);
    }

    /** @see AircraftEntity#resolveTarget(Player) */
    @Nullable
    public static LivingEntity resolveTarget(AircraftEntity craft, Player owner) {
        LivingEntity locked = resolveLocked(craft, owner,
                e -> e instanceof LivingEntity && !isAirborneTarget(e), LivingEntity.class);
        if (locked != null) return locked;
        if (!com.piranport.aviation.FireControlManager.getTargets(owner.getUUID()).isEmpty()) return null;

        // Auto-seek: scan for ground targets within the terminal-configured range
        if (craft.hasEverHadFireControl) return null;
        // 自动索敌必须加敌对过滤：否则鱼、友军、玩家都会被无差别自动开打。
        // 手动火控锁定分支在上方，保持原样不限制（尊重玩家意图）。
        return autoSeek(craft, owner, LivingEntity.class,
                e -> e.isAlive() && e != owner
                        && com.piranport.combat.CombatTargeting.isHostileTarget(owner, e)
                        && !(e instanceof net.minecraft.world.entity.Mob mob && mob.isNoAi())
                        && !isAirborneTarget(e),
                ModEquipmentConfig.AIRCRAFT_AUTONOMOUS_SEEK_RANGE.get());
    }

    /**
     * 自主机枪战斗机的索敌：玩家所属的飞机，或中立生物（非敌对、非玩家的 Mob）。
     * 不打玩家本体与深海单位（Monster/Enemy），避免战斗机向水面舰船开火。
     */
    @Nullable
    public static Entity resolveAutonomousFighterTarget(AircraftEntity craft) {
        if (!(craft.level() instanceof ServerLevel sl)) return null;
        AABB box = craft.getBoundingBox().inflate(ModEquipmentConfig.AIRCRAFT_AUTONOMOUS_SEEK_RANGE.get());
        return sl.getEntitiesOfClass(Entity.class, box,
                        e -> e.isAlive() && e != craft && isAutonomousFighterPrey(e))
                .stream()
                .min(java.util.Comparator.comparingDouble(craft::distanceToSqr))
                .orElse(null);
    }

    private static boolean isAutonomousFighterPrey(Entity e) {
        if (e instanceof AircraftEntity other) return other.getOwnerUUID() != null;
        return e instanceof net.minecraft.world.entity.Mob mob
                && !(mob instanceof net.minecraft.world.entity.monster.Enemy)
                && !mob.isNoAi();
    }

    /** @see AircraftEntity#resolveFighterTarget(Player) */
    @Nullable
    public static Entity resolveFighterTarget(AircraftEntity craft, Player owner) {
        // 空优开关默认 true：手动锁定也跳过非空中目标；玩家 U 键切换后允许锁地面
        boolean airOnly = com.piranport.aviation.FireControlManager.isFighterAirOnly(owner.getUUID());
        Entity locked = resolveLocked(craft, owner,
                e -> !airOnly || e instanceof AircraftEntity || isAirborneTarget(e), Entity.class);
        if (locked != null) return locked;
        if (!com.piranport.aviation.FireControlManager.getTargets(owner.getUUID()).isEmpty()) return null;

        // Auto-seek: only enemy aircraft (机枪只能对空)
        return autoSeek(craft, owner, AircraftEntity.class,
                e -> e.isAlive() && e != craft
                        && com.piranport.combat.CombatTargeting.isHostileTarget(owner, e),
                ModEquipmentConfig.AIRCRAFT_AUTONOMOUS_SEEK_RANGE.get());
    }
}
