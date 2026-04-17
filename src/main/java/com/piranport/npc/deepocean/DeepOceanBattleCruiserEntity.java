package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.CannonAttackGoal;
import com.piranport.npc.ai.goal.FleetAlertGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import com.piranport.npc.ai.goal.OrbitTargetGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 深海战巡 — Large cannons, high tracking frequency, no torpedoes.
 * HP:100 Armor:16 Speed:1.1 Detection:40 Orbit:24
 */
public class DeepOceanBattleCruiserEntity extends AbstractDeepOceanEntity {

    public DeepOceanBattleCruiserEntity(EntityType<? extends DeepOceanBattleCruiserEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.275)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.ARMOR, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new OrbitTargetGoal(this, 0.11));
        this.goalSelector.addGoal(2, new CannonAttackGoal(this));
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.7, 32));
        this.goalSelector.addGoal(6, new FleetAlertGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    @Override public double getOrbitDistance() { return 24.0; }
    @Override public int getFireInterval() { return 70; }
    @Override public float getShellDamage() { return 8.0f; }
    @Override public float getExplosionPower() { return 2.5f; }
    @Override public int getTrackingIntervalMin() { return 2; } // More frequent tracking
    @Override public int getTrackingIntervalMax() { return 4; }
}
