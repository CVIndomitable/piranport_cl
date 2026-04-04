package com.piranport.entity;

import com.piranport.PiranPort;
import com.piranport.config.ModCommonConfig;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    /** Prevents VT proximity detonation + onHit from double-exploding in the same tick. */
    private boolean exploded = false;

    /** Tracking (homing) shell: gently steers toward target each tick. */
    private int trackingTargetId = -1;
    /** Blend factor: 0.05 = ~5% correction per tick (gentle curve). */
    private static final double TRACKING_STEER = 0.05;

    /** Cone half-angle for VT proximity detection (30°). */
    private static final double VT_CONE_COS = Math.cos(Math.toRadians(30));
    /** Max search range for entities in the cone (blocks). */
    private static final double VT_DETECT_RANGE = 5.0;
    /** Distance at which VT detonates against entities (blocks). */
    private static final double VT_DETONATE_DIST = 3.0;
    /** Forward raycast length for block proximity detection (blocks). */
    private static final double VT_BLOCK_RANGE = 3.0;
    /** Grace period after launch before VT fuze arms (ticks). */
    private static final int VT_ARM_TICKS = 5;

    // Required constructor for entity type registration
    public CannonProjectileEntity(EntityType<? extends CannonProjectileEntity> type, Level level) {
        super(type, level);
    }

    // Constructor for firing
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

    /** Enable tracking toward a specific entity (by runtime entity ID). */
    public void setTracking(int entityId) {
        this.trackingTargetId = entityId;
    }

    @Override
    public void shootFromRotation(Entity shooter, float xRot, float yRot,
                                   float zRot, float speed, float inaccuracy) {
        super.shootFromRotation(shooter, xRot, yRot, zRot, speed, inaccuracy);
        this.initialSpeed = speed;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (isVT && tickCount > VT_ARM_TICKS) {
                checkProximityFuze();
            }
            if (trackingTargetId >= 0) {
                tickTracking();
            }
        }
    }

    /** Gently steer toward the tracked target each tick. */
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

    private void checkProximityFuze() {
        Vec3 velocity = getDeltaMovement();
        if (velocity.lengthSqr() < 0.01) return;
        Vec3 forward = velocity.normalize();
        Vec3 pos = position();

        // --- Entity proximity check ---
        AABB searchBox = getBoundingBox().inflate(VT_DETECT_RANGE);
        List<Entity> nearby = level().getEntities(this, searchBox, e -> {
            if (e == getOwner()) return false;
            // Friendly fire protection: skip players when config disabled
            if (!ModCommonConfig.FRIENDLY_FIRE_ENABLED.get()
                    && e instanceof Player && getOwner() instanceof Player) return false;
            if (e instanceof AircraftEntity aircraft) {
                Entity owner = getOwner();
                if (owner instanceof Player p && p.getUUID().equals(aircraft.getOwnerUUID())) return false;
            }
            return e.isAlive() && e.isPickable();
        });

        for (Entity entity : nearby) {
            Vec3 toTarget = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(pos);
            double dist = toTarget.length();
            if (dist < 0.1 || dist > VT_DETECT_RANGE) continue;
            double dot = toTarget.normalize().dot(forward);
            if (dot >= VT_CONE_COS && dist <= VT_DETONATE_DIST) {
                proximityDetonate();
                return;
            }
        }

        // --- Block proximity check (short raycast ahead) ---
        Vec3 ahead = pos.add(forward.scale(VT_BLOCK_RANGE));
        BlockHitResult blockHit = level().clip(new ClipContext(
                pos, ahead, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            double blockDist = blockHit.getLocation().distanceTo(pos);
            if (blockDist <= VT_BLOCK_RANGE) {
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
            if (isHE) {
                // HE: direct hit damage + area explosion for splash
                target.hurt(damageSources().explosion(this, getOwner()), damage);
                Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                        ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
                level().explode(this, getX(), getY(), getZ(), explosionPower, interaction);
            } else {
                // AP: 130% base direct damage, velocity-decayed like vanilla arrows,
                // ignores 50% of target armor
                float currentSpeed = (float) getDeltaMovement().length();
                float speedRatio = initialSpeed > 0 ? currentSpeed / initialSpeed : 1.0f;
                float apDamage = damage * 1.3f * speedRatio;
                if (target instanceof LivingEntity living) {
                    AttributeInstance armorAttr = living.getAttribute(Attributes.ARMOR);
                    ResourceLocation apPenId = ResourceLocation.fromNamespaceAndPath(
                            PiranPort.MOD_ID, "ap_penetration");
                    if (armorAttr != null) {
                        double halfArmor = living.getAttributeValue(Attributes.ARMOR) * 0.5;
                        armorAttr.addTransientModifier(new AttributeModifier(
                                apPenId, -halfArmor, AttributeModifier.Operation.ADD_VALUE));
                    }
                    try {
                        living.hurt(damageSources().thrown(this, getOwner()), apDamage);
                    } finally {
                        if (armorAttr != null) {
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
        player.sendSystemMessage(Component.translatable(key, weaponName, target.getDisplayName()));
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide && !exploded) {
            if (isHE) {
                Level.ExplosionInteraction interaction = ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()
                        ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
                level().explode(this, getX(), getY(), getZ(), explosionPower, interaction);
            } else {
                // AP: destroy block and drop as item if resistance < obsidian;
                // no block destruction when AP is in water
                if (ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get() && !isInWater()) {
                    BlockPos pos = result.getBlockPos();
                    BlockState state = level().getBlockState(pos);
                    float obsidianResistance = Blocks.OBSIDIAN.getExplosionResistance();
                    if (state.getBlock().getExplosionResistance() < obsidianResistance) {
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
        return 0.05;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putBoolean("IsHE", isHE);
        tag.putBoolean("IsVT", isVT);
        tag.putFloat("ExplosionPower", explosionPower);
        tag.putFloat("InitialSpeed", initialSpeed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        if (damage <= 0) damage = 4.0f; // fallback: avoid zero-damage projectiles after world reload
        isHE = tag.getBoolean("IsHE");
        isVT = tag.getBoolean("IsVT");
        explosionPower = tag.getFloat("ExplosionPower");
        if (tag.contains("InitialSpeed")) {
            initialSpeed = tag.getFloat("InitialSpeed");
        }
    }
}
