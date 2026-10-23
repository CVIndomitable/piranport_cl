package com.piranport.client;

import com.piranport.component.LoadedAmmo;
import com.piranport.registry.ModDataComponents;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

/**
 * 在武器物品图标上绘制空膛状态条。
 *
 * <p>鱼雷/导弹发射器已有 loaded/empty 两态图标，状态直接由图标表达，不再画黑条或绿条。
 * 只有深弹发射器没有两态图标，空膛时保留一条纯黑条提示。
 * 装填进度条也不画在物品图标上：统一由物品栏上方的 {@link ReloadProgressHudLayer} 大读条显示。
 */
public class WeaponReloadDecorator implements IItemDecorator {

    private static final int BAR_WIDTH = 13;
    private static final int BG_COLOR  = 0xFF000000;

    @Override
    public boolean render(GuiGraphics gui, Font font, ItemStack stack, int x, int y) {
        // 只处理没有两态图标的深弹发射器；满装填不画任何条。
        if (!(stack.getItem() instanceof com.piranport.item.DepthChargeLauncherItem)) {
            return false;
        }
        LoadedAmmo ammo = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (!ammo.hasAmmo()) {
            // 空膛：整条纯黑，同时把耐久条盖掉（黑底上露出原版彩色耐久条也很像"装了点什么"）
            drawBar(gui, stack, x, y, BAR_WIDTH, BG_COLOR);
        }
        return false;
    }

    /**
     * 在物品图标下方画一条水平指示条。
     *
     * <p>耐久物品下移到 y+14，避开原版 y+13 的耐久条；
     * 非耐久物品用 y+13。宽度 13、高度 2 与原版耐久条同规格。
     */
    private static void drawBar(GuiGraphics gui, ItemStack stack, int x, int y, int width, int color) {
        int barX = x + 2;
        int barY = stack.isDamageableItem() ? (y + 14) : (y + 13);
        gui.fill(barX, barY, barX + width, barY + 2, color);
    }

}
