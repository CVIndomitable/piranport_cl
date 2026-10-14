package com.piranport.entity;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.AttackStrategy;
import com.piranport.config.ModEquipmentConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 鱼雷机攻击策略（profile=torpedo）。 */
public class TorpedoAttackStrategy implements AttackStrategy {
    @Override public String id() { return AircraftDefinition.AttackProfile.TORPEDO.id(); }
    @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.TORPEDO; }

    @Override
    public void execute(AircraftEntity craft, Player owner) {
        boolean canFallback = craft.stateTicks >= ModEquipmentConfig.AIRCRAFT_ATTACK_FALLBACK_TICKS.get();
        LivingEntity target = AircraftCombat.resolveTarget(craft, owner);
        if (target == null) {
            if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
            return;
        }
        AircraftCombat.tickTorpedoBomberAttack(craft, owner, target);
    }

    @Override
    public void executeAutonomous(AircraftEntity craft) {
        LivingEntity t = craft.autonomousTarget;
        if (t == null) { craft.startReturning("target_lost"); return; }
        AircraftCombat.tickTorpedoBomberAttack(craft, null, t);
    }
}
