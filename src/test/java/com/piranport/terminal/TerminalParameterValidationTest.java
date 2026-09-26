package com.piranport.terminal;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TerminalParameterValidationTest {
    private static final List<TerminalParameterSpec> SPECS = List.of(
            new TerminalParameterSpec("cannon.test.min_elevation", "cannon", "test", "min_elevation",
                    TerminalParameterSpec.ValueType.DOUBLE, "-5", -90, 90),
            new TerminalParameterSpec("cannon.test.max_elevation", "cannon", "test", "max_elevation",
                    TerminalParameterSpec.ValueType.DOUBLE, "45", -90, 90),
            new TerminalParameterSpec("global.test.count", "global", "test", "count",
                    TerminalParameterSpec.ValueType.INTEGER, "2", 0, 10),
            new TerminalParameterSpec("global.test.enabled", "global", "test", "enabled",
                    TerminalParameterSpec.ValueType.BOOLEAN, "false", 0, 1));

    @Test
    void validatesWholeEffectiveBatchIndependentOfEditOrder() {
        Map<String, String> valid = Map.of("cannon.test.min_elevation", "60",
                "cannon.test.max_elevation", "70");
        assertEquals(Map.of("cannon.test.min_elevation", "60.0",
                "cannon.test.max_elevation", "70.0"), TerminalParameterValidation.checked(valid, SPECS));
        assertThrows(IllegalArgumentException.class, () -> TerminalParameterValidation.checked(
                Map.of("cannon.test.min_elevation", "60"), SPECS));
        assertThrows(IllegalArgumentException.class, () -> TerminalParameterValidation.checked(
                Map.of("cannon.test.min_elevation", "70", "cannon.test.max_elevation", "60"), SPECS));
        for (Map<String, String> invalid : List.of(Map.of("missing", "1"),
                Map.of("global.test.count", "1.5"), Map.of("global.test.count", "11"),
                Map.of("global.test.enabled", "1"))) {
            assertThrows(RuntimeException.class, () -> TerminalParameterValidation.checked(invalid, SPECS));
        }
    }

    @Test
    void csvImportIsPureAndRejectsAnyBadRow() throws IOException {
        Map<String, String> previous = Map.of("global.test.count", "3");
        String header = "\uFEFFkey,value\r\n";
        Map<String, String> parsed = TerminalParameterCsv.parse(header
                + "\"global.test.count\",\"4\"\r\n\"global.test.enabled\",\"true\"\r\n", previous, SPECS);
        assertEquals("4", parsed.get("global.test.count"));
        assertEquals("true", parsed.get("global.test.enabled"));
        for (String bad : List.of("unknown,4", "global.test.count,abc", "global.test.count,11",
                "global.test.count,4\r\nglobal.test.count,5", "global.test.count,\"4",
                "global.test.count,4\r\nglobal.test.enabled,maybe")) {
            assertThrows(IOException.class, () -> TerminalParameterCsv.parse(header + bad, previous, SPECS));
            assertEquals(Map.of("global.test.count", "3"), previous);
        }
        assertThrows(IOException.class, () -> TerminalParameterCsv.parse(header + "x".repeat(1_048_576), previous, SPECS));
    }
}
