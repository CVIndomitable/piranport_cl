package com.piranport.combat.cannon.fire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

class CannonFireRequestTest {
    @Test
    void rejectsInvalidPhysics() {
        assertEquals("velocity must be positive", CannonFireService.validatePhysics(1, 1, 0, 1, 0, 0, 0, 8));
        assertEquals("drag must be positive", CannonFireService.validatePhysics(1, 1, 2, 0, 0, 0, 0, 8));
        assertEquals("damage must be finite and non-negative", CannonFireService.validatePhysics(Float.NaN, 1, 2, 1, 0, 0, 0, 8));
        assertEquals("spread must be finite and non-negative", CannonFireService.validatePhysics(1, 1, 2, 1, 0, Float.NaN, 0, 8));
        assertEquals("sourceCaliber must be non-negative", CannonFireService.validatePhysics(1, 1, 2, 1, 0, 0, 0, -1));
    }

    @Test
    void type3ReservesAllPelletsBeforeFiringForEitherAdapter() {
        assertEquals(64, CannonFireService.entityCount(true));
        assertEquals(1, CannonFireService.entityCount(false));
        assertFalse(CannonFireService.withinLimit(1, 64, 64));
        assertTrue(CannonFireService.withinLimit(0, 64, 64));
        assertFalse(CannonFireService.withinLimit(0, 64, 0));
        assertFalse(CannonFireService.withinLimit(59, 2, 60));
    }

    @Test
    void multiBarrelType3CommitsOnlyCompleteShellPrefix() {
        var calls = new AtomicInteger();
        var result = CannonFireService.transact(List.of(true, true, true), shell -> {
            calls.incrementAndGet();
            return true;
        }, 0, 100, 0, 64, shell -> shell);
        assertEquals(1, result.shots());
        assertEquals(64, result.entities());
        assertFalse(result.complete());
        assertEquals(1, calls.get());
    }

    @Test
    void noCapacityLeavesAmmoAndEntitiesUntouched() {
        var result = CannonFireService.transact(List.of(true), shell -> {
            fail("capacity must reject before insertion");
            return true;
        }, 0, 100, 1, 64, shell -> shell);
        assertEquals(0, result.shots());
        assertEquals(0, result.entities());
    }

    @Test
    void insertionFailureRollsBackIncompleteShellButKeepsEarlierCost() {
        var live = new ArrayList<Integer>();
        var index = new AtomicInteger();
        var result = CannonFireService.transact(List.of(true, true), shell -> {
            int shellIndex = index.getAndIncrement();
            return CannonFireService.insertBatch(64, pellet -> shellIndex * 64 + pellet,
                    pellet -> {
                        if (pellet == 68) return false;
                        live.add(pellet);
                        return true;
                    }, pellet -> live.remove(Integer.valueOf(pellet)));
        }, 0, 100, 0, 128, shell -> shell);
        assertEquals(1, result.shots());
        assertEquals(64, result.entities());
        assertEquals(64, live.size());
        assertFalse(result.complete());
    }
}
