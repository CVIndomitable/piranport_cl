package com.piranport.terminal;

import java.util.Map;
import java.util.List;

/** 当前连接的参数覆盖镜像。服务端存档与客户端同步各自更新，玩法读取只走此入口。 */
public final class TerminalParameters {
    private static volatile Map<String, String> serverSnapshot = Map.of();
    private static volatile Map<String, String> clientSnapshot = Map.of();
    private static volatile Map<String, String> clientValues = Map.of();
    private static volatile List<TerminalParameterSpec> clientSpecs = List.of();
    private static volatile String clientMessage = "";
    private static volatile boolean dedicatedServer;
    private static volatile Thread serverThread;
    private static final ThreadLocal<Boolean> serverWork = ThreadLocal.withInitial(() -> false);
    private static volatile long serverRevision;
    private static volatile long clientRevision;
    private static volatile long syncSequence;
    // key -> spec 只读索引：getDouble/getInt 用它把解析出的值钳到 spec 的 [min, max]。
    // 客户端由 applyClient 同步的 spec 列表填充；服务端首次 miss 时从 TerminalParameterCatalog
    // 全量惰性构建（all() 会扫全物品/实体注册表，不能每 key 扫一次，故一次性建索引）。
    private static volatile Map<String, TerminalParameterSpec> clientSpecIndex = Map.of();
    private static volatile Map<String, TerminalParameterSpec> serverSpecIndex;

    private TerminalParameters() {}

    /** 取 key 的 spec；未知键返回 null。客户端读同步来的 spec；服务端读目录惰性索引。 */
    private static TerminalParameterSpec specFor(String key) {
        if (!(dedicatedServer || serverWork.get() || Thread.currentThread() == serverThread)) {
            return clientSpecIndex.get(key);
        }
        Map<String, TerminalParameterSpec> index = serverSpecIndex;
        if (index == null) {
            index = new java.util.HashMap<>();
            try {
                for (TerminalParameterSpec spec : TerminalParameterCatalog.all()) {
                    index.put(spec.key(), spec);
                }
            } catch (Throwable ignored) {
                // 目录在极早期（注册表未就绪）可能抛异常；此时放弃钳制，下次再试。
                return null;
            }
            serverSpecIndex = index;
        }
        return index.get(key);
    }

    /** 服务端目录变化（数据包重载等）后调用，强制下次读取重建 spec 索引。 */
    public static void invalidateServerSpecIndex() { serverSpecIndex = null; }

    public static void apply(Map<String, String> values, long nextRevision) {
        serverThread = Thread.currentThread();
        serverSnapshot = Map.copyOf(values);
        serverRevision = nextRevision;
    }

    public static void setDedicatedServer(boolean dedicated) { dedicatedServer = dedicated; }

    public static void clearServer() {
        serverSnapshot = Map.of();
        serverRevision = 0;
        dedicatedServer = false;
        serverThread = null;
    }

    public static void runServerWork(Runnable work) {
        boolean previous = serverWork.get();
        serverWork.set(true);
        try { work.run(); } finally { serverWork.set(previous); }
    }

    public static Map<String, String> serverOverrides() { return serverSnapshot; }
    public static long serverRevision() { return serverRevision; }

    public static void applyClient(Map<String, String> values, long nextRevision) {
        clientSnapshot = Map.copyOf(values);
        clientRevision = nextRevision;
        syncSequence++;
    }

    public static void applyClient(Map<String, String> values, Map<String, String> overrides,
                                   List<TerminalParameterSpec> specs, long nextRevision, String message) {
        clientValues = Map.copyOf(values);
        clientSpecs = List.copyOf(specs);
        clientMessage = message;
        Map<String, TerminalParameterSpec> index = new java.util.HashMap<>();
        for (TerminalParameterSpec spec : specs) index.put(spec.key(), spec);
        clientSpecIndex = Map.copyOf(index);
        applyClient(overrides, nextRevision);
    }

    public static void clearClient() {
        clientSnapshot = Map.of();
        clientValues = Map.of();
        clientSpecs = List.of();
        clientSpecIndex = Map.of();
        clientMessage = "";
        clientRevision++;
        syncSequence++;
    }

    public static long revision() { return clientRevision; }
    public static long syncSequence() { return syncSequence; }
    public static Map<String, String> overrides() { return clientSnapshot; }
    public static Map<String, String> clientValues() { return clientValues; }
    public static List<TerminalParameterSpec> clientSpecs() { return clientSpecs; }
    public static String clientMessage() { return clientMessage; }
    private static Map<String, String> active() {
        return dedicatedServer || serverWork.get() || Thread.currentThread() == serverThread
                ? serverSnapshot : clientValues;
    }

    private static Map<String, String> activeOverrides() {
        return dedicatedServer || serverWork.get() || Thread.currentThread() == serverThread
                ? serverSnapshot : clientSnapshot;
    }

    /**
     * 该键是否被策划「显式设置过」。服务端看服务端覆盖表、客户端看客户端覆盖表。
     * <p>
     * WHY 不能拿 {@link #getDouble} 的返回值判定：客户端同步的 {@code clientValues} 会把每个 spec 的
     * 键都填成 baseValue（未设置的键也在），所以「读到值」≠「被设置过」。只有覆盖表（serverSnapshot /
     * clientSnapshot）才只含策划改过的键。导弹发射器的「单型号 &gt; 共享默认 &gt; 物品基准」三层回退靠它判定。
     */
    public static boolean isOverridden(String key) {
        return activeOverrides().containsKey(key);
    }

    public static double getDouble(String key, double fallback) {
        String raw = active().get(key);
        if (raw == null) return fallback;
        try {
            double result = Double.parseDouble(raw);
            if (!Double.isFinite(result)) return fallback;
            // 解析成功后钳到 spec 的 [min, max]：存档里可能存着旧范围/手改存档的越界值，
            // 读取侧必须兜底，否则 UI 显示被钳、玩法却拿越界值生效，两侧口径分叉。
            TerminalParameterSpec spec = specFor(key);
            return spec != null ? spec.clamp(result) : result;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static int getInt(String key, int fallback) {
        String raw = active().get(key);
        if (raw == null) return fallback;
        try {
            int result = Integer.parseInt(raw);
            TerminalParameterSpec spec = specFor(key);
            return spec != null ? spec.clamp(result) : result;
        }
        catch (NumberFormatException e) { return fallback; }
    }

    public static boolean getBoolean(String key, boolean fallback) {
        String raw = active().get(key);
        return "true".equals(raw) ? true : "false".equals(raw) ? false : fallback;
    }
}
