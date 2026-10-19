package com.piranport.aviation.combat;

import com.piranport.aviation.AircraftDefinition;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 航空攻击策略 —— 每个 AttackProfile 对应一个真实现，注册表即分发器。
 * 策略只依赖 {@link AircraftAttackContext}，索敌与战术都在策略内部完成。
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
    void execute(AircraftAttackContext context, @Nullable Player owner);

    /** 执行自主飞机的一个攻击 tick。 */
    void executeAutonomous(AircraftAttackContext context);
}
