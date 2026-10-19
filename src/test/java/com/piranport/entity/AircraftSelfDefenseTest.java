package com.piranport.entity;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 自卫机枪覆盖范围：上/下与 前/后 是两组半空间，必须同时命中。 */
class AircraftSelfDefenseTest {
    private static final Vec3 FORWARD = new Vec3(0.0, 0.0, 1.0);
    private static final Vec3 ORIGIN = Vec3.ZERO;

    @Test
    void b25CoversEverythingExceptLowerHemisphere() {
        // B-25：上 + 前 + 后，关闭下
        assertTrue(covered(5.0, 10.0));   // 上 + 前
        assertTrue(covered(5.0, -10.0));  // 上 + 后
        assertFalse(covered(-5.0, 10.0)); // 下 + 前 -> 下半球被否决
        assertFalse(covered(-5.0, -10.0));// 下 + 后 -> 下半球被否决
    }

    @Test
    void fullRearHemisphereNeedsUpperAndLowerPlusRear() {
        // 一般攻击机：上 + 下 + 后 = 完整后半球
        assertTrue(coveredWith(true, true, false, true, new Vec3(0.0, 5.0, -10.0)));
        assertTrue(coveredWith(true, true, false, true, new Vec3(0.0, -5.0, -10.0)));
        assertFalse(coveredWith(true, true, false, true, new Vec3(0.0, 5.0, 10.0)));
        assertFalse(coveredWith(true, true, false, true, new Vec3(0.0, -5.0, 10.0)));
    }

    @Test
    void rearOnlyWithoutVerticalZonesHasNoCoverage() {
        assertFalse(coveredWith(false, false, false, true, new Vec3(0.0, 5.0, -10.0)));
    }

    private static boolean covered(double y, double z) {
        return coveredWith(true, false, true, true, new Vec3(0.0, y, z));
    }

    private static boolean coveredWith(boolean upper, boolean lower, boolean front,
                                       boolean rear, Vec3 target) {
        return AircraftSelfDefense.covered(ORIGIN, FORWARD, target, upper, lower, front, rear);
    }
}
