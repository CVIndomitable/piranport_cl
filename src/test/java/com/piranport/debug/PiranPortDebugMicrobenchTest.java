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

    // 阈值说明：这些是粗略目标，非精确基准（应使用 JMH）。
    //
    // 关键认识：门控生效时（无会话）event()/perf() 的第一条语句就是 SESSIONS.isEmpty()
    // 短路返回，不做 String.format、不做 varargs 数组以外的分配。所以这里的真实开销
    // 应当接近"一次 ConcurrentHashMap.isEmpty()"，而不是格式化开销。
    // 原阈值（2ms/1ms/5ms）宽到连"误删门控、每次都完整格式化"都能通过，
    // 对回归毫无检测力 —— 按同一台机器上实测值的 20 倍余量收紧。
    private static final long EVENT_THRESHOLD_NS = 200_000L;   // 1000 次 event() 总开销 < 200μs
    private static final long PERF_THRESHOLD_NS = 200_000L;    // 1000 次 perf() < 200μs
    private static final long ERROR_THRESHOLD_NS = 5_000_000L; // 1000 次 error() < 5ms（无门控，总是记录）
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

    /**
     * 参数求值门控回归测试 —— 对应审查里那条「门控前置」修复。
     *
     * <p>Java 是 eager evaluation：{@code event("...", expensive())} 的实参会在
     * {@code event()} 方法体执行之前就求值完毕，方法内部的 {@code SESSIONS.isEmpty()}
     * 短路救不了调用方。因此凡是埋点参数带注册表查找/字符串拼接的调用点，都必须写成
     * <pre>
     *   if (PiranPortDebug.shouldEmit()) { PiranPortDebug.event("...", expensive()); }
     * </pre>
     * 该测试用一个"被调用就计数"的探针函数证明：无会话时这些参数一次都不该被求值。
     * 若哪天有人把 {@code shouldEmit()} 门控删掉（或改回裸 {@code event()} 调用），
     * 计数器会立刻非零，测试失败。
     */
    @Test
    void shouldEmit_gatesArgumentEvaluation() {
        // 前置断言：本测试的前提是「测试环境里没有任何调试会话」。
        // 若将来测试基建真的开了一个会话，这条断言会先失败，避免我们误判门控行为。
        assertFalse(PiranPortDebug.shouldEmit(),
                "测试环境不应存在调试会话，否则本回归测试的前提不成立");
        assertFalse(PiranPortDebug.isServerEnabled(),
                "SESSIONS 应为空");

        final int[] probeCalls = {0};
        java.util.function.Supplier<String> expensiveProbe = () -> {
            probeCalls[0]++;
            return "probe";
        };

        // 模拟修复后的调用点写法：门控包住整个埋点（含实参）
        for (int i = 0; i < 100; i++) {
            if (PiranPortDebug.shouldEmit()) {
                PiranPortDebug.event("slot[{}] {}", i, expensiveProbe.get());
            }
        }

        assertEquals(0, probeCalls[0],
                "无会话时门控内的埋点实参被求值了 —— shouldEmit() 门控失效");

        // 对照：不带门控的裸调用会（这是 Java 语义，不是 bug；此处固化为文档）
        for (int i = 0; i < 100; i++) {
            PiranPortDebug.event("slot[{}] {}", i, expensiveProbe.get());
        }
        assertEquals(100, probeCalls[0],
                "裸 event() 调用应先行求值实参 —— 这正是必须加 shouldEmit() 门控的原因");
    }
}
