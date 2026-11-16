package com.piranport.aviation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AircraftLandingTimelineTest {
    @Test
    void approachesThenRollsAndReturnsToIdle() {
        var timeline = new AircraftLandingTimeline(16, 8, 6);
        assertEquals(0, timeline.approach(0));
        assertEquals(1.5f, timeline.scale(0, 0.38f));
        assertEquals(1, timeline.approach(16));
        assertEquals(0.38f, timeline.scale(16, 0.38f), 1e-6);
        assertEquals(0, timeline.roll(16));
        assertEquals(1, timeline.roll(24));
        assertEquals(1, timeline.levelWeight(6));
        assertEquals(1, timeline.levelWeight(24));
        assertEquals(0, timeline.levelWeight(30));
        assertEquals(30, timeline.durationTicks());
        float previous = 1.5f;
        for (float tick = 0; tick <= 30; tick += 0.125f) {
            float scale = timeline.scale(tick, 0.304f);
            assertTrue(scale <= previous + 1e-6);
            assertTrue(scale >= 0.304f - 1e-6);
            if (tick >= 16) assertEquals(0.304f, scale, 1e-6);
            previous = scale;
        }
    }

    @Test
    void shortApproachStillLevelsBeforeTouchdown() {
        var timeline = new AircraftLandingTimeline(1, 1, 100);
        assertEquals(1, timeline.levelWeight(1));
        assertEquals(1, timeline.levelWeight(2));
        assertEquals(0, timeline.levelWeight(102));
        assertEquals(0, timeline.approach(-1));
        assertEquals(1, timeline.roll(1000));
    }

    @Test
    void rejectsEmptyStages() {
        assertThrows(IllegalArgumentException.class, () -> new AircraftLandingTimeline(0, 8, 6));
        assertThrows(IllegalArgumentException.class, () -> new AircraftLandingTimeline(16, 0, 6));
        assertThrows(IllegalArgumentException.class, () -> new AircraftLandingTimeline(16, 8, 0));
    }
}
