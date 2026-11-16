package com.piranport.client.input;

/** 调试控制页使用的服务端确认状态镜像。 */
public final class DebugInputHandler {
    private static boolean debugEnabledClientState;
    private static boolean hitDisplayEnabled = true;

    private DebugInputHandler() {}

    public static boolean isDebugEnabledClient() { return debugEnabledClientState; }
    public static boolean isHitDisplayEnabled() { return hitDisplayEnabled; }

    public static void setDebugEnabledClient(boolean enabled) {
        debugEnabledClientState = enabled;
    }

    /** 仅由服务端命中显示状态回包调用。 */
    public static void setHitDisplayEnabled(boolean enabled) {
        hitDisplayEnabled = enabled;
    }

    public static void reset() {
        debugEnabledClientState = false;
        hitDisplayEnabled = true;
    }
}
