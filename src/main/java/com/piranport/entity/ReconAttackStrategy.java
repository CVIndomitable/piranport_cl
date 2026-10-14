package com.piranport.entity;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.AttackStrategy;
import net.minecraft.world.entity.player.Player;

/** 侦察机攻击策略（profile=recon）：侦察机无攻击行为，直接进入返航。 */
public class ReconAttackStrategy implements AttackStrategy {
    @Override public String id() { return AircraftDefinition.AttackProfile.RECON.id(); }
    @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.RECON; }

    @Override
    public void execute(AircraftEntity craft, Player owner) {
        craft.startReturning("recon_no_attack");
    }

    @Override
    public void executeAutonomous(AircraftEntity craft) {
        craft.startReturning("no_payload");
    }
}
