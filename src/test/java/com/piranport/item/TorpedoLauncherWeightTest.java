package com.piranport.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 发射器负重：ceil((1 + 联装) × 口径英寸² × 0.00275)。
 *
 * <p>2026-10-18 起系数由 0.0055 全局减半，本表同步为新值；
 * 原始文档表（数值/08）的旧值按 ×0.5 向上取整换算。
 */
class TorpedoLauncherWeightTest {

    @ParameterizedTest(name = "{0}mm×{1} = {2}")
    @CsvSource({
            "533, 2, 4",
            "533, 3, 5",
            "533, 4, 7",
            "533, 5, 8",
            "610, 3, 7",
            "610, 4, 8",
            "610, 5, 10",
            "610, 6, 12",
            "610, 7, 13",
            "720, 3, 9",
            "720, 4, 11",
    })
    void weightMatchesHalvedCoefficient(int caliber, int tubes, int expected) {
        assertEquals(expected, TorpedoLauncherItem.computeWeight(caliber, tubes));
    }

    @Test
    void caliberInchMapping() {
        assertEquals(21, TorpedoLauncherItem.caliberInches(533));
        assertEquals(24, TorpedoLauncherItem.caliberInches(610));
        assertEquals(28, TorpedoLauncherItem.caliberInches(720));
    }

    @Test
    void smallShipFleetConstraints() {
        // 吹雪级三座 610 三联 21 ≤ 40；阳炎级两座 610 四联 16 ≤ 40
        assertEquals(21, 3 * TorpedoLauncherItem.computeWeight(610, 3));
        assertEquals(16, 2 * TorpedoLauncherItem.computeWeight(610, 4));
    }
}
