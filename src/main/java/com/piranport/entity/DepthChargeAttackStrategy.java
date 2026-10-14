package com.piranport.entity;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.AttackStrategy;
import com.piranport.config.ModEquipmentConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 反潜深弹攻击策略（profile=depth_charge），委托 AircraftAswRecon。 */
public class DepthChargeAttackStrategy implements AttackStrategy {
    @Override public String id() { return AircraftDefinition.AttackProfile.DEPTH_CHARGE.id(); }
    @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.DEPTH_CHARGE; }

    @Override
    public void execute(AircraftEntity craft, Player owner) {
        boolean canFallback = craft.stateTicks >= ModEquipmentConfig.AIRCRAFT_ATTACK_FALLBACK_TICKS.get();
        LivingEntity target = AircraftAswRecon.resolveASWTarget(craft, owner);
        if (target == null) {
            if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
            return;
        }
        AircraftAswRecon.tickASWAttack(craft, owner, target);
    }

    @Override
    public void executeAutonomous(AircraftEntity craft) {
        LivingEntity t = craft.autonomousTarget;
        if (t == null) { craft.startReturning("target_lost"); return; }
        AircraftAswRecon.tickASWAttack(craft, null, t);
    }
}
