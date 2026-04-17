package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.CannonAttackGoal;
import com.piranport.npc.ai.goal.FleetAlertGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import com.piranport.npc.ai.goal.OrbitTargetGoal;
import com.piranport.npc.ai.goal.TorpedoAttackGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 深海重巡 — Large cannons + torpedoes.
 * HP:80 Armor:12 Speed:1.0 Detection:32 Orbit:20
 */
public class DeepOceanHeavyCruiserEntity extends AbstractDeepOceanEntity {

    public DeepOceanHeavyCruiserEntity(EntityType<? extends DeepOceanHeavyCruiserEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ARMOR, 12.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new OrbitTargetGoal(this, 0.10));
        this.goalSelector.addGoal(2, new CannonAttackGoal(this));
        this.goalSelector.addGoal(2, new TorpedoAttackGoal(this));
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.7, 32));
        this.goalSelector.addGoal(6, new FleetAlertGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    @Override public double getOrbitDistance() { return 20.0; }
    @Override public int getFireInterval() { return 80; }
    @Override public float getShellDamage() { return 7.0f; }
    @Override public float getExplosionPower() { return 2.0f; }
    @Override public int getTrackingIntervalMin() { return 3; }
    @Override public int getTrackingIntervalMax() { return 5; }
    @Override public boolean canUseTorpedoes() { return true; }
    @Override public float getTorpedoDamage() { return 12.0f; }
}
