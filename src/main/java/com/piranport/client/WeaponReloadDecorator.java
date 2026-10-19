package com.piranport.client;

import com.piranport.component.LoadedAmmo;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
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
        // 防空导弹发射器始终满装填：弹药直接从背包消耗，不存在 LOADED_AMMO 状态，
        // 也没有"空膛"可言——画空膛条等于谎报状态（见 docs/策划决策/武器/鱼雷-再装填机制.md）。
        boolean isAutoReloadMissile = isAutoReloadMissile(stack);

        // 1. Manual-reload launchers (torpedo, missile, depth charge) — 空膛画黑条。
        //    装填进度不再画在物品图标上，统一由物品栏上方的 ReloadProgressHudLayer 大读条显示。
        //    防空导弹不在此列：它走自动装填，不该有"空膛"态。
        boolean showEmptyBar = (stack.getItem() instanceof MissileLauncherItem
                && ((MissileLauncherItem) stack.getItem()).getMissileType()
                == com.piranport.entity.MissileEntity.MissileType.ANTI_AIR)
                || (!isAutoReloadMissile
                && (stack.getItem() instanceof TorpedoLauncherItem
                || stack.is(ModItems.SY1_LAUNCHER.get())
                || stack.is(ModItems.MK14_HARPOON_LAUNCHER.get())
                || stack.is(ModItems.SHIP_ROCKET_LAUNCHER.get())
                || stack.getItem() instanceof com.piranport.item.DepthChargeLauncherItem));

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

    /** 是否为自动装填导弹类型；防空弹药现在仍需先经 R 键装入膛内。 */
    private static boolean isAutoReloadMissile(ItemStack stack) {
        return stack.getItem() instanceof MissileLauncherItem ml && !ml.isManualReload();
    }

}
