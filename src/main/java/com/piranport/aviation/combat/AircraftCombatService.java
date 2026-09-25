package com.piranport.aviation.combat;

import com.piranport.aviation.AircraftDefinition;
import net.minecraft.world.entity.LivingEntity;
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
}
