package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.FleetAlertGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * 补给酱 — Non-combat supply entity. Does not attack. Drops resources on death.
 * HP:20 Armor:0 Speed:1.2 Detection:16
 */
public class DeepOceanSupplyEntity extends AbstractDeepOceanEntity {

    public DeepOceanSupplyEntity(EntityType<? extends DeepOceanSupplyEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)  // 1.2x relative speed
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
                .add(Attributes.ARMOR, 0.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new IdleWanderGoal(this, 0.8, 32));
        this.goalSelector.addGoal(5, new FleetAlertGoal(this));
        // No attack or target goals — supply ships don't fight
    }

    @Override
    public double getOrbitDistance() {
        return 0; // Does not orbit
    }
}
