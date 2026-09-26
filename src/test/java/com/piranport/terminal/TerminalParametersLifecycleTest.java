package com.piranport.terminal;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TerminalParametersLifecycleTest {
    @Test
    void integratedServerAndClientKeepSeparateEffectiveValues() throws Exception {
        TerminalParameters.clearServer();
        TerminalParameters.clearClient();
        try {
            TerminalParameters.apply(Map.of("speed", "3"), 5);
            TerminalParameters.applyClient(Map.of("speed", "7", "base", "9"),
                    Map.of("speed", "3"), List.of(), 5, "");
            assertEquals(3, TerminalParameters.getInt("speed", 0));
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread client = new Thread(() -> {
                try {
                    assertEquals(7, TerminalParameters.getInt("speed", 0));
                    assertEquals(9, TerminalParameters.getInt("base", 0));
                    assertEquals(Map.of("speed", "3"), TerminalParameters.overrides());
                } catch (Throwable throwable) { failure.set(throwable); }
            });
            client.start();
            client.join();
            if (failure.get() != null) throw new AssertionError(failure.get());
            long sequence = TerminalParameters.syncSequence();
            TerminalParameters.applyClient(Map.of("speed", "8"), Map.of(), List.of(), 5, "ack");
            assertEquals(sequence + 1, TerminalParameters.syncSequence());
            assertEquals("ack", TerminalParameters.clientMessage());
            TerminalParameters.clearClient();
            assertTrue(TerminalParameters.clientValues().isEmpty());
            assertTrue(TerminalParameters.overrides().isEmpty());
            assertTrue(TerminalParameters.clientSpecs().isEmpty());
            assertEquals(3, TerminalParameters.getInt("speed", 0));
        } finally {
            TerminalParameters.clearClient();
            TerminalParameters.clearServer();
        }
    }
}
