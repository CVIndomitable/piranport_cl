package com.piranport.aviation;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * 自卫机枪的覆盖分区。
 *
 * <p>四个分区都是半空间，不是互斥扇区：一架“又高又在后”的敌机同时属于
 * 上半球和后半球。机型可以启用其中任意若干项，目标落在任一启用分区内即可开火。
 */
public enum SelfDefenseZone implements StringRepresentable {
    UPPER("upper"),
    LOWER("lower"),
    FRONT("front"),
    REAR("rear");

    public static final Codec<SelfDefenseZone> CODEC =
            StringRepresentable.fromEnum(SelfDefenseZone::values);

    private final String id;

    SelfDefenseZone(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public String id() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

    public static SelfDefenseZone byName(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (SelfDefenseZone candidate : values()) {
            if (candidate.id.equals(normalized) || candidate.name().equalsIgnoreCase(normalized)) {
                return candidate;
            }
        }
        return null;
    }
}
