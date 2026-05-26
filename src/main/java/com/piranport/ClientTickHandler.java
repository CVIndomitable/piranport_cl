package com.piranport;

import com.piranport.client.input.ClientInputCoordinator;
import com.piranport.client.input.EntityHighlightHandler;

/**
 * 客户端 Tick 总入口 — 已拆分为 {@link com.piranport.client.input} 包下多个 Handler。
 *
 * <p>本类保留为向后兼容的外壳，将所有方法委托到 {@link ClientInputCoordinator} 和各 Handler。
 * {@code @EventBusSubscriber} 注解已移至 {@link ClientInputCoordinator}。
 *
 * @deprecated 直接使用 {@link ClientInputCoordinator} 和各 Handler。
 *   本外壳将在 v1.2.1 清理根包结构时移除。
 */
@Deprecated
public class ClientTickHandler {

    private ClientTickHandler() {}

    /** @see ClientInputCoordinator#resetClientState() */
    public static void resetClientState() {
        ClientInputCoordinator.resetClientState();
    }

    /** @see EntityHighlightHandler#isHighlightEnabled() */
    public static boolean isHighlightEnabled() {
        return EntityHighlightHandler.isHighlightEnabled();
    }
}
