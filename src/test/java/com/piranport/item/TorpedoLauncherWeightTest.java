package com.piranport.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 策划决策/数值/08：ceil((1+联装)×口径英寸²×0.0055)，文档表 11 格。 */
class TorpedoLauncherWeightTest {

    @ParameterizedTest(name = "{0}mm×{1} = {2}")
    @CsvSource({
            "533, 2, 8",
            "533, 3, 10",
            "533, 4, 13",
            "533, 5, 15",
            "610, 3, 13",
            "610, 4, 16",
            "610, 5, 20",
            "610, 6, 23",
            "610, 7, 26",
            "720, 3, 18",
            "720, 4, 22",
    })
    void weightMatchesDocTable(int caliber, int tubes, int expected) {
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
        // 吹雪级三座 610 三联 39 ≤ 40；阳炎级两座 610 四联 32 ≤ 40
        assertEquals(39, 3 * TorpedoLauncherItem.computeWeight(610, 3));
        assertEquals(32, 2 * TorpedoLauncherItem.computeWeight(610, 4));
    }
}
