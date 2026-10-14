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
        // 内置策略在 com.piranport.entity 包（需访问 AircraftEntity 包级成员），
        // 用全限定名避免 aviation.combat 反向依赖 entity 造成包循环 import。
        Map<String, AttackStrategy> map = new LinkedHashMap<>();
        map.put(AircraftDefinition.AttackProfile.NONE.id(), new com.piranport.entity.NoneAttackStrategy());
        map.put(AircraftDefinition.AttackProfile.GUN.id(), new com.piranport.entity.GunAttackStrategy());
        map.put(AircraftDefinition.AttackProfile.ROCKET.id(), new com.piranport.entity.RocketAttackStrategy());
        map.put(AircraftDefinition.AttackProfile.DIVE_BOMB.id(), new com.piranport.entity.DiveBombAttackStrategy());
        map.put(AircraftDefinition.AttackProfile.LEVEL_BOMB.id(), new com.piranport.entity.LevelBombAttackStrategy());
        map.put(AircraftDefinition.AttackProfile.TORPEDO.id(), new com.piranport.entity.TorpedoAttackStrategy());
        map.put(AircraftDefinition.AttackProfile.DEPTH_CHARGE.id(), new com.piranport.entity.DepthChargeAttackStrategy());
        map.put(AircraftDefinition.AttackProfile.RECON.id(), new com.piranport.entity.ReconAttackStrategy());
        return Map.copyOf(map);
    }

    private static String normalize(String id) { return id.trim().toLowerCase(java.util.Locale.ROOT); }
}
