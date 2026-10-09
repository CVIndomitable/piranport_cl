package com.piranport.config;

import com.piranport.terminal.TerminalParameterSpec;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TorpedoTerminalConfigTest {
    @Test
    void movementTuningIsRegisteredAsGlobalTorpedoParameters() {
        assertEquals(0.98, ModEquipmentConfig.TORPEDO_AIR_DROP_HORIZONTAL_DECAY.get(), 1.0e-9);
        Map<String, TerminalParameterSpec> specs = TerminalConfigValue.specs().stream()
                .filter(spec -> spec.key().startsWith("global.torpedo."))
                .collect(Collectors.toMap(TerminalParameterSpec::key, Function.identity()));

        assertSpec(specs, "air_drop_horizontal_decay", "0.98", 0.0, 1.0);
        assertSpec(specs, "air_drop_vertical_accel", "0.08", 0.0, 0.5);
        assertSpec(specs, "air_fall_horizontal_decay", "0.7", 0.0, 1.0);
        assertSpec(specs, "air_fall_vertical_accel", "0.25", 0.0, 0.5);
        assertSpec(specs, "wire_vertical_deadzone", "0.05", 0.0, 0.99);
    }

    @Test
    void torpedoMovementUsesTheMatchingTerminalParameters() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/piranport/entity/TorpedoEntity.java"));

        assertTrue(source.contains("TORPEDO_AIR_DROP_HORIZONTAL_DECAY.get()"));
        assertTrue(source.contains("TORPEDO_AIR_DROP_VERTICAL_ACCEL.get()"));
        assertTrue(source.contains("TORPEDO_AIR_FALL_HORIZONTAL_DECAY.get()"));
        assertTrue(source.contains("TORPEDO_AIR_FALL_VERTICAL_ACCEL.get()"));
        assertTrue(source.contains("TORPEDO_WIRE_VERTICAL_DEADZONE.get()"));
        assertFalse(source.contains("motion.x * 0.98"));
        assertFalse(source.contains("motion.y - 0.08"));
    }

    @Test
    void movementTuningHasReadableTerminalLabelsAndDescriptions() throws Exception {
        for (String locale : new String[] { "zh_cn", "en_us" }) {
            String language = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "src/main/resources/assets/piranport/lang/" + locale + ".json"));
            for (String property : new String[] {
                    "air_drop_horizontal_decay", "air_drop_vertical_accel", "air_fall_horizontal_decay",
                    "air_fall_vertical_accel", "wire_vertical_deadzone"
            }) {
                assertTrue(language.contains("gui.piranport.debug_terminal.property." + property),
                        locale + " missing label " + property);
                assertTrue(language.contains("gui.piranport.debug_terminal.description.global.torpedo." + property),
                        locale + " missing description " + property);
            }
        }
    }

    private static void assertSpec(Map<String, TerminalParameterSpec> specs, String property,
                                   String base, double min, double max) {
        TerminalParameterSpec spec = specs.get("global.torpedo." + property);
        assertNotNull(spec, property);
        assertEquals("equipment", spec.group());
        assertEquals("torpedo", spec.target());
        assertEquals(TerminalParameterSpec.ValueType.DOUBLE, spec.type());
        assertEquals(Double.parseDouble(base), Double.parseDouble(spec.baseValue()), 1.0e-9);
        assertEquals(min, spec.min());
        assertEquals(max, spec.max());
        assertEquals(base, spec.canonical(base));
        assertFalse(spec.isLinearSpeed());
        assertFalse(spec.isTickDuration());
    }
}
