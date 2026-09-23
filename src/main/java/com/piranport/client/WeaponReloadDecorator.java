package com.piranport.client;

import com.piranport.combat.TransformationManager;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.WeaponCooldown;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipCoreCombat;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

/**
 * 在武器物品图标上绘制耐久条样式的装填进度条。
 * 冷却进度红→黄→绿渐变，类似原版耐久条。
 */
public class WeaponReloadDecorator implements IItemDecorator {

    private static final int BAR_WIDTH = 13;
    private static final int BG_COLOR  = 0xFF000000;
    /** 已装填指示条：满绿，与原版"附魔光效 / 已充能"观感一致。 */
    private static final int READY_COLOR = 0xFF3FD23F;

    @Override
    public boolean render(GuiGraphics gui, Font font, ItemStack stack, int x, int y) {
        // Torpedo launcher without a way to reload: cooldown bar is misleading
        // (player would think it's reloading), so always show the empty bar instead.
        boolean torpedoCannotReload = false;
        if (stack.getItem() instanceof TorpedoLauncherItem) {
            LoadedAmmo loaded = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            if (!loaded.hasAmmo() && !hasTorpedoReloadEquipped()) {
                torpedoCannotReload = true;
            }
        }

        // 防空导弹发射器始终满装填：弹药直接从背包消耗，不存在 LOADED_AMMO 状态，
        // 也没有"空膛"可言——画空膛条等于谎报状态（见 docs/策划决策/武器/鱼雷-再装填机制.md）。
        // 冷却条仍要走第 1 段，否则玩家看不到 60s 装填进度。
        boolean isAutoReloadMissile = isAutoReloadMissile(stack);

        // 1. Cooldown bar takes priority (except for torpedo launchers that can't reload)
        WeaponCooldown cd = stack.get(ModDataComponents.WEAPON_COOLDOWN.get());
        if (cd != null && !torpedoCannotReload) {
            Minecraft mc = Minecraft.getInstance();
            long currentTick = mc.level != null ? mc.level.getGameTime() : 0L;

            float fraction = cd.getFraction(currentTick);
            if (fraction > 0f) {
                float progress = 1f - fraction;

                int color = Mth.hsvToRgb(progress / 3f, 1f, 1f) | 0xFF000000;

                int fillW = Math.round(BAR_WIDTH * progress);

                boolean hasDurability = stack.isDamageableItem();
                int barX = x + 2;
                // 耐久条在 y+13，避免遮挡：耐久物品装填条下移到 y+14
                int barY = hasDurability ? (y + 14) : (y + 13);

                gui.fill(barX, barY, barX + BAR_WIDTH, barY + 2, BG_COLOR);
                if (fillW > 0) {
                    gui.fill(barX, barY, barX + fillW, barY + 1, color);
                }
                return false;
            }
        }

        // 2. Phase 4: all cannons auto-resupply — show empty bar when no matching ammo in inventory
        if (stack.getItem() instanceof com.piranport.artillery.ArtilleryItem) {
            LoadedAmmo loaded = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            if (!loaded.hasAmmo() && !hasMatchingAmmoInInventory(stack)) {
                boolean hasDurability = stack.isDamageableItem();
                int barX = x + 2;
                int barY = hasDurability ? (y + 14) : (y + 13);
                gui.fill(barX, barY, barX + BAR_WIDTH, barY + 2, BG_COLOR);
            }
            return false;
        }

        // 3. Manual-reload launchers (torpedo, missile) — 空膛画黑条、满膛画绿条
        //    防空导弹不在此列：它走自动装填，不该有"空膛"态。
        boolean showEmptyBar = !isAutoReloadMissile
                && (stack.getItem() instanceof TorpedoLauncherItem
                || stack.is(ModItems.SY1_LAUNCHER.get())
                || stack.is(ModItems.MK14_HARPOON_LAUNCHER.get())
                || stack.is(ModItems.SHIP_ROCKET_LAUNCHER.get()));

        if (showEmptyBar) {
            LoadedAmmo ammo = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            if (!ammo.hasAmmo()) {
                // 空膛：整条纯黑，同时把耐久条盖掉（黑底上露出原版彩色耐久条也很像"装了点什么"）
                drawBar(gui, stack, x, y, BAR_WIDTH, BG_COLOR);
                return false;
            }
            // 已装填：必须画一条明确的"满"指示。
            // WHY：装填设施出来的发射器身上只有 LOADED_AMMO，没有 WEAPON_COOLDOWN，
            // 因此永远走不到第 1 段的冷却条分支；上面第 3 段在不装填时又是直接 return。
            // 结果就是"已装填"比"空膛"画得还少（什么都不画），玩家看图标只会以为没装上，
            // 而 tooltip（ClientItemHooks.weapon_ready）却报"已装填"——两者对不上。
            // 这里补一条满绿条，让"已装填"在图标上也有正向信号。
            drawBar(gui, stack, x, y, BAR_WIDTH, READY_COLOR);
            return false;
        }

        return false;
    }

    /**
     * 在物品图标下方画一条水平指示条。
     *
     * <p>条的位置与第 1 段冷却条保持一致：耐久物品下移到 y+14，避开原版 y+13 的耐久条；
     * 非耐久物品用 y+13。宽度 13、高度 2 与原版耐久条同规格。
     */
    private static void drawBar(GuiGraphics gui, ItemStack stack, int x, int y, int width, int color) {
        int barX = x + 2;
        int barY = stack.isDamageableItem() ? (y + 14) : (y + 13);
        gui.fill(barX, barY, barX + width, barY + 2, color);
    }

    /** 是否为自动装填导弹（防空导弹）：弹药直接从背包消耗，无 LOADED_AMMO 状态。 */
    private static boolean isAutoReloadMissile(ItemStack stack) {
        return stack.getItem() instanceof MissileLauncherItem ml && !ml.isManualReload();
    }

    private static boolean hasMatchingAmmoInInventory(ItemStack weapon) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return true;
        Inventory inv = player.getInventory();
        for (ItemStack s : inv.items) {
            if (!s.isEmpty() && ShipCoreCombat.matchesCaliber(s, weapon, player.level())) return true;
        }
        ItemStack oh = inv.offhand.get(0);
        if (!oh.isEmpty() && ShipCoreCombat.matchesCaliber(oh, weapon, player.level())) return true;
        return false;
    }

    private static boolean hasTorpedoReloadEquipped() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        ItemStack core = TransformationManager.findTransformedCore(player);
        if (core.isEmpty()) return false;
        return TransformationManager.hasTorpedoReloadEquipped(player, core);
    }
}
