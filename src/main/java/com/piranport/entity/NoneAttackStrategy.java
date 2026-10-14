package com.piranport.entity;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.AttackStrategy;
import net.minecraft.world.entity.player.Player;

/** 无挂载策略（profile=none）：无 payload，直接返航。 */
public class NoneAttackStrategy implements AttackStrategy {
    @Override public String id() { return AircraftDefinition.AttackProfile.NONE.id(); }
    @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.NONE; }

    @Override
    public void execute(AircraftEntity craft, Player owner) {
        craft.startReturning("no_payload");
    }

    @Override
    public void executeAutonomous(AircraftEntity craft) {
        craft.startReturning("no_payload");
    }
}
