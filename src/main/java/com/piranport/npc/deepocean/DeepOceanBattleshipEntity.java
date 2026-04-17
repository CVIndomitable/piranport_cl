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
 * 深海战列 — Super heavy cannons, long range, high arc bombardment.
 * HP:150 Armor:20 Speed:0.8 Detection:48 Orbit:28
 */
public class DeepOceanBattleshipEntity extends AbstractDeepOceanEntity {

    public DeepOceanBattleshipEntity(EntityType<? extends DeepOceanBattleshipEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new OrbitTargetGoal(this, 0.08));
        this.goalSelector.addGoal(2, new CannonAttackGoal(this));
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.6, 32));
        this.goalSelector.addGoal(6, new FleetAlertGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    @Override public double getOrbitDistance() { return 28.0; }
    @Override public int getFireInterval() { return 120; } // Slow but powerful
    @Override public float getShellDamage() { return 12.0f; }
    @Override public float getExplosionPower() { return 3.0f; }
    @Override public int getTrackingIntervalMin() { return 2; }
    @Override public int getTrackingIntervalMax() { return 4; }
}
