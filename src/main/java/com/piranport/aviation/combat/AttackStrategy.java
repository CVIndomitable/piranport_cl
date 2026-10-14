package com.piranport.aviation.combat;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.entity.AircraftEntity;
import net.minecraft.world.entity.player.Player;
import java.util.Objects;

/**
 * 航空攻击策略 —— 每个 AttackProfile 对应一个真实现，注册表即分发器。
 * 内置策略在 com.piranport.entity 包（XxxAttackStrategy），需访问 AircraftEntity 包级成员。
 */
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

    /** 执行玩家所属飞机的一个攻击 tick。 */
    void execute(AircraftEntity aircraft, Player owner);

    /** 执行自主飞机的一个攻击 tick。 */
    void executeAutonomous(AircraftEntity aircraft);
}
