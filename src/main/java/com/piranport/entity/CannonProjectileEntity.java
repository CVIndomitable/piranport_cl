package com.piranport.entity;

import com.piranport.PiranPort;
import com.piranport.artillery.config.override.ConfigOverrideManager;
import com.piranport.combat.CannonImpactEffectBroadcaster;
import com.piranport.combat.FireApplyHelper;
import com.piranport.combat.ShipTypeMitigationHelper;
import com.piranport.config.ModArtilleryConfig;
import com.piranport.config.ModCommonConfig;
import com.piranport.config.ModProjectilesConfig;
import com.piranport.network.CannonImpactEffectPayload;
import com.piranport.registry.ModBlocks;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class CannonProjectileEntity extends ThrowableItemProjectile {
    private float damage = 6f;
    private boolean isHE = true;
    private boolean isVT = false;
    private float explosionPower = 1.5f;
    private float initialSpeed = 2.0f;
    /** Phase 2: 自定义阻力系数（每 tick 按比例衰减速度）。 */
    private float dragCoeff = 0.01f;
    /** 防止 VT 近炸引信 + onHit 在同一 tick 内双重爆炸。 */
    private boolean exploded = false;
    /** VT 弹水下计时器（tick），水中累计超过60tick（3秒）后近炸。 */
    private int underwaterTicks = 0;
    /** 炮弹首次入水反馈只播放一次。 */
    private boolean waterEntryEffectSent = false;
    /** 破空音效节流，避免齐射时每 tick 刷声音。 */
    private int lastWhistleTick = -1000;

    /** 追踪（自导引）炮弹：每 tick 向目标轻微转向。 */
    private int trackingTargetId = -1;
    /** 混合因子：0.05 = 每 tick 约 5% 修正（平缓曲线）。 */
    private static final double TRACKING_STEER = 0.05;

    /** 自定义重力（真实比例，使用时除以 196 换算为 MC 比例）。0 表示使用默认值。 */
    private float customGravity = 0f;

    /** 缓存的黑曜石爆炸抗性，避免每次碰撞都创建 Explosion 对象。初始化在构造函数中完成。 */
    private final float cachedObsidianResistance;

    /** 进程内单调递增 ID，保证同 tick 齐射时临时属性名不重复。 */
    private static final AtomicLong AP_MODIFIER_SEQUENCE = new AtomicLong();

    /** 发射此炮弹的火炮口径（inch/2 近似单位，>8 为大口径）。策划 §5 决策：AP 大口径对小型船过穿。 */
    private int sourceCaliber = 0;

    // 客户端位置插值（防止服务端位置同步跳跃导致的抖动）
    private int clientLerpSteps;
    private double clientLerpX, clientLerpY, clientLerpZ;

    private float initObsidianResistance(Level level) {
        Explosion ctx = new Explosion(level, null, 0, 0, 0,
                1.0f, false, Explosion.BlockInteraction.KEEP);
        return Blocks.OBSIDIAN.defaultBlockState()
                .getExplosionResistance(level, BlockPos.ZERO, ctx);
    }

    // ===== VT 近炸引信参数（静态配置，所有炮弹共享） =====
    /** VT 锥形检测范围（格），弹头前方该距离内的目标才会触发近炸。 */
    private static double vtDetectRange;
    /** VT 锥形半角（度），目标方向与弹头速度方向的夹角在此范围内才触发。 */
    private static double vtConeHalfAngleDeg;
    /** VT 检测间隔（tick），每 N tick 执行一次锥形区域扫描。 */
    private static int vtCheckInterval;
    /** 方块接近检测的前方射线长度（格）。 */
    private static double vtBlockRange;
    /** 发射后 VT 引信解锁前的宽限期（tick）。 */
    private static int vtArmTicks;

    static {
        loadVtConfigValues();
    }

    /** 从配置加载VT引信参数（静态初始化，所有实例共享）*/
    private static void loadVtConfigValues() {
        vtDetectRange = ModArtilleryConfig.VT_DETECT_RANGE.get();
        vtConeHalfAngleDeg = ModArtilleryConfig.VT_CONE_HALF_ANGLE.get();
        vtCheckInterval = Math.max(1, ModArtilleryConfig.PERF_VT_CHECK_INTERVAL.get());
        vtBlockRange = ModArtilleryConfig.VT_BLOCK_RANGE.get();
        vtArmTicks = ModArtilleryConfig.VT_ARM_TICKS.get();
    }

    // 实体类型注册所需的构造器
    public CannonProjectileEntity(EntityType<? extends CannonProjectileEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.cachedObsidianResistance = initObsidianResistance(level);
    }

    // 发射用构造器
    public CannonProjectileEntity(Level level, LivingEntity shooter,
                                   ItemStack shellItem, float damage,
                                   boolean isHE, float explosionPower) {
        super(ModEntityTypes.CANNON_PROJECTILE.get(), shooter, level);
        setItem(shellItem);
        this.damage = damage;
        this.isHE = isHE;
        this.explosionPower = explosionPower;
        this.cachedObsidianResistance = initObsidianResistance(level);
    }

    public void setVT(boolean vt) {
        this.isVT = vt;
    }

    /** 启用对特定实体的追踪（通过运行时实体 ID）。 */
    public void setTracking(int entityId) {
        this.trackingTargetId = entityId;
    }

    public void setDragCoeff(float dragCoeff) {
        this.dragCoeff = dragCoeff;
    }

    public void setCustomGravity(float g) {
        this.customGravity = g;
    }

    /** 标记发射火炮口径；策划 §5 决策：AP 大口径对小船过穿伤害 5%。 */
    public void setSourceCaliber(int caliber) {
        this.sourceCaliber = caliber;
    }

    public int getSourceCaliber() {
        return sourceCaliber;
    }

    /**
     * 客户端位置插值：存储目标位置，在 tick() 中平滑过渡。
     * 解决 Entity 默认 lerpTo 直接 setPos 导致 xo/yo/zo 与 x/y/z 相同、
     * partial-tick 插值失效的镜头抖动问题。
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

    @Override
    public void shootFromRotation(Entity shooter, float xRot, float yRot,
                                   float zRot, float speed, float inaccuracy) {
        super.shootFromRotation(shooter, xRot, yRot, zRot, speed, inaccuracy);
        this.initialSpeed = speed;
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, velocity, inaccuracy);
        this.initialSpeed = velocity;
    }

    @Override
    public void tick() {
        // Phase 2: 应用自定义阻力系数。阻力只衰减速度，不会把炮弹直接钉停在空中。
        if (dragCoeff > 0) {
            setDeltaMovement(applyDragCoefficient(getDeltaMovement(), dragCoeff));
        }

        super.tick();

        // 客户端位置插值：每 tick 向目标位置靠近一步
        if (level().isClientSide && clientLerpSteps > 0) {
            double d = 1.0 / (double) clientLerpSteps;
            setPos(getX() + (clientLerpX - getX()) * d,
                   getY() + (clientLerpY - getY()) * d,
                   getZ() + (clientLerpZ - getZ()) * d);
            clientLerpSteps--;
        }

        // Phase 10: 客户端尾迹粒子 + 飞行音效
        if (level().isClientSide && tickCount % 2 == 0 && !isRemoved()) {
            Vec3 pos = position();
            Vec3 v = getDeltaMovement();
            if (v.length() > 0.1) {
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        pos.x, pos.y, pos.z,
                        -v.x * 0.05 + random.nextGaussian() * 0.01,
                        -v.y * 0.05 + random.nextGaussian() * 0.01,
                        -v.z * 0.05 + random.nextGaussian() * 0.01);
                if (random.nextFloat() < 0.1f) {
                    level().addParticle(ParticleTypes.SMALL_FLAME,
                            pos.x, pos.y, pos.z, 0, 0, 0);
                }
            }
        }

        if (!level().isClientSide) {
            // 水中弹药销毁：所有弹种通用（VT 弹在 tickVT 中另有近炸逻辑）
            if (!isVT && isInWater()) {
                sendWaterEntryEffectIfNeeded();
                handleUnderwaterDestruction(isHE);
            }
            if (isVT && tickCount > vtArmTicks) {
                tickVT();
            }
            tickWhistleSound();
            if (trackingTargetId >= 0) {
                tickTracking();
            }
        }
    }

    private void tickWhistleSound() {
        if (tickCount < 8 || tickCount - lastWhistleTick < 24) return;
        double speed = getDeltaMovement().length();
        if (speed < 1.6) return;
        lastWhistleTick = tickCount;
        SoundEvent whistle = getWhistleSound(speed);
        float volume = (float) Math.min(1.6, 0.25 + speed * 0.2 + explosionPower * 0.04f);
        float pitch = getWhistlePitch(whistle);
        level().playSound(null, getX(), getY(), getZ(),
                whistle, SoundSource.PLAYERS, volume, pitch);
    }

    private SoundEvent getWhistleSound(double speed) {
        if (speed >= 4.2) return ModSounds.SHELL_WHISTLE_FAST.get();
        if (explosionPower >= 3.5f || initialSpeed >= 3.2f) return ModSounds.SHELL_WHISTLE_HEAVY.get();
        return ModSounds.SHELL_WHISTLE.get();
    }

    private float getWhistlePitch(SoundEvent whistle) {
        if (whistle == ModSounds.SHELL_WHISTLE_FAST.get()) {
            return 1.08f + random.nextFloat() * 0.22f;
        }
        if (whistle == ModSounds.SHELL_WHISTLE_HEAVY.get()) {
            return 0.66f + random.nextFloat() * 0.16f;
        }
        return 0.8f + random.nextFloat() * 0.25f;
    }

    private void sendWaterEntryEffectIfNeeded() {
        if (waterEntryEffectSent) return;
        waterEntryEffectSent = true;
        float scale = Math.max(0.6f, Math.min(4.0f,
                explosionPower * 0.55f + (float) getDeltaMovement().length() * 0.35f));
        sendImpactEffect(CannonImpactEffectPayload.Kind.WATER, scale);
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS,
                Math.min(2.0f, 0.8f + scale * 0.2f), 0.75f + random.nextFloat() * 0.15f);
    }

    private static Vec3 applyDragCoefficient(Vec3 velocity, float dragCoeff) {
        if (!Float.isFinite(dragCoeff) || dragCoeff <= 0f) return velocity;
        double speed = velocity.length();
        if (speed <= 1.0e-9) return Vec3.ZERO;
        double scale = 1.0 / (1.0 + Math.max(0.0, dragCoeff));
        return velocity.scale(scale);
    }

    /**
     * 处理水中弹药销毁逻辑（所有弹种通用）。
     * @param shouldExplode 是否在销毁时触发爆炸（HE弹为true，AP弹为false）
     * @return 是否已达到销毁时间
     */
    private boolean handleUnderwaterDestruction(boolean shouldExplode) {
        underwaterTicks++;
        int maxTicks = (int) (ModArtilleryConfig.ARTILLERY_UNDERWATER_DESTROY_TIME.get() * 20);
        if (underwaterTicks >= maxTicks) {
            exploded = true;
            if (shouldExplode && getProjectileBoolean("UNDERWATER_EXPLODE", ModProjectilesConfig.UNDERWATER_EXPLODE.get())) {
                Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                        ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
                double multiplier = getProjectileDouble("UNDERWATER_EXPLOSION_MULTIPLIER",
                        ModProjectilesConfig.UNDERWATER_EXPLOSION_MULTIPLIER.get());
                float scaledPower = explosionPower * (float) multiplier;
                level().explode(this, getX(), getY(), getZ(), scaledPower, interaction);
                sendImpactEffect(CannonImpactEffectPayload.Kind.HE, scaledPower);
            }
            discard();
            return true;
        }
        return false;
    }

    /** 每 tick 向追踪目标轻微转向。 */
    private void tickTracking() {
        Entity target = level().getEntity(trackingTargetId);
        if (target == null || !target.isAlive()) {
            trackingTargetId = -1;
            return;
        }
        Vec3 vel = getDeltaMovement();
        double speed = vel.length();
        if (speed < 0.1) {
            trackingTargetId = -1;
            return;
        }
        Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(position()).normalize();
        Vec3 currentDir = vel.normalize();
        Vec3 newDir = currentDir.scale(1.0 - TRACKING_STEER)
                .add(toTarget.scale(TRACKING_STEER)).normalize();
        setDeltaMovement(newDir.scale(speed));
    }

    /** 每 tick 调用，但实际引信检测按 vtCheckInterval 间隔执行。 */
    private void tickVT() {
        loadVtConfigValues();
        // VT 弹水中延时爆炸（使用近炸引信）
        if (isInWater()) {
            sendWaterEntryEffectIfNeeded();
            if (handleUnderwaterDestruction(false)) {
                proximityDetonate();
            }
            return;
        }
        underwaterTicks = 0;

        // 检测频率控制：每 vtCheckInterval tick 执行一次
        if (tickCount % vtCheckInterval != 0) return;

        checkProximityFuze();
    }

    private static double getVtConeCosThreshold() {
        double angle = vtConeHalfAngleDeg;
        if (!Double.isFinite(angle) || angle <= 0.0 || angle >= 180.0) {
            angle = 30.0;
        }
        return Math.cos(Math.toRadians(angle));
    }

    private void checkProximityFuze() {
        Vec3 velocity = getDeltaMovement();
        if (velocity.lengthSqr() < 0.01) return;
        Vec3 pos = position();

        // 锥形区域检测：只检测弹头速度方向前方锥体内的实体
        double rangeSquared = vtDetectRange * vtDetectRange;
        AABB searchBox = new AABB(
                pos.x - vtDetectRange, pos.y - vtDetectRange, pos.z - vtDetectRange,
                pos.x + vtDetectRange, pos.y + vtDetectRange, pos.z + vtDetectRange);
        List<Entity> nearby = level().getEntities(this, searchBox, e ->
                e instanceof LivingEntity
                        && e != getOwner()
                        && e.isAlive()
                        && !com.piranport.combat.FriendlyFireHelper.shouldBlockHit(e, getOwner()));

        Vec3 velNorm = velocity.normalize();
        for (Entity entity : nearby) {
            Vec3 toTarget = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(pos);
            double distSquared = toTarget.lengthSqr();
            if (distSquared > rangeSquared) continue;

            // P2优化: 直接比较点积与cos阈值，避免昂贵的acos和toDegrees计算
            Vec3 targetDir = toTarget.normalize();
            double dot = velNorm.dot(targetDir);
            if (dot >= getVtConeCosThreshold()) {
                proximityDetonate();
                return;
            }
        }

        // 方块接近检测（前方短射线），保持原逻辑
        Vec3 forward = velocity.normalize();
        Vec3 ahead = pos.add(forward.scale(vtBlockRange));
        BlockHitResult blockHit = level().clip(new ClipContext(
                pos, ahead, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            double blockDist = blockHit.getLocation().distanceTo(pos);
            if (blockDist <= vtBlockRange) {
                proximityDetonate();
            }
        }
    }

    private void proximityDetonate() {
        if (exploded) return;
        exploded = true;
        Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
        level().explode(this, getX(), getY(), getZ(), explosionPower, interaction);
        sendImpactEffect(CannonImpactEffectPayload.Kind.VT, explosionPower);
        level().playSound(null, getX(), getY(), getZ(),
                ModSounds.CANNON_EXPLOSION.get(), SoundSource.PLAYERS, 2.0f, 0.9f + random.nextFloat() * 0.2f);
        level().broadcastEntityEvent(this, (byte) 3);
        discard();
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SMALL_HE_SHELL.get();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (com.piranport.combat.FriendlyFireHelper.shouldBlockHit(target, getOwner())) return false;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide && !exploded) {
            Entity target = result.getEntity();
            // 联装炮齐射：同 tick 多发命中同目标时，重置无敌帧让每发都造成伤害
            target.invulnerableTime = 0;
            if (isHE) {
                // HE：直击伤害 + 范围爆炸溅射
                target.hurt(damageSources().explosion(this, getOwner()), damage);
                Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                        ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
                level().explode(this, getX(), getY(), getZ(), explosionPower, interaction);
                // 依据：策划决策/战斗/04-起火Debuff替代原版着火.md（HE 命中按口径概率施火）
                if (target instanceof LivingEntity living) {
                    FireApplyHelper.tryApplyFire(living, getItem().getItem(), false);
                }
                sendImpactEffect(CannonImpactEffectPayload.Kind.HE, explosionPower);
                level().playSound(null, getX(), getY(), getZ(),
                        ModSounds.CANNON_EXPLOSION.get(), SoundSource.PLAYERS, 2.0f, 0.9f + random.nextFloat() * 0.2f);
            } else {
                // AP：130% 基础直击伤害，与原版箭矢一样随速度衰减，忽略 50% 目标护甲
                float currentSpeed = (float) getDeltaMovement().length();
                float speedRatio = initialSpeed > 0 ? currentSpeed / initialSpeed : 1.0f;
                float apMultiplier = (float) getProjectileDouble("AP_DAMAGE_MULTIPLIER",
                        ModProjectilesConfig.AP_DAMAGE_MULTIPLIER.get());
                float apDamage = damage * apMultiplier * speedRatio;
                // 依据：策划决策/数值/05-船型职能分化修订.md（大口径 AP 对小型船过穿 5%）
                if (target instanceof LivingEntity) {
                    apDamage = ShipTypeMitigationHelper.applyLargeApOverpen(apDamage, sourceCaliber);
                }
                float apArmorIgnore = (float) getProjectileDouble("AP_ARMOR_IGNORE",
                        ModProjectilesConfig.AP_ARMOR_IGNORE.get());
                // 依据：策划决策/武器/弹药-AP弹穿甲设计.md（91 式 20% / 一式 50% 护甲忽略）
                Item ammoItem = getItem().getItem();
                if (ammoItem == ModItems.TYPE_91_AP_SHELL.get()) {
                    apArmorIgnore = 0.20f;
                } else if (ammoItem == ModItems.TYPE_1_AP_SHELL.get()) {
                    apArmorIgnore = 0.50f;
                }
                if (apArmorIgnore < 0) apArmorIgnore = 0;
                if (apArmorIgnore > 1) apArmorIgnore = 1;
                level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.2f);
                if (target instanceof LivingEntity living) {
                    AttributeInstance armorAttr = living.getAttribute(Attributes.ARMOR);
                    // 使用 UUID + tickCount + random 生成唯一 ID，避免同 tick 多枚 AP 弹齐射时 ID 冲突
                    ResourceLocation apPenId = ResourceLocation.fromNamespaceAndPath(
                            PiranPort.MOD_ID, "ap_penetration/" + getUUID() + "_" + tickCount + "_"
                                    + AP_MODIFIER_SEQUENCE.incrementAndGet());
                    boolean applied = false;
                    if (armorAttr != null) {
                        armorAttr.removeModifier(apPenId);
                        double halfArmor = living.getAttributeValue(Attributes.ARMOR) * apArmorIgnore;
                        armorAttr.addTransientModifier(new AttributeModifier(
                                apPenId, -halfArmor, AttributeModifier.Operation.ADD_VALUE));
                        applied = true;
                    }
                    try {
                        // 依据：策划决策/数值/05-船型职能分化修订.md（小型船 AP/HE 单次封顶 = maxHealth/4）
                        if (ShipTypeMitigationHelper.isSmallTransformedPlayer(living)) {
                            apDamage = ShipTypeMitigationHelper.capSmallShipDamage(apDamage, living.getMaxHealth());
                        }
                        living.hurt(damageSources().thrown(this, getOwner()), apDamage);
                    } finally {
                        if (applied && armorAttr != null) {
                            armorAttr.removeModifier(apPenId);
                        }
                    }
                } else {
                    target.hurt(damageSources().thrown(this, getOwner()), apDamage);
                }
                sendImpactEffect(CannonImpactEffectPayload.Kind.AP, Math.max(0.5f, explosionPower * 0.45f));
            }
            notifyOwner(target);
        }
    }

    private void notifyOwner(Entity target) {
        Entity owner = getOwner();
        if (!(owner instanceof Player player)) return;
        Component weaponName = getItem().getHoverName();
        String key = target.isAlive() ? "message.piranport.weapon_hit" : "message.piranport.weapon_kill";
        com.piranport.combat.HitNotifier.send(player, Component.translatable(key, weaponName, target.getDisplayName()));
    }

    private double getProjectileDouble(String key, double defaultValue) {
        return ConfigOverrideManager.getProjectileConfigDouble(key, defaultValue, level());
    }

    private boolean getProjectileBoolean(String key, boolean defaultValue) {
        return ConfigOverrideManager.getProjectileConfigBoolean(key, defaultValue, level());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide && !exploded) {
            if (isHE) {
                if (isInWater()) {
                    sendWaterEntryEffectIfNeeded();
                }
                boolean breakBlocks = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get() && !isInWater();
                Level.ExplosionInteraction interaction = breakBlocks
                        ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
                level().explode(this, getX(), getY(), getZ(), explosionPower, interaction);
                sendImpactEffect(CannonImpactEffectPayload.Kind.HE, explosionPower);
                level().playSound(null, getX(), getY(), getZ(),
                        ModSounds.CANNON_EXPLOSION.get(), SoundSource.PLAYERS, 2.0f, 0.9f + random.nextFloat() * 0.2f);
            } else {
                // AP：若抗性 < 黑曜石则破坏方块并以物品形式掉落；AP 在水中时不破坏方块
                if (ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get() && !isInWater()) {
                    BlockPos pos = result.getBlockPos();
                    BlockState state = level().getBlockState(pos);
                    if (state.getExplosionResistance(level(), pos, null) < cachedObsidianResistance) {
                        level().destroyBlock(pos, true);
                    }
                }
                sendImpactEffect(CannonImpactEffectPayload.Kind.AP, Math.max(0.5f, explosionPower * 0.45f));
            }
        }
    }

    private void sendImpactEffect(CannonImpactEffectPayload.Kind kind, float power) {
        CannonImpactEffectBroadcaster.send(level(), getX(), getY(), getZ(), power, kind);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().broadcastEntityEvent(this, (byte) 3);
            discard();
        }
    }

    @Override
    protected double getDefaultGravity() {
        // customGravity 是真实比例（如 9.8），除以 196 换算为 MC 内部比例
        return customGravity > 0f ? customGravity / 196.0 : 9.8 / 196.0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putBoolean("IsHE", isHE);
        tag.putBoolean("IsVT", isVT);
        tag.putFloat("ExplosionPower", explosionPower);
        tag.putFloat("InitialSpeed", initialSpeed);
        tag.putFloat("DragCoeff", dragCoeff);
        tag.putInt("UnderwaterTicks", underwaterTicks);
        tag.putBoolean("Exploded", exploded);
        tag.putFloat("CustomGravity", customGravity);
        tag.putInt("SourceCaliber", sourceCaliber);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        if (damage <= 0) damage = 4.0f; // 兜底：避免世界重载后出现零伤害的投射物
        isHE = tag.getBoolean("IsHE");
        isVT = tag.getBoolean("IsVT");
        explosionPower = tag.getFloat("ExplosionPower");
        if (tag.contains("InitialSpeed")) {
            initialSpeed = tag.getFloat("InitialSpeed");
        }
        if (tag.contains("DragCoeff")) {
            dragCoeff = tag.getFloat("DragCoeff");
        }
        if (tag.contains("UnderwaterTicks")) {
            underwaterTicks = tag.getInt("UnderwaterTicks");
        }
        if (tag.contains("Exploded")) {
            exploded = tag.getBoolean("Exploded");
        }
        if (tag.contains("CustomGravity")) {
            customGravity = tag.getFloat("CustomGravity");
        }
        if (tag.contains("SourceCaliber")) {
            sourceCaliber = tag.getInt("SourceCaliber");
        }
    }
}
