package com.piranport.client;

import com.piranport.combat.TransformationManager;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.WeaponCooldown;
import com.piranport.item.ShipCoreItem;
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
                int barY = hasDurability ? (y + 11) : (y + 13);

                gui.fill(barX, barY, barX + BAR_WIDTH, barY + 2, BG_COLOR);
                if (fillW > 0) {
                    gui.fill(barX, barY, barX + fillW, barY + 1, color);
                }
                return false;
            }
        }

        // 2. Phase 4: all cannons auto-resupply — show empty bar when no matching ammo in inventory
        if (stack.getItem() instanceof com.piranport.artillery.ArtilleryItem) {
            if (!hasMatchingAmmoInInventory(stack)) {
                boolean hasDurability = stack.isDamageableItem();
                int barX = x + 2;
                int barY = hasDurability ? (y + 11) : (y + 13);
                gui.fill(barX, barY, barX + BAR_WIDTH, barY + 2, BG_COLOR);
            }
            return false;
        }

        // 3. Manual-reload launchers (torpedo, missile) — show empty bar when not loaded
        boolean showEmptyBar = stack.getItem() instanceof TorpedoLauncherItem
                || stack.is(ModItems.SY1_LAUNCHER.get())
                || stack.is(ModItems.MK14_HARPOON_LAUNCHER.get())
                || stack.is(ModItems.SHIP_ROCKET_LAUNCHER.get());

        if (showEmptyBar) {
            LoadedAmmo ammo = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            if (!ammo.hasAmmo()) {
                boolean hasDurability = stack.isDamageableItem();
                int barX = x + 2;
                int barY = hasDurability ? (y + 11) : (y + 13);
                gui.fill(barX, barY, barX + BAR_WIDTH, barY + 2, BG_COLOR);
                return false;
            }
        }

        return false;
    }

    private static boolean hasMatchingAmmoInInventory(ItemStack weapon) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return true;
        Inventory inv = player.getInventory();
        for (ItemStack s : inv.items) {
            if (!s.isEmpty() && ShipCoreItem.matchesCaliber(s, weapon)) return true;
        }
        ItemStack oh = inv.offhand.get(0);
        if (!oh.isEmpty() && ShipCoreItem.matchesCaliber(oh, weapon)) return true;
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
