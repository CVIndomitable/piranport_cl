package com.piranport.aviation.combat;

import com.piranport.aviation.AircraftDefinition;
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

    public static AttackStrategy forProfile(AircraftDefinition.AttackProfile profile) {
        Objects.requireNonNull(profile, "profile");
        return strategies.getOrDefault(profile.id(), strategies.get(AircraftDefinition.AttackProfile.NONE.id()));
    }

    public static Map<String, AttackStrategy> snapshot() { return strategies; }

    public static synchronized void register(AttackStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy");
        String id = normalize(strategy.id());
        if (id.isEmpty()) throw new IllegalArgumentException("strategy id must not be blank");
        Map<String, AttackStrategy> next = new LinkedHashMap<>(strategies);
        next.put(id, strategy);
        strategies = Map.copyOf(next);
    }

    public static synchronized void resetToBuiltIns() { strategies = BUILT_INS; }

    private static Map<String, AttackStrategy> createBuiltIns() {
        Map<String, AttackStrategy> map = new LinkedHashMap<>();
        for (AircraftDefinition.AttackProfile profile : AircraftDefinition.AttackProfile.values()) {
            map.put(profile.id(), new ProfileStrategy(profile));
        }
        return Map.copyOf(map);
    }

    private static String normalize(String id) { return id.trim().toLowerCase(java.util.Locale.ROOT); }

    private record ProfileStrategy(AircraftDefinition.AttackProfile profile) implements AttackStrategy {
        @Override public String id() { return profile.id(); }
        @Override public AircraftDefinition.AttackProfile profile() { return profile; }
    }
}
