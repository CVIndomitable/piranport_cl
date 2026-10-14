package com.piranport.entity;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.AttackStrategy;
import com.piranport.config.ModEquipmentConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 火箭机攻击策略（profile=rocket）。
 * 先尝试对地面目标的导弹齐射（一次性消耗弹药），打完后退化为机枪对空。
 */
public class RocketAttackStrategy implements AttackStrategy {
    @Override public String id() { return AircraftDefinition.AttackProfile.ROCKET.id(); }
    @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.ROCKET; }

    @Override
    public void execute(AircraftEntity craft, Player owner) {
        boolean canFallback = craft.stateTicks >= ModEquipmentConfig.AIRCRAFT_ATTACK_FALLBACK_TICKS.get();
        if (!craft.hasFired && craft.remainingAmmo > 0) {
            LivingEntity groundTarget = AircraftCombat.resolveTarget(craft, owner);
            if (groundTarget != null) {
                AircraftCombat.tickRocketFighterMissileRun(craft, owner, groundTarget);
                return;
            }
        }
        Entity airTarget = AircraftCombat.resolveFighterTarget(craft, owner);
        if (airTarget == null) {
            if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
            return;
        }
        AircraftCombat.tickFighterAttack(craft, owner, airTarget);
    }

    @Override
    public void executeAutonomous(AircraftEntity craft) {
        LivingEntity t = craft.autonomousTarget;
        if (t == null) { craft.startReturning("target_lost"); return; }
        if (!craft.hasFired && craft.remainingAmmo > 0) {
            AircraftCombat.tickRocketFighterMissileRun(craft, null, t);
        } else {
            AircraftCombat.tickFighterAttack(craft, null, t);
        }
    }
}
