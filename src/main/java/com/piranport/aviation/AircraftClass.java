package com.piranport.aviation;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * 飞机的展示分类。
 *
 * <p>用于终端/提示/创造模式分组。攻击方式由 {@link AircraftDefinition.AttackProfile}
 * 决定；侦察机是唯一由机种直接判定的类别（{@code RECON} 不声明攻击配置），
 * 反潜能力仍由定义上的语义化查询（{@code hasSonar()}）决定。
 */
public enum AircraftClass implements StringRepresentable {
    FIGHTER("fighter"),
    ROCKET_FIGHTER("rocket_fighter"),
    DIVE_BOMBER("dive_bomber"),
    LEVEL_BOMBER("level_bomber"),
    TORPEDO_BOMBER("torpedo_bomber"),
    ASW("asw"),
    RECON("recon");

    public static final Codec<AircraftClass> CODEC = StringRepresentable.fromEnum(AircraftClass::values);

    private final String serializedName;

    AircraftClass(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    /** 仅用于日志/调试显示，不参与数据解析。 */
    public String displayName() {
        return serializedName;
    }

    public static AircraftClass byName(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (AircraftClass candidate : values()) {
            if (candidate.serializedName.equals(normalized) || candidate.name().equalsIgnoreCase(normalized)) {
                return candidate;
            }
        }
        return null;
    }
}
