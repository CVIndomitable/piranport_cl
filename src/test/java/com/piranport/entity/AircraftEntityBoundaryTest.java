package com.piranport.entity;

import com.piranport.component.AircraftInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** 飞机实体边界的稳定 ID 与旧序号兼容测试。 */
class AircraftEntityBoundaryTest {
    @Test
    void stateIdsAreStableAndUnknownIdsDoNotBecomeAValidState() {
        assertEquals("recon_active", stableStateId(AircraftEntity.FlightState.RECON_ACTIVE));
        assertEquals(AircraftEntity.FlightState.ATTACKING, stateById(" ATTACKING "));
        assertNull(stateById("state_added_later"));
    }

    @Test
    void aircraftTypeIdsAcceptSerializedAndLegacyNames() {
        assertEquals(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                aircraftTypeById("torpedo_bomber"));
        assertEquals(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                aircraftTypeById("TORPEDO_BOMBER"));
        assertNull(aircraftTypeById("missing_type"));
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

    private static AircraftInfo.AircraftType aircraftTypeById(String id) {
        if (id == null) return null;
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(AircraftInfo.AircraftType.values())
                .filter(type -> type.getSerializedName().equals(normalized)
                        || type.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }
}
