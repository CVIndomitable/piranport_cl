package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.CannonAttackGoal;
import com.piranport.npc.ai.goal.FleetAlertGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import com.piranport.npc.ai.goal.OrbitTargetGoal;
import com.piranport.entity.CannonProjectileEntity;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Foundation boss for the abyssal world: a heavy flagship using deep-ocean fleet AI. */
public class DeepOceanFlagshipEntity extends AbstractDeepOceanEntity {
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(DeepOceanFlagshipEntity.class, EntityDataSerializers.INT);

    private int phase = 1;
    private int specialAttackCooldown = 100;
    private int shockwaveCooldown = 80;

    public DeepOceanFlagshipEntity(EntityType<? extends DeepOceanFlagshipEntity> type, Level level) {
        super(type, level);
        this.xpReward = 80;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 320.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 26.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 1);
    }

    public int getPhase() {
        return entityData.get(DATA_PHASE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new OrbitTargetGoal(this, 0.075));
        this.goalSelector.addGoal(2, new CannonAttackGoal(this));
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.45, 40));
        this.goalSelector.addGoal(6, new FleetAlertGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    @Override public double getOrbitDistance() { return 34.0; }
    @Override public int getFireInterval() { return phase >= 3 ? 50 : (phase >= 2 ? 65 : 80); }
    @Override public float getShellDamage() { return phase >= 3 ? 20.0f : 16.0f; }
    @Override public float getExplosionPower() { return phase >= 3 ? 5.5f : (phase >= 2 ? 4.6f : 4.0f); }
    @Override public int getTrackingIntervalMin() { return 1; }
    @Override public int getTrackingIntervalMax() { return 3; }
    @Override public boolean canUseTorpedoes() { return true; }
    @Override public float getTorpedoDamage() { return phase >= 3 ? 22.0f : 18.0f; }
    @Override public boolean canLaunchAircraft() { return true; }
    @Override public int getMaxAircraft() { return phase >= 3 ? 6 : (phase >= 2 ? 5 : 4); }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel serverLevel) {
            updateBossPhase();
            tickSpecialAttacks(serverLevel);
        } else if (isAlive() && getPhase() >= 2 && tickCount % 12 == 0) {
            level().addParticle(getPhase() >= 3 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.WITCH,
                    getX() + (random.nextDouble() - 0.5) * getBbWidth(),
                    getY() + 1.2 + random.nextDouble() * getBbHeight() * 0.4,
                    getZ() + (random.nextDouble() - 0.5) * getBbWidth(),
                    0.0, 0.02, 0.0);
        }
    }

    private void tickSpecialAttacks(ServerLevel serverLevel) {
        if (!isAlive()) return;
        if (specialAttackCooldown > 0) specialAttackCooldown--;
        if (shockwaveCooldown > 0) shockwaveCooldown--;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;

        if (phase >= 2 && specialAttackCooldown <= 0) {
            fireAbyssalBarrage(serverLevel, target);
            specialAttackCooldown = phase >= 3 ? 85 : 120;
        }

        if (phase >= 3 && shockwaveCooldown <= 0 && distanceToSqr(target) <= 100.0) {
            emitCloseRangeShockwave(serverLevel);
            shockwaveCooldown = 90;
        }
    }

    private void fireAbyssalBarrage(ServerLevel serverLevel, LivingEntity target) {
        int shellCount = phase >= 3 ? 6 : 4;
        float damage = phase >= 3 ? 16.0f : 12.0f;
        float explosion = phase >= 3 ? 4.8f : 3.8f;

        for (int i = 0; i < shellCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 1.5 + random.nextDouble() * (phase >= 3 ? 5.5 : 4.0);
            Vec3 impact = target.position().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);

            serverLevel.sendParticles(ParticleTypes.SOUL,
                    impact.x, target.getY() + 0.15, impact.z,
                    10, 0.35, 0.05, 0.35, 0.02);
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    impact.x, target.getY() + 0.1, impact.z,
                    8, 0.45, 0.05, 0.45, 0.08);

            CannonProjectileEntity shell = new CannonProjectileEntity(
                    serverLevel, this, new ItemStack(ModItems.LARGE_HE_SHELL.get()), damage, true, explosion);
            shell.setDragCoeff(0.002f);
            shell.setCustomGravity(18.0f);
            shell.setPos(impact.x, target.getY() + 14.0 + random.nextDouble() * 4.0, impact.z);
            shell.shoot(0.0, -1.0, 0.0, phase >= 3 ? 1.35f : 1.15f, 0.02f);
            serverLevel.addFreshEntity(shell);
        }

        level().playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE,
                phase >= 3 ? 1.8f : 1.3f, 0.75f);
        if (target instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.piranport.deep_ocean_flagship_barrage"), true);
        }
    }

    private void emitCloseRangeShockwave(ServerLevel serverLevel) {
        serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 1.0, getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
        serverLevel.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.2, getZ(),
                60, 2.8, 0.35, 2.8, 0.18);
        level().playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE,
                1.5f, 0.8f);

        for (Player player : serverLevel.getEntitiesOfClass(Player.class, getBoundingBox().inflate(9.0),
                p -> p.isAlive() && !p.isSpectator())) {
            double dist = Math.max(1.0, distanceTo(player));
            float damage = (float) Math.max(4.0, 11.0 - dist);
            player.hurt(damageSources().mobAttack(this), damage);
            Vec3 away = player.position().subtract(position());
            if (away.lengthSqr() < 0.001) {
                away = new Vec3(random.nextDouble() - 0.5, 0.0, random.nextDouble() - 0.5);
            }
            away = away.normalize();
            player.push(away.x * 1.15, 0.35, away.z * 1.15);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 3, 1));
            player.displayClientMessage(Component.translatable("message.piranport.deep_ocean_flagship_shockwave"), true);
        }
    }

    private void updateBossPhase() {
        if (!isAlive() || getMaxHealth() <= 0.0f) return;
        float healthRatio = getHealth() / getMaxHealth();
        int nextPhase = healthRatio <= 0.35f ? 3 : (healthRatio <= 0.65f ? 2 : 1);
        if (nextPhase > phase) {
            enterPhase(nextPhase);
        }
    }

    private void enterPhase(int nextPhase) {
        phase = nextPhase;
        entityData.set(DATA_PHASE, nextPhase);
        if (!(level() instanceof ServerLevel serverLevel)) return;

        addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 8, nextPhase >= 3 ? 1 : 0));
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 15, nextPhase >= 3 ? 1 : 0));
        if (nextPhase >= 3) {
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 20, 1));
            addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 5, 1));
        }

        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.2, getZ(),
                nextPhase >= 3 ? 80 : 45, 1.2, 0.8, 1.2, 0.05);
        serverLevel.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.2, getZ(),
                nextPhase >= 3 ? 70 : 35, 1.6, 0.2, 1.6, 0.2);
        level().playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE,
                nextPhase >= 3 ? 2.0f : 1.4f, nextPhase >= 3 ? 0.75f : 0.9f);

        summonEscortWave(serverLevel, nextPhase);
        if (getTarget() instanceof Player player) {
            player.displayClientMessage(Component.translatable(
                    "message.piranport.deep_ocean_flagship_phase_" + nextPhase), true);
        }
    }

    private void summonEscortWave(ServerLevel serverLevel, int nextPhase) {
        if (nextPhase == 2) {
            summonEscort(serverLevel, ModEntityTypes.DEEP_OCEAN_DESTROYER.get(), 0.0, 5.0);
            summonEscort(serverLevel, ModEntityTypes.DEEP_OCEAN_DESTROYER.get(), Math.PI, 5.0);
            summonEscort(serverLevel, ModEntityTypes.DEEP_OCEAN_LIGHT_CRUISER.get(), Math.PI / 2.0, 7.0);
        } else if (nextPhase >= 3) {
            summonEscort(serverLevel, ModEntityTypes.DEEP_OCEAN_HEAVY_CRUISER.get(), 0.0, 7.0);
            summonEscort(serverLevel, ModEntityTypes.DEEP_OCEAN_DESTROYER.get(), Math.PI * 0.66, 6.0);
            summonEscort(serverLevel, ModEntityTypes.DEEP_OCEAN_DESTROYER.get(), Math.PI * 1.33, 6.0);
            summonEscort(serverLevel, ModEntityTypes.DEEP_OCEAN_LIGHT_CARRIER.get(), Math.PI, 8.0);
        }
    }

    private void summonEscort(ServerLevel serverLevel,
                              EntityType<? extends AbstractDeepOceanEntity> type,
                              double angle, double radius) {
        AbstractDeepOceanEntity escort = type.create(serverLevel);
        if (escort == null) return;
        double x = getX() + Math.cos(angle) * radius;
        double z = getZ() + Math.sin(angle) * radius;
        escort.moveTo(x, getY(), z, getYRot(), 0.0f);
        escort.setFleetGroupId(getFleetGroupId());
        escort.setTarget(getTarget());
        serverLevel.addFreshEntity(escort);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FlagshipPhase", phase);
        tag.putInt("SpecialAttackCooldown", specialAttackCooldown);
        tag.putInt("ShockwaveCooldown", shockwaveCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FlagshipPhase")) {
            phase = Math.max(1, Math.min(3, tag.getInt("FlagshipPhase")));
        }
        entityData.set(DATA_PHASE, phase);
        if (tag.contains("SpecialAttackCooldown")) {
            specialAttackCooldown = Math.max(0, tag.getInt("SpecialAttackCooldown"));
        }
        if (tag.contains("ShockwaveCooldown")) {
            shockwaveCooldown = Math.max(0, tag.getInt("ShockwaveCooldown"));
        }
    }
}
