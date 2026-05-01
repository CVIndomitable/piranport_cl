package com.piranport.component;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** 武器种类标签 DataComponent，用于区分武器槽物品的类型。 */
public enum WeaponCategory implements StringRepresentable {
    AIRCRAFT("aircraft"),
    TORPEDO("torpedo"),
    CANNON("cannon"),
    ARMOR("armor"),
    MISSILE("missile"),
    ENGINE("engine"),
    DEPTH_CHARGE("depth_charge");

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

    // STREAM_CODEC removed for 1.20.1 (阶段7 NBT重写时处理)
}
