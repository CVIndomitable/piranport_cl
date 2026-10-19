package com.piranport.dungeon.escort;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EscortRouteTest {

    private static final double EPS = 1e-9;

    private static EscortRoute lShape() {
        return EscortRoute.planned(List.of(new Vec3(0, 64, 0), new Vec3(10, 64, 0), new Vec3(10, 64, 10)));
    }

    @Test
    void progressIsPathLengthFraction() {
        EscortRoute route = lShape();
        assertEquals(0.0, route.progress(0, 0), EPS);
        assertEquals(0.5, route.progress(10, 0), EPS);
        assertEquals(0.75, route.progress(10, 5), EPS);
        assertEquals(1.0, route.progress(10, 10), EPS);
    }

    @Test
    void progressProjectsOntoNearestSegmentOfDetour() {
        assertEquals(0.75, lShape().progress(9, 5), EPS);
    }

    @Test
    void progressClampsOutsideRoute() {
        EscortRoute route = lShape();
        assertEquals(0.0, route.progress(-50, 0), EPS);
        assertEquals(1.0, route.progress(10, 50), EPS);
    }

    @Test
    void pointAndDirectionFollowArcLength() {
        EscortRoute route = lShape();
        Vec3 mid = route.pointAt(15);
        assertEquals(10.0, mid.x, EPS);
        assertEquals(5.0, mid.z, EPS);
        Vec3 dir = route.directionAt(15);
        assertEquals(0.0, dir.x, EPS);
        assertEquals(1.0, dir.z, EPS);
        assertEquals(10.0, route.pointAt(100).x, EPS);
        assertEquals(10.0, route.pointAt(100).z, EPS);
    }

    @Test
    void waypointAdvancesOnceWithinReach() {
        EscortRoute route = lShape();
        Vec3 first = route.currentWaypoint(0, 0);
        assertEquals(10.0, first.x, EPS);
        assertEquals(0.0, first.z, EPS);
        assertEquals(10.0, route.currentWaypoint(5, 0).x, EPS);
        assertEquals(10.0, route.currentWaypoint(9, 0).z, EPS);
        assertEquals(10.0, route.currentWaypoint(10, 10).z, EPS);
    }

    @Test
    void replansEveryHundredTicksWhileMoving() {
        EscortRoute route = lShape();
        for (int i = 1; i < EscortRoute.REPLAN_INTERVAL_TICKS; i++) {
            assertFalse(route.advanceClock(i * 2.0, 0));
        }
        assertTrue(route.advanceClock(200, 0));
    }

    @Test
    void stalledTransportRequestsReplanEarly() {
        EscortRoute route = lShape();
        for (int i = 1; i < EscortRoute.STALL_TICKS; i++) {
            assertFalse(route.advanceClock(0, 0));
        }
        assertTrue(route.advanceClock(0, 0));
    }

    @Test
    void unreachableFlagMarksStraightFallback() {
        assertTrue(EscortRoute.unreachable(new Vec3(0, 64, 0), new Vec3(5, 64, 0)).unreachable());
        assertFalse(EscortRoute.straight(new Vec3(0, 64, 0), new Vec3(5, 64, 0)).unreachable());
    }
}
