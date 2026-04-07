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
 * 深海航母 — Fleet carrier, launches 4 aircraft (2 fighters + 2 attackers).
 * HP:80 Armor:8 Speed:0.9 Detection:64 Orbit:40
 */
public class DeepOceanCarrierEntity extends AbstractDeepOceanEntity {

    public DeepOceanCarrierEntity(EntityType<? extends DeepOceanCarrierEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.225)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ARMOR, 8.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new OrbitTargetGoal(this, 0.09));
        this.goalSelector.addGoal(2, new AircraftLaunchGoal(this));
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.6, 32));
        this.goalSelector.addGoal(6, new FleetAlertGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    @Override public double getOrbitDistance() { return 40.0; }
    @Override public boolean canLaunchAircraft() { return true; }
    @Override public int getMaxAircraft() { return 4; }
}
