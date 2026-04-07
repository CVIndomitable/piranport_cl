package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 深海轻巡 — Medium cannons + torpedoes.
 * HP:50 Armor:8 Speed:1.2 Detection:32 Orbit:16
 */
public class DeepOceanLightCruiserEntity extends AbstractDeepOceanEntity {

    public DeepOceanLightCruiserEntity(EntityType<? extends DeepOceanLightCruiserEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ARMOR, 8.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new OrbitTargetGoal(this, 0.12));
        this.goalSelector.addGoal(2, new CannonAttackGoal(this));
        this.goalSelector.addGoal(2, new TorpedoAttackGoal(this));
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.8, 32));
        this.goalSelector.addGoal(6, new FleetAlertGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    @Override public double getOrbitDistance() { return 16.0; }
    @Override public int getFireInterval() { return 80; }
    @Override public float getShellDamage() { return 5.0f; }
    @Override public float getExplosionPower() { return 1.5f; }
    @Override public int getTrackingIntervalMin() { return 3; }
    @Override public int getTrackingIntervalMax() { return 6; }
    @Override public boolean canUseTorpedoes() { return true; }
    @Override public float getTorpedoDamage() { return 10.0f; }
}
