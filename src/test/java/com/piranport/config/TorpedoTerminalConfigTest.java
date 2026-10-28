package com.piranport.config;

import com.piranport.terminal.TerminalParameterSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TorpedoTerminalConfigTest {

    /** 参数是在 ModEquipmentConfig 类初始化时注册进 TerminalConfigValue.specs() 的；
     *  测试必须先触发该类初始化，否则 specs() 是空表。 */
    @BeforeAll
    static void initConfigRegistry() {
        ModEquipmentConfig.TORPEDO_BOMBER_TORPEDO_ENTRY_ANGLE.get();
    }

    @Test
    void movementTuningIsRegisteredAsGlobalTorpedoParameters() {
        Map<String, TerminalParameterSpec> specs = TerminalConfigValue.specs().stream()
                .filter(spec -> spec.key().startsWith("global.torpedo."))
                .collect(Collectors.toMap(TerminalParameterSpec::key, Function.identity()));

        // 用户口径 2026-10-28：鱼雷要「平着飞出去再落水」，水平不再衰减、垂直接近原版重力。
        assertSpec(specs, "air_fall_horizontal_decay", "1.0", 0.0, 1.0);
        assertSpec(specs, "air_fall_vertical_accel", "0.03", 0.0, 0.5);
        assertSpec(specs, "air_fall_terminal_speed", "0.3", 0.05, 1.0);
        assertSpec(specs, "wire_vertical_deadzone", "0.05", 0.0, 0.99);
    }

    /** 空投下落改为固定入水角后，旧的水平衰减/垂直加速度不得再注册（避免死参数）。 */
    @Test
    void removedAirDropDecayParametersAreNoLongerRegistered() {
        assertTrue(TerminalConfigValue.specs().stream()
                .noneMatch(spec -> spec.key().startsWith("global.torpedo.air_drop_")));
        assertTrue(TerminalConfigValue.specs().stream()
                .noneMatch(spec -> spec.key().endsWith(".torpedo_vertical_speed")));
    }

    @Test
    void torrentBomberEntryAngleIsRegistered() {
        TerminalParameterSpec spec = TerminalConfigValue.specs().stream()
                .filter(s -> s.key().equals("global.torpedo_bomber_combat.entry_angle"))
                .findFirst()
                .orElseThrow();
        assertEquals("aircraft", spec.group());
        assertEquals("torpedo_bomber_combat", spec.target());
        assertEquals(TerminalParameterSpec.ValueType.DOUBLE, spec.type());
        assertEquals(45.0, Double.parseDouble(spec.baseValue()), 1.0e-9);
        assertEquals(1.0, spec.min(), 1.0e-9);
        assertEquals(89.0, spec.max(), 1.0e-9);
        assertEquals(45.0, ModEquipmentConfig.TORPEDO_BOMBER_TORPEDO_ENTRY_ANGLE.get(), 1.0e-9);
    }

    @Test
    void torpedoMovementUsesTheMatchingTerminalParameters() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/piranport/entity/TorpedoEntity.java"));

        assertTrue(source.contains("TORPEDO_AIR_FALL_HORIZONTAL_DECAY.get()"));
        assertTrue(source.contains("TORPEDO_AIR_FALL_VERTICAL_ACCEL.get()"));
        assertTrue(source.contains("TORPEDO_AIR_FALL_TERMINAL_SPEED.get()"));
        assertTrue(source.contains("TORPEDO_WIRE_VERTICAL_DEADZONE.get()"));
        assertTrue(source.contains("TORPEDO_BOMBER_TORPEDO_ENTRY_ANGLE.get()"));
        // 空中回退必须走纯函数，不能再在实体里写死「乘衰减 + 加加速度」。
        assertTrue(source.contains("TorpedoAirFallBallistics."));
        assertFalse(source.contains("motion.y - ModEquipmentConfig.TORPEDO_AIR_FALL_VERTICAL_ACCEL"));
        assertFalse(source.contains("motion.x * ModEquipmentConfig.TORPEDO_AIR_FALL_HORIZONTAL_DECAY"));
        assertFalse(source.contains("motion.x * 0.98"));
        assertFalse(source.contains("motion.y - 0.08"));
    }

    @Test
    void movementTuningHasReadableTerminalLabelsAndDescriptions() throws Exception {
        for (String locale : new String[] { "zh_cn", "en_us" }) {
            String language = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "src/main/resources/assets/piranport/lang/" + locale + ".json"));
            for (String property : new String[] {
                    "air_fall_horizontal_decay", "air_fall_vertical_accel",
                    "air_fall_terminal_speed", "wire_vertical_deadzone"
            }) {
                assertTrue(language.contains("gui.piranport.debug_terminal.property." + property),
                        locale + " missing label " + property);
                assertTrue(language.contains("gui.piranport.debug_terminal.description.global.torpedo." + property),
                        locale + " missing description " + property);
            }
            // 新参数走属性级兜底描述（global.torpedo_bomber_combat.* 没有逐键描述）。
            assertTrue(language.contains("gui.piranport.debug_terminal.property.entry_angle"),
                    locale + " missing label entry_angle");
            assertTrue(language.contains("gui.piranport.debug_terminal.description.property.entry_angle"),
                    locale + " missing description entry_angle");
            assertFalse(language.contains("gui.piranport.debug_terminal.property.air_drop_horizontal_decay"),
                    locale + " still has removed label air_drop_horizontal_decay");
            assertFalse(language.contains("gui.piranport.debug_terminal.property.torpedo_vertical_speed"),
                    locale + " still has removed label torpedo_vertical_speed");
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
