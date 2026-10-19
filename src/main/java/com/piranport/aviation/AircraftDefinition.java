package com.piranport.aviation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * 飞机的静态定义。定义只描述一架机型的能力与基础数值，不保存某一架飞机的燃油、装填或目标。
 *
 * <p>定义 ID 就是飞机物品的注册名（例如 {@code piranport:seafire}），不再有
 * “物品名 / canonical 名 / 旧机种名”三套身份。数据文件路径为
 * {@code data/<namespace>/aircraft/<path>.json}，加载时据此生成 ID。
 *
 * <p>侦察机（{@link AircraftClass#RECON}）不声明 {@code attack_profile}，
 * 也不允许挂载；其余机种必须声明攻击配置。
 */
public record AircraftDefinition(
        ResourceLocation id,
        AircraftClass aircraftClass,
        @Nullable AttackProfile attackProfile,
        @Nullable ResourceLocation payloadItem,
        String visualId,
        int fuelCapacity,
        int ammoCapacity,
        float panelDamage,
        float panelSpeed,
        int weight,
        int health,
        int attackCooldown,
        @Nullable SelfDefense selfDefense,
        Nation nation
) {
    public AircraftDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(aircraftClass, "aircraftClass");
        Objects.requireNonNull(nation, "nation");
        if (visualId == null || visualId.isBlank()) {
            throw new IllegalArgumentException("visual_id is required for " + id);
        }
        visualId = AircraftVisualIds.normalize(visualId);
        if (!AircraftVisualIds.isKnown(visualId)) {
            throw new IllegalArgumentException("unknown visual_id '" + visualId + "' for " + id
                    + "; known ids: " + AircraftVisualIds.known());
        }
        if (fuelCapacity < 1 || ammoCapacity < 0 || weight < 0 || health < 1 || attackCooldown < 1) {
            throw new IllegalArgumentException("invalid capacities/health/cooldown for " + id);
        }
        if (!Float.isFinite(panelDamage) || panelDamage < 0.0F) {
            throw new IllegalArgumentException("panel_damage must be finite and non-negative for " + id);
        }
        if (!Float.isFinite(panelSpeed) || panelSpeed <= 0.0F) {
            throw new IllegalArgumentException("panel_speed must be finite and positive for " + id);
        }
        if (selfDefense != null && aircraftClass == AircraftClass.RECON) {
            throw new IllegalArgumentException("recon aircraft must not declare self_defense: " + id);
        }
        validateCombination(id, aircraftClass, attackProfile, payloadItem);
    }

    /**
     * 兼容构造：占位定义与旧调用方不声明 nation 时按 {@link Nation#UNKNOWN} 处理。
     * 系别只影响分系别数值（如航空鱼雷入水速度），缺失不应阻断定义构造。
     */
    public AircraftDefinition(ResourceLocation id, AircraftClass aircraftClass,
                              @Nullable AttackProfile attackProfile,
                              @Nullable ResourceLocation payloadItem, String visualId,
                              int fuelCapacity, int ammoCapacity, float panelDamage,
                              float panelSpeed, int weight, int health, int attackCooldown,
                              @Nullable SelfDefense selfDefense) {
        this(id, aircraftClass, attackProfile, payloadItem, visualId, fuelCapacity, ammoCapacity,
                panelDamage, panelSpeed, weight, health, attackCooldown, selfDefense, Nation.UNKNOWN);
    }

    /** 静态定义的 JSON 形状；id 来自文件路径，不写在 JSON 里。 */
    public record JsonDefinition(
            AircraftClass aircraftClass,
            Optional<AttackProfile> attackProfile,
            Optional<ResourceLocation> payloadItem,
            String visualId,
            int fuelCapacity,
            int ammoCapacity,
            float panelDamage,
            float panelSpeed,
            int weight,
            int health,
            int attackCooldown,
            Optional<SelfDefense> selfDefense,
            Nation nation
    ) {
        public static final Codec<JsonDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                AircraftClass.CODEC.fieldOf("aircraft_class").forGetter(JsonDefinition::aircraftClass),
                AttackProfile.CODEC.optionalFieldOf("attack_profile")
                        .forGetter(JsonDefinition::attackProfile),
                ResourceLocation.CODEC.optionalFieldOf("payload_item").forGetter(JsonDefinition::payloadItem),
                Codec.STRING.fieldOf("visual_id").forGetter(JsonDefinition::visualId),
                Codec.intRange(1, 100_000).fieldOf("fuel_capacity").forGetter(JsonDefinition::fuelCapacity),
                Codec.intRange(0, 100_000).fieldOf("ammo_capacity").forGetter(JsonDefinition::ammoCapacity),
                Codec.FLOAT.fieldOf("panel_damage").forGetter(JsonDefinition::panelDamage),
                Codec.FLOAT.fieldOf("panel_speed").forGetter(JsonDefinition::panelSpeed),
                Codec.intRange(0, 100_000).fieldOf("weight").forGetter(JsonDefinition::weight),
                Codec.intRange(1, 100_000).fieldOf("health").forGetter(JsonDefinition::health),
                Codec.intRange(1, 12_000).fieldOf("attack_cooldown").forGetter(JsonDefinition::attackCooldown),
                SelfDefense.CODEC.optionalFieldOf("self_defense").forGetter(JsonDefinition::selfDefense),
                Nation.CODEC.optionalFieldOf("nation", Nation.UNKNOWN).forGetter(JsonDefinition::nation)
        ).apply(instance, JsonDefinition::new));

        public AircraftDefinition withId(ResourceLocation id) {
            return new AircraftDefinition(id, aircraftClass, attackProfile.orElse(null),
                    payloadItem.orElse(null),
                    visualId, fuelCapacity, ammoCapacity, panelDamage, panelSpeed, weight,
                    health, attackCooldown, selfDefense.orElse(null), nation);
        }

        public static JsonDefinition from(AircraftDefinition definition) {
            return new JsonDefinition(definition.aircraftClass(),
                    Optional.ofNullable(definition.attackProfile()),
                    Optional.ofNullable(definition.payloadItem()), definition.visualId(),
                    definition.fuelCapacity(), definition.ammoCapacity(), definition.panelDamage(),
                    definition.panelSpeed(), definition.weight(), definition.health(),
                    definition.attackCooldown(), Optional.ofNullable(definition.selfDefense()),
                    definition.nation());
        }
    }

    /** Whether this definition consumes a manually loaded payload before launch. */
    public boolean requiresPayload() {
        return payloadItem != null;
    }

    /** Whether the aircraft uses the ordinary fighter gun targeting path. */
    public boolean usesGunAttack() {
        return attackProfile == AttackProfile.GUN || attackProfile == AttackProfile.ROCKET;
    }

    /** 仅普通机枪战斗机（火箭机的自主索敌走对地目标，不走空优目标）。 */
    public boolean isGunFighter() {
        return attackProfile == AttackProfile.GUN;
    }

    /** Whether the aircraft is treated as an air-to-air/rocket fighter at launch. */
    public boolean usesBulletAttack() {
        return usesGunAttack();
    }

    /** Whether the aircraft is a pure attack aircraft (payload, no guns). */
    public boolean isPayloadAttacker() {
        return !usesGunAttack() && requiresPayload();
    }

    public boolean isRecon() {
        // 侦察机由机种判定：侦察机不声明攻击配置
        return aircraftClass == AircraftClass.RECON;
    }

    public boolean hasSonar() {
        return attackProfile == AttackProfile.DEPTH_CHARGE;
    }

    private static void validateCombination(ResourceLocation id, AircraftClass aircraftClass,
                                            @Nullable AttackProfile attackProfile,
                                            @Nullable ResourceLocation payloadItem) {
        if (aircraftClass == AircraftClass.RECON) {
            // 侦察机不声明攻击配置，也不允许挂载
            if (attackProfile == null && payloadItem == null) {
                return;
            }
            throw invalidCombination(id, aircraftClass, attackProfile, payloadItem);
        }
        boolean valid = attackProfile != null && (switch (aircraftClass) {
            case FIGHTER -> (attackProfile == AttackProfile.GUN || attackProfile == AttackProfile.NONE)
                    && payloadItem == null;
            case ROCKET_FIGHTER -> attackProfile == AttackProfile.ROCKET && payloadItem == null;
            case DIVE_BOMBER -> attackProfile == AttackProfile.DIVE_BOMB
                    && isPayload(payloadItem, "aerial_bomb");
            case LEVEL_BOMBER -> attackProfile == AttackProfile.LEVEL_BOMB
                    && isPayload(payloadItem, "aerial_bomb");
            case TORPEDO_BOMBER -> attackProfile == AttackProfile.TORPEDO
                    && isPayload(payloadItem, "aerial_torpedo");
            case ASW -> attackProfile == AttackProfile.DEPTH_CHARGE
                    && isPayload(payloadItem, "depth_charge");
            case RECON -> false; // 已在方法开头处理，这里仅为穷尽枚举
        });
        if (!valid) {
            throw invalidCombination(id, aircraftClass, attackProfile, payloadItem);
        }
    }

    private static IllegalArgumentException invalidCombination(ResourceLocation id,
                                                              AircraftClass aircraftClass,
                                                              @Nullable AttackProfile attackProfile,
                                                              @Nullable ResourceLocation payloadItem) {
        return new IllegalArgumentException("invalid aircraft definition combination for " + id
                + ": class=" + aircraftClass.getSerializedName()
                + ", profile=" + (attackProfile == null ? "null" : attackProfile.id())
                + ", payload=" + payloadItem);
    }

    private static boolean isPayload(@Nullable ResourceLocation payloadItem, String expectedPath) {
        return payloadItem != null
                && "piranport".equals(payloadItem.getNamespace())
                && expectedPath.equals(payloadItem.getPath());
    }

    public enum AttackProfile implements StringRepresentable {
        NONE("none"), GUN("gun"), ROCKET("rocket"), DIVE_BOMB("dive_bomb"),
        LEVEL_BOMB("level_bomb"), TORPEDO("torpedo"), DEPTH_CHARGE("depth_charge");

        public static final Codec<AttackProfile> CODEC =
                StringRepresentable.fromEnum(AttackProfile::values);

        private final String id;

        AttackProfile(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }

        public String id() {
            return id;
        }

        public static AttackProfile byId(String value) {
            if (value == null || value.isBlank()) return null;
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (AttackProfile candidate : values()) {
                if (candidate.id.equals(normalized) || candidate.name().equalsIgnoreCase(normalized)) {
                    return candidate;
                }
            }
            return null;
        }
    }

    /**
     * 飞机所属系别。数据文件用 {@code nation} 声明（缺省 {@code unknown}）。
     * 目前只用于按国籍区分的分系别数值（航空鱼雷入水速度）；不参与合成、掉落等逻辑。
     */
    public enum Nation implements StringRepresentable {
        US("us"), JP("jp"), GB("gb"), DE("de"), FR("fr"), IT("it"), UNKNOWN("unknown");

        public static final Codec<Nation> CODEC = StringRepresentable.fromEnum(Nation::values);

        private final String id;

        Nation(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }
}
