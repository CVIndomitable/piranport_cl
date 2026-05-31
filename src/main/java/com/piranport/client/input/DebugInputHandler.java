package com.piranport.client.input;

import com.piranport.debug.PiranPortDebug;
import com.piranport.network.DebugCooldownOverridePayload;
import com.piranport.network.DebugTogglePayload;
import com.piranport.network.HitDisplayTogglePayload;
import com.piranport.network.SnapshotRequestPayload;
import com.piranport.client.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 调试功能输入（F8/Shift+F8/N/J键）。
 *
 * <p><b>线程模型</b>: 客户端渲染线程（单线程），无需同步。
 * <p><b>状态管理</b>:
 *   - cooldownOverrideClientState: 调试冷却覆盖，在 onClientDisconnect 中重置。
 *   - hitDisplayEnabled: 命中显示开关，持久化于客户端会话。
 */
public class DebugInputHandler {

    private static boolean cooldownOverrideClientState = false;
    private static boolean hitDisplayEnabled = true;

    private DebugInputHandler() {}

    public static boolean isHitDisplayEnabled() { return hitDisplayEnabled; }

    public static void reset() {
        cooldownOverrideClientState = false;
        hitDisplayEnabled = true;
    }

    /** 处理所有调试快捷键输入。 */
    public static void handleDebugKeys(Minecraft mc) {
        if (mc.player == null) return;

        // F8 / Shift+F8：调试开关 / 快照
        while (ModKeyMappings.DEBUG_TOGGLE.consumeClick()) {
            if (Screen.hasShiftDown()) {
                PacketDistributor.sendToServer(new SnapshotRequestPayload());
                mc.player.displayClientMessage(
                        Component.literal("[PP] Snapshot written to logs/piranport-debug.log"), true);
            } else {
                boolean nowEnabled = PiranPortDebug.toggleClient();
                PacketDistributor.sendToServer(new DebugTogglePayload(nowEnabled));
                mc.player.displayClientMessage(
                        Component.literal(nowEnabled ? "[PP DEBUG] ON" : "[PP DEBUG] OFF"), true);
            }
        }

        // N 键：调试冷却覆盖
        while (ModKeyMappings.DEBUG_COOLDOWN_OVERRIDE.consumeClick()) {
            cooldownOverrideClientState = !cooldownOverrideClientState;
            boolean nowEnabled = cooldownOverrideClientState;
            PacketDistributor.sendToServer(new DebugCooldownOverridePayload(nowEnabled));
            mc.player.displayClientMessage(
                    Component.translatable(
                            nowEnabled ? "message.piranport.debug_cooldown_override_on"
                                       : "message.piranport.debug_cooldown_override_off"),
                    true);
        }

        // J 键：切换命中显示
        while (ModKeyMappings.HIT_DISPLAY_TOGGLE.consumeClick()) {
            hitDisplayEnabled = !hitDisplayEnabled;
            PacketDistributor.sendToServer(new HitDisplayTogglePayload(hitDisplayEnabled));
            mc.player.displayClientMessage(
                    Component.translatable(
                            hitDisplayEnabled ? "message.piranport.hit_display_on"
                                              : "message.piranport.hit_display_off"),
                    true);
        }
    }
}
