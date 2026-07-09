package com.piranport;

import com.piranport.platform.ClientHooks;

/**
 * 客户端 Tick 总入口 — 已拆分为 client input 包下多个 Handler。
 *
 * <p>本类保留为向后兼容的外壳，通过 {@link ClientHooks} 委托到客户端实现。
 * {@code @EventBusSubscriber} 注解已移至客户端 input 协调器。
 *
 * @deprecated 直接使用 {@link ClientHooks} 或客户端 input Handler。
 *   本外壳将在 v1.2.1 清理根包结构时移除。
 */
@Deprecated
public class ClientTickHandler {

    private ClientTickHandler() {}

    public static void resetClientState() {
        ClientHooks.resetClientState();
    }

    public static boolean isHighlightEnabled() {
        return ClientHooks.isHighlightEnabled();
    }
}
