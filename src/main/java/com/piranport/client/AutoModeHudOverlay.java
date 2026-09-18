package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.combat.AASilenceManager;
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

/** 右下防空炮图标：开亮、关暗，静默额外叠加斜线，不改变总开关的亮暗。 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class AutoModeHudOverlay implements LayeredDraw.Layer {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "auto_mode_hud");

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR,
                ID, new AutoModeHudOverlay());
    }

    @Override
    public void render(GuiGraphics gfx, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui
                || !TransformationManager.isPlayerTransformed(mc.player)) return;
        boolean enabled = AutoModeState.fromStack(
                TransformationManager.findTransformedCore(mc.player)) == AutoModeState.ON;
        boolean silenced = AASilenceManager.isSilenced(mc.player);
        int x = gfx.guiWidth() - 28;
        int y = gfx.guiHeight() - 54;
        int color = enabled ? 0xFFFFDD55 : 0xFF666666;
        gfx.fill(x, y, x + 24, y + 24, 0x99000000);
        // 两根仰角炮管、炮塔与底座，使用像素图形避免额外材质依赖。
        for (int i = 0; i < 8; i++) {
            gfx.fill(x + 9 + i, y + 11 - i, x + 11 + i, y + 13 - i, color);
            gfx.fill(x + 5 + i, y + 9 - i, x + 7 + i, y + 11 - i, color);
        }
        gfx.fill(x + 6, y + 12, x + 17, y + 17, color);
        gfx.fill(x + 10, y + 16, x + 13, y + 20, color);
        gfx.fill(x + 4, y + 20, x + 20, y + 22, color);
        if (silenced) {
            for (int i = 0; i < 20; i++) {
                gfx.fill(x + 2 + i, y + 2 + i, x + 4 + i, y + 4 + i, 0xFFFF5555);
            }
        }
        Component label = Component.translatable(enabled
                ? "hud.piranport.auto_mode_on" : "hud.piranport.auto_mode_off");
        gfx.drawString(mc.font, label, x - mc.font.width(label) - 5, y + 8, color, true);
    }
}
