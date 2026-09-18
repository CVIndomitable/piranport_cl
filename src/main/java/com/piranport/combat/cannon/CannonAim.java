package com.piranport.combat.cannon;

import net.minecraft.world.phys.Vec3;

/** 火炮瞄准指令；网络编号只在边界解码，内部使用明确的指令类型。 */
public sealed interface CannonAim {
    int NONE = 0;
    int TARGET = 1;
    int MAX_RANGE = 2;
    int DIRECT_TARGET = 3;

    record NoAim() implements CannonAim {}
    record Aimed(Vec3 target) implements CannonAim {}
    record DirectAim(Vec3 target) implements CannonAim {}
    record MaxRange() implements CannonAim {}

    static CannonAim decodeAim(int mode, double x, double y, double z) {
        return switch (mode) {
            case TARGET -> new Aimed(new Vec3(x, y, z));
            case MAX_RANGE -> new MaxRange();
            case DIRECT_TARGET -> new DirectAim(new Vec3(x, y, z));
            default -> new NoAim();
        };
    }
}
