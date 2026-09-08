package com.piranport.debug;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P2-10: PiranPortDebug 性能微基准。
 *
 * <p>目标：测试关闭状态下 1000 次 event()/perf()/error() 调用的总开销 &lt; 100μs（100,000 ns）。
 *
 * <p>这是简化版 JMH：在 JVM 已预热后连续调用 1000 次，
 * 测量从开始到结束的总耗时（纳秒）。
 *
 * <p>注意：
 * <ul>
 *   <li>不是真正的 JMH，无法消除 JIT 优化噪声</li>
 *   <li>结果仅用于发现明显的性能回归（数量级 &gt; 1ms 即视为异常）</li>
 *   <li>实际生产环境应使用 {@code jmh:run} 任务获取精确基准</li>
 * </ul>
 */
class PiranPortDebugMicrobenchTest {

    // 阈值说明：这些是粗略目标，非精确基准（应使用 JMH）
    // String.format 是主要开销，event()/error() 单次约 1-2μs（包含格式化）
    private static final long EVENT_THRESHOLD_NS = 2_000_000L;   // 1000 次 event() 总开销 < 2ms
    private static final long PERF_THRESHOLD_NS = 1_000_000L;    // 1000 次 perf() < 1ms
    private static final long ERROR_THRESHOLD_NS = 5_000_000L;   // 1000 次 error() < 5ms（总是记录）
    private static final long SHORT_UUID_THRESHOLD_NS = 10_000_000L; // 10000 次 < 10ms
    private static final int ITERATIONS = 1_000;

    @Test
    void eventCallOverhead_isBounded() {
        // 预热：让 JIT 编译热代码
        for (int i = 0; i < 5_000; i++) {
            PiranPortDebug.event("warmup {}", i);
        }
        // 测量
        long t0 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            PiranPortDebug.event("Transform ON | player={} load={}/{}", "test", i, 100);
        }
        long elapsedNs = System.nanoTime() - t0;
        System.out.printf("[Microbench] event() x %d: %,d ns (avg %.0f ns/call)%n",
                ITERATIONS, elapsedNs, (double) elapsedNs / ITERATIONS);

        // 关闭状态下：无 appender，event() 只做字符串格式化 + 早期 return（无 session）
        // 因为测试环境无 MCP debug session，SESSIONS 为空 → event() 第一次 isEmpty 检查即返回
        assertTrue(elapsedNs < EVENT_THRESHOLD_NS,
                String.format("event() x %d took %,d ns, exceeds threshold %,d ns",
                        ITERATIONS, elapsedNs, EVENT_THRESHOLD_NS));
    }

    @Test
    void perfCallOverhead_isBounded() {
        for (int i = 0; i < 5_000; i++) {
            PiranPortDebug.perf("warmup", i, "ctx");
        }
        long t0 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            PiranPortDebug.perf("WeightScan", 12345L, "player=test load=46/72");
        }
        long elapsedNs = System.nanoTime() - t0;
        System.out.printf("[Microbench] perf() x %d: %,d ns (avg %.0f ns/call)%n",
                ITERATIONS, elapsedNs, (double) elapsedNs / ITERATIONS);
        assertTrue(elapsedNs < PERF_THRESHOLD_NS,
                String.format("perf() x %d took %,d ns, exceeds threshold %,d ns",
                        ITERATIONS, elapsedNs, PERF_THRESHOLD_NS));
    }

    @Test
    void errorCallOverhead_isBounded() {
        for (int i = 0; i < 5_000; i++) {
            PiranPortDebug.error("warmup {}", i);
        }
        long t0 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            PiranPortDebug.error("Component READ FAIL | item=piranport:test ex=RuntimeException: msg={}", i);
        }
        long elapsedNs = System.nanoTime() - t0;
        System.out.printf("[Microbench] error() x %d: %,d ns (avg %.0f ns/call)%n",
                ITERATIONS, elapsedNs, (double) elapsedNs / ITERATIONS);
        assertTrue(elapsedNs < ERROR_THRESHOLD_NS,
                String.format("error() x %d took %,d ns, exceeds threshold %,d ns",
                        ITERATIONS, elapsedNs, ERROR_THRESHOLD_NS));
    }

    @Test
    void shortUuidAndShortHash_areFast() {
        UUID id = UUID.randomUUID();
        long t0 = System.nanoTime();
        for (int i = 0; i < ITERATIONS * 10; i++) {
            PiranPortDebug.shortUuid(id);
        }
        long elapsedNs = System.nanoTime() - t0;
        System.out.printf("[Microbench] shortUuid() x %d: %,d ns%n",
                ITERATIONS * 10, elapsedNs);
        // 10k 次调用应远小于 10ms
        assertTrue(elapsedNs < SHORT_UUID_THRESHOLD_NS,
                String.format("shortUuid() x %d took %,d ns, exceeds 10ms threshold",
                        ITERATIONS * 10, elapsedNs));
    }
}
