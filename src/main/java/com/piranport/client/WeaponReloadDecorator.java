package com.piranport.client;

import com.piranport.component.LoadedAmmo;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

/**
 * 在武器物品图标上绘制空膛/满膛状态条。
 *
 * <p>装填进度条不画在物品图标上：统一由物品栏上方的 {@link ReloadProgressHudLayer} 大读条显示。
 */
public class WeaponReloadDecorator implements IItemDecorator {

    private static final int BAR_WIDTH = 13;
    private static final int BG_COLOR  = 0xFF000000;
    /** 已装填指示条（导弹/深弹）：满绿，与原版"附魔光效 / 已充能"观感一致。 */
    private static final int READY_COLOR = 0xFF3FD23F;

    @Override
    public boolean render(GuiGraphics gui, Font font, ItemStack stack, int x, int y) {
        // 所有鱼雷/导弹/深弹发射器都有空膛态：
        // - 鱼雷、反舰/火箭导弹走装填设施；
        // - 防空导弹也要先按 R 装入一枚（见 ReloadHelper#reloadAntiAirMissile）。
        // 统一读 LOADED_AMMO 画条，不再按型号硬编码白名单，避免新增发射器漏画。
        boolean showEmptyBar = stack.getItem() instanceof TorpedoLauncherItem
                || stack.getItem() instanceof MissileLauncherItem
                || stack.getItem() instanceof com.piranport.item.DepthChargeLauncherItem;

        if (showEmptyBar) {
            LoadedAmmo ammo = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            if (!ammo.hasAmmo()) {
                // 空膛：整条纯黑，同时把耐久条盖掉（黑底上露出原版彩色耐久条也很像"装了点什么"）
                drawBar(gui, stack, x, y, BAR_WIDTH, BG_COLOR);
                return false;
            }
            // 已装填：鱼雷发射器靠 loaded/empty 图标切换分辨，不再画满条。
            // 其他手动发射器没有可靠的图标切换，保留满条。
            if (!(stack.getItem() instanceof TorpedoLauncherItem)) {
                drawBar(gui, stack, x, y, BAR_WIDTH, READY_COLOR);
            }
            return false;
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
