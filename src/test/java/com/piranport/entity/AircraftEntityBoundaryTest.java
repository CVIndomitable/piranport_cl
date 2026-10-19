package com.piranport.entity;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** 飞机实体状态 ID 的稳定性测试。 */
class AircraftEntityBoundaryTest {
    @Test
    void stateIdsAreStableAndUnknownIdsDoNotBecomeAValidState() {
        assertEquals("recon_active", stableStateId(AircraftEntity.FlightState.RECON_ACTIVE));
        assertEquals(AircraftEntity.FlightState.ATTACKING, stateById(" ATTACKING "));
        assertNull(stateById("state_added_later"));
    }

    private static String stableStateId(AircraftEntity.FlightState state) {
        return state.name().toLowerCase(Locale.ROOT);
    }

    private static AircraftEntity.FlightState stateById(String id) {
        if (id == null) return null;
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(AircraftEntity.FlightState.values())
                .filter(state -> stableStateId(state).equals(normalized))
                .findFirst()
                .orElse(null);
    }
}
