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

    private TerminalParameters() {}

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
        applyClient(overrides, nextRevision);
    }

    public static void clearClient() {
        clientSnapshot = Map.of();
        clientValues = Map.of();
        clientSpecs = List.of();
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

    public static double getDouble(String key, double fallback) {
        String raw = active().get(key);
        if (raw == null) return fallback;
        try {
            double result = Double.parseDouble(raw);
            return Double.isFinite(result) ? result : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static int getInt(String key, int fallback) {
        String raw = active().get(key);
        if (raw == null) return fallback;
        try { return Integer.parseInt(raw); }
        catch (NumberFormatException e) { return fallback; }
    }

    public static boolean getBoolean(String key, boolean fallback) {
        String raw = active().get(key);
        return "true".equals(raw) ? true : "false".equals(raw) ? false : fallback;
    }
}
