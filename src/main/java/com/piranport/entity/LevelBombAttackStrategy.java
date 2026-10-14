package com.piranport.entity;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.AttackStrategy;
import com.piranport.config.ModEquipmentConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 水平轰炸攻击策略（profile=level_bomb）。 */
public class LevelBombAttackStrategy implements AttackStrategy {
    @Override public String id() { return AircraftDefinition.AttackProfile.LEVEL_BOMB.id(); }
    @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.LEVEL_BOMB; }

    @Override
    public void execute(AircraftEntity craft, Player owner) {
        boolean canFallback = craft.stateTicks >= ModEquipmentConfig.AIRCRAFT_ATTACK_FALLBACK_TICKS.get();
        LivingEntity target = AircraftCombat.resolveTarget(craft, owner);
        if (target == null) {
            if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
            return;
        }
        AircraftCombat.tickLevelBomberAttack(craft, owner, target);
    }

    @Override
    public void executeAutonomous(AircraftEntity craft) {
        LivingEntity t = craft.autonomousTarget;
        if (t == null) { craft.startReturning("target_lost"); return; }
        AircraftCombat.tickLevelBomberAttack(craft, null, t);
    }
}
