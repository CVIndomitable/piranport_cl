package com.piranport.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientBridgeTest {
    @AfterEach
    void resetBridge() {
        ClientHooks.install(new NoopClientBridge());
    }

    @Test
    void serverDefaultsWorkWithoutMinecraftClient() {
        ClientHooks.install(new NoopClientBridge());
        assertFalse(ClientHooks.isClient());
        assertNull(ClientHooks.getClientPlayer());
        assertEquals(-1, ClientHooks.getClientGameTime());
        assertEquals(123, ClientHooks.getAircraftGlowColor(null, 123));
        assertDoesNotThrow(() -> ClientHooks.openDungeonContinueScreen(BlockPos.ZERO, "stage", 1));
        assertDoesNotThrow(() -> ClientHooks.playSound(null, 1, 1));
    }

    @Test
    void soundAndLecternArgumentsReachTypedClientImplementation() {
        RecordingBridge recording = new RecordingBridge();
        ClientHooks.install(recording);
        SoundEvent sound = SoundEvent.createVariableRangeEvent(ResourceLocation.parse("piranport:test"));
        BlockPos pos = new BlockPos(1, 64, 2);

        ClientHooks.playSound(sound, 0.5f, 1.5f);
        ClientHooks.openDungeonContinueScreen(pos, "stage", 3);

        assertTrue(ClientHooks.isClient());
        assertSame(sound, recording.sound);
        assertEquals(0.5f, recording.volume);
        assertEquals(1.5f, recording.pitch);
        assertEquals(pos, recording.pos);
        assertEquals("stage", recording.stage);
        assertEquals(3, recording.cleared);
    }

    @Test
    void clientFailureIsNotSilentlyConvertedIntoDefaultValue() {
        IllegalStateException failure = new IllegalStateException("client failure");
        ClientHooks.install(new NoopClientBridge() {
            @Override
            public boolean isInReconMode() { throw failure; }
        });
        assertSame(failure, assertThrows(IllegalStateException.class, ClientHooks::isInReconMode));
    }

    private static final class RecordingBridge extends NoopClientBridge {
        SoundEvent sound;
        float volume;
        float pitch;
        BlockPos pos;
        String stage;
        int cleared;

        @Override
        public boolean isClient() { return true; }

        @Override
        public void playSound(SoundEvent sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        @Override
        public void openDungeonContinueScreen(BlockPos pos, String stage, int cleared) {
            this.pos = pos;
            this.stage = stage;
            this.cleared = cleared;
        }
    }
}
