package com.piranport.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AASilenceStateTest {
    @Test
    void silenceEndsAtExactlyOneHundredTicksAfterFire() {
        AASilenceState state = new AASilenceState();
        state.start(1234);
        assertTrue(state.isActive(1234));
        assertTrue(state.isActive(1333));
        assertFalse(state.isActive(1334));
    }

    @Test
    void eachLaunchOrFireRefreshesTheFullWindow() {
        AASilenceState state = new AASilenceState();
        state.start(100);
        state.start(180);
        assertTrue(state.isActive(279));
        assertFalse(state.isActive(280));
    }

    @Test
    void clearedOrNewPlayerDoesNotInheritOldWindow() {
        AASilenceState old = new AASilenceState();
        old.start(500);
        assertFalse(new AASilenceState().isActive(500));
        old.clear();
        assertFalse(old.isActive(500));
    }

    @Test
    void synchronizedClientEndsAtTheSameTick() {
        AASilenceState server = new AASilenceState();
        AASilenceState client = new AASilenceState();
        server.start(600);
        client.setEndsAt(server.endsAt());
        assertEquals(server.isActive(699), client.isActive(699));
        assertEquals(server.isActive(700), client.isActive(700));
    }
}
