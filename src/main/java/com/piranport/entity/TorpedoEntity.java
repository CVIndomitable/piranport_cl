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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class TorpedoEntity extends ThrowableItemProjectile {
    private int caliber = 533;
    private float damage = 18f;
    private float torpedoSpeed = 1.2f;
    private int lifetime = 1200; // 1 minute hard cap
    private float explosionRadius = 2.0f;
    private boolean magnetic = false;
    private boolean exploded = false;
    private Component sourceAircraftName;

    /** Wire-guided torpedo state. */
    private boolean wireGuided = false;
    private Vec3 launchPos = null;
    /** Maximum wire control distance in blocks. */
    private static final double WIRE_MAX_RANGE = 16.0;
    /** Yaw adjustment per steer input (degrees). */
    private static final float WIRE_STEER_DEGREES = 8.0f;

    /** Acoustic homing torpedo state. */
    private boolean acoustic = false;
    /** Detection range for moving entities (blocks). */
    private static final double ACOUSTIC_DETECT_RANGE = 25.0;
    /** Reduced detection range for sneaking entities (blocks). */
    private static final double ACOUSTIC_SNEAK_RANGE = 10.0;
    /** Maximum turn rate per tick (degrees). */
    private static final float ACOUSTIC_MAX_TURN_DEG = 8.0f;
    /** Grace period before acoustic homing activates (ticks). */
    private static final int ACOUSTIC_ARM_TICKS = 10;

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
            this.torpedoSpeed = 1.2f;
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
            this.torpedoSpeed = 1.0f;
        }
    }

    /**
     * Steer the torpedo left or right by adjusting its horizontal velocity direction.
     * @param direction -1 for left, +1 for right
     */
    public void steer(int direction) {
        if (!wireGuided) return;
        Vec3 motion = getDeltaMovement();
        double angleRad = Math.toRadians(WIRE_STEER_DEGREES * direction);
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double newX = motion.x * cos - motion.z * sin;
        double newZ = motion.x * sin + motion.z * cos;
        setDeltaMovement(newX, motion.y, newZ);
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

        // 1.6. 线导鱼雷掉线检测 — launchPos lazily initialized after setPos
        if (!level().isClientSide() && wireGuided && launchPos == null) {
            launchPos = position();
        }
        if (!level().isClientSide() && wireGuided && launchPos != null) {
            double dist = position().distanceTo(launchPos);
            if (dist > WIRE_MAX_RANGE) {
                wireGuided = false;
                Entity owner = getOwner();
                if (owner instanceof Player player) {
                    player.displayClientMessage(
                            Component.translatable("message.piranport.torpedo_wire_lost"), true);
                }
            }
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
            // 水面航行：恢复水平速度，清除垂直速度
            double currentH = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            if (currentH > 0.001) {
                double scale = torpedoSpeed / currentH;
                setDeltaMovement(motion.x * scale, 0, motion.z * scale);
            } else {
                setDeltaMovement(motion.x, 0, motion.z);
            }
        } else if (inWater) {
            // 在水中：恢复水平速度并向上浮
            double currentH = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            if (currentH > 0.001) {
                double scale = torpedoSpeed / currentH;
                setDeltaMovement(motion.x * scale, motion.y + 0.04, motion.z * scale);
            } else {
                setDeltaMovement(motion.x, motion.y + 0.04, motion.z);
            }
        } else {
            // 空中自由下落：水平急速衰减 + 强重力，让鱼雷在 ~3 格内入水
            setDeltaMovement(motion.x * 0.70, motion.y - 0.25, motion.z * 0.70);
        }
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
        AABB searchBox = getBoundingBox().inflate(ACOUSTIC_DETECT_RANGE);
        Entity bestTarget = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity e : level().getEntities(this, searchBox, e -> {
            if (e == getOwner()) return false;
            if (com.piranport.combat.FriendlyFireHelper.shouldBlockHit(e, getOwner())) return false;
            return e.isAlive() && e.isPickable() && e instanceof LivingEntity;
        })) {
            double dist = distanceTo(e);
            // 潜行实体缩减检测范围
            double range = (e instanceof LivingEntity living && living.isShiftKeyDown())
                    ? ACOUSTIC_SNEAK_RANGE : ACOUSTIC_DETECT_RANGE;
            if (dist > range) continue;
            // 声音检测：实体必须在移动（速度 > 0.01）或有声音事件
            Vec3 vel = e.getDeltaMovement();
            double speed = vel.x * vel.x + vel.y * vel.y + vel.z * vel.z;
            if (speed < 0.0001 && e instanceof LivingEntity living && living.isShiftKeyDown()) {
                continue; // 静止+潜行 = 不产生声音
            }
            if (dist < bestDist) {
                bestDist = dist;
                bestTarget = e;
            }
        }

        if (bestTarget == null) return;

        // 计算转向：限制最大转角
        Vec3 motion = getDeltaMovement();
        double hSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (hSpeed < 0.001) return;

        double currentYaw = Math.atan2(motion.z, motion.x);
        double dx = bestTarget.getX() - getX();
        double dz = bestTarget.getZ() - getZ();
        double targetYaw = Math.atan2(dz, dx);

        double deltaYaw = targetYaw - currentYaw;
        // 归一化到 [-PI, PI]
        while (deltaYaw > Math.PI) deltaYaw -= 2 * Math.PI;
        while (deltaYaw < -Math.PI) deltaYaw += 2 * Math.PI;

        double maxTurnRad = Math.toRadians(ACOUSTIC_MAX_TURN_DEG);
        if (deltaYaw > maxTurnRad) deltaYaw = maxTurnRad;
        if (deltaYaw < -maxTurnRad) deltaYaw = -maxTurnRad;

        double newYaw = currentYaw + deltaYaw;
        setDeltaMovement(Math.cos(newYaw) * hSpeed, motion.y, Math.sin(newYaw) * hSpeed);
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
            if (magnetic) {
                // 磁性鱼雷：HE式爆炸
                target.hurt(damageSources().explosion(this, getOwner()), damage);
                magneticDetonate();
            } else {
                target.hurt(damageSources().thrown(this, getOwner()), damage);
                // 附加进水 debuff（3秒，每秒 1 点魔法伤害）
                if (target instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(ModMobEffects.FLOODING, 60, 0));
                }
                discard();
            }
            notifyOwner(target);
        }
    }

    public void setSourceAircraftName(Component name) {
        this.sourceAircraftName = name;
    }

    private void notifyOwner(Entity target) {
        Entity owner = getOwner();
        if (!(owner instanceof Player player)) return;
        Component weaponName = sourceAircraftName != null ? sourceAircraftName : getDefaultItem().getDescription();
        String key = target.isAlive() ? "message.piranport.weapon_hit" : "message.piranport.weapon_kill";
        player.sendSystemMessage(Component.translatable(key, weaponName, target.getDisplayName()));
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide() && !exploded) {
            // 水中命中方块：静默消失，不爆炸
            if (level().getBlockState(result.getBlockPos()).getFluidState().is(Fluids.WATER)) {
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

    @Override
    protected double getDefaultGravity() {
        return 0.0; // 重力由 tick() 中的自定义逻辑控制
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
                sourceAircraftName = null;
            }
        }
    }
}
