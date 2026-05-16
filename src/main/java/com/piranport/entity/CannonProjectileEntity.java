package com.piranport.entity;

import com.piranport.PiranPort;
import com.piranport.config.ModArtilleryConfig;
import com.piranport.config.ModCommonConfig;
import com.piranport.config.ModProjectilesConfig;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.util.List;

public class CannonProjectileEntity extends ThrowableItemProjectile {
    private float damage = 6f;
    private boolean isHE = true;
    private boolean isVT = false;
    private float explosionPower = 1.5f;
    private float initialSpeed = 2.0f;
    /** Phase 2: 阻力系数（每 tick 速度保留比例 = 1 - dragCoeff） */
    private float dragCoeff = 0.01f;
    /** 防止 VT 近炸引信 + onHit 在同一 tick 内双重爆炸。 */
    private boolean exploded = false;
    /** VT 弹水下计时器（tick），水中累计超过60tick（3秒）后近炸。 */
    private int underwaterTicks = 0;

    /** 追踪（自导引）炮弹：每 tick 向目标轻微转向。 */
    private int trackingTargetId = -1;
    /** 混合因子：0.05 = 每 tick 约 5% 修正（平缓曲线）。 */
    private static final double TRACKING_STEER = 0.05;

    /** 自定义重力（真实比例，使用时除以 196 换算为 MC 比例）。0 表示使用默认值。 */
    private float customGravity = 0f;

    /** 缓存的黑曜石爆炸抗性，避免每次碰撞都创建 Explosion 对象。 */
    private static float cachedObsidianResistance = -1f;
    private static float getObsidianResistance(Level level, BlockPos pos) {
        if (cachedObsidianResistance < 0) {
            Explosion ctx = new Explosion(level, null, 0, 0, 0,
                    1.0f, false, Explosion.BlockInteraction.KEEP);
            cachedObsidianResistance = Blocks.OBSIDIAN.defaultBlockState()
                    .getExplosionResistance(level, pos, ctx);
        }
        return cachedObsidianResistance;
    }

    // ===== VT 近炸引信参数（从 ModArtilleryConfig 读取） =====
    /** VT 锥形检测范围（格），弹头前方该距离内的目标才会触发近炸。 */
    private double vtDetectRange = ModArtilleryConfig.VT_DETECT_RANGE.get();
    /** VT 锥形半角（度），目标方向与弹头速度方向的夹角在此范围内才触发。 */
    private double vtConeHalfAngleDeg = ModArtilleryConfig.VT_CONE_HALF_ANGLE.get();
    /** VT 检测间隔（tick），每 N tick 执行一次锥形区域扫描。 */
    private int vtCheckInterval = ModArtilleryConfig.PERF_VT_CHECK_INTERVAL.get();
    /** 方块接近检测的前方射线长度（格）。 */
    private double vtBlockRange = ModArtilleryConfig.VT_BLOCK_RANGE.get();
    /** 发射后 VT 引信解锁前的宽限期（tick）。 */
    private int vtArmTicks = ModArtilleryConfig.VT_ARM_TICKS.get();

