package com.piranport.entity;

import com.piranport.combat.TorpedoGuidanceManager;
import com.piranport.config.ModCommonConfig;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.platform.ClientHooks;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class TorpedoEntity extends ThrowableItemProjectile {

    // ===== 同步字段（方案2）：客户端要跑同一套运动 AI，必须知道航速与模式标志 =====
    // 这些标志原本是服务端私有字段，客户端实体走默认构造器（速度 1.0、标志全 false），
    // 声导/氧气/空投等改航速时两边速度不一致，客户端每 tick 被服务端硬校正 → 抖动主因。
    private static final EntityDataAccessor<Float> DATA_SPEED =
            SynchedEntityData.defineId(TorpedoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_MAGNETIC =
            SynchedEntityData.defineId(TorpedoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_WIRE_GUIDED =
            SynchedEntityData.defineId(TorpedoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ACOUSTIC =
            SynchedEntityData.defineId(TorpedoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_OXYGEN =
            SynchedEntityData.defineId(TorpedoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_AIR_DROP =
            SynchedEntityData.defineId(TorpedoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_AIR_DROP_DIR_X =
            SynchedEntityData.defineId(TorpedoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_AIR_DROP_DIR_Z =
            SynchedEntityData.defineId(TorpedoEntity.class, EntityDataSerializers.FLOAT);

    // ===== 方案1：客户端位置插值（照抄 CannonProjectileEntity 的成熟写法）=====
    // 原版 Entity.lerpTo 直接 setPos，导致 xo==x、partial-tick 插值失效 → 抖动。
    private int clientLerpSteps;
    private double clientLerpX, clientLerpY, clientLerpZ;

    // ===== 水下贴水面（用户口径 2026-10-05）=====
    /** 水面搜索的哨兵值：列内窗口没找到水。 */
    private static final int NO_WATER = Integer.MIN_VALUE;
    /** 原版静止水面顶面相对所在方块底边的高度（物理恒等量，非手感参数）。 */
    private static final double STILL_WATER_SURFACE_HEIGHT = 0.875;
    /** 空中下坠的水平衰减／垂直加速度，沿用原实现（未改动的手感参数）。 */
    private static final double AIR_FALL_HORIZONTAL_DECAY = 0.70;
    private static final double AIR_FALL_VERTICAL_ACCEL = 0.25;
    /**
     * 线导垂直输入死区：视线 y 分量绝对值小于该值时视为"没有垂直输入"，
     * 由 surface_* 参数接管深度收敛。避免玩家视线略微俯仰就把深度交给输入、破坏贴水面手感。
     */
    private static final double WIRE_VERTICAL_DEADZONE = 0.05;

    private int caliber = 533;
    private float damage = 18f;
    private float torpedoSpeed = 1.0f;
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

    // Phase 27：策划 §3.3 氧气鱼雷状态（无可见航迹 + 高速）
    private boolean oxygen = false;
    private static final double MID_RANGE = 15.0;
    private static final float CLOSE_TURN_MULTIPLIER = 1.5f;
    private static final float FAR_TURN_MULTIPLIER = 0.6f;
    private Entity lockedTarget = null;
    private UUID lockedTargetUuid = null;
    private int lockDuration = 0;
    private static final int MIN_LOCK_DURATION = 60;
    private static final double LOCK_BREAK_DISTANCE = 30.0;
    /** 声导鱼雷目标切换阈值：新目标距离需小于当前目标的此比例才切换 */
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
            this.lifetime = 1200;
            this.explosionRadius = 2.5f;
        } else {
            this.damage = 18f;
            this.lifetime = 1200;
            this.explosionRadius = 2.0f;
        }
        // 走 setter 而非直接赋字段：同步字段要同时写进 entityData，随生成包下发给客户端。
        setSpeed(0.9f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPEED, 1.0f);
        builder.define(DATA_MAGNETIC, false);
        builder.define(DATA_WIRE_GUIDED, false);
        builder.define(DATA_ACOUSTIC, false);
        builder.define(DATA_OXYGEN, false);
        builder.define(DATA_AIR_DROP, false);
        builder.define(DATA_AIR_DROP_DIR_X, 0.0f);
        builder.define(DATA_AIR_DROP_DIR_Z, 0.0f);
    }

    public float getTorpedoSpeed() { return torpedoSpeed; }

    public void setDamage(float damage) { this.damage = damage; }

    public void setSpeed(float speed) {
        this.torpedoSpeed = speed;
        entityData.set(DATA_SPEED, speed);
    }

    public void setLifetime(int lifetime) { this.lifetime = lifetime; }

    public void setMagnetic(boolean magnetic) {
        this.magnetic = magnetic;
        entityData.set(DATA_MAGNETIC, magnetic);
    }

    public void setWireGuided(boolean wireGuided) {
        this.wireGuided = wireGuided;
        entityData.set(DATA_WIRE_GUIDED, wireGuided);
        if (wireGuided) wireMaxRange = -1; // 重置缓存，下次使用时重新读取模拟距离
    }

    public boolean isWireGuided() { return wireGuided; }

    public void setAirDrop(boolean airDrop, Vec3 direction) {
        this.airDrop = airDrop;
        Vec3 horizontal = new Vec3(direction.x, 0, direction.z);
        double horizontalLen = horizontal.length();
        if (horizontalLen < 0.001) {
            this.airDropDirection = new Vec3(0, 0, 1);
        } else {
            this.airDropDirection = horizontal.normalize();
        }
        // 方向也要同步：客户端会跑同一个空投分支，方向不同会让入水瞬间被服务端拽回。
        entityData.set(DATA_AIR_DROP, airDrop);
        entityData.set(DATA_AIR_DROP_DIR_X, (float) this.airDropDirection.x);
        entityData.set(DATA_AIR_DROP_DIR_Z, (float) this.airDropDirection.z);
    }

    public void setAcoustic(boolean acoustic) {
        this.acoustic = acoustic;
        entityData.set(DATA_ACOUSTIC, acoustic);
        if (acoustic) setSpeed(0.7f);
    }

    /** Phase 27：策划 §3.3 氧气鱼雷 — 高速且无可见航迹 */
    public void setOxygen(boolean oxygen) {
        this.oxygen = oxygen;
        entityData.set(DATA_OXYGEN, oxygen);
        if (oxygen) {
            // 氧气推进：航速 +30% (0.9 → 1.17 blocks/tick)
            setSpeed(this.torpedoSpeed * 1.3f);
        }
    }

    public boolean isOxygen() { return oxygen; }

    public void cutWire() { setWireGuided(false); }

    /** 线长等于服务器模拟距离，超出时断开（缓存值，避免每tick访问Server→PlayerList） */
    private double wireMaxRange = -1;

    private double getWireMaxRange() {
        if (wireMaxRange < 0 && level().getServer() != null) {
            int simChunks = level().getServer().getPlayerList().getSimulationDistance();
            wireMaxRange = simChunks > 0 ? simChunks * 16.0 : WIRE_FALLBACK_RANGE;
        }
        return wireMaxRange > 0 ? wireMaxRange : WIRE_FALLBACK_RANGE;
    }

    /**
     * 玩家制导：跟随玩家视线方向，并按水面参数维持"水面之下贴水面"的深度。
     *
     * <p><b>方案4（2026-10-05）：线导期间客户端权威</b>。两侧都跑本方法：
     * <ul>
     *   <li>客户端：本地玩家正在引导本鱼雷时，用 {@code mc.player} 的视线直接写 deltaMovement，
     *       不再等 2 tick 一次的服务端输入回包。配合 {@link #isControlledByLocalInstance()}
     *       让原版跳过服务端位置校正，玩家看到的就是本地预测的真实位置。</li>
     *   <li>服务端：仍按上传的输入包驱动一份权威副本（供命中/爆炸判定），并对断线、超程、
     *       寿命等做兜底；它不是渲染来源，但必须与客户端保持一致以免命中分叉。</li>
     * </ul>
     *
     * <p>垂直方向：玩家有垂直输入时以输入为准（仅在会冲出水面时截断为不上浮）；
     * 无垂直输入时复用 {@code global.torpedo.surface_*} 参数向目标深度收敛，
     * 因此线导鱼雷也会从发射深度下潜到水面之下。
     */
    private boolean applyGuidedMovement() {
        // ---- 1) 取方向输入：客户端看本地视线，服务端看上传包 ----
        double ix, iy, iz;
        boolean hasInput;
        if (level().isClientSide()) {
            Player local = ClientHooks.getClientPlayer();
            if (local == null || !ClientHooks.isGuidingTorpedo(getId())) return false;
            Vec3 look = local.getLookAngle();
            ix = look.x;
            iy = look.y;
            iz = look.z;
            hasInput = true;
        } else {
            Entity owner = getOwner();
            if (!(owner instanceof ServerPlayer sp)) return false;
            UUID guided = TorpedoGuidanceManager.getGuidedTorpedo(sp.getUUID());
            if (guided == null || !guided.equals(getUUID())) return false;
            // 先消费新包；没有新包的 tick 复用上一包，避免服务端垂直解算与客户端逐 tick 预测交替分叉。
            float[] input = TorpedoGuidanceManager.consumeInput(sp.getUUID());
            if (input == null) input = TorpedoGuidanceManager.getLastInput(sp.getUUID());
            hasInput = input != null && (input[0] != 0 || input[1] != 0 || input[2] != 0);
            if (hasInput) {
                ix = input[0];
                iy = input[1];
                iz = input[2];
            } else {
                Vec3 h = headingFromMotion();
                ix = h.x;
                iy = 0;
                iz = h.z;
            }
        }

        // ---- 2) 水平方向归一化，垂直单独解算 ----
        double hLen = Math.sqrt(ix * ix + iz * iz);
        double dirX, dirZ;
        if (hLen < 0.001) {
            Vec3 h = headingFromMotion();
            dirX = h.x;
            dirZ = h.z;
        } else {
            dirX = ix / hLen;
            dirZ = iz / hLen;
        }

        Vec3 motion = getDeltaMovement();
        double vy;
        double horizSpeed = torpedoSpeed;
        if (hasInput && Math.abs(iy) > WIRE_VERTICAL_DEADZONE) {
            // 玩家垂直输入优先：按完整视线归一化，水平随俯仰角缩短，总速不超过鱼雷航速
            // （与旧实现一致；若只给水平满速+垂直输入，俯冲时总速会超标到 √2 倍）。
            double len = Math.sqrt(ix * ix + iy * iy + iz * iz);
            if (len < 0.001) {
                vy = 0;
            } else {
                horizSpeed = torpedoSpeed * (hLen / len);
                vy = torpedoSpeed * (iy / len);
            }
        } else {
            vy = solveSurfaceVy();
            if (Double.isNaN(vy)) {
                // 无水面 / 在水面之上且未进入捕获窗口：垂直沿用原空中下坠手感，水平保持玩家朝向
                setDeltaMovement(dirX * torpedoSpeed,
                        motion.y - AIR_FALL_VERTICAL_ACCEL,
                        dirZ * torpedoSpeed);
                return true;
            }
        }

        // 不得冲出水面：上方无液体时禁止上浮（与旧逻辑一致）
        if (vy > 0 && !hasWaterAbove()) vy = 0;

        setDeltaMovement(dirX * horizSpeed, vy, dirZ * horizSpeed);
        return true;
    }

    /** 鱼雷正上方 0.5 格处是否为液体；用于阻止鱼雷冲出水面。 */
    private boolean hasWaterAbove() {
        BlockPos above = BlockPos.containing(getX(), getY() + 0.5, getZ());
        return level().getBlockState(above).getFluidState().is(Fluids.WATER);
    }

    private Vec3 headingFromMotion() {
        Vec3 m = getDeltaMovement();
        double len = m.length();
        if (len < 0.001) return new Vec3(0, 0, 1);
        return m.scale(1.0 / len);
    }

    // ==================== 客户端插值（方案1） ====================

    /**
     * 客户端位置插值：只存目标位置，在 tick() 里按步数指数衰减逼近。
     * 原版 Entity.lerpTo 直接 setPos，导致 xo/yo/zo 与 x/y/z 相同、partial-tick 插值失效，
     * 服务端每 tick 一同步就把客户端模拟位置拽回 → 抖动。旋转不插值：鱼雷朝向由速度矢量
     * 决定（TorpedoRenderer），且线导时客户端用玩家视角直接写 yRot/xRot，插值会与之打架。
     */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (level().isClientSide()) {
            this.clientLerpSteps = steps + 2;
            this.clientLerpX = x;
            this.clientLerpY = y;
            this.clientLerpZ = z;
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, steps);
    }

    @Override public double lerpTargetX() { return clientLerpSteps > 0 ? clientLerpX : getX(); }
    @Override public double lerpTargetY() { return clientLerpSteps > 0 ? clientLerpY : getY(); }
    @Override public double lerpTargetZ() { return clientLerpSteps > 0 ? clientLerpZ : getZ(); }

    /**
     * 方案4：线导期间本地玩家驾驶时忽略服务端速度包。
     *
     * <p>原版 {@code ClientPacketListener.handleSetEntityMotion} 直接调用 {@code entity.lerpMotion}，
     * 不走 {@code isControlledByLocalInstance} 判定；NeoForge 的 {@code EntityType.defaultTrackDeltasSupplier}
     * 对鱼雷返回 true，服务端每 tick 都会广播速度包。若不忽略，服务端速度会在实体 tick 前覆盖本地预测
     * （{@code ThrowableProjectile.tick} 先按当前 deltaMovement 移动），位置仍被悄悄拽向服务端。
     */
    @Override
    public void lerpMotion(double x, double y, double z) {
        if (level().isClientSide()
                && entityData.get(DATA_WIRE_GUIDED)
                && ClientHooks.isGuidingTorpedo(getId())) {
            return;
        }
        super.lerpMotion(x, y, z);
    }

    /**
     * 方案4：线导期间客户端权威，跳过原版服务端位置/传送校正。
     *
     * <p>原版 {@code Entity.isControlledByLocalInstance()}（1.21.1 {@code Entity.java:3215}）对非载具实体
     * 返回 {@code isEffectiveAi()}，即客户端恒 false、服务端恒 true。客户端因此会在
     * {@code ClientPacketListener.handleMoveEntity}（{@code ClientPacketListener.java:625}）与
     * {@code handleTeleportEntity}（{@code :578}）里用服务端位置调用 {@code lerpTo}，把本地预测硬拽回去。
     * 线导时本地玩家就是驾驶者，位置以本地预测为准，故对"本地玩家正在引导的线导鱼雷"返回 true。
     *
     * <p>注意只影响线导中的本地鱼雷：其它玩家/无人鱼雷仍按原版接受校正。
     */
    @Override
    public boolean isControlledByLocalInstance() {
        if (level().isClientSide()
                && entityData.get(DATA_WIRE_GUIDED)
                && ClientHooks.isGuidingTorpedo(getId())) {
            return true;
        }
        return super.isControlledByLocalInstance();
    }

    /** 客户端每 tick 从同步数据刷新本地状态，保证与服务端跑同一套运动 AI。 */
    private void syncClientState() {
        this.torpedoSpeed = entityData.get(DATA_SPEED);
        this.magnetic = entityData.get(DATA_MAGNETIC);
        this.wireGuided = entityData.get(DATA_WIRE_GUIDED);
        this.acoustic = entityData.get(DATA_ACOUSTIC);
        this.oxygen = entityData.get(DATA_OXYGEN);
        this.airDrop = entityData.get(DATA_AIR_DROP);
        if (this.airDrop) {
            this.airDropDirection = new Vec3(
                    entityData.get(DATA_AIR_DROP_DIR_X), 0, entityData.get(DATA_AIR_DROP_DIR_Z));
        }
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

    /**
     * Tick 调用链（短路执行，首次匹配成功的模式接管本 tick）：
     *   resolveLockedTarget → tickLifetime → tickMagneticProximity → tickProximityHit
     *   → tickWireGuidance【线导激活时 return，跳过声导和后续】
     *   → tickAcousticHoming
     *   → tickAirDrop【空投阶段强制接管运动，跳过水面AI】
     *   → tickSurfaceAI
     *
     * 模式互斥关系：
     *   - 线导激活时：声导归位被线导 return 短路
     *   - 空投阶段：接管运动控制，跳过水面巡航
     *   - 磁性近炸：独立于运动模式，只检查是否引爆，不接管运动
     */
    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;

        if (level().isClientSide()) {
            // 方案1：向服务端目标位置按步数平滑逼近，避免 setPos 硬拽。
            if (clientLerpSteps > 0) {
                double d = 1.0 / (double) clientLerpSteps;
                setPos(getX() + (clientLerpX - getX()) * d,
                       getY() + (clientLerpY - getY()) * d,
                       getZ() + (clientLerpZ - getZ()) * d);
                clientLerpSteps--;
            }
            // 方案2：先同步航速/模式标志，再跑与服务端相同的运动 AI。
            syncClientState();
        }

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

    /**
     * 剩余航程检查：超时爆炸。
     *
     * <p>寿命由服务端裁决：{@code lifetime} 字段不下发（客户端默认 1200，服务端是
     * {@code range*20}，可能被调试终端按型号改过），客户端自行倒计时会与服务端分叉——
     * 线导时表现为客户端雷体提前 discard、第一人称镜头被强制退出。客户端改为只等服务端移除包。
     */
    private boolean tickLifetime() {
        if (level().isClientSide()) return false;
        if (--lifetime > 0) return false;
        if (!exploded) {
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
        if (level().isClientSide()) {
            // 方案4：客户端权威。本地玩家在引导本鱼雷时，由本地预测直接驱动运动。
            // 断线/超程/寿命等服务端专属判定不在此镜像，靠服务端下发 wireGuided=false 或移除实体兜底。
            return wireGuided && applyGuidedMovement();
        }
        if (wireGuided && launchPos == null) {
            launchPos = position();
        }
        if (wireGuided) {
            double maxRange = getWireMaxRange();
            Entity owner = getOwner();
            if (owner == null) {
                setWireGuided(false);
            } else {
                double dist = position().distanceTo(owner.position());
                if (dist > maxRange) {
                    setWireGuided(false);
                    if (owner instanceof ServerPlayer sp) {
                        TorpedoGuidanceManager.endGuidance(sp);
                        sp.displayClientMessage(
                                Component.translatable("message.piranport.torpedo_wire_lost"), true);
                    }
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
            entityData.set(DATA_AIR_DROP, false);
            setDeltaMovement(airDropDirection.x * torpedoSpeed, 0, airDropDirection.z * torpedoSpeed);
        } else {
            setDeltaMovement(motion.x * 0.98, motion.y - 0.08, motion.z * 0.98);
        }
        return true;
    }

    /**
     * 水下航行 AI：鱼雷保持在水面之下一点航行（用户口径 2026-10-05）。
     *
     * <p>旧实现只在"脚部在空气、正下方是水"时把垂直速度清零，于是雷体正好卡在水面
     * 之上的空气方块里 → 视觉上"浮在水面上"。现改为按列内水柱顶面定位：
     * 目标脚部 Y = 水面顶面 − 中心目标深度 − 半高，使雷体中心稳定停在水面之下
     * {@code TORPEDO_SURFACE_DEPTH} 格处。
     *
     * <p>水深超过 {@code TORPEDO_SURFACE_CAPTURE_RANGE} 时保持既有垂直运动（深水巡航），
     * 不强行把潜艇/水中发射的深雷拽上水面。水面搜索只在实体所在区块读取方块状态，
     * 客户端与服务端跑的是同一段代码。
     */
    private void tickSurfaceAI() {
        Vec3 motion = getDeltaMovement();

        // 水平方向始终按航速巡航（方向沿用当前速度方向）
        double h = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        double dirX = h > 0.001 ? motion.x / h : 0.0;
        double dirZ = h > 0.001 ? motion.z / h : 0.0;

        double vy = solveSurfaceVy();
        if (Double.isNaN(vy)) {
            // 附近没有水（陆地上空等）／在水面之上且未进入捕获窗口：维持原有的减速坠落
            setDeltaMovement(motion.x * AIR_FALL_HORIZONTAL_DECAY,
                    motion.y - AIR_FALL_VERTICAL_ACCEL,
                    motion.z * AIR_FALL_HORIZONTAL_DECAY);
            return;
        }

        setDeltaMovement(dirX * torpedoSpeed, vy, dirZ * torpedoSpeed);
    }

    /**
     * 按列内水柱顶面与 {@code global.torpedo.surface_*} 参数解算"水下贴水面"的目标垂直速度。
     * 线导（{@link #applyGuidedMovement()}）与无人巡航（{@link #tickSurfaceAI()}）共用，保证两侧手感一致。
     *
     * @return 目标 vy；返回 {@link Double#NaN} 表示当前不适用（附近无水，或在水面之上且尚未进入捕获窗口），
     *         调用方应退回空中坠落手感。
     */
    private double solveSurfaceVy() {
        int topWaterY = findTopWaterY();
        if (topWaterY == NO_WATER) return Double.NaN;

        double targetFeetY = topWaterY + STILL_WATER_SURFACE_HEIGHT
                - surfaceDepth() - getBbHeight() * 0.5;
        double err = targetFeetY - getY();
        double capture = surfaceCaptureRange();

        if (Math.abs(err) <= capture) {
            // 已在目标深度附近：按比例逼近，并限制垂直速度（避免上浮/下潜过猛）
            double maxVy = surfaceVerticalMaxSpeed();
            return Mth.clamp(err * surfaceVerticalAdjust(), -maxVy, maxVy);
        }
        if (err < 0) {
            // 水面之上但还没进入捕获窗口：交由调用方按原空中手感下坠
            return Double.NaN;
        }
        // 水下且离目标深度超出捕获窗口：保持既有垂直运动（深水巡航）
        return getDeltaMovement().y;
    }

    /**
     * 在鱼雷所在列内向上/向下搜索水柱顶面（最高水方块的 Y）。
     * 向上搜的格数取 {@code ceil(captureRange) + 1}：保证深水时"最近水面"落在搜索窗顶端、
     * 其与目标的误差必然大于捕获窗口，从而进入"保持深水"分支，不会把深雷拽上来。
     */
    private int findTopWaterY() {
        BlockPos base = blockPosition();
        int up = Mth.ceil(surfaceCaptureRange()) + 1;
        int down = up + 1;
        for (int dy = up; dy >= -down; dy--) {
            BlockPos p = base.offset(0, dy, 0);
            if (level().getBlockState(p).getFluidState().is(Fluids.WATER)) {
                return p.getY();
            }
        }
        return NO_WATER;
    }

    private static double surfaceDepth() {
        return ModEquipmentConfig.TORPEDO_SURFACE_DEPTH.get();
    }

    private static double surfaceVerticalAdjust() {
        return ModEquipmentConfig.TORPEDO_SURFACE_VERTICAL_ADJUST.get();
    }

    private static double surfaceVerticalMaxSpeed() {
        return ModEquipmentConfig.TORPEDO_SURFACE_VERTICAL_MAX_SPEED.get();
    }

    private static double surfaceCaptureRange() {
        return ModEquipmentConfig.TORPEDO_SURFACE_CAPTURE_RANGE.get();
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

    // P2优化: 声导扫描节流，避免每tick扫描
    private int acousticScanCooldown = 0;

    /** 声导追踪：向最近的有声目标转向 */
    private void acousticHoming() {
        // 验证当前锁定目标是否仍然有效
        if (lockedTarget != null && (!lockedTarget.isAlive() || lockedTarget.isRemoved()
                || distanceTo(lockedTarget) > LOCK_BREAK_DISTANCE)) {
            lockedTarget = null;
            lockDuration = 0;
        }

        // 如果有锁定目标且在稳定期内，继续追踪
        if (lockedTarget != null && lockDuration < MIN_LOCK_DURATION) {
            lockDuration++;
            turnTowardsTarget(lockedTarget, distanceTo(lockedTarget));
            return;
        }

        // P2优化: 如果有锁定目标且冷却中，继续追踪不扫描
        if (lockedTarget != null && --acousticScanCooldown > 0) {
            turnTowardsTarget(lockedTarget, distanceTo(lockedTarget));
            return;
        }
        acousticScanCooldown = 5; // 每5tick扫描一次

        // 扫描新目标
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

        // 转向锁定目标
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
                    // 依据：策划决策/战斗/02-Buff系统核心设计.md（进水时长 8 秒 = 160 tick）
                    living.addEffect(new MobEffectInstance(ModMobEffects.FLOODING, 160, 0));
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

    /**
     * 3D 模型比碰撞箱长得多（雷体 1.25 格，碰撞箱只有 0.5x0.25），
     * 放宽视锥剔除盒，免得雷头雷尾还画面上时整根雷被剔掉。
     */
    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(0.75);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        BlockState hitState = level().getBlockState(result.getBlockPos());
        if (isPassThroughForTorpedo(hitState)) return;

        // 纯水不是实体碰撞面；waterlogged 实心方块则仍按障碍物处理并爆炸
        if (isPureWater(hitState)) return;

        super.onHitBlock(result);
        if (!level().isClientSide() && !exploded) {
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

    private static boolean isPureWater(BlockState state) {
        return state.getFluidState().is(Fluids.WATER)
                && state.getFluidState().isSource()
                && (!state.hasProperty(BlockStateProperties.WATERLOGGED)
                        || !state.getValue(BlockStateProperties.WATERLOGGED));
    }

    @Override
    protected double getDefaultGravity() { return 0.0; }

    /** 鱼雷免疫爆炸击退，防止多枚鱼雷互相干扰弹道 */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return true;
        return super.isInvulnerableTo(source);
    }

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
        tag.putBoolean("Oxygen", oxygen);
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
        if (torpedoSpeed <= 0) torpedoSpeed = 1.0f;
        lifetime = tag.getInt("Lifetime");
        if (lifetime <= 0) lifetime = 1200;
        explosionRadius = tag.getFloat("ExplosionRadius");
        if (explosionRadius <= 0) explosionRadius = 2.0f;
        magnetic = tag.getBoolean("Magnetic");
        wireGuided = tag.getBoolean("WireGuided");
        acoustic = tag.getBoolean("Acoustic");
        oxygen = tag.getBoolean("Oxygen");
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

        // NBT 只在服务端反序列化；把同步字段推进 entityData，随生成包下发给客户端，
        // 否则读档后的鱼雷在客户端仍是默认速度/标志，两边 AI 又会不一致。
        entityData.set(DATA_SPEED, torpedoSpeed);
        entityData.set(DATA_MAGNETIC, magnetic);
        entityData.set(DATA_WIRE_GUIDED, wireGuided);
        entityData.set(DATA_ACOUSTIC, acoustic);
        entityData.set(DATA_OXYGEN, oxygen);
        entityData.set(DATA_AIR_DROP, airDrop);
        entityData.set(DATA_AIR_DROP_DIR_X, (float) airDropDirection.x);
        entityData.set(DATA_AIR_DROP_DIR_Z, (float) airDropDirection.z);
    }
}
