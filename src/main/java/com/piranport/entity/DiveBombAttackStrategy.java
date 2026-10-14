package com.piranport.entity;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.AttackStrategy;
import com.piranport.config.ModEquipmentConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 俯冲轰炸攻击策略（profile=dive_bomb）。 */
public class DiveBombAttackStrategy implements AttackStrategy {
    @Override public String id() { return AircraftDefinition.AttackProfile.DIVE_BOMB.id(); }
    @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.DIVE_BOMB; }

    @Override
    public void execute(AircraftEntity craft, Player owner) {
        boolean canFallback = craft.stateTicks >= ModEquipmentConfig.AIRCRAFT_ATTACK_FALLBACK_TICKS.get();
        LivingEntity target = AircraftCombat.resolveTarget(craft, owner);
        if (target == null) {
            if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
            return;
        }
        AircraftCombat.tickDiveBomberAttack(craft, owner, target);
    }

    @Override
    public void executeAutonomous(AircraftEntity craft) {
        LivingEntity t = craft.autonomousTarget;
        if (t == null) { craft.startReturning("target_lost"); return; }
        AircraftCombat.tickDiveBomberAttack(craft, null, t);
    }
}
