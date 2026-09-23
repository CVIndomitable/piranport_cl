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
 *   - debugEnabledClientState: F8 调试开关本地显示，初始由服务端 ack 校正。
 *   - hitDisplayEnabled: 命中显示开关，持久化于客户端会话。
 *
 * <p><b>权威归服务端</b>：本类里的布尔量只是"最后一次服务端 ack 说到的状态"，
 * 按键时不预先翻转——服务端可能因权限 / 互斥 / 属主校验而拒绝（见
 * {@link com.piranport.network.DebugToggleAckPayload} 与
 * {@link com.piranport.network.DebugCooldownOverridePayload}），
 * 乐观翻转会让本地状态与被拒绝的请求一起说谎。按键只发包 + 一条"已发送"提示，
 * 真实状态由 ack 覆盖。
 */
public class DebugInputHandler {

    private static boolean debugEnabledClientState = false;
    private static boolean hitDisplayEnabled = true;

    private DebugInputHandler() {}

    public static boolean isHitDisplayEnabled() { return hitDisplayEnabled; }

    public static boolean isDebugEnabledClient() { return debugEnabledClientState; }

    /** 由 DebugToggleAckPayload 校正本地状态，避免与服务端偏离 */
    public static void setDebugEnabledClient(boolean enabled) {
        debugEnabledClientState = enabled;
    }

    /**
     * 测试模式水印状态同步入口。
     *
     * <p>为什么保留空实现：测试模式的 HUD 水印渲染尚未落地（原计划由 ClientHooks
     * 读 {@code PiranPortTestTools.WATERMARK_TEXT} 画，实际没有调用方）。
     * 这里刻意不缓存布尔量——存了也没人读，只会像之前那样留下一个误导性的
     * "P2-9 已实现"注释；水印真正实装时应该在这个方法里接 HUD 渲染层。
     */
    public static void setTestModeClient(boolean enabled) {
        // no-op：等待水印渲染实装
    }

    public static void reset() {
        debugEnabledClientState = false;
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
            // cooldownOverrideClientState 已移除：服务端是唯一权威，
            // 本地想翻转的目标值由"我认为的当前值取反"退化为"请求开启"，
            // 因为 ack 不带回该标志；由服务端幂等（ALREADY_ON / ALREADY_OFF）兜住。
            PacketDistributor.sendToServer(new DebugCooldownOverridePayload(true));
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