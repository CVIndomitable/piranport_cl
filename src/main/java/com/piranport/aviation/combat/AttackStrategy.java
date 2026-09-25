package com.piranport.aviation.combat;

import com.piranport.aviation.AircraftDefinition;
import java.util.Objects;

/** 无状态的航空攻击策略描述；实际动作仍由现有战斗系统执行。 */
@FunctionalInterface
public interface AttackStrategy {
    String id();

    default AircraftDefinition.AttackProfile profile() {
        String id = id();
        for (AircraftDefinition.AttackProfile candidate : AircraftDefinition.AttackProfile.values()) {
            if (candidate.id().equalsIgnoreCase(id)) return candidate;
        }
        throw new IllegalArgumentException("unknown attack strategy profile: " + id);
    }

    default boolean supports(AircraftDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return profile() == definition.attackProfile();
    }
}
