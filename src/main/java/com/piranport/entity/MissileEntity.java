package com.piranport.entity;

import com.piranport.PiranPort;
import com.piranport.aviation.FireControlManager;
import com.piranport.combat.FriendlyFireHelper;
import com.piranport.config.ModCommonConfig;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * 导弹/火箭弹实体 — 三种模式：
 * ANTI_SHIP: 制导追踪 + 直伤 + 穿甲
 * ANTI_AIR:  制导追踪(优先空中) + 爆炸
 * ROCKET:    直线飞行 + 爆炸
 */
public class MissileEntity extends ThrowableItemProjectile {

    public enum MissileType {
        /** 反舰导弹: 初速0.04, +0.04/tick, max 2.4 */
        ANTI_SHIP(0.04f, 0.04f, 2.4f,
                "tooltip.piranport.missile.type.anti_ship", ChatFormatting.RED),
        /** 防空导弹: 初速0.05, +0.05/tick, max 3.0 */
        ANTI_AIR(0.05f, 0.05f, 3.0f,
                "tooltip.piranport.missile.type.anti_air", ChatFormatting.AQUA),
        /** 火箭弹: 初速0.04, +0.04/tick, max 2.0 */
        ROCKET(0.04f, 0.04f, 2.0f,
                "tooltip.piranport.missile.type.rocket", ChatFormatting.GOLD);

        public final float initialSpeed;
        public final float speedIncrement;
        public final float maxSpeed;
        public final String translationKey;
        public final ChatFormatting color;

        MissileType(float initial, float increment, float max,
                    String translationKey, ChatFormatting color) {
            this.initialSpeed = initial;
            this.speedIncrement = increment;
            this.maxSpeed = max;
            this.translationKey = translationKey;
            this.color = color;
        }
    }

    private MissileType missileType = MissileType.ANTI_SHIP;
    private float damage = 20f;
    private float armorPen = 0f;
    private float explosionPower = 2.0f;
    private float currentSpeed;
    /** Registry key of the display item (ammo item). */
    private String displayItemId = "";
    /** Cached resolved display item (resolved lazily from displayItemId). */
    private Item cachedDisplayItem = null;
    private static final int MAX_LIFETIME = 600; // 30 seconds
    private static final double SEARCH_RANGE = 32.0;
    /** Maximum turn rate per tick (degrees). */
    private static final float MAX_TURN_DEG = 12.0f;
    /** Cached tracking target for guided missiles. */
    private Entity trackedTarget = null;
    /** UUID of the tracked target, used for NBT persistence. */
    private UUID trackedTargetUUID = null;
    /** When true, skip automatic target search and keep the manually assigned target. */
    private boolean manualTarget = false;
    /** Ticks until next target search (throttle). */
    private int targetSearchCooldown = 0;

    private static final String AP_PEN_PATH_PREFIX = "missile_ap/";

    /** Required by entity type registration. */
    public MissileEntity(EntityType<? extends MissileEntity> type, Level level) {
        super(type, level);
        this.currentSpeed = MissileType.ANTI_SHIP.initialSpeed;
    }

    /** Spawned by ShipCoreItem; position, velocity, and owner set externally. */
    public MissileEntity(Level level, MissileType type, float damage, float armorPen,
                          float explosionPower, String displayItemId) {
        super(ModEntityTypes.MISSILE_ENTITY.get(), level);
        this.missileType = type;
        this.damage = damage;
        this.armorPen = armorPen;
        this.explosionPower = explosionPower;
        this.currentSpeed = type.initialSpeed;
        this.displayItemId = displayItemId;
    }

    /** 手动指定追踪目标，跳过自动搜索逻辑。传 null 仅清目标、恢复自动搜索。 */
    public void setTrackedTarget(Entity target) {
        this.trackedTarget = target;
        this.trackedTargetUUID = target != null ? target.getUUID() : null;
        this.manualTarget = (target != null);
    }

    @Override
    protected Item getDefaultItem() {
        if (cachedDisplayItem != null) return cachedDisplayItem;
        cachedDisplayItem = resolveDisplayItem();
        return cachedDisplayItem;
    }

