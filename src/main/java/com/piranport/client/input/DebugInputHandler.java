package com.piranport.client.input;

import com.piranport.network.DebugCooldownOverridePayload;
import com.piranport.network.DebugTogglePayload;
import com.piranport.network.HitDisplayTogglePayload;
import com.piranport.network.SnapshotRequestPayload;
import com.piranport.client.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 调试功能输入（F8/Shift+F8/N/J键）。
 *
 * <p><b>线程模型</b>: 客户端渲染线程（单线程），无需同步。
 * <p><b>状态管理</b>:
 *   - debugEnabledClientState: F8 调试开关本地显示，由服务端 ack 校正。
 *   - testModeClientState: N 键测试模式本地显示，由 TestModeWatermarkPayload 校正。
 *   - hitDisplayEnabled: 命中显示开关，持久化于客户端会话。
 *
 * <p><b>权威归服务端</b>：本类里的布尔量只是"最后一次服务端 ack 说到的状态"，
 * 按键时不预先翻转——服务端可能因权限 / 互斥 / 属主校验而拒绝（见
 * {@link com.piranport.network.DebugToggleAckPayload} 与
 * {@link com.piranport.network.DebugCooldownOverridePayload}），
 * 乐观翻转会让本地状态与被拒绝的请求一起说谎。按键只发包 + 一条提示，
 * 真实状态由 ack 覆盖。
 */
public class DebugInputHandler {

    private static boolean debugEnabledClientState = false;
    private static boolean hitDisplayEnabled = true;

    /**
     * 测试模式是否开启（客户端镜像）。
     *
     * <p>由 {@link com.piranport.network.TestModeWatermarkPayload} 校正 —— 服务端在每次
     * 成功开/关测试模式后都会推一份（含 player login 时的状态补推），因此这个值只在
     * 「刚按 N、ack 尚未到达」的瞬间可能过期，N 键据此取反的误判最多持续一个往返。
     */
    private static boolean testModeClientState = false;

    private DebugInputHandler() {}

    public static boolean isHitDisplayEnabled() { return hitDisplayEnabled; }

    public static boolean isDebugEnabledClient() { return debugEnabledClientState; }

    /** 由 DebugToggleAckPayload 校正本地状态，避免与服务端偏离 */
    public static void setDebugEnabledClient(boolean enabled) {
        debugEnabledClientState = enabled;
    }

    /**
     * 由 TestModeWatermarkPayload 校正测试模式本地状态。
     *
     * <p>注意：这个标志目前只服务于 N 键的「取反得到目标状态」，不驱动任何 HUD 渲染
     * （水印渲染层尚未实装）。
     */
    public static void setTestModeClient(boolean enabled) {
        testModeClientState = enabled;
    }

    public static boolean isTestModeClient() {
        return testModeClientState;
    }

    public static void reset() {
        debugEnabledClientState = false;
        testModeClientState = false;
        hitDisplayEnabled = true;
    }

    /** 处理所有调试快捷键输入。 */
    public static void handleDebugKeys(Minecraft mc) {
        if (mc.player == null) return;

        // F8 / Shift+F8：调试开关 / 快照
        while (ModKeyMappings.DEBUG_TOGGLE.consumeClick()) {
            if (Screen.hasShiftDown()) {
                PacketDistributor.sendToServer(new SnapshotRequestPayload(true));
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.piranport.snapshot_requested"),
                        true);
            } else {
                // 请求翻转：目标状态由本地镜像取反，但本地镜像不先改——
                // 服务端 TEST_ACTIVE / NO_PERMISSION 拒绝时镜像必须保持原值。
                boolean wantEnabled = !debugEnabledClientState;
                PacketDistributor.sendToServer(new DebugTogglePayload(wantEnabled));
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                wantEnabled ? "message.piranport.debug_on" : "message.piranport.debug_off"),
                        true);
            }
        }

        // N 键：调试冷却覆盖（独立测试工具）
        while (ModKeyMappings.DEBUG_COOLDOWN_OVERRIDE.consumeClick()) {
            // 必须携带目标状态：服务端 toggleFor(uuid, want) 的 want=false 才是"关闭"，
            // 永远发 true 会让 N 键只能开不能关（服务端对已开启返回 ALREADY_ON 且不翻转）。
            // 本地镜像由 TestModeWatermarkPayload 校正 —— toggleFor 无论开/关都会推一份，
            // 所以这里取的"我认为的当前值取反"最多错一次，之后立刻被服务端纠正。
            boolean wantEnabled = !testModeClientState;
            PacketDistributor.sendToServer(new DebugCooldownOverridePayload(wantEnabled));
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            wantEnabled ? "message.piranport.debug_cooldown_override_on"
                                        : "message.piranport.debug_cooldown_override_off"),
                    true);
        }

        // J 键：切换命中显示
        while (ModKeyMappings.HIT_DISPLAY_TOGGLE.consumeClick()) {
            hitDisplayEnabled = !hitDisplayEnabled;
            PacketDistributor.sendToServer(new HitDisplayTogglePayload(hitDisplayEnabled));
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            hitDisplayEnabled ? "message.piranport.hit_display_on"
                                              : "message.piranport.hit_display_off"),
                    true);
        }
    }
}