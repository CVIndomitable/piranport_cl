package com.piranport.entity;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.AttackStrategy;
import com.piranport.config.ModEquipmentConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 机枪 fighter 攻击策略（profile=gun）。
 * 战斗机弹药无限；索敌走「敌机优先、火控锁定兜底」。
 */
public class GunAttackStrategy implements AttackStrategy {
    @Override public String id() { return AircraftDefinition.AttackProfile.GUN.id(); }
    @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.GUN; }

    @Override
    public void execute(AircraftEntity craft, Player owner) {
        boolean canFallback = craft.stateTicks >= ModEquipmentConfig.AIRCRAFT_ATTACK_FALLBACK_TICKS.get();
        Entity target = AircraftCombat.resolveFighterTarget(craft, owner);
        if (target == null) {
            if (canFallback) craft.setState(AircraftEntity.FlightState.CRUISING);
            return;
        }
        AircraftCombat.tickFighterAttack(craft, owner, target);
    }

    @Override
    public void executeAutonomous(AircraftEntity craft) {
        Entity target = AircraftCombat.resolveAutonomousFighterTarget(craft);
        if (target == null) { craft.setState(AircraftEntity.FlightState.CRUISING); return; }
        AircraftCombat.tickFighterAttack(craft, null, target);
    }
}
