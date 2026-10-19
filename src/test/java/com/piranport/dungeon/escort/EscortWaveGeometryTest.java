package com.piranport.dungeon.escort;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EscortWaveGeometryTest {

    private static final double EPS = 1e-9;

    @Test
    void progressIsZeroAtStartAndOneAtTarget() {
        assertEquals(0.0, EscortWaveGeometry.progress(0, 0, 100, 0, 0, 0), EPS);
        assertEquals(1.0, EscortWaveGeometry.progress(0, 0, 100, 0, 100, 0), EPS);
        assertEquals(0.5, EscortWaveGeometry.progress(0, 0, 100, 0, 50, 0), EPS);
    }

    @Test
    void progressClampsOutsideSegment() {
        assertEquals(0.0, EscortWaveGeometry.progress(0, 0, 100, 0, -50, 0), EPS);
    }

    @Test
    void degenerateRouteIsAlreadyComplete() {
        assertEquals(1.0, EscortWaveGeometry.progress(5, 5, 5, 5, 0, 0), EPS);
    }

    @Test
    void pathAnchorLeadsTransportAlongRoute() {
        var a = EscortWaveGeometry.anchors(0, 0, 100, 0, 0.0, 48, 60, 1.0);
        assertEquals(48.0, a.pathX(), EPS);
        assertEquals(0.0, a.pathZ(), EPS);
    }

    @Test
    void flankAnchorIsPerpendicularToRouteOnChosenSide() {
        var left = EscortWaveGeometry.anchors(0, 0, 100, 0, 0.0, 48, 60, 1.0);
        assertEquals(48.0, left.flankX(), EPS);
        assertEquals(60.0, left.flankZ(), EPS);

        var right = EscortWaveGeometry.anchors(0, 0, 100, 0, 0.0, 48, 60, -1.0);
        assertEquals(48.0, right.flankX(), EPS);
        assertEquals(-60.0, right.flankZ(), EPS);
    }

    @Test
    void pathAnchorMovesWithProgress() {
        var a = EscortWaveGeometry.anchors(0, 0, 100, 0, 0.5, 48, 60, 1.0);
        assertEquals(98.0, a.pathX(), EPS);
    }

    @Test
    void pathAnchorIsClampedToTarget() {
        var a = EscortWaveGeometry.anchors(0, 0, 30, 0, 0.0, 48, 60, 1.0);
        assertEquals(30.0, a.pathX(), EPS);
        assertEquals(0.0, a.pathZ(), EPS);
    }
}