    private Item resolveDisplayItem() {
        if (displayItemId != null && !displayItemId.isEmpty()) {
            var rl = ResourceLocation.tryParse(displayItemId);
            if (rl != null) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(rl);
                if (item.isPresent()) return item.get();
            }
        }
        return ModItems.SY1_MISSILE.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0; // 导弹不受重力影响
    }

    @Override
    public boolean isCurrentlyGlowing() {
        if (level().isClientSide() && com.piranport.ClientTickHandler.isHighlightEnabled()) {
            return true;
        }
        return super.isCurrentlyGlowing();
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;

        // 超时消失
        if (tickCount > MAX_LIFETIME) {
            if (!level().isClientSide()) discard();
            return;
        }

        if (!level().isClientSide()) {
            // 加速
            currentSpeed = Math.min(currentSpeed + missileType.speedIncrement, missileType.maxSpeed);

            Vec3 motion = getDeltaMovement();

            if (missileType == MissileType.ROCKET) {
                // 火箭弹：直线飞行，维持速度
                double totalSpeed = motion.length();
                if (totalSpeed > 0.001) {
                    double scale = currentSpeed / totalSpeed;
                    setDeltaMovement(motion.x * scale, motion.y * scale, motion.z * scale);
                }
            } else {
                // 反舰/防空导弹：制导追踪（目标缓存，每5tick重新搜索）
                // Restore target from UUID after chunk reload / server restart
                if (trackedTarget == null && trackedTargetUUID != null
                        && level() instanceof ServerLevel sl) {
                    trackedTarget = sl.getEntity(trackedTargetUUID);
                    if (trackedTarget == null) {
                        trackedTargetUUID = null;
                    }
                }
                if (trackedTarget != null && (!trackedTarget.isAlive() || trackedTarget.isRemoved())) {
                    trackedTarget = null;
                    trackedTargetUUID = null;
                }
                // 手动制导导弹丢失目标后维持直线飞行，不自动重新搜索（与玩家锁定语义一致）
                if (trackedTarget == null && !manualTarget && --targetSearchCooldown <= 0) {
                    trackedTarget = findTarget();
                    targetSearchCooldown = 5;
                }
                if (trackedTarget != null) {
                    homeToward(trackedTarget);
                } else {
                    // 无目标：维持方向和速度
                    double totalSpeed = motion.length();
                    if (totalSpeed > 0.001) {
                        double scale = currentSpeed / totalSpeed;
                        setDeltaMovement(motion.x * scale, motion.y * scale, motion.z * scale);
                    }
                }
            }
        }
    }

    /**
     * 寻找目标：优先火控锁定 → 最近敌对生物（防空优先空中单位）
     */
    private Entity findTarget() {
        Entity owner = getOwner();
        if (owner instanceof Player player && level() instanceof ServerLevel serverLevel) {
            // 1. 火控锁定目标
            List<UUID> fcTargets = FireControlManager.getTargets(player.getUUID());
            for (UUID targetUUID : fcTargets) {
                Entity target = serverLevel.getEntity(targetUUID);
                if (target != null && target.isAlive()) {
                    // 排除有容器的实体（箱子矿车等）
                    if (target instanceof net.minecraft.world.Container) continue;
                    // 反舰/防空导弹不锁定水下目标
                    if ((missileType == MissileType.ANTI_SHIP || missileType == MissileType.ANTI_AIR)
                            && target.isUnderWater()) continue;
                    return target;
                }
            }
        }

        // 2. 半径32格内最近敌对生物
        AABB searchBox = getBoundingBox().inflate(SEARCH_RANGE);
        Entity bestTarget = null;
        double bestDist = Double.MAX_VALUE;
        // 反舰/防空导弹统一剔除水下目标
        boolean excludeUnderwater = missileType == MissileType.ANTI_SHIP
                || missileType == MissileType.ANTI_AIR;
        boolean antiAirOnlyFlying = missileType == MissileType.ANTI_AIR;

        for (Entity e : level().getEntities(this, searchBox, e -> {
            if (e == getOwner()) return false;
            if (FriendlyFireHelper.shouldBlockHit(e, getOwner())) return false;
            // Phantom extends FlyingMob (not Monster) — use Enemy interface to cover all hostile mobs.
            if (!(e instanceof Enemy)) return false;
            if (e instanceof net.minecraft.world.Container) return false;
            if (!e.isAlive() || !e.isPickable()) return false;
            if (excludeUnderwater && e.isUnderWater()) return false;
            // 防空导弹自动锁定仅限飞行目标，地面目标需手动火控锁定
            if (antiAirOnlyFlying && e.onGround()) return false;
            return true;
        })) {
            double dist = distanceTo(e);
            if (dist > SEARCH_RANGE) continue;
            if (dist < bestDist) {
                bestTarget = e;
                bestDist = dist;
            }
        }
        return bestTarget;
    }

    /**
     * 向目标追踪转向，每tick最多转 MAX_TURN_DEG 度。
     */
    private void homeToward(Entity target) {
        Vec3 motion = getDeltaMovement();
        Vec3 toTarget = target.position().add(0, target.getBbHeight() / 2, 0).subtract(position());
        double dist = toTarget.length();
        if (dist < 0.001) return;

        Vec3 targetDir = toTarget.normalize();
        Vec3 currentDir = motion.length() > 0.001 ? motion.normalize() : targetDir;

        double dot = currentDir.dot(targetDir);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        double angleBetween = Math.acos(dot);
        double maxTurnRad = Math.toRadians(MAX_TURN_DEG);

        Vec3 newDir;
        if (angleBetween <= maxTurnRad) {
            newDir = targetDir;
        } else {
            double t = maxTurnRad / angleBetween;
            newDir = new Vec3(
                    currentDir.x + (targetDir.x - currentDir.x) * t,
                    currentDir.y + (targetDir.y - currentDir.y) * t,
                    currentDir.z + (targetDir.z - currentDir.z) * t
            ).normalize();
        }

        setDeltaMovement(newDir.x * currentSpeed, newDir.y * currentSpeed, newDir.z * currentSpeed);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (FriendlyFireHelper.shouldBlockHit(target, getOwner())) return false;
        // 与 findTarget 一致：导弹飞行路径上不应被箱子矿车/船等容器实体拦截
        if (target instanceof net.minecraft.world.Container) return false;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide()) {
            Entity target = result.getEntity();

            if (missileType == MissileType.ANTI_SHIP) {
                // 反舰导弹：直接伤害 + 穿甲
                if (armorPen > 0 && target instanceof LivingEntity living) {
                    AttributeInstance armorAttr = living.getAttribute(Attributes.ARMOR);
                    // 每次命中用 entity UUID 生成唯一 ID，避免同 tick 多枚导弹命中同一目标时
                    // addTransientModifier 因 ID 重复抛 IllegalArgumentException
                    ResourceLocation apId = ResourceLocation.fromNamespaceAndPath(
                            PiranPort.MOD_ID, AP_PEN_PATH_PREFIX + getUUID());
                    boolean applied = false;
                    if (armorAttr != null) {
                        armorAttr.removeModifier(apId);
                        armorAttr.addTransientModifier(new AttributeModifier(
                                apId, -armorPen,
                                AttributeModifier.Operation.ADD_VALUE));
                        applied = true;
                    }
                    try {
                        living.hurt(damageSources().thrown(this, getOwner()), damage);
                    } finally {
                        if (applied) {
                            armorAttr.removeModifier(apId);
                        }
                    }
                } else {
                    target.hurt(damageSources().thrown(this, getOwner()), damage);
                }
            } else {
                // 防空导弹 / 火箭弹：爆炸
                target.hurt(damageSources().explosion(this, getOwner()), damage);
                Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                        ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
                level().explode(this, getX(), getY(), getZ(), explosionPower, interaction);
            }

            notifyOwner(target);
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide()) {
            if (missileType != MissileType.ANTI_SHIP) {
                // 防空导弹 / 火箭弹：碰到方块爆炸
                Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                        ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
                level().explode(this, getX(), getY(), getZ(), explosionPower, interaction);
            }
            notifyOwnerMiss();
            discard();
        }
    }

    private void notifyOwner(Entity target) {
        Entity owner = getOwner();
        if (!(owner instanceof Player player)) return;
        Component weaponName = getDefaultItem().getDescription();
        String key = target.isAlive() ? "message.piranport.weapon_hit" : "message.piranport.weapon_kill";
        player.sendSystemMessage(Component.translatable(key, weaponName, target.getDisplayName()));
    }

    private void notifyOwnerMiss() {
        Entity owner = getOwner();
        if (!(owner instanceof Player player)) return;
        Component weaponName = getDefaultItem().getDescription();
        player.sendSystemMessage(Component.translatable("message.piranport.weapon_miss", weaponName));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // 写 name() 以避免枚举重排/新增时旧存档读成错误类型
        tag.putString("MissileType", missileType.name());
        tag.putFloat("MissileDamage", damage);
        tag.putFloat("ArmorPen", armorPen);
        tag.putFloat("ExplosionPower", explosionPower);
        tag.putFloat("CurrentSpeed", currentSpeed);
        tag.putString("DisplayItemId", displayItemId);
        tag.putBoolean("ManualTarget", manualTarget);
        if (trackedTargetUUID != null) {
            tag.putUUID("TrackedTargetUUID", trackedTargetUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        missileType = readMissileType(tag);
        damage = tag.getFloat("MissileDamage");
        armorPen = tag.getFloat("ArmorPen");
        explosionPower = tag.getFloat("ExplosionPower");
        currentSpeed = tag.getFloat("CurrentSpeed");
        if (currentSpeed <= 0) currentSpeed = missileType.initialSpeed;
        displayItemId = tag.getString("DisplayItemId");
        cachedDisplayItem = null;
        manualTarget = tag.getBoolean("ManualTarget");
        if (tag.hasUUID("TrackedTargetUUID")) {
            trackedTargetUUID = tag.getUUID("TrackedTargetUUID");
        }
    }

    private static MissileType readMissileType(CompoundTag tag) {
        // 新存档（字符串 name）
        if (tag.contains("MissileType", net.minecraft.nbt.Tag.TAG_STRING)) {
            String name = tag.getString("MissileType");
            try {
                return MissileType.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                return MissileType.ANTI_SHIP;
            }
        }
        // 旧存档兼容（int ordinal）
        if (tag.contains("MissileType", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            int ord = tag.getInt("MissileType");
            MissileType[] vals = MissileType.values();
            if (ord >= 0 && ord < vals.length) return vals[ord];
        }
        return MissileType.ANTI_SHIP;
    }
}
