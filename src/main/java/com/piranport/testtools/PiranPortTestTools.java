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
     * 测试模式水印文本（红色）。
     *
     * <p>注意：HUD 水印渲染层**尚未实装**（没有读取方）。当前的可见提示是
     * {@link com.piranport.network.TestModeWatermarkPayload} 发的聊天栏消息，
     * 客户端状态由 {@code DebugInputHandler.setTestModeClient} 保存供 N 键取反。
     * 实装 HUD 时从这里取文案即可。
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
            // 互斥检查：禁止在「调用者本人」有调试会话时开启测试模式。
            // 原实现用全局 isServerEnabled()（!SESSIONS.isEmpty()），会因别人开了会话
            // 而错误拒绝本人，也会让「测试模式与调试互斥」被顺序颠倒绕过。
            if (PiranPortDebug.isSessionActive(playerUuid)) {
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
            // 所有权校验：只有开启者本人（或服务端停机/登出路径）能关闭，
            // 否则任意 OP 都能关掉别人的测试模式，导致对方客户端水印永久残留。
            if (activePlayerUuid != null && playerUuid != null
                    && !activePlayerUuid.equals(playerUuid)) {
                return ToggleResult.NOT_OWNER;
            }
            java.util.UUID owner = activePlayerUuid;
            long sid = currentTestSessionId;
            cooldownOverrideEnabled = false;
            activePlayerUuid = null;
            currentTestSessionId = -1L;
            try {
                var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                // 水印撤销必须发给真正的属主，否则被劫持方的客户端状态永远停在 ON
                java.util.UUID target = owner != null ? owner : playerUuid;
                if (server != null && target != null) {
                    var sp = server.getPlayerList().getPlayer(target);
                    if (sp != null) {
                        PacketDistributor.sendToPlayer(sp,
                                new com.piranport.network.TestModeWatermarkPayload(false, sid));
                    }
                }
            } catch (Exception ignored) {}
            PiranPortDebug.event("[TEST] session=#{} reason=USER status=CLOSED by={}",
                    sid, PiranPortDebug.shortUuid(playerUuid));
            return ToggleResult.CLOSED;
        }
    }

    /** 服务端停止时强制关闭 */
    public static void closeAll() {
        // 早退条件必须同时看两个字段：只看布尔时，若有「布尔已 false 但 activePlayerUuid 非空」
        // 的中间态，属主信息与水印撤销会被跳过。
        if (!cooldownOverrideEnabled && activePlayerUuid == null) return;
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
     *
     * <p>无 owner 重载：仅供拿不到施法玩家的调用点使用，等价于「不享受覆盖」。
     * 新代码应一律使用 {@link #applyCooldownOverride(java.util.UUID, int)}。
     */
    public static int applyCooldownOverride(int ticks) {
        return applyCooldownOverride(null, ticks);
    }

    /**
     * 应用冷却覆盖（带所有者校验）。
     * 只有当前测试模式的属主才享受覆盖，避免 OP 开测试后全服玩家一起被改写冷却。
     *
     * @param owner 触发本次冷却的玩家 UUID；{@code null} 表示无玩家上下文（如女仆实体），不享受覆盖
     */
    public static int applyCooldownOverride(java.util.UUID owner, int ticks) {
        if (isActiveOwner(owner) && ticks > COOLDOWN_OVERRIDE_TICKS) {
            return COOLDOWN_OVERRIDE_TICKS;
        }
        return ticks;
    }

    /**
     * 测试模式下消耗物品时跳过（与 {@link PiranPortDebug#consumeAmmo} 等价但隔离）
     * 这是为了避免测试人员因为弹药耗尽而无法复现问题。
     *
     * <p>无 owner 重载：{@code null} 视为非属主，正常消耗。
     */
    public static void consumeAmmo(net.minecraft.world.item.ItemStack stack, int count) {
        consumeAmmo(null, stack, count);
    }

    /** 见 {@link #consumeAmmo(net.minecraft.world.item.ItemStack, int)}，带所有者校验 */
    public static void consumeAmmo(java.util.UUID owner,
                                   net.minecraft.world.item.ItemStack stack, int count) {
        if (!isActiveOwner(owner)) stack.shrink(count);
    }

    /**
     * 是否为当前测试模式的属主。测试模式是 JVM 全局态，但只有开启者本人应受其影响——
     * 否则任一 OP 按 N 会让全服玩家的武器冷却与弹药消耗一起被改写。
     */
    public static boolean isActiveOwner(java.util.UUID owner) {
        return cooldownOverrideEnabled && owner != null && owner.equals(activePlayerUuid);
    }

    public enum ToggleResult {
        OPENED(false, "OPENED"),
        CLOSED(false, "CLOSED"),
        ALREADY_ON(true, "ALREADY_ON"),
        ALREADY_OFF(false, "ALREADY_OFF"),
        DEBUG_ACTIVE(false, "DEBUG_ACTIVE"),
        /** 调用者不是当前测试模式的属主，拒绝关闭 */
        NOT_OWNER(false, "NOT_OWNER");

        public final boolean enabled;
        public final String status;

        ToggleResult(boolean enabled, String status) {
            this.enabled = enabled;
            this.status = status;
        }
    }
}