package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.FleetAlertGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import com.piranport.npc.ai.goal.SubmergeGoal;
import com.piranport.npc.ai.goal.TorpedoAttackGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * 深海潜艇 — Submerges in combat, invisible, torpedoes only.
 * HP:40 Armor:2 Speed:0.8 Detection:24 Orbit:N/A (uses SubmergeGoal)
 */
public class DeepOceanSubmarineEntity extends AbstractDeepOceanEntity {

    public DeepOceanSubmarineEntity(EntityType<? extends DeepOceanSubmarineEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SubmergeGoal(this));
        this.goalSelector.addGoal(2, new TorpedoAttackGoal(this));
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.6, 32));
        this.goalSelector.addGoal(6, new FleetAlertGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    @Override
    protected boolean canSubmerge() {
        return true;
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        if (type == NeoForgeMod.WATER_TYPE.value()) return false;
        return super.canDrownInFluidType(type);
    }

    @Override public boolean canUseTorpedoes() { return true; }
    @Override public float getTorpedoDamage() { return 12.0f; }
}
