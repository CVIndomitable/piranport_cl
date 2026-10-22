package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.artillery.ArtilleryItem;
import com.piranport.combat.TransformationManager;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.WeaponCooldown;
import com.piranport.item.AircraftItem;
import com.piranport.item.DepthChargeLauncherItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * HUD 层：显示武器装填进度条。
 *
 * <p>物品图标上的小读条已移除，这里是装填进度的唯一显示。火炮、舰载机、鱼雷/导弹/深弹
 * 发射器共用 {@code WEAPON_COOLDOWN} 计时，因此只认组件与武器类型。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class ReloadProgressHudLayer {

    /**
     * 装填进度条挂靠的原版 GUI 层：经验条（hotbar）层，所以宽度也照抄经验条的 182。
     * 配置检查器直接读这个字符串，改层就等于改样式。
     */
    public static final String STYLE = "HOTBAR";

    private static final int BAR_WIDTH = 182;  // 与原版经验条同宽
    private static final int BAR_HEIGHT = 5;

    // 动画状态追踪
    private static long lastVisibleTick = 0;
    private static long completionTick = -1;
    private static final int FADE_IN_TICKS = 5;        // 0.25秒淡入
    private static final int FADE_OUT_TICKS = 10;      // 0.5秒淡出
    private static final int STAY_VISIBLE_TICKS = 40;  // 装填完成后保持2秒
    
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!event.getName().getPath().equalsIgnoreCase(STYLE)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 主手优先，副手兜底：鱼雷发射器经常放副手。
        ItemStack weapon = mc.player.getMainHandItem();
        if (!hasReloadBar(weapon, mc.player)) {
            weapon = mc.player.getOffhandItem();
            if (!hasReloadBar(weapon, mc.player)) return;
        }

        // 检查是否有冷却
        WeaponCooldown cooldown = weapon.get(ModDataComponents.WEAPON_COOLDOWN.get());
        if (cooldown == null) return;

        long currentTime = mc.level.getGameTime();
        if (!cooldown.isOnCooldown(currentTime)) {
            // 装填完成后的淡出处理
            if (completionTick >= 0) {
                float progress = 1.0f;
                renderProgressBar(event.getGuiGraphics(), mc, progress, currentTime);
            }
            return;
        }

        // 计算进度
        float progress = cooldown.getProgress(currentTime);

        // 渲染进度条
        renderProgressBar(event.getGuiGraphics(), mc, progress, currentTime);
    }
    
    /**
     * 该武器是否用大读条显示 {@code WEAPON_COOLDOWN}。
     *
     * <p>没有装填手段的空膛鱼雷发射器，开火后的冷却不是"装填中"，不画读条，
     * 与原物品图标小读条的行为保持一致。
     */
    private static boolean hasReloadBar(ItemStack stack, Player player) {
        if (stack.isEmpty()) return false;
        boolean isWeapon = stack.getItem() instanceof ArtilleryItem
                || stack.getItem() instanceof AircraftItem
                || stack.getItem() instanceof TorpedoLauncherItem
                || stack.getItem() instanceof MissileLauncherItem
                || stack.getItem() instanceof DepthChargeLauncherItem;
        if (!isWeapon) return false;
        WeaponCooldown cooldown = stack.get(ModDataComponents.WEAPON_COOLDOWN.get());
        if (cooldown == null) return false;
        if (stack.getItem() instanceof TorpedoLauncherItem) {
            LoadedAmmo loaded = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            if (!loaded.hasAmmo()) {
                ItemStack core = TransformationManager.findTransformedCore(player);
                boolean canReload = !core.isEmpty()
                        && TransformationManager.hasTorpedoReloadEquipped(player, core);
                if (!canReload) return false;
            }
        }
        return true;
    }

    private static void renderProgressBar(GuiGraphics graphics, Minecraft mc, float progress, long currentTick) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - 48; // 物品栏上方16px

        // 计算透明度
        float alpha = 1.0f;
        if (progress >= 1.0f) {
            // 装填完成
            if (completionTick < 0) {
                completionTick = currentTick; // 记录完成时刻
            }
            long ticksSinceComplete = currentTick - completionTick;

            if (ticksSinceComplete < STAY_VISIBLE_TICKS) {
                // 保持完全可见
                alpha = 1.0f;
            } else {
                // 淡出阶段
                long fadeProgress = ticksSinceComplete - STAY_VISIBLE_TICKS;
                alpha = Math.max(0, 1.0f - (float) fadeProgress / FADE_OUT_TICKS);
                if (alpha <= 0) {
                    completionTick = -1; // 重置状态
                    return; // 完全透明，不绘制
                }
            }
        } else {
            // 装填中
            completionTick = -1; // 重置完成状态

            // 淡入阶段（首次显示）
            long ticksSinceStart = currentTick - lastVisibleTick;
            if (lastVisibleTick == 0 || ticksSinceStart > 100) {
                // P1修复: 首次显示时alpha从0开始，避免突然出现
                lastVisibleTick = currentTick;
                alpha = 0f;
            } else if (ticksSinceStart < FADE_IN_TICKS) {
                alpha = Math.min(1.0f, (float) ticksSinceStart / FADE_IN_TICKS);
            }
        }

        // 应用透明度到所有颜色值
        int alphaInt = (int) (alpha * 255) << 24;
        int alphaMask = 0x00FFFFFF;

        // 绘制进度条背景（纯黑色底）
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT,
                (0xFF000000 & alphaMask) | alphaInt);

        // 绘制进度条（单色橙红）
        int progressWidth = (int) (BAR_WIDTH * progress);
        if (progressWidth > 0) {
            graphics.fill(x, y, x + progressWidth, y + BAR_HEIGHT,
                    (0xFFFF6B3D & alphaMask) | alphaInt);
        }

        // 绘制百分比文本（进度条上方居中）
        String percentText = String.format("%d%%", (int) (progress * 100));
        int textWidth = mc.font.width(percentText);
        int textColor = (0xFFFFFFFF & alphaMask) | alphaInt;
        graphics.drawString(mc.font, percentText,
                x + (BAR_WIDTH - textWidth) / 2,
                y - mc.font.lineHeight - 2,
                textColor, true);
    }
}
