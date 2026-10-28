package com.piranport.client;

import com.piranport.component.LoadedAmmo;
import com.piranport.component.WeaponCooldown;
import com.piranport.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

/**
 * 在武器物品图标上绘制状态条。
 *
 * <p>两条互不重叠的指示：
 * <ul>
 *   <li>深弹发射器的空膛纯黑条（没有两态图标，空膛只能靠黑条提示）；</li>
 *   <li>正在装填中的装备在耐久条正上方的小装填进度条，用于看其他装备的装填情况。</li>
 * </ul>
 *
 * <p>鱼雷/导弹发射器已有 loaded/empty 两态图标，状态直接由图标表达，不画空膛黑条。
 * 装填中的大读条仍由 {@link ReloadProgressHudLayer} 负责，这里只画"不在大读条上"的那些装备。
 */
public class WeaponReloadDecorator implements IItemDecorator {

    private static final int BAR_WIDTH = 13;
    private static final int BG_COLOR  = 0xFF000000;
    /** 与 {@link ReloadProgressHudLayer} 大读条同色，保持视觉一致。 */
    private static final int RELOAD_COLOR = 0xFFFF6B3D;

    @Override
    public boolean render(GuiGraphics gui, Font font, ItemStack stack, int x, int y) {
        drawDepthChargeEmptyBar(gui, stack, x, y);
        drawReloadProgressBar(gui, stack, x, y);
        // 只用了 fill，没有改动渲染状态，不需要 NeoForge 重置状态。
        return false;
    }

    /** 深弹发射器空膛：整条纯黑，同时盖掉原版彩色耐久条。 */
    private static void drawDepthChargeEmptyBar(GuiGraphics gui, ItemStack stack, int x, int y) {
        // 只处理没有两态图标的深弹发射器；满装填不画任何条。
        if (!(stack.getItem() instanceof com.piranport.item.DepthChargeLauncherItem)) {
            return;
        }
        LoadedAmmo ammo = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (!ammo.hasAmmo()) {
            // 空膛：整条纯黑，同时把耐久条盖掉（黑底上露出原版彩色耐久条也很像"装了点什么"）
            drawBar(gui, x, stack.isDamageableItem() ? (y + 14) : (y + 13), BAR_WIDTH, BG_COLOR);
        }
    }

    /**
     * 不在主手/大读条上的装备，装填中时在耐久条正上方画一条小进度条。
     *
     * <p>隐藏条件（WHY）：
     * <ul>
     *   <li>大读条正在显示的那一件（主手，或主手没有可装填武器时的副手兜底）——同一件装备
     *       同时出现大条和小条是重复信息；</li>
     *   <li>进度不在 (0, 1) 开区间——全空（刚开火/没有进度）和全满（装填完成）时不需要提示，
     *       这也是用户要的"全空或者全满都不显示"；</li>
     *   <li>不是可装填武器、没有 {@code WEAPON_COOLDOWN}、或鱼雷发射器没装再装填部件——
     *       复用大读条的 {@link ReloadProgressHudLayer#hasReloadBar}，避免旧存档里的异常冷却
     *       被误画成装填条。</li>
     * </ul>
     */
    private static void drawReloadProgressBar(GuiGraphics gui, ItemStack stack, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        // 物品装饰在部分预览界面（如无玩家上下文）也会被调用，没有玩家就无从判断主手，直接不画。
        if (player == null || mc.level == null) return;

        // 跳过「大读条正在显示的那一件」，否则同一件装备会同时出现大条和小条。
        // 判定必须与大读条完全一致：主手优先，主手没有可装填武器时才轮到副手兜底。
        // 用实例相等而不是 item 相等，否则背包里第二把同款武器也会被误跳过。
        if (stack == player.getMainHandItem()) return;
        if (stack == player.getOffhandItem()
                && !ReloadProgressHudLayer.hasReloadBar(player.getMainHandItem(), player)) {
            return;
        }

        // 与大读条共用武器类型 + 组件判断，非武器/无冷却/无再装填部件的鱼雷发射器不画。
        if (!ReloadProgressHudLayer.hasReloadBar(stack, player)) return;

        WeaponCooldown cooldown = stack.get(ModDataComponents.WEAPON_COOLDOWN.get());
        if (cooldown == null) return;
        long now = mc.level.getGameTime();
        if (!cooldown.isOnCooldown(now)) return;

        float progress = cooldown.getProgress(now);
        // isOnCooldown 已经保证 < 1，这里仍显式卡开区间：全空或全满都不画。
        if (progress <= 0f || progress >= 1f) return;

        // 耐久物品的原版耐久条在 y+13..y+15，小读条放在 y+11..y+13 正上方；
        // 非耐久物品没有原版耐久条，直接占用 y+13..y+15（与空膛黑条同一基准）。
        int barY = stack.isDamageableItem() ? (y + 11) : (y + 13);
        int barX = x + 2;
        gui.fill(barX, barY, barX + BAR_WIDTH, barY + 2, BG_COLOR);
        // 进度极小时至少露 1px 橙色，避免只剩黑底看起来像"空"。
        int progressWidth = Math.max(1, (int) (BAR_WIDTH * progress));
        gui.fill(barX, barY, barX + progressWidth, barY + 2, RELOAD_COLOR);
    }

    /** 在物品图标下方画一条水平指示条（宽度 13、高度 2，与原版耐久条同规格）。 */
    private static void drawBar(GuiGraphics gui, int x, int barY, int width, int color) {
        int barX = x + 2;
        gui.fill(barX, barY, barX + width, barY + 2, color);
    }

}
