package com.piranport.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScopeTargetingTest {
    private static final AABB SHIP = new AABB(-0.4, 63, 50, 0.4, 64.8, 50.8);

    @Test
    void aimingAtBodyOrHeadPreservesTheActualIntersectionHeight() {
        for (double height : new double[]{63.2, 64.0, 64.6}) {
            Vec3 start = new Vec3(0, height, 0);
            Vec3 end = new Vec3(0, height, 500);
            ScopeTargeting.Target target = ScopeTargeting.pick(start, end, miss(end), List.of(SHIP));

            assertTrue(target.valid());
            assertTrue(target.entity());
            assertEquals(new Vec3(0, height, 50), target.position(), "命中点不能被替换为实体脚底");
        }
    }

    @Test
    void surfaceAimStopsAtWaterInsteadOfTheSeabedOrRayEnd() {
        Vec3 start = new Vec3(0, 65, 0);
        Vec3 end = new Vec3(0, 45, 500);
        Vec3 water = new Vec3(0, 63, 50);
        ScopeTargeting.Target target = ScopeTargeting.pick(start, end, surface(water), List.of());

        assertTrue(target.valid());
        assertFalse(target.entity());
        assertEquals(water, target.position());
    }

    @Test
    void entityAboveWaterTakesPriorityOnlyWhenItsIntersectionIsNearer() {
        Vec3 start = new Vec3(0, 65, 0);
        Vec3 end = new Vec3(0, 55, 500);
        ScopeTargeting.Target target = ScopeTargeting.pick(start, end,
                surface(new Vec3(0, 63, 100)), List.of(SHIP));

        assertTrue(target.entity());
        assertEquals(new Vec3(0, 64, 50), target.position());
    }

    @Test
    void closerWallOrWaterOccludesTheEntity() {
        Vec3 start = new Vec3(0, 64, 0);
        Vec3 end = new Vec3(0, 64, 500);
        Vec3 wall = new Vec3(0, 64, 25);
        ScopeTargeting.Target target = ScopeTargeting.pick(start, end, surface(wall), List.of(SHIP));

        assertTrue(target.valid());
        assertFalse(target.entity());
        assertEquals(wall, target.position());
    }

    @Test
    void overlappingTargetsUseTheNearestIntersectionRegardlessOfIterationOrder() {
        Vec3 start = new Vec3(0, 64, 0);
        Vec3 end = new Vec3(0, 64, 500);
        AABB nearShip = SHIP.move(0, 0, -30);

        for (List<AABB> bounds : List.of(List.of(SHIP, nearShip), List.of(nearShip, SHIP))) {
            ScopeTargeting.Target target = ScopeTargeting.pick(start, end, miss(end), bounds);
            assertEquals(new Vec3(0, 64, 20), target.position());
        }
    }

    @Test
    void emptySeaHorizonRemainsAnExplicitMiss() {
        Vec3 start = new Vec3(0, 65, 0);
        Vec3 end = new Vec3(0, 65, 500);
        ScopeTargeting.Target target = ScopeTargeting.pick(start, end, miss(end), List.of(SHIP));

        assertFalse(target.valid());
        assertFalse(target.entity());
        assertEquals(end, target.position());
    }

    private static BlockHitResult miss(Vec3 end) {
        return BlockHitResult.miss(end, Direction.NORTH, BlockPos.containing(end));
    }

    private static BlockHitResult surface(Vec3 position) {
        return new BlockHitResult(position, Direction.UP, BlockPos.containing(position), false);
    }
}
