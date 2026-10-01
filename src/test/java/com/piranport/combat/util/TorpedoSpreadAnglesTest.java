package com.piranport.combat.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 修复「五联只发 1 条却扣 5 发」：散布角数组长度必须等于管数。 */
class TorpedoSpreadAnglesTest {

    @Test
    void lengthEqualsTubeCountForAllRegisteredTubes() {
        for (int n = 1; n <= 7; n++) {
            assertEquals(n, CombatFireUtils.getSpreadAngles(n).length, "tubes=" + n);
        }
    }

    @Test
    void twoToFourKeepLegacyTable() {
        assertArrayEquals(new float[]{-3f, 3f}, CombatFireUtils.getSpreadAngles(2));
        assertArrayEquals(new float[]{-4f, 0f, 4f}, CombatFireUtils.getSpreadAngles(3));
        assertArrayEquals(new float[]{-6f, -2f, 2f, 6f}, CombatFireUtils.getSpreadAngles(4));
    }

    @Test
    void fiveTubesDefaultSpreadIsFiveDegreesEvenlySpaced() {
        float[] a = CombatFireUtils.getSpreadAngles(5);
        assertArrayEquals(new float[]{-5f, -2.5f, 0f, 2.5f, 5f}, a, 1e-5f);
    }

    @Test
    void evenSpreadSymmetricAndBounded() {
        float[] a = CombatFireUtils.evenSpread(7, 5.0);
        assertEquals(-5f, a[0], 1e-5f);
        assertEquals(5f, a[6], 1e-5f);
        assertEquals(0f, a[3], 1e-5f);
        assertArrayEquals(new float[]{0f, 0f, 0f}, CombatFireUtils.evenSpread(3, Double.NaN), 0f);
    }
}
