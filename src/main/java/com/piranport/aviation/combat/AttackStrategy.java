package com.piranport.aviation.combat;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.entity.AircraftCombat;
import com.piranport.entity.AircraftEntity;
import net.minecraft.world.entity.player.Player;
import java.util.Objects;

/** 航空攻击策略；默认执行器把既有战术分支作为迁移期实现。 */
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

    /**
     * 执行玩家所属飞机的一个攻击 tick。
     *
     * <p>策略注册表的扩展点在这里生效；内置策略仍委托旧战斗分支，避免在迁移时复制战术代码。
     */
    default void execute(AircraftEntity aircraft, Player owner) {
        AircraftCombat.tickAttacking(aircraft, owner);
    }

    /** 执行自主飞机的一个攻击 tick。 */
    default void executeAutonomous(AircraftEntity aircraft) {
        AircraftCombat.tickAutonomousAttacking(aircraft);
    }
}