    // 实体类型注册所需的构造器
    public CannonProjectileEntity(EntityType<? extends CannonProjectileEntity> type, Level level) {
        super(type, level);
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

    @Override
    public void shootFromRotation(Entity shooter, float xRot, float yRot,
                                   float zRot, float speed, float inaccuracy) {
        super.shootFromRotation(shooter, xRot, yRot, zRot, speed, inaccuracy);
        this.initialSpeed = speed;
    }

    @Override
    public void tick() {
        // Phase 2: 应用阻力（在 super.tick() 的重力生效前）
        Vec3 vel = getDeltaMovement();
        if (vel.length() > 0.01 && dragCoeff > 0) {
            vel = vel.scale(Math.max(0.1, 1.0 - dragCoeff));
            setDeltaMovement(vel);
        }

        super.tick();

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
                underwaterTicks++;
                int maxTicks = (int) (ModArtilleryConfig.ARTILLERY_UNDERWATER_DESTROY_TIME.get() * 20);
                if (underwaterTicks >= maxTicks) {
                    if (isHE && ModProjectilesConfig.UNDERWATER_EXPLODE.get()) {
                        Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                                ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
                        level().explode(this, getX(), getY(), getZ(), explosionPower, interaction);
                    }
                    discard();
                }
            }
            if (isVT && tickCount > vtArmTicks) {
                tickVT();
            }
            if (trackingTargetId >= 0) {
                tickTracking();
            }
        }
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
        if (speed < 0.1) return;
        Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(position()).normalize();
        Vec3 currentDir = vel.normalize();
        Vec3 newDir = currentDir.scale(1.0 - TRACKING_STEER)
                .add(toTarget.scale(TRACKING_STEER)).normalize();
        setDeltaMovement(newDir.scale(speed));
    }

    /** 每 tick 调用，但实际引信检测按 vtCheckInterval 间隔执行。 */
    private void tickVT() {
        // VT 弹水中延时爆炸（按HE处理）
        if (isInWater()) {
            underwaterTicks++;
            int maxUnderwater = (int) (ModArtilleryConfig.ARTILLERY_UNDERWATER_DESTROY_TIME.get() * 20);
            if (underwaterTicks >= maxUnderwater) {
                proximityDetonate();
            }
            return;
        }
        underwaterTicks = 0;

        // 检测频率控制：每 vtCheckInterval tick 执行一次
        if (tickCount % vtCheckInterval != 0) return;

        checkProximityFuze();
    }

    private void checkProximityFuze() {
        Vec3 velocity = getDeltaMovement();
        if (velocity.lengthSqr() < 0.01) return;
        Vec3 pos = position();

        // 锥形区域检测：只检测弹头速度方向前方锥体内的实体
        AABB searchBox = getBoundingBox().inflate(vtDetectRange);
        List<Entity> nearby = level().getEntities(this, searchBox, e ->
                e instanceof LivingEntity
                        && e != getOwner()
                        && e.isAlive());

        for (Entity entity : nearby) {
            Vec3 toTarget = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(pos);
            double dist = toTarget.length();
            if (dist > vtDetectRange) continue;

            // 锥形过滤：检查目标方向与弹头速度方向的夹角
            Vec3 velNorm = velocity.normalize();
            Vec3 targetDir = toTarget.normalize();
            double dot = velNorm.dot(targetDir);
            double angleDeg = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
            if (angleDeg <= vtConeHalfAngleDeg) {
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
                level().playSound(null, getX(), getY(), getZ(),
                        ModSounds.CANNON_EXPLOSION.get(), SoundSource.PLAYERS, 2.0f, 0.9f + random.nextFloat() * 0.2f);
            } else {
                // AP：130% 基础直击伤害，与原版箭矢一样随速度衰减，忽略 50% 目标护甲
                float currentSpeed = (float) getDeltaMovement().length();
                float speedRatio = initialSpeed > 0 ? currentSpeed / initialSpeed : 1.0f;
                float apMultiplier = ModProjectilesConfig.AP_DAMAGE_MULTIPLIER.get().floatValue();
                float apDamage = damage * apMultiplier * speedRatio;
                float apArmorIgnore = ModProjectilesConfig.AP_ARMOR_IGNORE.get().floatValue();
                if (apArmorIgnore < 0) apArmorIgnore = 0;
                if (apArmorIgnore > 1) apArmorIgnore = 1;
                level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.2f);
                if (target instanceof LivingEntity living) {
                    AttributeInstance armorAttr = living.getAttribute(Attributes.ARMOR);
                    // P0 #4: 使用 UUID + tickCount 生成唯一 ID，避免同 tick 多枚 AP 弹齐射时 ID 冲突
                    ResourceLocation apPenId = ResourceLocation.fromNamespaceAndPath(
                            PiranPort.MOD_ID, "ap_penetration/" + getUUID() + "_" + tickCount);
                    boolean applied = false;
                    if (armorAttr != null) {
                        armorAttr.removeModifier(apPenId);
                        double halfArmor = living.getAttributeValue(Attributes.ARMOR) * apArmorIgnore;
                        armorAttr.addTransientModifier(new AttributeModifier(
                                apPenId, -halfArmor, AttributeModifier.Operation.ADD_VALUE));
                        applied = true;
                    }
                    try {
                        living.hurt(damageSources().thrown(this, getOwner()), apDamage);
                    } finally {
                        if (applied) {
                            armorAttr.removeModifier(apPenId);
                        }
                    }
                } else {
                    target.hurt(damageSources().thrown(this, getOwner()), apDamage);
                }
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

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide && !exploded) {
            if (isHE) {
                boolean breakBlocks = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get() && !isInWater();
                Level.ExplosionInteraction interaction = breakBlocks
                        ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
                level().explode(this, getX(), getY(), getZ(), explosionPower, interaction);
                level().playSound(null, getX(), getY(), getZ(),
                        ModSounds.CANNON_EXPLOSION.get(), SoundSource.PLAYERS, 2.0f, 0.9f + random.nextFloat() * 0.2f);
            } else {
                // AP：若抗性 < 黑曜石则破坏方块并以物品形式掉落；AP 在水中时不破坏方块
                if (ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get() && !isInWater()) {
                    BlockPos pos = result.getBlockPos();
                    BlockState state = level().getBlockState(pos);
                    if (state.getExplosionResistance(level(), pos, null) < getObsidianResistance(level(), pos)) {
                        level().destroyBlock(pos, true);
                    }
                }
            }
        }
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
    }
}
