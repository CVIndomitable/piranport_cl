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
}
