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
 *   - cooldownOverrideClientState: 调试冷却覆盖，在 onClientDisconnect 中重置。
 *   - hitDisplayEnabled: 命中显示开关，持久化于客户端会话。
 *
 * <p>反馈统一由服务端 {@link com.piranport.network.DebugToggleAckPayload} 负责，
 * 客户端不再立即提示成功/失败，避免假成功。
 */
public class DebugInputHandler {

    private static boolean debugEnabledClientState = false;
    private static boolean cooldownOverrideClientState = false;
    private static boolean hitDisplayEnabled = true;

    private DebugInputHandler() {}

    public static boolean isHitDisplayEnabled() { return hitDisplayEnabled; }

    public static boolean isDebugEnabledClient() { return debugEnabledClientState; }

    public static boolean isCooldownOverrideClientState() {
        return cooldownOverrideClientState;
    }

    /** 由 DebugToggleAckPayload 校正本地状态，避免与服务端偏离 */
    public static void setDebugEnabledClient(boolean enabled) {
        debugEnabledClientState = enabled;
    }

    public static void reset() {
        debugEnabledClientState = false;
        cooldownOverrideClientState = false;
        hitDisplayEnabled = true;
    }

    /** 处理所有调试快捷键输入。 */
    public static void handleDebugKeys(Minecraft mc) {
        if (mc.player == null) return;

        // F8 / Shift+F8：调试开关 / 快照
        while (ModKeyMappings.DEBUG_TOGGLE.consumeClick()) {
            if (Screen.hasShiftDown()) {
                PacketDistributor.sendToServer(new SnapshotRequestPayload(true));
                // 反馈由服务端 SnapshotRequestPayload 处理后通过 ClientHooks 返回
            } else {
                debugEnabledClientState = !debugEnabledClientState;
                PacketDistributor.sendToServer(new DebugTogglePayload(debugEnabledClientState));
            }
        }

        // N 键：调试冷却覆盖（独立测试工具）
        while (ModKeyMappings.DEBUG_COOLDOWN_OVERRIDE.consumeClick()) {
            cooldownOverrideClientState = !cooldownOverrideClientState;
            boolean nowEnabled = cooldownOverrideClientState;
            PacketDistributor.sendToServer(new DebugCooldownOverridePayload(nowEnabled));
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            nowEnabled ? "message.piranport.debug_cooldown_override_on"
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