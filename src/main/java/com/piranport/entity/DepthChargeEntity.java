package com.piranport.entity;

import com.piranport.config.ModCommonConfig;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

/**
 * 深水炸弹 — 从飞机投下或由深弹投射器发射，重力较大，入水后继续下沉。
 * 检测到附近实体时引爆，爆炸后对该高度水平范围内的实体造成无视护甲的魔法伤害。
 */
public class DepthChargeEntity extends ThrowableItemProjectile {

    private float damage = 14f;
    private float explosionPower = 3.0f;
    private boolean detonated = false;
    private static final int MAX_LIFETIME = 600; // 30 seconds

    /** 近炸引信起爆前的安全延迟（ticks）。 */
    private static final int ARM_TICKS = 5;
    /** 近炸检测距离（blocks）。 */
    private static final double DETECT_RANGE = 5.0;
    /** 爆炸后水平伤害半径（blocks）。 */
    private static final double BLAST_RADIUS = 8.0;
    /** 爆炸后垂直伤害容差（blocks，上下各此值）。 */
    private static final double BLAST_HEIGHT = 4.0;

    /** Required by entity type registration. */
    public DepthChargeEntity(EntityType<? extends DepthChargeEntity> type, Level level) {
        super(type, level);
    }

    /** Spawned by AircraftEntity; position and velocity set externally after construction. */
    public DepthChargeEntity(Level level, float damage, float explosionPower) {
        super(ModEntityTypes.DEPTH_CHARGE.get(), level);
        this.damage = damage;
        this.explosionPower = explosionPower;
    }

    /** Spawned by DepthChargeLauncherItem via ShipCoreItem firing. */
    public DepthChargeEntity(Level level, LivingEntity shooter, float damage, float explosionPower) {
        super(ModEntityTypes.DEPTH_CHARGE.get(), shooter, level);
        this.damage = damage;
        this.explosionPower = explosionPower;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.DEPTH_CHARGE.get();
    }

    @Override
    public boolean isCurrentlyGlowing() {
        if (level().isClientSide() && com.piranport.ClientTickHandler.isHighlightEnabled()) {
            return true;
        }
        return super.isCurrentlyGlowing();
    }

    /** Heavy — sinks faster than aerial bombs. */
    @Override
    protected double getDefaultGravity() {
        return 0.08;
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved() || detonated) return;

        if (!level().isClientSide()) {
            // 超时消失
            if (tickCount > MAX_LIFETIME) {
                discard();
                return;
            }
            // 近炸检测：安全延迟后扫描附近实体
            if (tickCount > ARM_TICKS) {
                checkProximity();
            }
        }
    }

    /** 扫描检测范围内的存活实体，发现目标后引爆。 */
    private void checkProximity() {
        AABB searchBox = getBoundingBox().inflate(DETECT_RANGE);
        List<Entity> nearby = level().getEntities(this, searchBox, e -> {
            if (e == getOwner()) return false;
            if (com.piranport.combat.FriendlyFireHelper.shouldBlockHit(e, getOwner())) return false;
            return e.isAlive() && e.isPickable() && e instanceof LivingEntity;
        });
        if (!nearby.isEmpty()) {
            detonate();
        }
    }

    /**
     * 引爆：对该高度水平范围内的实体造成无视护甲的魔法伤害 + 视觉爆炸效果。
     * 伤害范围为以爆炸点为中心、BLAST_RADIUS 水平半径、±BLAST_HEIGHT 垂直容差的扁平圆柱。
     */
    private void detonate() {
        if (detonated) return;
        detonated = true;

        double cx = getX(), cy = getY(), cz = getZ();

        // 区域魔法伤害（无视护甲）
        AABB damageBox = new AABB(
                cx - BLAST_RADIUS, cy - BLAST_HEIGHT, cz - BLAST_RADIUS,
                cx + BLAST_RADIUS, cy + BLAST_HEIGHT, cz + BLAST_RADIUS);
        List<Entity> targets = level().getEntities(this, damageBox, e -> {
            if (e == getOwner()) return false;
            if (com.piranport.combat.FriendlyFireHelper.shouldBlockHit(e, getOwner())) return false;
            return e.isAlive() && e instanceof LivingEntity;
        });
        for (Entity target : targets) {
            // 距离衰减：中心全额伤害，边缘半额
            double dist = Math.sqrt(
                    (target.getX() - cx) * (target.getX() - cx) +
                    (target.getZ() - cz) * (target.getZ() - cz));
            float ratio = 1.0f - (float) (dist / BLAST_RADIUS) * 0.5f;
            // 多枚深弹齐投：重置无敌帧保证每枚都造成伤害
            target.invulnerableTime = 0;
            target.hurt(damageSources().indirectMagic(this, getOwner()), damage * Math.max(ratio, 0.5f));
            notifyOwner(target);
        }

        // 视觉爆炸（不破坏方块）
        level().explode(this, cx, cy, cz, explosionPower, Level.ExplosionInteraction.NONE);
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
        if (!level().isClientSide() && !detonated) {
            detonate();
        }
    }

    private void notifyOwner(Entity target) {
        Entity owner = getOwner();
        if (!(owner instanceof Player player)) return;
        Component weaponName = getDefaultItem().getDescription();
        String key = target.isAlive() ? "message.piranport.weapon_hit" : "message.piranport.weapon_kill";
        player.sendSystemMessage(Component.translatable(key, weaponName, target.getDisplayName()));
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!level().isClientSide() && !detonated) {
            detonate();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("ChargeDamage", damage);
        tag.putFloat("ExplosionPower", explosionPower);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ChargeDamage")) damage = tag.getFloat("ChargeDamage");
        if (tag.contains("ExplosionPower")) explosionPower = tag.getFloat("ExplosionPower");
    }
}
