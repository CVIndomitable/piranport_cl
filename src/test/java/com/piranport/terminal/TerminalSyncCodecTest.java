package com.piranport.terminal;

import com.piranport.network.SyncTerminalParametersPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TerminalSyncCodecTest {
    @Test
    void roundtripPreservesEffectiveValuesAndMetadata() {
        TerminalParameterSpec spec = new TerminalParameterSpec("global.test.speed", "global", "test", "speed",
                TerminalParameterSpec.ValueType.DOUBLE, "1.0", 0, 100);
        SyncTerminalParametersPayload sent = new SyncTerminalParametersPayload(12,
                Map.of(spec.key(), "7.0"), Map.of(spec.key(), "7.0"), List.of(spec), "saved");
        ByteBuf buffer = Unpooled.buffer();
        try {
            SyncTerminalParametersPayload.STREAM_CODEC.encode(buffer, sent);
            assertEquals(sent, SyncTerminalParametersPayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }

    @Test
    void rejectsOversizedMapsAndMalformedCounts() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            SyncTerminalParametersPayload.STREAM_CODEC.encode(buffer,
                    new SyncTerminalParametersPayload(1, Map.of(), Map.of(), List.of(), ""));
            buffer.clear();
            net.minecraft.network.codec.ByteBufCodecs.VAR_LONG.encode(buffer, 1L);
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT.encode(buffer, 4097);
            assertThrows(RuntimeException.class, () -> SyncTerminalParametersPayload.STREAM_CODEC.decode(buffer));
            ByteBuf oversized = Unpooled.buffer();
            try {
                assertThrows(IllegalArgumentException.class, () -> SyncTerminalParametersPayload.STREAM_CODEC.encode(
                        oversized, new SyncTerminalParametersPayload(1, Map.of(), Map.of(),
                                java.util.Collections.nCopies(4097, new TerminalParameterSpec("key", "g", "t", "p",
                                        TerminalParameterSpec.ValueType.BOOLEAN, "false", 0, 1)), "")));
            } finally { oversized.release(); }
        } finally { buffer.release(); }
    }
}
