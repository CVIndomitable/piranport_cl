package com.piranport.entity;

import com.piranport.config.ModCommonConfig;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class TorpedoEntity extends ThrowableItemProjectile {
    private int caliber = 533;
    private float damage = 18f;
    private float torpedoSpeed = 1.2f;
    private int lifetime = 1200; // 1 minute hard cap
    private float explosionRadius = 2.0f;
    private boolean magnetic = false;
    private boolean exploded = false;
    private Component sourceAircraftName;
    private Entity sourceAircraft;

    /** Wire-guided torpedo state. */
    private boolean wireGuided = false;
    private Vec3 launchPos = null;
    /** Fallback wire range in blocks when simulation distance is unavailable. */
    private static final double WIRE_FALLBACK_RANGE = 16.0;

    /** Acoustic homing torpedo state. */
    private boolean acoustic = false;
    /** Detection range for moving entities (blocks). */
    private static final double ACOUSTIC_DETECT_RANGE = 25.0;
    /** Reduced detection range for sneaking entities (blocks). */
    private static final double ACOUSTIC_SNEAK_RANGE = 10.0;
    /** Maximum turn rate per tick (degrees). */
    private static final float ACOUSTIC_MAX_TURN_DEG = 3.0f;
    /** Grace period before acoustic homing activates (ticks). */
    private static final int ACOUSTIC_ARM_TICKS = 10;
    /** Adaptive turn rate multipliers based on distance */
    private static final double CLOSE_RANGE = 5.0;
    private static final double MID_RANGE = 15.0;
    private static final float CLOSE_TURN_MULTIPLIER = 1.5f;
    private static final float FAR_TURN_MULTIPLIER = 0.6f;
    /** Currently locked target for acoustic homing */
    private Entity lockedTarget = null;
    /** Persisted UUID of locked target for cross-load entity resolution */
    private UUID lockedTargetUuid = null;
    /** Ticks since target was locked */
    private int lockDuration = 0;
    /** Minimum lock duration before allowing target switch (ticks) */
    private static final int MIN_LOCK_DURATION = 40;
    /** Maximum distance to maintain lock (blocks) */
    private static final double LOCK_BREAK_DISTANCE = 30.0;
    /** Distance advantage threshold for target switching (ratio) */
    private static final double TARGET_SWITCH_THRESHOLD = 0.7;

    /** 空投下落阶段（鱼雷机投弹后垂直入水，再切巡航） */
    private boolean airDrop = false;
    private Vec3 airDropDirection = Vec3.ZERO;

    /** Distance at which magnetic proximity fuze detonates (blocks). */
    private static final double MAGNETIC_DETONATE_DIST = 3.0;
    /** Grace period after launch before magnetic fuze arms (ticks). */
    private static final int MAGNETIC_ARM_TICKS = 5;

    // Required constructor for entity type registration
    public TorpedoEntity(EntityType<? extends TorpedoEntity> type, Level level) {
        super(type, level);
    }

    // Constructor for launching
    public TorpedoEntity(Level level, LivingEntity shooter, int caliber) {
        super(ModEntityTypes.TORPEDO_ENTITY.get(), shooter, level);
        this.caliber = caliber;
        if (caliber == 610) {
            this.damage = 28f;
            this.torpedoSpeed = 1.0f;
            this.lifetime = 1200;
            this.explosionRadius = 2.5f;
        } else {
            this.damage = 18f;
            this.torpedoSpeed = 1.0f;
            this.lifetime = 1200;
            this.explosionRadius = 2.0f;
        }
    }

    public float getTorpedoSpeed() {
        return torpedoSpeed;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setSpeed(float speed) {
        this.torpedoSpeed = speed;
    }

    public void setLifetime(int lifetime) {
        this.lifetime = lifetime;
    }

    public void setMagnetic(boolean magnetic) {
        this.magnetic = magnetic;
    }

    public void setWireGuided(boolean wireGuided) {
        this.wireGuided = wireGuided;
        // launchPos is set lazily on first tick (after setPos has been called)
    }

    public boolean isWireGuided() {
        return wireGuided;
    }

    public void setAirDrop(boolean airDrop, Vec3 direction) {
        this.airDrop = airDrop;
        this.airDropDirection = new Vec3(direction.x, 0, direction.z).normalize();
    }

    public void setAcoustic(boolean acoustic) {
        this.acoustic = acoustic;
        if (acoustic) {
            this.torpedoSpeed = 0.7f;
        }
    }

    /** Drop the wire: torpedo continues ballistic cruising without player input. */
    public void cutWire() {
        wireGuided = false;
    }

    /** Wire length in blocks — equal to server simulation distance so the torpedo disconnects
     *  precisely when it leaves the ticking zone around the owning player. */
    private double getWireMaxRange() {
        if (level().getServer() != null) {
            // getPlayerList() is never null on a valid server instance
            int simChunks = level().getServer().getPlayerList().getSimulationDistance();
            if (simChunks > 0) return simChunks * 16.0;
        }
        return WIRE_FALLBACK_RANGE;
    }

    /**
     * Player-guided movement: follows the latest direction input at full torpedoSpeed,
     * blocked from surfacing (the block above must be water). Returns true if guided this tick
     * (caller should skip the cruise AI).
     */
    private boolean applyGuidedMovement() {
        Entity owner = getOwner();
        if (!(owner instanceof net.minecraft.server.level.ServerPlayer sp)) return false;
        java.util.UUID guided = com.piranport.combat.TorpedoGuidanceManager.getGuidedTorpedo(sp.getUUID());
        if (guided == null || !guided.equals(getUUID())) return false;

        float[] input = com.piranport.combat.TorpedoGuidanceManager.consumeInput(sp.getUUID());
        Vec3 dir;
        if (input != null && (input[0] != 0 || input[1] != 0 || input[2] != 0)) {
            double len = Math.sqrt(input[0] * input[0] + input[1] * input[1] + input[2] * input[2]);
            if (len < 0.001) dir = headingFromMotion();
            else dir = new Vec3(input[0] / len, input[1] / len, input[2] / len);
        } else {
            dir = headingFromMotion();
        }

        // Prevent surfacing: if the block directly above is not water, forbid upward velocity.
        BlockPos above = BlockPos.containing(getX(), getY() + 0.5, getZ());
        boolean waterAbove = level().getBlockState(above).getFluidState().is(Fluids.WATER);
        double vy = dir.y;
        if (!waterAbove && vy > 0) vy = 0;

        double speed = torpedoSpeed;
        setDeltaMovement(dir.x * speed, vy * speed, dir.z * speed);
        return true;
    }

    /** Current unit heading derived from velocity; falls back to +Z if motionless. */
    private Vec3 headingFromMotion() {
        Vec3 m = getDeltaMovement();
        double len = m.length();
        if (len < 0.001) return new Vec3(0, 0, 1);
        return m.scale(1.0 / len);
    }

    @Override
    public boolean isCurrentlyGlowing() {
        if (level().isClientSide() && com.piranport.ClientTickHandler.isHighlightEnabled()) {
            return true;
        }
        return super.isCurrentlyGlowing();
    }

    @Override
    protected Item getDefaultItem() {
        return switch (caliber) {
            case 610 -> ModItems.TORPEDO_610MM.get();
            case 720 -> ModItems.TORPEDO_720MM_TYPE0.get();
            case 530 -> ModItems.TORPEDO_530MM_TYPE95.get();
            default -> ModItems.TORPEDO_533MM.get();
        };
    }

    @Override
    public void tick() {
        // super.tick() handles movement, collision detection, drag (0.99), and gravity (0 via getDefaultGravity)
        super.tick();

        if (isRemoved()) return;

        // Resolve locked target from UUID if needed (e.g., after chunk reload)
        if (lockedTarget == null && lockedTargetUuid != null
                && level() instanceof net.minecraft.server.level.ServerLevel sl) {
            Entity entity = sl.getEntity(lockedTargetUuid);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                lockedTarget = living;
                lockedTargetUuid = null; // resolved
            } else {
                lockedTargetUuid = null; // target gone
                lockDuration = 0;
            }
        }

        // 1. 剩余航程检查 — 超时自爆
        if (--lifetime <= 0) {
            if (!level().isClientSide() && !exploded) {
                exploded = true;
                Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                        ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
                level().explode(this, getX(), getY(), getZ(), explosionRadius, interaction);
            }
            discard();
            return;
        }

        // 1.5. 磁性鱼雷近炸检测
        if (!level().isClientSide() && magnetic && tickCount > MAGNETIC_ARM_TICKS) {
            checkMagneticProximity();
            if (exploded) return;
        }

        // 1.55. 扁平 hitbox (0.5×0.25) 会让鱼雷从船体下方/甲板上方滑过而不触发 vanilla 扫掠碰撞；
        // 空投落水/水面巡航阶段主动近炸：发现有效目标就当作命中处理。
        if (!level().isClientSide() && !exploded && !airDrop && tickCount > 2 && !isRemoved()) {
            Entity nearby = findProximityHitTarget();
            if (nearby != null) {
                onHitEntity(new EntityHitResult(nearby));
                return;
            }
        }

        // 1.6. 线导鱼雷掉线检测 — 距离 owner 超过模拟距离时切线
        if (!level().isClientSide() && wireGuided && launchPos == null) {
            launchPos = position();
        }
        if (!level().isClientSide() && wireGuided) {
            double maxRange = getWireMaxRange();
            Entity owner = getOwner();
            double dist = owner != null ? position().distanceTo(owner.position())
                    : (launchPos != null ? position().distanceTo(launchPos) : 0.0);
            if (dist > maxRange) {
                wireGuided = false;
                if (owner instanceof net.minecraft.server.level.ServerPlayer sp) {
                    com.piranport.combat.TorpedoGuidanceManager.endGuidance(sp);
                    sp.displayClientMessage(
                            Component.translatable("message.piranport.torpedo_wire_lost"), true);
                }
            }
        }

        // 1.65. 线导鱼雷玩家制导：覆盖巡航 AI，维持速度并限制不得出水面
        if (!level().isClientSide() && wireGuided) {
            if (applyGuidedMovement()) return;
        }

        // 1.7. 声导鱼雷追踪
        if (!level().isClientSide() && acoustic && tickCount > ACOUSTIC_ARM_TICKS) {
            acousticHoming();
        }

        // 空投下落阶段：垂直入水后才切换为巡航
        if (airDrop) {
            Vec3 motion = getDeltaMovement();
            BlockPos pos = blockPosition();
            boolean inWater = level().getBlockState(pos).getFluidState().is(Fluids.WATER);
            boolean inAir = level().getBlockState(pos).isAir();
            boolean waterBelow = level().getBlockState(pos.below()).getFluidState().is(Fluids.WATER);

            if (inWater || (inAir && waterBelow)) {
                // 着水 → 切换为水面巡航
                airDrop = false;
                setDeltaMovement(airDropDirection.x * torpedoSpeed, 0, airDropDirection.z * torpedoSpeed);
            } else {
                // 空中自由落体
                setDeltaMovement(motion.x * 0.98, motion.y - 0.08, motion.z * 0.98);
            }
            return;
        }

        Vec3 motion = getDeltaMovement();

        // 水面贴合 AI
        BlockPos pos = blockPosition();
        boolean inAir = level().getBlockState(pos).isAir();
        boolean waterBelow = level().getBlockState(pos.below()).getFluidState().is(Fluids.WATER);
        boolean inWater = level().getBlockState(pos).getFluidState().is(Fluids.WATER);

        if (inAir && waterBelow) {
            // 水面航行：精确保持方向，恢复水平速度，清除垂直速度
            double currentH = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            if (currentH > 0.001) {
                // Normalize direction to avoid floating-point drift
                double dirX = motion.x / currentH;
                double dirZ = motion.z / currentH;
                setDeltaMovement(dirX * torpedoSpeed, 0, dirZ * torpedoSpeed);
            } else {
                setDeltaMovement(motion.x, 0, motion.z);
            }
        } else if (inWater) {
            // 在水中：精确保持方向，恢复水平速度并向上浮
            double currentH = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            if (currentH > 0.001) {
                // Normalize direction to avoid floating-point drift
                double dirX = motion.x / currentH;
                double dirZ = motion.z / currentH;
                setDeltaMovement(dirX * torpedoSpeed, motion.y + 0.04, dirZ * torpedoSpeed);
            } else {
                setDeltaMovement(motion.x, motion.y + 0.04, motion.z);
            }
        } else {
            // 空中自由下落：水平急速衰减 + 强重力，让鱼雷在 ~3 格内入水
            setDeltaMovement(motion.x * 0.70, motion.y - 0.25, motion.z * 0.70);
        }
    }

    /**
     * 扫描鱼雷周围的有效命中目标（垂直 ±1.2 格容忍甲板/船桥高差，水平 0.8 格容忍擦身）。
     * 返回最近的一个用于触发 onHitEntity；无目标返回 null。
     */
    private Entity findProximityHitTarget() {
        AABB scanBox = getBoundingBox().inflate(0.8, 1.2, 0.8);
        Entity best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (Entity e : level().getEntities(this, scanBox, e -> {
            if (e == getOwner()) return false;
            if (!e.isAlive() || !e.isPickable()) return false;
            if (com.piranport.combat.FriendlyFireHelper.shouldBlockHit(e, getOwner())) return false;
            return true;
        })) {
            double d = distanceToSqr(e);
            if (d < bestDistSq) {
                bestDistSq = d;
                best = e;
            }
        }
        return best;
    }

    private void checkMagneticProximity() {
        AABB searchBox = getBoundingBox().inflate(MAGNETIC_DETONATE_DIST);
        java.util.List<Entity> nearby = level().getEntities(this, searchBox, e -> {
            if (e == getOwner()) return false;
            if (com.piranport.combat.FriendlyFireHelper.shouldBlockHit(e, getOwner())) return false;
            return e.isAlive() && e.isPickable();
        });

        for (Entity entity : nearby) {
            double dist = entity.distanceTo(this);
            if (dist <= MAGNETIC_DETONATE_DIST) {
                magneticDetonate();
                return;
            }
        }
    }

    /**
     * 声导追踪：扫描范围内实体，向最近的"有声音"目标转向。
     * 移动中的实体检测范围25格，潜行实体缩减到10格。
     */
    private void acousticHoming() {
        // 1. Check if current locked target is still valid
        if (lockedTarget != null) {
            if (!lockedTarget.isAlive() || lockedTarget.isRemoved()) {
                lockedTarget = null;
                lockDuration = 0;
            } else {
                double dist = distanceTo(lockedTarget);
                if (dist > LOCK_BREAK_DISTANCE) {
                    lockedTarget = null;  // Target too far, unlock
                    lockDuration = 0;
                } else if (lockDuration < MIN_LOCK_DURATION) {
                    lockDuration++;
                    // Continue tracking current target
                    turnTowardsTarget(lockedTarget, dist);
                    return;
                }
            }
        }

        // 2. Scan for new target (only when unlocked or lock duration sufficient)
        Entity bestTarget = scanForTarget();

        // 3. Target switching logic: new target must be significantly closer
        if (bestTarget != null) {
            double newDist = distanceTo(bestTarget);
            if (lockedTarget == null) {
                lockedTarget = bestTarget;
                lockDuration = 0;
            } else {
                double currentDist = distanceTo(lockedTarget);
                // New target must be 30% closer to switch
                if (newDist < currentDist * TARGET_SWITCH_THRESHOLD) {
                    lockedTarget = bestTarget;
                    lockDuration = 0;
                }
            }
        }

        if (lockedTarget != null) {
            turnTowardsTarget(lockedTarget, distanceTo(lockedTarget));
        }
        // else: no target, maintain current heading (straight line)
    }

    /**
     * Scan for valid acoustic targets within detection range.
     * Returns the closest valid target or null if none found.
     */
    private Entity scanForTarget() {
        AABB searchBox = getBoundingBox().inflate(ACOUSTIC_DETECT_RANGE);
        Entity bestTarget = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity e : level().getEntities(this, searchBox, e -> {
            if (e == getOwner()) return false;
            if (com.piranport.combat.FriendlyFireHelper.shouldBlockHit(e, getOwner())) return false;
            return e.isAlive() && e.isPickable() && e instanceof LivingEntity;
        })) {
            double dist = distanceTo(e);
            // Sneaking entities have reduced detection range
            double range = (e instanceof LivingEntity living && living.isShiftKeyDown())
                    ? ACOUSTIC_SNEAK_RANGE : ACOUSTIC_DETECT_RANGE;
            if (dist > range) continue;
            // Sound detection: entity must be moving (speed > 0.0001) or making sound
            Vec3 vel = e.getDeltaMovement();
            double speed = vel.x * vel.x + vel.y * vel.y + vel.z * vel.z;
            if (speed < 0.0001 && e instanceof LivingEntity living && living.isShiftKeyDown()) {
                continue; // Stationary + sneaking = no sound
            }
            if (dist < bestDist) {
                bestDist = dist;
                bestTarget = e;
            }
        }
        return bestTarget;
    }

    /**
     * Turn towards the target with adaptive turn rate and obstacle avoidance.
     */
    private void turnTowardsTarget(Entity target, double distance) {
        Vec3 motion = getDeltaMovement();
        double hSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (hSpeed < 0.001) return;

        double currentYaw = Math.atan2(motion.z, motion.x);
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double targetYaw = Math.atan2(dz, dx);

        double deltaYaw = targetYaw - currentYaw;
        // Normalize to [-PI, PI]
        while (deltaYaw > Math.PI) deltaYaw -= 2 * Math.PI;
        while (deltaYaw < -Math.PI) deltaYaw += 2 * Math.PI;

        double maxTurnRad = Math.toRadians(getAdaptiveTurnRate(distance));

        // If target is directly behind (|deltaYaw| ≈ π), use intelligent turn direction selection
        if (Math.abs(Math.abs(deltaYaw) - Math.PI) < 0.1) {
            boolean leftClear = !hasObstacleInDirection(currentYaw - maxTurnRad, hSpeed);
            boolean rightClear = !hasObstacleInDirection(currentYaw + maxTurnRad, hSpeed);
            if (leftClear && !rightClear) {
                deltaYaw = -maxTurnRad;
            } else if (rightClear && !leftClear) {
                deltaYaw = maxTurnRad;
            } else {
                deltaYaw = (random.nextBoolean() ? 1 : -1) * maxTurnRad;
            }
        } else {
            if (deltaYaw > maxTurnRad) deltaYaw = maxTurnRad;
            if (deltaYaw < -maxTurnRad) deltaYaw = -maxTurnRad;
        }

        double newYaw = currentYaw + deltaYaw;
        setDeltaMovement(Math.cos(newYaw) * hSpeed, motion.y, Math.sin(newYaw) * hSpeed);
    }

    /**
     * Calculate adaptive turn rate based on distance to target.
     * Closer targets allow sharper turns for better tracking.
     */
    private float getAdaptiveTurnRate(double distanceToTarget) {
        if (distanceToTarget < CLOSE_RANGE) {
            return ACOUSTIC_MAX_TURN_DEG * CLOSE_TURN_MULTIPLIER;  // 4.5° for close range
        } else if (distanceToTarget < MID_RANGE) {
            return ACOUSTIC_MAX_TURN_DEG;  // 3.0° for mid range
        } else {
            return ACOUSTIC_MAX_TURN_DEG * FAR_TURN_MULTIPLIER;  // 1.8° for far range
        }
    }

    private boolean hasObstacleInDirection(double yaw, double distance) {
        Vec3 start = position();
        Vec3 end = start.add(Math.cos(yaw) * distance, 0, Math.sin(yaw) * distance);
        net.minecraft.world.phys.BlockHitResult hit = level().clip(
                new net.minecraft.world.level.ClipContext(start, end,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        return hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS;
    }

    private void magneticDetonate() {
        if (exploded) return;
        exploded = true;
        Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
        level().explode(this, getX(), getY(), getZ(), explosionRadius, interaction);
        discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (com.piranport.combat.FriendlyFireHelper.shouldBlockHit(target, getOwner())) return false;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide() && !exploded) {
            Entity target = result.getEntity();

            // Use the aircraft as the direct source if available, otherwise use this torpedo
            Entity directSource = sourceAircraft != null ? sourceAircraft : this;

            if (magnetic) {
                // 磁性鱼雷：HE式爆炸
                target.hurt(damageSources().explosion(directSource, getOwner()), damage);
                magneticDetonate();
            } else {
                target.hurt(damageSources().thrown(directSource, getOwner()), damage);
                // 附加进水 debuff（3秒，每秒 1 点魔法伤害）
                if (target instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(ModMobEffects.FLOODING, 60, 0));
                }
                discard();
            }

            // Explicitly set last hurt by mob to make hostile mobs aggressive
            if (target instanceof LivingEntity living) {
                Entity ownerEntity = getOwner();
                if (ownerEntity instanceof LivingEntity lo) {
                    living.setLastHurtByMob(lo);
                } else if (ownerEntity instanceof AircraftEntity ac && ac.getOwner() instanceof LivingEntity lo) {
                    living.setLastHurtByMob(lo);
                }
            }

            notifyOwner(target);
        }
    }

    public void setSourceAircraftName(Component name) {
        this.sourceAircraftName = name;
    }

    public void setSourceAircraft(Entity aircraft) {
        this.sourceAircraft = aircraft;
    }

    private void notifyOwner(Entity target) {
        Entity owner = getOwner();
        if (!(owner instanceof Player player)) return;
        Component weaponName = sourceAircraftName != null ? sourceAircraftName : getDefaultItem().getDescription();
        String key = target.isAlive() ? "message.piranport.weapon_hit" : "message.piranport.weapon_kill";
        com.piranport.combat.HitNotifier.send(player, Component.translatable(key, weaponName, target.getDisplayName()));
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        BlockState hitState = level().getBlockState(result.getBlockPos());
        if (isPassThroughForTorpedo(hitState)) {
            return;
        }

        super.onHitBlock(result);
        if (!level().isClientSide() && !exploded) {
            // 水中命中方块：静默消失，不爆炸
            if (hitState.getFluidState().is(Fluids.WATER)) {
                discard();
                return;
            }
            // 非水中命中方块：小范围爆炸
            if (magnetic) {
                magneticDetonate();
            } else {
                Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                        ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
                level().explode(this, getX(), getY(), getZ(), explosionRadius, interaction);
                discard();
            }
        }
    }

    private static boolean isPassThroughForTorpedo(BlockState state) {
        return state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0; // 重力由 tick() 中的自定义逻辑控制
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide() && wireGuided) {
            Entity owner = getOwner();
            if (owner instanceof net.minecraft.server.level.ServerPlayer sp) {
                java.util.UUID guided = com.piranport.combat.TorpedoGuidanceManager.getGuidedTorpedo(sp.getUUID());
                if (guided != null && guided.equals(getUUID())) {
                    com.piranport.combat.TorpedoGuidanceManager.endGuidance(sp);
                }
            }
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Caliber", caliber);
        tag.putFloat("Damage", damage);
        tag.putFloat("TorpedoSpeed", torpedoSpeed);
        tag.putInt("Lifetime", lifetime);
        tag.putFloat("ExplosionRadius", explosionRadius);
        tag.putBoolean("Magnetic", magnetic);
        tag.putBoolean("WireGuided", wireGuided);
        tag.putBoolean("Acoustic", acoustic);
        tag.putBoolean("AirDrop", airDrop);
        if (airDrop) {
            tag.putDouble("AirDropDirX", airDropDirection.x);
            tag.putDouble("AirDropDirZ", airDropDirection.z);
        }
        if (launchPos != null) {
            tag.putDouble("LaunchX", launchPos.x);
            tag.putDouble("LaunchY", launchPos.y);
            tag.putDouble("LaunchZ", launchPos.z);
        }
        if (sourceAircraftName != null) {
            tag.putString("SourceAircraftName",
                    Component.Serializer.toJson(sourceAircraftName, registryAccess()));
        }
        // Save target locking state (prefer live reference, fall back to persisted UUID)
        if (lockedTarget != null) {
            tag.putUUID("LockedTargetUUID", lockedTarget.getUUID());
        } else if (lockedTargetUuid != null) {
            tag.putUUID("LockedTargetUUID", lockedTargetUuid);
        }
        tag.putInt("LockDuration", lockDuration);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        caliber = tag.getInt("Caliber");
        damage = tag.getFloat("Damage");
        torpedoSpeed = tag.getFloat("TorpedoSpeed");
        if (torpedoSpeed <= 0) torpedoSpeed = 1.2f;
        lifetime = tag.getInt("Lifetime");
        if (lifetime <= 0) lifetime = 1200;
        explosionRadius = tag.getFloat("ExplosionRadius");
        if (explosionRadius <= 0) explosionRadius = 2.0f;
        magnetic = tag.getBoolean("Magnetic");
        wireGuided = tag.getBoolean("WireGuided");
        acoustic = tag.getBoolean("Acoustic");
        airDrop = tag.getBoolean("AirDrop");
        if (airDrop && tag.contains("AirDropDirX")) {
            airDropDirection = new Vec3(tag.getDouble("AirDropDirX"), 0, tag.getDouble("AirDropDirZ"));
        }
        if (tag.contains("LaunchX")) {
            launchPos = new Vec3(tag.getDouble("LaunchX"), tag.getDouble("LaunchY"), tag.getDouble("LaunchZ"));
        }
        if (tag.contains("SourceAircraftName")) {
            try {
                sourceAircraftName = Component.Serializer.fromJson(
                        tag.getString("SourceAircraftName"), registryAccess());
            } catch (Exception e) {
                com.piranport.PiranPort.LOGGER.warn("Failed to deserialize SourceAircraftName: {}", tag.getString("SourceAircraftName"), e);
                sourceAircraftName = null;
            }
        }
        // Load target locking state; entity is resolved in tick() when level is ready
        if (tag.hasUUID("LockedTargetUUID")) {
            this.lockedTargetUuid = tag.getUUID("LockedTargetUUID");
        }
        this.lockDuration = tag.getInt("LockDuration");
    }
}
