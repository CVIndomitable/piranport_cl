package com.piranport.aviation.combat.strategy;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.AircraftAttackContext;
import com.piranport.aviation.combat.AttackStrategy;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** 无挂载策略（profile=none）：直接返航。 */
public final class NoneAttackStrategy implements AttackStrategy {
    @Override public String id() { return AircraftDefinition.AttackProfile.NONE.id(); }
    @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.NONE; }

    @Override
    public void execute(AircraftAttackContext context, @Nullable Player owner) {
        context.startReturning("no_payload");
    }

    @Override
    public void executeAutonomous(AircraftAttackContext context) {
        context.startReturning("no_payload");
    }
}
