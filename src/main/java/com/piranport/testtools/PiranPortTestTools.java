package com.piranport.testtools;

import com.piranport.debug.PiranPortDebug;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 测试工具集 — 与调试系统隔离。
 *
 * <p>职责：提供会修改玩法行为（冷却覆盖、弹药免消耗等）的"测试人员专用工具"。
 * 这些工具会污染游戏取证的纯净度，因此与 {@link PiranPortDebug} 严格隔离：
 * <ul>
 *   <li>独立的会话编号（testSessionId），互不干扰</li>
 *   <li>独立权限等级（OP 等级 2）</li>
 *   <li>互斥规则：测试模式开启时拒绝开启调试会话（避免证据污染）</li>
 *   <li>水印提示：开启时客户端显示红色 {@code [PP TEST MODE]} 横幅</li>
 *   <li>日志标签：每次操作日志自动附加 {@code [TEST]} 标签</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *   <li>不与 {@link PiranPortDebug} 共享任何状态字段</li>
 *   <li>读取消耗（弹药、物品）时检测测试模式并跳过，但保留其他逻辑（飞机起飞、武器开火）</li>
 *   <li>玩家开启测试模式前应确认无调试会话在跑（{@link PiranPortDebug#isServerEnabled()}）</li>
 * </ul>
 */
public final class PiranPortTestTools {

    private PiranPortTestTools() {}

    /** 冷却覆盖的全局开关（与 {@link PiranPortDebug} 解耦） */
    private static volatile boolean cooldownOverrideEnabled = false;

    /** 当前测试模式的玩家 UUID（同一时刻仅允许一名玩家开启） */
    private static volatile java.util.UUID activePlayerUuid = null;

    /** 测试会话编号（递增） */
    private static final java.util.concurrent.atomic.AtomicLong TEST_SESSION_COUNTER =
            new java.util.concurrent.atomic.AtomicLong(1);

    /** 当前测试会话编号；未开启时为 -1 */
    private static volatile long currentTestSessionId = -1L;

    /** 冷却覆盖时长（5 秒 = 100 tick） */
    public static final int COOLDOWN_OVERRIDE_TICKS = 100;

    /**
     * 测试模式水印文本（红色），客户端会在 HUD 渲染。
     * 渲染逻辑由 ClientHooks/ClientItemHooks 实现。
     */
    public static final String WATERMARK_TEXT = "§c[PP TEST MODE] §r测试模式 — 日志可能被污染";

    // -------------------------------------------------------------------------
    // Toggle (called from DebugCooldownOverridePayload handler)
    // -------------------------------------------------------------------------

    /**
     * 玩家请求开启/关闭测试模式。
     *
     * @return ToggleResult 枚举，便于反馈
     */
    public static ToggleResult toggleFor(java.util.UUID playerUuid, boolean wantEnabled) {
        if (wantEnabled) {
            // 互斥检查：禁止在调试会话激活时开启测试模式
            if (PiranPortDebug.isServerEnabled()) {
                return ToggleResult.DEBUG_ACTIVE;
            }
            if (cooldownOverrideEnabled) {
                return ToggleResult.ALREADY_ON;
            }
            cooldownOverrideEnabled = true;
            activePlayerUuid = playerUuid;
            currentTestSessionId = TEST_SESSION_COUNTER.getAndIncrement();
            // 通知客户端显示水印
            try {
                var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    var sp = server.getPlayerList().getPlayer(playerUuid);
                    if (sp != null) {
                        PacketDistributor.sendToPlayer(sp,
                                new com.piranport.network.TestModeWatermarkPayload(true, currentTestSessionId));
                    }
                }
            } catch (Exception ignored) {}
            PiranPortDebug.event("[TEST] session=#{} player={} status=OPENED",
                    currentTestSessionId, PiranPortDebug.shortUuid(playerUuid));
            return ToggleResult.OPENED;
        } else {
            if (!cooldownOverrideEnabled) {
                return ToggleResult.ALREADY_OFF;
            }
            long sid = currentTestSessionId;
            cooldownOverrideEnabled = false;
            activePlayerUuid = null;
            currentTestSessionId = -1L;
            try {
                var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null && playerUuid != null) {
                    var sp = server.getPlayerList().getPlayer(playerUuid);
                    if (sp != null) {
                        PacketDistributor.sendToPlayer(sp,
                                new com.piranport.network.TestModeWatermarkPayload(false, sid));
                    }
                }
            } catch (Exception ignored) {}
            PiranPortDebug.event("[TEST] session=#{} reason=USER status=CLOSED", sid);
            return ToggleResult.CLOSED;
        }
    }

    /** 服务端停止时强制关闭 */
    public static void closeAll() {
        if (!cooldownOverrideEnabled) return;
        long sid = currentTestSessionId;
        cooldownOverrideEnabled = false;
        java.util.UUID prev = activePlayerUuid;
        activePlayerUuid = null;
        currentTestSessionId = -1L;
        PiranPortDebug.event("[TEST] session=#{} reason=SERVER_STOP status=CLOSED", sid);
        if (prev != null) {
            try {
                var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    var sp = server.getPlayerList().getPlayer(prev);
                    if (sp != null) {
                        PacketDistributor.sendToPlayer(sp,
                                new com.piranport.network.TestModeWatermarkPayload(false, sid));
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    /** 玩家登出时清理 */
    public static void onPlayerLogout(java.util.UUID playerUuid) {
        if (playerUuid.equals(activePlayerUuid)) {
            closeAll();
        }
    }

    // -------------------------------------------------------------------------
    // Public read API
    // -------------------------------------------------------------------------

    public static boolean isCooldownOverrideEnabled() {
        return cooldownOverrideEnabled;
    }

    public static boolean isTestModeActive() {
        return cooldownOverrideEnabled;
    }

    public static long currentTestSessionId() {
        return currentTestSessionId;
    }

    public static java.util.UUID activePlayerUuid() {
        return activePlayerUuid;
    }

    /**
     * 应用冷却覆盖：当 ticks &gt; COOLDOWN_OVERRIDE_TICKS 时返回 COOLDOWN_OVERRIDE_TICKS。
     * 这是测试工具的核心功能，等价于"所有武器冷却都缩短到 5 秒"。
     */
    public static int applyCooldownOverride(int ticks) {
        if (cooldownOverrideEnabled && ticks > COOLDOWN_OVERRIDE_TICKS) {
            return COOLDOWN_OVERRIDE_TICKS;
        }
        return ticks;
    }

    /**
     * 测试模式下消耗物品时跳过（与 {@link PiranPortDebug#consumeAmmo} 等价但隔离）
     * 这是为了避免测试人员因为弹药耗尽而无法复现问题。
     */
    public static void consumeAmmo(net.minecraft.world.item.ItemStack stack, int count) {
        if (!cooldownOverrideEnabled) stack.shrink(count);
    }

    public enum ToggleResult {
        OPENED(false, "OPENED"),
        CLOSED(false, "CLOSED"),
        ALREADY_ON(true, "ALREADY_ON"),
        ALREADY_OFF(false, "ALREADY_OFF"),
        DEBUG_ACTIVE(false, "DEBUG_ACTIVE");

        public final boolean enabled;
        public final String status;

        ToggleResult(boolean enabled, String status) {
            this.enabled = enabled;
            this.status = status;
        }
    }
}