package com.piranport.aviation.combat;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.entity.AircraftEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/** 航空战斗入口：按定义里的 attack profile 分发到注册表。 */
public final class AircraftCombatService {
    private AircraftCombatService() {}

    public static AttackStrategy strategyFor(AircraftDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        // 侦察机的 profile 为 null，注册表兜底返回 NONE 策略，避免 NPE
        return AttackStrategyRegistry.forProfile(definition.attackProfile());
    }

    public static String strategyIdFor(AircraftDefinition definition) {
        return strategyFor(definition).id();
    }

    public static boolean supports(AircraftDefinition definition, String strategyId) {
        Objects.requireNonNull(definition, "definition");
        return AttackStrategyRegistry.byId(strategyId)
                .map(strategy -> strategy.supports(definition))
                .orElse(false);
    }

    /** 玩家所属飞机的一个攻击 tick。 */
    public static void tickAttacking(AircraftEntity aircraft, Player owner) {
        Objects.requireNonNull(aircraft, "aircraft");
        Objects.requireNonNull(owner, "owner");
        executeAttacking(aircraft.getResolvedDefinition(), aircraft, owner);
    }

    /** 自主飞机的一个攻击 tick。 */
    public static void tickAutonomousAttacking(AircraftEntity aircraft) {
        Objects.requireNonNull(aircraft, "aircraft");
        executeAutonomous(aircraft.getResolvedDefinition(), aircraft);
    }

    /** 包级执行 seam，供运行时入口和纯策略测试共用。 */
    static void executeAttacking(AircraftDefinition definition, AircraftAttackContext context,
                                 @Nullable Player owner) {
        strategyFor(definition).execute(context, owner);
    }

    /** 包级执行 seam，供自主飞机入口和纯策略测试共用。 */
    static void executeAutonomous(AircraftDefinition definition, AircraftAttackContext context) {
        strategyFor(definition).executeAutonomous(context);
    }
}
