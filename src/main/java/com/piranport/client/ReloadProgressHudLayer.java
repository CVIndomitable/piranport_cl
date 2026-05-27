package com.piranport.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.piranport.PiranPort;
import com.piranport.artillery.ArtilleryItem;
import com.piranport.component.WeaponCooldown;
import com.piranport.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * HUD 层：显示火炮装填进度条
 * Issue 10: 在 HUD 上显示装填进度，使其更加明显
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class ReloadProgressHudLayer {
    
    private static final ResourceLocation PROGRESS_BAR_TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/gui/reload_bar.png");
    
    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 8;
    private static final int BAR_PADDING = 4;
    
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (event.getName() != VanillaGuiLayers.HOTBAR) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        ItemStack mainHand = mc.player.getMainHandItem();
        if (!(mainHand.getItem() instanceof ArtilleryItem)) return;
        
        // 检查是否有冷却
        WeaponCooldown cooldown = mainHand.get(ModDataComponents.WEAPON_COOLDOWN.get());
        if (cooldown == null) return;
        
        long currentTime = mc.level.getGameTime();
        if (!cooldown.isOnCooldown(currentTime)) return;
        
        // 计算进度
        float progress = cooldown.getProgress(currentTime);
        float remaining = cooldown.getRemainingTicks(currentTime);
        
        // 渲染进度条
        renderProgressBar(event.getGuiGraphics(), mc, progress, remaining);
    }
    
    private static void renderProgressBar(GuiGraphics graphics, Minecraft mc, float progress, float remaining) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // 位置：准星下方
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight / 2 + 30;
        
        // 绘制背景
        graphics.fill(x - BAR_PADDING, y - BAR_PADDING, 
                x + BAR_WIDTH + BAR_PADDING, y + BAR_HEIGHT + BAR_PADDING, 
                0x80000000);
        
        // 绘制进度条背景
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF404040);
        
        // 绘制进度条
        int progressWidth = (int) (BAR_WIDTH * progress);
        if (progressWidth > 0) {
            // 根据进度改变颜色：红->黄->绿
            int color;
            if (progress < 0.33f) {
                color = 0xFFFF4040; // 红色
            } else if (progress < 0.66f) {
                color = 0xFFFFFF40; // 黄色
            } else {
                color = 0xFF40FF40; // 绿色
            }
            graphics.fill(x, y, x + progressWidth, y + BAR_HEIGHT, color);
        }
        
        // 绘制边框
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y, 0xFF808080); // 上
        graphics.fill(x - 1, y + BAR_HEIGHT, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xFF808080); // 下
        graphics.fill(x - 1, y, x, y + BAR_HEIGHT, 0xFF808080); // 左
        graphics.fill(x + BAR_WIDTH, y, x + BAR_WIDTH + 1, y + BAR_HEIGHT, 0xFF808080); // 右
        
        // 绘制百分比文本
        String percentText = String.format("%.0f%%", progress * 100);
        int textWidth = mc.font.width(percentText);
        graphics.drawString(mc.font, percentText, 
                x + (BAR_WIDTH - textWidth) / 2, 
                y + BAR_HEIGHT + 2, 
                0xFFFFFFFF, true);
        
        // 绘制剩余时间（秒）
        if (remaining > 0) {
            String timeText = String.format("%.1fs", remaining / 20.0f);
            graphics.drawString(mc.font, timeText, 
                    x + BAR_WIDTH + BAR_PADDING + 2, 
                    y + (BAR_HEIGHT - mc.font.lineHeight) / 2, 
                    0xFFCCCCCC, false);
        }
        
        // 绘制标签
        String label = "装填中";
        graphics.drawString(mc.font, label, 
                x - BAR_PADDING - mc.font.width(label) - 2, 
                y + (BAR_HEIGHT - mc.font.lineHeight) / 2, 
                0xFFFFFFFF, false);
    }
}
