package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.combat.AutoModeState;
import com.piranport.combat.TransformationManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 自动模式三态图标 — 渲染于屏幕右下角（HOTBAR 层上方）。
 *
 * <p>策划决策/副本/14：H 键"自动模式"总开关右下图标。</p>
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class AutoModeHudOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "auto_mode_hud");

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR,
                ID,
                new AutoModeHudOverlay()
        );
    }

    @Override
    public void render(GuiGraphics gfx, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!TransformationManager.isPlayerTransformed(mc.player)) return;

        AutoModeState mode = AutoModeState.fromStack(
                TransformationManager.findTransformedCore(mc.player));
        if (mode == AutoModeState.OFF) return; // OFF 状态不显示图标

        int screenW = gfx.guiWidth();
        int screenH = gfx.guiHeight();

        // 右下角，与 HOTBAR 对齐：Y = 屏幕高度 - 文字高度 - 4px 边距
        int textW = mc.font.width(getModeLabel(mode));
        int x = screenW - textW - 4;
        int y = screenH - mc.font.lineHeight - 4;

        // 半透明背景
        int bgColor = switch (mode) {
            case AA_ONLY -> 0x8800AAFF;   // 蓝色 = 仅防空
            case FULL_AUTO -> 0x88FFAA00;  // 黄色 = 全自动
            case OFF -> 0x88000000;
        };
        gfx.fill(x - 2, y - 1, x + textW + 2, y + mc.font.lineHeight + 1, bgColor);

        // 文字
        int textColor = switch (mode) {
            case AA_ONLY -> 0xFF55AAFF;
            case FULL_AUTO -> 0xFFFFDD55;
            case OFF -> 0xFFAAAAAA;
        };
        gfx.drawString(mc.font, getModeLabel(mode), x, y, textColor, false);
    }

    private static String getModeLabel(AutoModeState mode) {
        return Component.translatable(switch (mode) {
            case AA_ONLY -> "hud.piranport.auto_mode_aa_only";
            case FULL_AUTO -> "hud.piranport.auto_mode_full";
            case OFF -> "hud.piranport.auto_mode_off";
        }).getString();
    }
}
