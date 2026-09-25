package com.piranport.aviation;

import com.piranport.component.AircraftInfo;

import java.util.Objects;

/**
 * 飞机的静态定义快照。定义只描述一架机型的能力与基础数值，不保存某一架飞机的燃油、装填或目标。
 */
public record AircraftDefinition(
        String id,
        AircraftInfo.AircraftType aircraftClass,
        AttackProfile attackProfile,
        PayloadType payloadType,
        String visualId,
        int fuelCapacity,
        int ammoCapacity,
        float panelDamage,
        float panelSpeed,
        int weight,
        AircraftInfo.BombingMode bombingMode,
        int health,
        int attackCooldown
) {
    /** Compatibility constructor for existing callers and legacy definitions. */
    public AircraftDefinition(String id, AircraftInfo.AircraftType aircraftClass,
                              AttackProfile attackProfile, PayloadType payloadType,
                              String visualId, int fuelCapacity, int ammoCapacity,
                              float panelDamage, float panelSpeed, int weight,
                              AircraftInfo.BombingMode bombingMode) {
        this(id, aircraftClass, attackProfile, payloadType, visualId, fuelCapacity, ammoCapacity,
                panelDamage, panelSpeed, weight, bombingMode, legacyHealth(aircraftClass),
                legacyCooldown(aircraftClass));
    }

    public AircraftDefinition {
        id = requireId(id);
        Objects.requireNonNull(aircraftClass, "aircraftClass");
        Objects.requireNonNull(attackProfile, "attackProfile");
        Objects.requireNonNull(payloadType, "payloadType");
        visualId = requireId(visualId);
        Objects.requireNonNull(bombingMode, "bombingMode");
        if (fuelCapacity < 1 || ammoCapacity < 0 || weight < 0 || health < 1 || attackCooldown < 1) {
            throw new IllegalArgumentException("aircraft capacities and weight must be non-negative");
        }
        if (!Float.isFinite(panelDamage) || panelDamage < 0.0F) {
            throw new IllegalArgumentException("panelDamage must be finite and non-negative");
        }
        if (!Float.isFinite(panelSpeed) || panelSpeed <= 0.0F) {
            throw new IllegalArgumentException("panelSpeed must be finite and positive");
        }
        validateCombination(aircraftClass, attackProfile, payloadType, id);
    }

    /** 从旧 AircraftInfo 生成迁移期定义；旧数值保持原样，避免本阶段改变平衡。 */
    public static AircraftDefinition fromLegacy(String id, AircraftInfo info) {
        Objects.requireNonNull(info, "info");
        AircraftInfo.AircraftType type = info.aircraftType();
        AttackProfile profile = AttackProfile.from(type, info.bombingMode());
        PayloadType payload = PayloadType.from(type);
        return new AircraftDefinition(id, type, profile, payload, type.getSerializedName(),
                info.fuelCapacity(), info.ammoCapacity(), info.panelDamage(), info.panelSpeed(),
                info.weight(), info.bombingMode(), legacyHealth(type), legacyCooldown(type));
    }

    private static int legacyHealth(AircraftInfo.AircraftType type) {
        return switch (type) {
            case FIGHTER -> 20;
            case ROCKET_FIGHTER -> 20;
            case DIVE_BOMBER -> 15;
            case LEVEL_BOMBER -> 12;
            case TORPEDO_BOMBER -> 15;
            case ASW -> 12;
            case RECON -> 10;
        };
    }

    private static int legacyCooldown(AircraftInfo.AircraftType type) {
        return switch (type) {
            case FIGHTER -> 5;
            case ROCKET_FIGHTER -> 40;
            case DIVE_BOMBER, LEVEL_BOMBER -> 1;
            case TORPEDO_BOMBER -> 40;
            case ASW -> 30;
            case RECON -> 1;
        };
    }

    public String payloadRegistryName() {
        return payloadType.registryName;
    }

    /** Whether this definition consumes a manually loaded payload before launch. */
    public boolean requiresPayload() {
        // Rocket fighters carry their rockets as the aircraft's own ammunition.
        // Keep their historical launch path: R only refuels them, while the
        // rocket salvo consumes ammoCapacity during combat.
        return payloadType != PayloadType.NONE && payloadType != PayloadType.ROCKET_AMMO;
    }

    /** Whether the aircraft uses the ordinary fighter gun targeting path. */
    public boolean usesGunAttack() {
        return attackProfile == AttackProfile.GUN;
    }

    /** Whether the aircraft is treated as an air-to-air/rocket fighter at launch. */
    public boolean usesBulletAttack() {
        return attackProfile == AttackProfile.GUN || attackProfile == AttackProfile.ROCKET;
    }

    public boolean hasPayload() {
        return payloadType != PayloadType.NONE;
    }

    private static void validateCombination(AircraftInfo.AircraftType aircraftClass,
                                             AttackProfile attackProfile,
                                             PayloadType payloadType,
                                             String id) {
        boolean valid = switch (aircraftClass) {
            case FIGHTER -> attackProfile == AttackProfile.GUN || attackProfile == AttackProfile.NONE;
            case ROCKET_FIGHTER -> attackProfile == AttackProfile.ROCKET;
            case DIVE_BOMBER -> attackProfile == AttackProfile.DIVE_BOMB && payloadType == PayloadType.AERIAL_BOMB;
            case LEVEL_BOMBER -> attackProfile == AttackProfile.LEVEL_BOMB && payloadType == PayloadType.AERIAL_BOMB;
            case TORPEDO_BOMBER -> attackProfile == AttackProfile.TORPEDO && payloadType == PayloadType.AERIAL_TORPEDO;
            case ASW -> attackProfile == AttackProfile.DEPTH_CHARGE && payloadType == PayloadType.DEPTH_CHARGE;
            case RECON -> (attackProfile == AttackProfile.NONE || attackProfile == AttackProfile.RECON)
                    && payloadType == PayloadType.NONE;
        };
        if (!valid) {
            throw new IllegalArgumentException("invalid aircraft definition combination: " + id);
        }
    }

    private static String requireId(String value) {
        if (value == null || value.isBlank() || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("aircraft IDs must be non-blank and contain no spaces");
        }
        return value;
    }

    public enum AttackProfile {
        NONE("none"), GUN("gun"), ROCKET("rocket"), DIVE_BOMB("dive_bomb"),
        LEVEL_BOMB("level_bomb"), TORPEDO("torpedo"), DEPTH_CHARGE("depth_charge"), RECON("recon");

        private final String id;
        AttackProfile(String id) { this.id = id; }
        public String id() { return id; }

        static AttackProfile from(AircraftInfo.AircraftType type, AircraftInfo.BombingMode mode) {
            return switch (type) {
                case FIGHTER -> GUN;
                case ROCKET_FIGHTER -> ROCKET;
                case DIVE_BOMBER -> DIVE_BOMB;
                case LEVEL_BOMBER -> LEVEL_BOMB;
                case TORPEDO_BOMBER -> TORPEDO;
                case ASW -> DEPTH_CHARGE;
                case RECON -> RECON;
            };
        }
    }

    public enum PayloadType {
        NONE(""), AERIAL_TORPEDO("piranport:aerial_torpedo"), AERIAL_BOMB("piranport:aerial_bomb"),
        DEPTH_CHARGE("piranport:depth_charge"), ROCKET_AMMO("piranport:rocket_ammo");

        private final String registryName;
        PayloadType(String registryName) { this.registryName = registryName; }
        public String registryName() { return registryName; }

        static PayloadType from(AircraftInfo.AircraftType type) {
            return switch (type) {
                case TORPEDO_BOMBER -> AERIAL_TORPEDO;
                case DIVE_BOMBER, LEVEL_BOMBER -> AERIAL_BOMB;
                case ASW -> DEPTH_CHARGE;
                case ROCKET_FIGHTER -> ROCKET_AMMO;
                default -> NONE;
            };
        }
    }
}
