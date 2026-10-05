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

    /**
     * 覆盖表成员判定必须独立于 clientValues 的基准填充：客户端快照默认会给每个 spec 填 baseValue，
     * 若用「读到值」来判断「设置过」，导弹发射器三层回退会在客户端误命中共享默认值。
     */
    @Test
    void isOverriddenIgnoresBaseFilledClientValues() {
        TerminalParameters.clearServer();
        TerminalParameters.clearClient();
        try {
            TerminalParameters.apply(Map.of("missile_launcher.sy1_launcher.damage", "42"), 1);
            assertTrue(TerminalParameters.isOverridden("missile_launcher.sy1_launcher.damage"));
            assertFalse(TerminalParameters.isOverridden("global.missile_launcher.damage"));

            // apply() 把当前线程标记为服务端线程，读侧会优先服务端快照；先清掉再验客户端语义。
            TerminalParameters.clearServer();

            // 客户端 values 含未设置的基准值，但 overrides 为空。
            TerminalParameters.applyClient(
                    Map.of("global.missile_launcher.damage", "24",
                            "missile_launcher.mk14_harpoon_launcher.damage", "24"),
                    Map.of(), List.of(), 2, "");
            assertEquals(24.0, TerminalParameters.getDouble("global.missile_launcher.damage", -1), 1.0e-9);
            assertFalse(TerminalParameters.isOverridden("global.missile_launcher.damage"),
                    "基准填充值不得被当成显式覆盖");

            // 真正设置共享键后，覆盖表成员判定才为真。
            TerminalParameters.applyClient(
                    Map.of("global.missile_launcher.damage", "30"),
                    Map.of("global.missile_launcher.damage", "30"), List.of(), 3, "");
            assertTrue(TerminalParameters.isOverridden("global.missile_launcher.damage"));
        } finally {
            TerminalParameters.clearClient();
            TerminalParameters.clearServer();
        }
    }
}
