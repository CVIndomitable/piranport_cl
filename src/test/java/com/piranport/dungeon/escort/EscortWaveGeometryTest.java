package com.piranport.dungeon.escort;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EscortWaveGeometryTest {

    private static final double EPS = 1e-9;

    private static EscortRoute straightEastward() {
        return EscortRoute.straight(new Vec3(0, 64, 0), new Vec3(100, 64, 0));
    }

    @Test
    void pathAnchorLeadsTransportAlongRoute() {
        var a = EscortWaveGeometry.anchors(straightEastward(), 0.0, 48, 60, 1.0);
        assertEquals(48.0, a.pathX(), EPS);
        assertEquals(0.0, a.pathZ(), EPS);
    }

    @Test
    void flankAnchorIsPerpendicularToRouteOnChosenSide() {
        var left = EscortWaveGeometry.anchors(straightEastward(), 0.0, 48, 60, 1.0);
        assertEquals(48.0, left.flankX(), EPS);
        assertEquals(60.0, left.flankZ(), EPS);

        var right = EscortWaveGeometry.anchors(straightEastward(), 0.0, 48, 60, -1.0);
        assertEquals(48.0, right.flankX(), EPS);
        assertEquals(-60.0, right.flankZ(), EPS);
    }

    @Test
    void pathAnchorMovesWithProgress() {
        var a = EscortWaveGeometry.anchors(straightEastward(), 0.5, 48, 60, 1.0);
        assertEquals(98.0, a.pathX(), EPS);
    }

    @Test
    void pathAnchorIsClampedToTarget() {
        EscortRoute short30 = EscortRoute.straight(new Vec3(0, 64, 0), new Vec3(30, 64, 0));
        var a = EscortWaveGeometry.anchors(short30, 0.0, 48, 60, 1.0);
        assertEquals(30.0, a.pathX(), EPS);
        assertEquals(0.0, a.pathZ(), EPS);
    }

    @Test
    void pathAnchorFollowsDetourAroundCorner() {
        EscortRoute detour = EscortRoute.planned(List.of(new Vec3(0, 64, 0), new Vec3(10, 64, 0), new Vec3(10, 64, 10)));
        var a = EscortWaveGeometry.anchors(detour, 0.75, 0, 2, 1.0);
        assertEquals(10.0, a.pathX(), EPS);
        assertEquals(5.0, a.pathZ(), EPS);
        assertEquals(8.0, a.flankX(), EPS);
        assertEquals(5.0, a.flankZ(), EPS);
    }
}
