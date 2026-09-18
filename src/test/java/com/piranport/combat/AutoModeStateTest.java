package com.piranport.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutoModeStateTest {
    @Test
    void hSwitchesAllAutomaticSystemsOnThenOff() {
        assertEquals(AutoModeState.ON, AutoModeState.OFF.next());
        assertEquals(AutoModeState.OFF, AutoModeState.OFF.next().next());
    }

    @Test
    void explicitOffWinsOverLegacyEnabledFlag() {
        assertEquals(AutoModeState.OFF, AutoModeState.resolve(0, true));
        assertEquals(AutoModeState.ON, AutoModeState.resolve(null, true));
        assertEquals(AutoModeState.OFF, AutoModeState.resolve(null, false));
    }

    @Test
    void formerIntermediateAndFullModesBecomeTheSameOnState() {
        assertEquals(AutoModeState.ON, AutoModeState.resolve(1, false));
        assertEquals(AutoModeState.ON, AutoModeState.resolve(2, false));
        assertEquals(AutoModeState.OFF, AutoModeState.resolve(999, true));
    }
}
