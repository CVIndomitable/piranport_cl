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
}
