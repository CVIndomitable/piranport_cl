package com.piranport.entity;

import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.UUID;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class BulletEntity extends ThrowableItemProjectile {

    private float damage = 2f;
    private Component sourceAircraftName;
    private UUID sourceAircraftUuid;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
    }

    public BulletEntity(Level level, float damage) {
        super(ModEntityTypes.BULLET.get(), level);
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PROJECTILE_BULLET.get();
    }

    private static final int MAX_LIFETIME = 600; // 30 seconds

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > MAX_LIFETIME) discard();

        // Phase 10: 弹头尾迹粒子
        if (level().isClientSide && tickCount % 2 == 0 && !isRemoved()) {
            Vec3 pos = position();
            Vec3 v = getDeltaMovement();
            if (v.length() > 0.1) {
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        pos.x, pos.y, pos.z,
                        -v.x * 0.05 + random.nextGaussian() * 0.01,
                        -v.y * 0.05 + random.nextGaussian() * 0.01,
                        -v.z * 0.05 + random.nextGaussian() * 0.01);
            }
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.005;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (com.piranport.combat.FriendlyFireHelper.shouldBlockHit(target, getOwner())) return false;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide()) {
            Entity target = result.getEntity();

            // Use the aircraft as the direct source if available, otherwise use owner
            Entity directSource = this;
            if (sourceAircraftUuid != null && level() instanceof net.minecraft.server.level.ServerLevel sl) {
                Entity aircraft = sl.getEntity(sourceAircraftUuid);
                if (aircraft != null) directSource = aircraft;
            }
            target.hurt(damageSources().thrown(directSource, getOwner()), damage);

            // 显式设置最后受伤来源，使敌对生物进入仇恨状态
            if (target instanceof LivingEntity living) {
                Entity ownerEntity = getOwner();
                if (ownerEntity instanceof LivingEntity lo) {
                    living.setLastHurtByMob(lo);
                } else if (ownerEntity instanceof AircraftEntity ac && ac.getOwner() instanceof LivingEntity lo) {
                    living.setLastHurtByMob(lo);
                }
            }

            notifyOwner(target);
            discard();
        }
    }

    public void setSourceAircraftName(Component name) {
        this.sourceAircraftName = name;
    }

    public void setSourceAircraft(Entity aircraft) {
        this.sourceAircraftUuid = aircraft.getUUID();
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
        super.onHitBlock(result);
        if (!level().isClientSide()) {
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("BulletDamage", damage);
        if (sourceAircraftName != null) {
            tag.putString("SourceAircraftName",
                    Component.Serializer.toJson(sourceAircraftName, registryAccess()));
        }
        if (sourceAircraftUuid != null) {
            tag.putUUID("SourceAircraftUUID", sourceAircraftUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BulletDamage")) damage = tag.getFloat("BulletDamage");
        if (tag.hasUUID("SourceAircraftUUID")) {
            sourceAircraftUuid = tag.getUUID("SourceAircraftUUID");
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
    }
}
