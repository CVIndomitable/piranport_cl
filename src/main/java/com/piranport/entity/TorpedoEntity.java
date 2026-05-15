package com.piranport.entity;

import com.piranport.combat.TorpedoGuidanceManager;
import com.piranport.config.ModCommonConfig;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class TorpedoEntity extends ThrowableItemProjectile {
    private int caliber = 533;
    private float damage = 18f;
    private float torpedoSpeed = 1.2f;
    private int lifetime = 1200;
    private float explosionRadius = 2.0f;
    private boolean magnetic = false;
    private boolean exploded = false;
    private Component sourceAircraftName;
    private Entity sourceAircraft;

    // 线导鱼雷状态
    private boolean wireGuided = false;
    private Vec3 launchPos = null;
    private static final double WIRE_FALLBACK_RANGE = 16.0;

    // 声导鱼雷状态
    private boolean acoustic = false;
    private static final double ACOUSTIC_DETECT_RANGE = 25.0;
    private static final double ACOUSTIC_SNEAK_RANGE = 10.0;
    private static final float ACOUSTIC_MAX_TURN_DEG = 3.0f;
    private static final int ACOUSTIC_ARM_TICKS = 10;
    private static final double CLOSE_RANGE = 5.0;
    private static final double MID_RANGE = 15.0;
    private static final float CLOSE_TURN_MULTIPLIER = 1.5f;
    private static final float FAR_TURN_MULTIPLIER = 0.6f;
    private Entity lockedTarget = null;
    private UUID lockedTargetUuid = null;
    private int lockDuration = 0;
    private static final int MIN_LOCK_DURATION = 40;
    private static final double LOCK_BREAK_DISTANCE = 30.0;
    private static final double TARGET_SWITCH_THRESHOLD = 0.7;

    // 空投下落阶段
    private boolean airDrop = false;
    private Vec3 airDropDirection = Vec3.ZERO;

    // 磁性近炸
    private static final double MAGNETIC_DETONATE_DIST = 3.0;
    private static final int MAGNETIC_ARM_TICKS = 5;

    public TorpedoEntity(EntityType<? extends TorpedoEntity> type, Level level) {
        super(type, level);
    }

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

    public float getTorpedoSpeed() { return torpedoSpeed; }

    public void setDamage(float damage) { this.damage = damage; }

    public void setSpeed(float speed) { this.torpedoSpeed = speed; }

    public void setLifetime(int lifetime) { this.lifetime = lifetime; }

    public void setMagnetic(boolean magnetic) { this.magnetic = magnetic; }

    public void setWireGuided(boolean wireGuided) {
        this.wireGuided = wireGuided;
    }

    public boolean isWireGuided() { return wireGuided; }

    public void setAirDrop(boolean airDrop, Vec3 direction) {
        this.airDrop = airDrop;
        this.airDropDirection = new Vec3(direction.x, 0, direction.z).normalize();
    }

    public void setAcoustic(boolean acoustic) {
        this.acoustic = acoustic;
        if (acoustic) this.torpedoSpeed = 0.7f;
    }

    public void cutWire() { wireGuided = false; }

    /** 线长等于服务器模拟距离，超出时断开 */
    private double getWireMaxRange() {
        if (level().getServer() != null) {
            int simChunks = level().getServer().getPlayerList().getSimulationDistance();
            if (simChunks > 0) return simChunks * 16.0;
        }
        return WIRE_FALLBACK_RANGE;
    }

    /** 玩家制导：跟随最新方向输入，限制不得出水面 */
    private boolean applyGuidedMovement() {
        Entity owner = getOwner();
        if (!(owner instanceof ServerPlayer sp)) return false;
        UUID guided = TorpedoGuidanceManager.getGuidedTorpedo(sp.getUUID());
        if (guided == null || !guided.equals(getUUID())) return false;

        float[] input = TorpedoGuidanceManager.consumeInput(sp.getUUID());
        Vec3 dir;
        if (input != null && (input[0] != 0 || input[1] != 0 || input[2] != 0)) {
            double len = Math.sqrt(input[0] * input[0] + input[1] * input[1] + input[2] * input[2]);
            if (len < 0.001) dir = headingFromMotion();
            else dir = new Vec3(input[0] / len, input[1] / len, input[2] / len);
        } else {
            dir = headingFromMotion();
        }

        BlockPos above = BlockPos.containing(getX(), getY() + 0.5, getZ());
        boolean waterAbove = level().getBlockState(above).getFluidState().is(Fluids.WATER);
        double vy = dir.y;
        if (!waterAbove && vy > 0) vy = 0;

        double speed = torpedoSpeed;
        setDeltaMovement(dir.x * speed, vy * speed, dir.z * speed);
        return true;
    }

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

    // ==================== tick 方法 ====================

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;

        resolveLockedTarget();
        if (tickLifetime()) return;
        if (tickMagneticProximity()) return;
        if (tickProximityHit()) return;
        if (tickWireGuidance()) return;
        tickAcousticHoming();
        if (tickAirDrop()) return;
        tickSurfaceAI();
    }

    /** 从 UUID 恢复锁定目标（跨区块加载后） */
    private void resolveLockedTarget() {
        if (lockedTarget == null && lockedTargetUuid != null && level() instanceof ServerLevel sl) {
            Entity entity = sl.getEntity(lockedTargetUuid);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                lockedTarget = living;
                lockedTargetUuid = null;
            } else {
                lockedTargetUuid = null;
                lockDuration = 0;
            }
        }
    }

    /** 剩余航程检查：超时爆炸 */
    private boolean tickLifetime() {
        if (--lifetime > 0) return false;
        if (!level().isClientSide() && !exploded) {
            exploded = true;
            explodeCurrentPos();
        }
        discard();
        return true;
    }

    /** 磁性近炸检测 */
    private boolean tickMagneticProximity() {
        if (level().isClientSide() || !magnetic || tickCount <= MAGNETIC_ARM_TICKS) return false;
        checkMagneticProximity();
        return exploded;
    }

    /** 水面巡航阶段主动近炸检测（扁平 hitbox 易滑过目标） */
    private boolean tickProximityHit() {
        if (level().isClientSide() || exploded || airDrop || tickCount <= 2 || isRemoved()) return false;
        Entity nearby = findProximityHitTarget();
        if (nearby != null) {
            onHitEntity(new EntityHitResult(nearby));
            return true;
        }
        return false;
    }

    /** 线导：断线检测 + 玩家制导 */
    private boolean tickWireGuidance() {
        if (level().isClientSide()) return false;
        if (wireGuided && launchPos == null) {
            launchPos = position();
        }
        if (wireGuided) {
            double maxRange = getWireMaxRange();
            Entity owner = getOwner();
            double dist = owner != null ? position().distanceTo(owner.position())
                    : (launchPos != null ? position().distanceTo(launchPos) : 0.0);
            if (dist > maxRange) {
                wireGuided = false;
                if (owner instanceof ServerPlayer sp) {
                    TorpedoGuidanceManager.endGuidance(sp);
                    sp.displayClientMessage(
                            Component.translatable("message.piranport.torpedo_wire_lost"), true);
                }
            }
        }
        if (wireGuided && applyGuidedMovement()) return true;
        return false;
    }

    /** 声导追踪 */
    private void tickAcousticHoming() {
        if (!level().isClientSide() && acoustic && tickCount > ACOUSTIC_ARM_TICKS) {
            acousticHoming();
        }
    }

    /** 空投下落阶段：垂直入水后切换巡航 */
    private boolean tickAirDrop() {
        if (!airDrop) return false;
        Vec3 motion = getDeltaMovement();
        BlockPos pos = blockPosition();
        boolean inWater = level().getBlockState(pos).getFluidState().is(Fluids.WATER);
        boolean inAir = level().getBlockState(pos).isAir();
        boolean waterBelow = level().getBlockState(pos.below()).getFluidState().is(Fluids.WATER);

        if (inWater || (inAir && waterBelow)) {
            airDrop = false;
            setDeltaMovement(airDropDirection.x * torpedoSpeed, 0, airDropDirection.z * torpedoSpeed);
        } else {
            setDeltaMovement(motion.x * 0.98, motion.y - 0.08, motion.z * 0.98);
        }
        return true;
    }

    /** 水面贴合 AI */
    private void tickSurfaceAI() {
        Vec3 motion = getDeltaMovement();
        BlockPos pos = blockPosition();
        boolean inAir = level().getBlockState(pos).isAir();
        boolean waterBelow = level().getBlockState(pos.below()).getFluidState().is(Fluids.WATER);
        boolean inWater = level().getBlockState(pos).getFluidState().is(Fluids.WATER);

        if (inAir && waterBelow) {
            double currentH = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            if (currentH > 0.001) {
                double dirX = motion.x / currentH;
                double dirZ = motion.z / currentH;
                setDeltaMovement(dirX * torpedoSpeed, 0, dirZ * torpedoSpeed);
            } else {
                setDeltaMovement(motion.x, 0, motion.z);
            }
        } else if (inWater) {
            double currentH = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            if (currentH > 0.001) {
                double dirX = motion.x / currentH;
                double dirZ = motion.z / currentH;
                setDeltaMovement(dirX * torpedoSpeed, motion.y + 0.04, dirZ * torpedoSpeed);
            } else {
                setDeltaMovement(motion.x, motion.y + 0.04, motion.z);
            }
        } else {
            setDeltaMovement(motion.x * 0.70, motion.y - 0.25, motion.z * 0.70);
        }
    }

    // ==================== 辅助方法 ====================

    /** 在当前位置产生爆炸 */
    private void explodeCurrentPos() {
        Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
        level().explode(this, getX(), getY(), getZ(), explosionRadius, interaction);
    }

    /** 扫描鱼雷周围的有效命中目标 */
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
            if (entity.distanceTo(this) <= MAGNETIC_DETONATE_DIST) {
                magneticDetonate();
                return;
            }
        }
    }

    /** 声导追踪：向最近的有声目标转向 */
    private void acousticHoming() {
        if (lockedTarget != null) {
            if (!lockedTarget.isAlive() || lockedTarget.isRemoved()) {
                lockedTarget = null;
                lockDuration = 0;
            } else {
                double dist = distanceTo(lockedTarget);
                if (dist > LOCK_BREAK_DISTANCE) {
                    lockedTarget = null;
                    lockDuration = 0;
                } else if (lockDuration < MIN_LOCK_DURATION) {
                    lockDuration++;
                    turnTowardsTarget(lockedTarget, dist);
                    return;
                }
            }
        }

        Entity bestTarget = scanForTarget();
        if (bestTarget != null) {
            double newDist = distanceTo(bestTarget);
            if (lockedTarget == null) {
                lockedTarget = bestTarget;
                lockDuration = 0;
            } else {
                double currentDist = distanceTo(lockedTarget);
                if (newDist < currentDist * TARGET_SWITCH_THRESHOLD) {
                    lockedTarget = bestTarget;
                    lockDuration = 0;
                }
            }
        }

        if (lockedTarget != null) {
            turnTowardsTarget(lockedTarget, distanceTo(lockedTarget));
        }
    }

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
            double range = (e instanceof LivingEntity living && living.isShiftKeyDown())
                    ? ACOUSTIC_SNEAK_RANGE : ACOUSTIC_DETECT_RANGE;
            if (dist > range) continue;
            Vec3 vel = e.getDeltaMovement();
            double speed = vel.x * vel.x + vel.y * vel.y + vel.z * vel.z;
            if (speed < 0.0001 && e instanceof LivingEntity living && living.isShiftKeyDown()) continue;
            if (dist < bestDist) {
                bestDist = dist;
                bestTarget = e;
            }
        }
        return bestTarget;
    }

    private void turnTowardsTarget(Entity target, double distance) {
        Vec3 motion = getDeltaMovement();
        double hSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (hSpeed < 0.001) return;

        double currentYaw = Math.atan2(motion.z, motion.x);
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double targetYaw = Math.atan2(dz, dx);

        double deltaYaw = targetYaw - currentYaw;
        while (deltaYaw > Math.PI) deltaYaw -= 2 * Math.PI;
        while (deltaYaw < -Math.PI) deltaYaw += 2 * Math.PI;

        double maxTurnRad = Math.toRadians(getAdaptiveTurnRate(distance));

        if (Math.abs(Math.abs(deltaYaw) - Math.PI) < 0.1) {
            boolean leftClear = !hasObstacleInDirection(currentYaw - maxTurnRad, hSpeed);
            boolean rightClear = !hasObstacleInDirection(currentYaw + maxTurnRad, hSpeed);
            if (leftClear && !rightClear) deltaYaw = -maxTurnRad;
            else if (rightClear && !leftClear) deltaYaw = maxTurnRad;
            else deltaYaw = (random.nextBoolean() ? 1 : -1) * maxTurnRad;
        } else {
            if (deltaYaw > maxTurnRad) deltaYaw = maxTurnRad;
            if (deltaYaw < -maxTurnRad) deltaYaw = -maxTurnRad;
        }

        double newYaw = currentYaw + deltaYaw;
        setDeltaMovement(Math.cos(newYaw) * hSpeed, motion.y, Math.sin(newYaw) * hSpeed);
    }

    private float getAdaptiveTurnRate(double distanceToTarget) {
        if (distanceToTarget < CLOSE_RANGE) return ACOUSTIC_MAX_TURN_DEG * CLOSE_TURN_MULTIPLIER;
        else if (distanceToTarget < MID_RANGE) return ACOUSTIC_MAX_TURN_DEG;
        else return ACOUSTIC_MAX_TURN_DEG * FAR_TURN_MULTIPLIER;
    }

    private boolean hasObstacleInDirection(double yaw, double distance) {
        Vec3 start = position();
        Vec3 end = start.add(Math.cos(yaw) * distance, 0, Math.sin(yaw) * distance);
        BlockHitResult hit = level().clip(
                new net.minecraft.world.level.ClipContext(start, end,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        return hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS;
    }

    private void magneticDetonate() {
        if (exploded) return;
        exploded = true;
        explodeCurrentPos();
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
            Entity directSource = sourceAircraft != null ? sourceAircraft : this;

            if (magnetic) {
                target.hurt(damageSources().explosion(directSource, getOwner()), damage);
                magneticDetonate();
            } else {
                target.hurt(damageSources().thrown(directSource, getOwner()), damage);
                if (target instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(ModMobEffects.FLOODING, 60, 0));
                }
                discard();
            }

            if (target instanceof LivingEntity living) {
                Entity ownerEntity = getOwner();
                if (ownerEntity instanceof LivingEntity lo) {
                    living.setLastHurtByMob(lo);
                } else if (ownerEntity instanceof com.piranport.entity.AircraftEntity ac && ac.getOwner() instanceof LivingEntity lo) {
                    living.setLastHurtByMob(lo);
                }
            }

            notifyOwner(target);
        }
    }

    public void setSourceAircraftName(Component name) { this.sourceAircraftName = name; }

    public void setSourceAircraft(Entity aircraft) { this.sourceAircraft = aircraft; }

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
        if (isPassThroughForTorpedo(hitState)) return;

        super.onHitBlock(result);
        if (!level().isClientSide() && !exploded) {
            if (hitState.getFluidState().is(Fluids.WATER)) {
                discard();
                return;
            }
            if (magnetic) {
                magneticDetonate();
            } else {
                explodeCurrentPos();
                discard();
            }
        }
    }

    private static boolean isPassThroughForTorpedo(BlockState state) {
        return state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT);
    }

    @Override
    protected double getDefaultGravity() { return 0.0; }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide() && wireGuided) {
            Entity owner = getOwner();
            if (owner instanceof ServerPlayer sp) {
                UUID guided = TorpedoGuidanceManager.getGuidedTorpedo(sp.getUUID());
                if (guided != null && guided.equals(getUUID())) {
                    TorpedoGuidanceManager.endGuidance(sp);
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
        if (tag.hasUUID("LockedTargetUUID")) {
            this.lockedTargetUuid = tag.getUUID("LockedTargetUUID");
        }
        this.lockDuration = tag.getInt("LockDuration");
    }
}
