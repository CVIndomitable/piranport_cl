package com.piranport.dungeon.instance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DungeonScalingTest {

    @Test
    void waveCountIsCeilOfBaseTimesOnePlusPointTwoPerExtraPlayer() {
        // base=1: 1, ceil(1.2)=2, ceil(1.4)=2, ceil(1.6)=2
        assertEquals(1, DungeonScaling.waveCount(1, 1));
        assertEquals(2, DungeonScaling.waveCount(1, 2));
        assertEquals(2, DungeonScaling.waveCount(1, 3));
        assertEquals(2, DungeonScaling.waveCount(1, 4));
        // base=3: 3, ceil(3.6)=4, ceil(4.2)=5, ceil(4.8)=5
        assertEquals(3, DungeonScaling.waveCount(3, 1));
        assertEquals(4, DungeonScaling.waveCount(3, 2));
        assertEquals(5, DungeonScaling.waveCount(3, 3));
        assertEquals(5, DungeonScaling.waveCount(3, 4));
        // base=5: 5, 6, 7, 8 (exact integers, no float drift)
        assertEquals(6, DungeonScaling.waveCount(5, 2));
        assertEquals(7, DungeonScaling.waveCount(5, 3));
        assertEquals(8, DungeonScaling.waveCount(5, 4));
    }

    @Test
    void waveCountClampsPlayersAndBase() {
        assertEquals(1, DungeonScaling.waveCount(0, 0));
        assertEquals(DungeonScaling.waveCount(3, 4), DungeonScaling.waveCount(3, 9));
    }

    @Test
    void healthScaleIsOnePlusHalfPerExtraPlayer() {
        assertEquals(1.0, DungeonScaling.healthScale(1));
        assertEquals(1.5, DungeonScaling.healthScale(2));
        assertEquals(2.5, DungeonScaling.healthScale(4));
        assertEquals(2.5, DungeonScaling.healthScale(7));
    }

    @Test
    void nodeDifficultyOverridesStageAndStageIsFallback() {
        assertEquals(1.8, DungeonScaling.effectiveDifficulty(1.8, 1.2));
        assertEquals(1.2, DungeonScaling.effectiveDifficulty(0.0, 1.2));
        assertEquals(1.2, DungeonScaling.effectiveDifficulty(-1.0, 1.2));
        assertEquals(0.1, DungeonScaling.effectiveDifficulty(0.01, 1.2));
        assertEquals(1.0, DungeonScaling.effectiveDifficulty(0.0, 0.0));
    }

    @Test
    void lostTransportOnlyFromChapterTwo() {
        assertFalse(DungeonScaling.lostTransportEligible("chapter_1"));
        assertTrue(DungeonScaling.lostTransportEligible("chapter_2"));
        assertTrue(DungeonScaling.lostTransportEligible("chapter_7"));
        assertFalse(DungeonScaling.lostTransportEligible("event_goldencatcat"));
        assertFalse(DungeonScaling.lostTransportEligible("tutorial"));
        assertFalse(DungeonScaling.lostTransportEligible(null));
    }

    @Test
    void firstClearParticipationRequiresBeingInsideBossTile() {
        // tile 128, center (64,64) → [0,128)
        assertTrue(DungeonScaling.isInsideNodeTile(64, 64, 64, 64, 128));
        assertTrue(DungeonScaling.isInsideNodeTile(0, 127.9, 64, 64, 128));
        assertFalse(DungeonScaling.isInsideNodeTile(128, 64, 64, 64, 128));
        assertFalse(DungeonScaling.isInsideNodeTile(-0.1, 64, 64, 64, 128));
        assertFalse(DungeonScaling.isInsideNodeTile(300, 300, 64, 64, 128));
    }
}
