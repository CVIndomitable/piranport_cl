package com.piranport.terminal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TerminalParameterSpecTest {
    @Test
    void rejectsNonFiniteAndOutOfRangeBeforeMutation() {
        TerminalParameterSpec spec = new TerminalParameterSpec("global.projectiles.ap_armor_ignore",
                "projectiles", "projectiles", "ap_armor_ignore",
                TerminalParameterSpec.ValueType.DOUBLE, "0.5", 0, 1);
        for (String invalid : new String[] { "NaN", "Infinity", "-0.1", "1.01", "not-a-number" }) {
            assertThrows(RuntimeException.class, () -> spec.canonical(invalid));
        }
        assertEquals("0.75", spec.canonical(" 0.75 "));
    }

    @Test
    void rejectsOversizedMetadataAndBooleanImpostors() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalParameterSpec("key", "g".repeat(129),
                "target", "property", TerminalParameterSpec.ValueType.BOOLEAN, "false", 0, 1));
        TerminalParameterSpec toggle = new TerminalParameterSpec("toggle", "group", "target", "property",
                TerminalParameterSpec.ValueType.BOOLEAN, "false", 0, 1);
        assertThrows(IllegalArgumentException.class, () -> toggle.canonical("1"));
    }

    @Test
    void linearSpeedsAreEditedInBlocksPerSecondButStoredPerTick() {
        TerminalParameterSpec speed = new TerminalParameterSpec("aircraft.test.panel_speed",
                "aircraft", "test", "panel_speed", TerminalParameterSpec.ValueType.DOUBLE,
                "0.5", 0.01, 10);
        assertEquals("10.000000", speed.displayValue("0.5"));
        assertEquals("0.75", speed.canonicalDisplay("15"));
        assertEquals(0.2, speed.displayMin(), 1.0e-9);
        assertEquals(200.0, speed.displayMax(), 1.0e-9);
    }

    @Test
    void tickDurationsAreEditedInSecondsButStoredInTicks() {
        TerminalParameterSpec reload = new TerminalParameterSpec("aircraft.test.reload_time",
                "aircraft", "test", "reload_time", TerminalParameterSpec.ValueType.INTEGER,
                "60", 1, 12000);
        assertEquals("3.00", reload.displayValue("60"));
        assertEquals("60", reload.canonicalDisplay("3"));
        assertEquals(0.05, reload.displayMin(), 1.0e-9);
        assertEquals(600.0, reload.displayMax(), 1.0e-9);
    }

    @Test
    void floatWideningNoiseIsFormattedToHumanReadableValue() {
        // 0.05f 加宽成 double 是 0.05000000074505806，显示层应压回 0.05
        TerminalParameterSpec spread = new TerminalParameterSpec("cannon.test.vertical_spread",
                "cannon", "test", "vertical_spread", TerminalParameterSpec.ValueType.DOUBLE,
                Double.toString((double) 0.05f), 0, 180);
        String noisy = Double.toString((double) 0.05f);
        assertEquals("0.05", spread.displayValue(noisy));
        // 回填串必须仍可解析：编辑框直接回车不应因为格式化而报错
        assertEquals("0.05", spread.canonicalDisplay(spread.displayValue(noisy)));
    }

    @Test
    void specialCaseFormatsAreNotAffected() {
        TerminalParameterSpec drag = new TerminalParameterSpec("cannon.test.drag_coeff",
                "cannon", "test", "drag_coeff", TerminalParameterSpec.ValueType.DOUBLE,
                "0.00001", 0.00001, 1);
        assertEquals("1.000000e-05", drag.displayValue("0.00001"));

        TerminalParameterSpec speed = new TerminalParameterSpec("aircraft.test.panel_speed",
                "aircraft", "test", "panel_speed", TerminalParameterSpec.ValueType.DOUBLE,
                "0.5", 0.01, 10);
        assertEquals("10.000000", speed.displayValue("0.5"));

        TerminalParameterSpec reload = new TerminalParameterSpec("aircraft.test.reload_time",
                "aircraft", "test", "reload_time", TerminalParameterSpec.ValueType.INTEGER,
                "60", 1, 12000);
        assertEquals("3.00", reload.displayValue("60"));
    }

    @Test
    void plainValuesKeepTheirExactForm() {
        TerminalParameterSpec range = new TerminalParameterSpec("global.test.range", "test", "test",
                "range", TerminalParameterSpec.ValueType.DOUBLE, "12000", 0, 12000);
        assertEquals("12000", range.displayValue("12000"));
        assertEquals("100", range.displayValue("100"));
        assertEquals("1.15", range.displayValue("1.15"));

        TerminalParameterSpec count = new TerminalParameterSpec("global.test.count", "test", "test",
                "count", TerminalParameterSpec.ValueType.INTEGER, "100000", 0, 1000000);
        assertEquals("100000", count.displayValue("100000"));

        TerminalParameterSpec toggle = new TerminalParameterSpec("global.test.toggle", "test", "test",
                "toggle", TerminalParameterSpec.ValueType.BOOLEAN, "true", 0, 1);
        assertEquals("true", toggle.displayValue("true"));
    }
}
