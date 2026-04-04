package com.piranport.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** 武器种类标签 DataComponent，用于区分武器槽物品的类型。 */
public enum WeaponCategory implements StringRepresentable {
    AIRCRAFT("aircraft"),
    TORPEDO("torpedo"),
    CANNON("cannon"),
    ARMOR("armor");

    private final String serializedName;

    WeaponCategory(String name) {
        this.serializedName = name;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public static final Codec<WeaponCategory> CODEC =
            StringRepresentable.fromEnum(WeaponCategory::values);

    public static final StreamCodec<ByteBuf, WeaponCategory> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(i -> {
                WeaponCategory[] vals = WeaponCategory.values();
                return (i >= 0 && i < vals.length) ? vals[i] : CANNON;
            }, Enum::ordinal);
}
