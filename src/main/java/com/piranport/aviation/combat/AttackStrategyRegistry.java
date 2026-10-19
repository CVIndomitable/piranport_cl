package com.piranport.aviation.combat;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.strategy.DepthChargeAttackStrategy;
import com.piranport.aviation.combat.strategy.DiveBombAttackStrategy;
import com.piranport.aviation.combat.strategy.GunAttackStrategy;
import com.piranport.aviation.combat.strategy.LevelBombAttackStrategy;
import com.piranport.aviation.combat.strategy.NoneAttackStrategy;
import com.piranport.aviation.combat.strategy.RocketAttackStrategy;
import com.piranport.aviation.combat.strategy.TorpedoAttackStrategy;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** 稳定 profile 到攻击策略的注册表。 */
public final class AttackStrategyRegistry {
    private static final Map<String, AttackStrategy> BUILT_INS = createBuiltIns();
    private static volatile Map<String, AttackStrategy> strategies = BUILT_INS;

    private AttackStrategyRegistry() {}

    public static Optional<AttackStrategy> byId(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(strategies.get(normalize(id)));
    }

    /**
     * profile 为 null 表示侦察机（不声明攻击配置），兜底返回 NONE 策略。
     * 正常流程侦察机不会进入 ATTACKING，这里只是防御性兜底。
     */
    public static AttackStrategy forProfile(@Nullable AircraftDefinition.AttackProfile profile) {
        if (profile == null) {
            return strategies.get(AircraftDefinition.AttackProfile.NONE.id());
        }
        return strategies.getOrDefault(profile.id(), strategies.get(AircraftDefinition.AttackProfile.NONE.id()));
    }

    public static Map<String, AttackStrategy> snapshot() {
        return strategies;
    }

    public static synchronized void register(AttackStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy");
        String id = normalize(strategy.id());
        if (id.isEmpty()) throw new IllegalArgumentException("strategy id must not be blank");
        Map<String, AttackStrategy> next = new LinkedHashMap<>(strategies);
        next.put(id, strategy);
        strategies = Map.copyOf(next);
    }

    public static synchronized void resetToBuiltIns() {
        strategies = BUILT_INS;
    }

    private static Map<String, AttackStrategy> createBuiltIns() {
        Map<String, AttackStrategy> map = new LinkedHashMap<>();
        map.put(AircraftDefinition.AttackProfile.NONE.id(), new NoneAttackStrategy());
        map.put(AircraftDefinition.AttackProfile.GUN.id(), new GunAttackStrategy());
        map.put(AircraftDefinition.AttackProfile.ROCKET.id(), new RocketAttackStrategy());
        map.put(AircraftDefinition.AttackProfile.DIVE_BOMB.id(), new DiveBombAttackStrategy());
        map.put(AircraftDefinition.AttackProfile.LEVEL_BOMB.id(), new LevelBombAttackStrategy());
        map.put(AircraftDefinition.AttackProfile.TORPEDO.id(), new TorpedoAttackStrategy());
        map.put(AircraftDefinition.AttackProfile.DEPTH_CHARGE.id(), new DepthChargeAttackStrategy());
        return Map.copyOf(map);
    }

    private static String normalize(String id) {
        return id.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
