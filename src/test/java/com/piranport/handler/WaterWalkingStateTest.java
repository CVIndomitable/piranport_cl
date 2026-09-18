package com.piranport.handler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/** 水面锁定与方向缓存的行为回归，不需要启动 Minecraft 世界。 */
class WaterWalkingStateTest {
    @Test
    void separatePlayerInstancesNeverShareSurfaceOrDirection() {
        WaterWalkingState client = new WaterWalkingState();
        WaterWalkingState server = new WaterWalkingState();
        client.bindToLevel(new Object());
        server.bindToLevel(new Object());

        client.resolveSurfaceY(80.0);
        client.forwardDirection(90.0f);
        server.resolveSurfaceY(63.0);
        server.forwardDirection(0.0f);

        assertEquals(80.0, client.resolveSurfaceY(79.0));
        assertEquals(63.0, server.resolveSurfaceY(62.0));
        assertEquals(-1.0, client.forwardDirection(90.0f).x(), 1e-6);
        assertEquals(0.0, server.forwardDirection(0.0f).x(), 1e-6);
        assertEquals(1.0, server.forwardDirection(0.0f).z(), 1e-6);
    }

    @Test
    void changingLevelRebasesSurfaceAndDirectionEvenWithSmallYawChange() {
        WaterWalkingState state = new WaterWalkingState();
        state.bindToLevel(new Object());
        state.resolveSurfaceY(100.0);
        WaterWalkingState.Direction oldDirection = state.forwardDirection(0.0f);

        state.bindToLevel(new Object());

        assertEquals(40.0, state.resolveSurfaceY(40.0));
        assertEquals(40.0, state.resolveSurfaceY(39.0));
        WaterWalkingState.Direction newDirection = state.forwardDirection(3.0f);
        assertNotSame(oldDirection, newDirection);
        assertEquals(-Math.sin(Math.toRadians(3.0)), newDirection.x(), 1e-6);
    }

    @Test
    void repeatedTicksInSameLevelPreserveLockAndDirectionCache() {
        WaterWalkingState state = new WaterWalkingState();
        Object level = new Object();
        state.bindToLevel(level);
        state.resolveSurfaceY(63.0);
        WaterWalkingState.Direction direction = state.forwardDirection(0.0f);

        state.bindToLevel(level);

        assertEquals(63.0, state.resolveSurfaceY(62.0));
        assertSame(direction, state.forwardDirection(5.0f));
        assertNotSame(direction, state.forwardDirection(6.0f));
    }

    @Test
    void leavingSurfaceAllowsEnteringLowerWaterWithoutSnappingBack() {
        WaterWalkingState state = new WaterWalkingState();
        state.resolveSurfaceY(63.0);

        state.clearSurface();

        assertEquals(40.0, state.resolveSurfaceY(40.0));
        assertEquals(40.0, state.resolveSurfaceY(39.0));
    }

    @Test
    void surfaceLockPreservesExistingToleranceAndAllowsUpwardMovement() {
        WaterWalkingState state = new WaterWalkingState();

        assertEquals(63.0, state.resolveSurfaceY(63.0));
        assertEquals(62.95, state.resolveSurfaceY(62.95));
        assertEquals(62.9, state.resolveSurfaceY(62.9));
        assertEquals(63.0, state.resolveSurfaceY(62.89));
        assertEquals(64.0, state.resolveSurfaceY(64.0));
    }
}
