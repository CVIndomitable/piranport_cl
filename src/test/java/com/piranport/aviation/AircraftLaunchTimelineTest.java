package com.piranport.aviation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AircraftLaunchTimelineTest {
    @Test
    void boundariesAndScale() {
        var timeline = new AircraftLaunchTimeline(6, 8, 16);
        assertEquals(0f, timeline.deployment(-1));
        assertEquals(1f, timeline.deployment(6));
        assertEquals(0f, timeline.roll(6));
        assertEquals(1f, timeline.roll(14));
        assertEquals(0f, timeline.flight(14));
        assertEquals(0.38f, timeline.scale(14));
        assertEquals(1f, timeline.flight(30));
        assertEquals(1.5f, timeline.scale(30), 0.00001f);
        assertEquals(1.5f, timeline.scale(100), 0.00001f);
    }

    @Test
    void deckStaysLevelUntilDepartureAndAlwaysFinishesFolding() {
        for (int flightTicks : new int[]{1, 6, 16, 100}) {
            var timeline = new AircraftLaunchTimeline(6, 8, flightTicks);
            assertEquals(0, timeline.deckDeployment(0));
            assertEquals(1, timeline.deckDeployment(6));
            assertEquals(1, timeline.deckDeployment(13.999f));
            assertEquals(1, timeline.deckDeployment(14));
            assertEquals(0, timeline.deckDeployment(timeline.durationTicks()));
            float previousScale = timeline.scale(0);
            for (float age = 0; age <= timeline.durationTicks(); age += 0.25f) {
                float scale = timeline.scale(age);
                assertTrue(scale >= previousScale);
                if (age <= timeline.departureTick()) assertEquals(0.38f, scale);
                previousScale = scale;
            }
        }
    }

    @Test
    void rejectsEmptyStages() {
        assertThrows(IllegalArgumentException.class, () -> new AircraftLaunchTimeline(0, 8, 16));
        assertThrows(IllegalArgumentException.class, () -> new AircraftLaunchTimeline(6, 0, 16));
        assertThrows(IllegalArgumentException.class, () -> new AircraftLaunchTimeline(6, 8, 0));
    }
}
