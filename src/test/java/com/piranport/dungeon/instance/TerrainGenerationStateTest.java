package com.piranport.dungeon.instance;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 验证分帧游标和存档恢复，不启动完整 Minecraft 世界。 */
class TerrainGenerationStateTest {
    @Test
    void progressesInOrderAndDoesNotSkipPhase() {
        var state = new TerrainGenerationState();
        state.initialize(256, 256, 42L, 3);
        assertEquals(TerrainGenerationState.Phase.BASE, state.phase());
        state.advance(8192);
        assertEquals(8192, state.cursor());
        state.nextPhase();
        assertEquals(TerrainGenerationState.Phase.FEATURES, state.phase());
        assertEquals(0, state.cursor());
    }

    @Test
    void serializedCursorResumesAndReadyIsTerminal() {
        var source = new TerrainGenerationState();
        source.initialize(-10, 20, 123L, 5);
        source.advance(99);
        CompoundTag tag = source.save(new CompoundTag(), null);
        var restored = TerrainGenerationState.load(tag, null);
        assertTrue(restored.initialized());
        assertEquals(99, restored.cursor());
        assertEquals(-10, restored.startX());
        assertEquals(5, restored.terrainOrdinal());
        restored.markReady();
        restored.nextPhase();
        assertEquals(TerrainGenerationState.Phase.READY, restored.phase());
    }

    @Test
    void nodeBaseProgressSurvivesReloadWithoutRestarting() {
        var state = new TerrainGenerationState();
        state.initializeNode(256, 768, 42L, 2, false, false);
        assertEquals(TerrainGenerationState.Phase.BASE, state.phase());
        state.advance(12345);
        var restored = TerrainGenerationState.load(state.save(new CompoundTag(), null), null);
        restored.initializeNode(256, 768, 42L, 2, false, false);
        assertEquals(TerrainGenerationState.Phase.BASE, restored.phase());
        assertEquals(12345, restored.cursor());
    }

    @Test
    void legacyNodeSkipsBaseOnlyWhenSharedSeaExistsOrNodeWasEntered() {
        var oldNode = new TerrainGenerationState();
        oldNode.initializeNode(256, 256, 42L, 0, true, false);
        CompoundTag legacy = oldNode.save(new CompoundTag(), null);
        legacy.putInt("Version", 3);
        legacy.remove("NodeBasePlanned");

        var incomplete = TerrainGenerationState.load(legacy, null);
        incomplete.initializeNode(256, 256, 42L, 0, false, false);
        assertEquals(TerrainGenerationState.Phase.BASE, incomplete.phase());
        var completedSea = TerrainGenerationState.load(legacy, null);
        completedSea.initializeNode(256, 256, 42L, 0, true, false);
        assertEquals(TerrainGenerationState.Phase.FEATURES, completedSea.phase());
        var entered = TerrainGenerationState.load(legacy, null);
        entered.initializeNode(256, 256, 42L, 0, false, true);
        assertEquals(TerrainGenerationState.Phase.FEATURES, entered.phase());
    }

    @Test
    void legacyProgressedNodeIsNeverReset() {
        var oldNode = new TerrainGenerationState();
        oldNode.initializeNode(256, 256, 42L, 0, true, false);
        oldNode.advance(12);
        CompoundTag legacy = oldNode.save(new CompoundTag(), null);
        legacy.putInt("Version", 3);
        legacy.remove("NodeBasePlanned");
        var restored = TerrainGenerationState.load(legacy, null);
        restored.initializeNode(256, 256, 42L, 0, false, false);
        assertEquals(TerrainGenerationState.Phase.FEATURES, restored.phase());
        assertEquals(12, restored.cursor());
    }

    @Test
    void completedLegacyNodeRemainsReadyWithoutRebuilding() {
        var oldNode = new TerrainGenerationState();
        oldNode.initializeNode(256, 256, 42L, 0, true, false);
        oldNode.markReady();
        CompoundTag legacy = oldNode.save(new CompoundTag(), null);
        legacy.putInt("Version", 3);
        legacy.remove("NodeBasePlanned");
        var restored = TerrainGenerationState.load(legacy, null);
        restored.initializeNode(256, 256, 42L, 0, true, true);
        assertEquals(TerrainGenerationState.Phase.READY, restored.phase());
    }

    @Test
    void remainingWorkCountsNodeBaseAndEachPhaseSeparately() {
        long base = 512L * 512 * 17;
        assertEquals(base, TerrainGenerationPipeline.baseBlockCount());
        assertEquals(base + 500 + 75 + 1000,
                TerrainGenerationPipeline.remainingBlocks(TerrainGenerationState.Phase.BASE, 0, 500, 75, 1000));
        assertEquals(base - 8192 + 500 + 75 + 1000,
                TerrainGenerationPipeline.remainingBlocks(TerrainGenerationState.Phase.BASE, 8192, 500, 75, 1000));
        assertEquals(400 + 75 + 1000,
                TerrainGenerationPipeline.remainingBlocks(TerrainGenerationState.Phase.FEATURES, 100, 500, 75, 1000));
        assertEquals(0, TerrainGenerationPipeline.remainingBlocks(TerrainGenerationState.Phase.READY, 0, 500, 75, 1000));
    }

    @Test
    void nodeBaseColumnsStayInsideTheirOwnTile() {
        int centerX = -768, centerZ = 1280;
        assertEquals(-1024, TerrainGenerationPipeline.baseColumnX(centerX, 0));
        assertEquals(1024, TerrainGenerationPipeline.baseColumnZ(centerZ, 0));
        assertEquals(-513, TerrainGenerationPipeline.baseColumnX(centerX, 512 * 512 - 1));
        assertEquals(1535, TerrainGenerationPipeline.baseColumnZ(centerZ, 512 * 512 - 1));
        assertEquals(-1024, TerrainGenerationPipeline.baseColumnX(centerX, 512));
        assertEquals(1025, TerrainGenerationPipeline.baseColumnZ(centerZ, 512));
    }
}
