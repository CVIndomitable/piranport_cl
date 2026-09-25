package com.piranport.aviation.combat;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.AircraftDefinitionService;
import com.piranport.component.AircraftInfo;
import com.piranport.entity.AircraftCombat;
import com.piranport.entity.AircraftEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.Objects;
import java.util.Optional;

/** 航空战斗入口的迁移适配器；不复制现有攻击逻辑。 */
public final class AircraftCombatService {
    private AircraftCombatService() {}

    public static AttackStrategy strategyFor(AircraftDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return AttackStrategyRegistry.forProfile(definition.attackProfile());
    }

    public static String strategyIdFor(AircraftDefinition definition) { return strategyFor(definition).id(); }

    public static Optional<LivingEntity> resolveTarget(
            AircraftDefinition definition, TargetProvider provider, TargetProvider.TargetContext context) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(context, "context");
        if (!context.definition().id().equals(definition.id())) {
            throw new IllegalArgumentException("target context definition does not match aircraft definition");
        }
        return Optional.ofNullable(provider.findTarget(context));
    }

    public static boolean supports(AircraftDefinition definition, String strategyId) {
        Objects.requireNonNull(definition, "definition");
        return AttackStrategyRegistry.byId(strategyId).map(s -> s.supports(definition)).orElse(false);
    }

    /** Runtime attack entry point; tactical behavior remains in AircraftCombat during migration. */
    public static void tickAttacking(AircraftEntity aircraft, Player owner) {
        Objects.requireNonNull(aircraft, "aircraft");
        Objects.requireNonNull(owner, "owner");
        strategyFor(definitionFor(aircraft));
        AircraftCombat.tickAttacking(aircraft, owner);
    }

    /** Runtime entry point for autonomous aircraft. */
    public static void tickAutonomousAttacking(AircraftEntity aircraft) {
        Objects.requireNonNull(aircraft, "aircraft");
        strategyFor(definitionFor(aircraft));
        AircraftCombat.tickAutonomousAttacking(aircraft);
    }

    private static AircraftDefinition definitionFor(AircraftEntity aircraft) {
        String id = aircraft.getAircraftDefinitionId();
        AircraftDefinition definition = AircraftDefinitionService.find(id);
        if (definition != null) return definition;
        AircraftInfo info = new AircraftInfo(aircraft.getAircraftType(), 1, 0, 0, 0, 1, 0,
                AircraftInfo.BombingMode.DIVE, false, id);
        return AircraftDefinitionService.resolve(info, id);
    }
}
