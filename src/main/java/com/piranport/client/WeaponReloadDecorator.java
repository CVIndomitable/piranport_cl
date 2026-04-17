package com.piranport.client;

import com.piranport.combat.TransformationManager;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.WeaponCooldown;
import com.piranport.config.ModCommonConfig;
import com.piranport.item.CannonItem;
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
 * Renders a durability-bar-style reload progress bar on weapon items (no-GUI mode).
 * Color transitions red → yellow → green as cooldown progresses, like vanilla durability.
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
                // progress: 0 = just started cooldown, 1 = about to be ready
                float progress = 1f - fraction;

                // Vanilla-style color: red(0) → yellow(0.5) → green(1)
                int color = Mth.hsvToRgb(progress / 3f, 1f, 1f) | 0xFF000000;

                int fillW = Math.round(BAR_WIDTH * progress);

                int barX = x + 2;
                int barY = y + 13;

                gui.fill(barX, barY, barX + BAR_WIDTH, barY + 2, BG_COLOR);
                if (fillW > 0) {
                    gui.fill(barX, barY, barX + fillW, barY + 1, color);
                }
                return false;
            }
        }

        // 2. Empty bar for manual-reload weapons when not loaded
        boolean showEmptyBar = false;
        if (!ModCommonConfig.AUTO_RESUPPLY_ENABLED.get()) {
            // Non-small-caliber cannons and torpedo launchers use manual reload
            showEmptyBar = (stack.getItem() instanceof com.piranport.item.CannonItem
                    && !stack.is(ModItems.SMALL_GUN.get()) && !stack.is(ModItems.SINGLE_SMALL_GUN.get()))
                    || stack.getItem() instanceof TorpedoLauncherItem;
        }
        // Manual-reload missiles (anti-ship/rocket) always need LoadedAmmo regardless of config
        if (!showEmptyBar) {
            showEmptyBar = stack.is(ModItems.SY1_LAUNCHER.get())
                    || stack.is(ModItems.MK14_HARPOON_LAUNCHER.get())
                    || stack.is(ModItems.SHIP_ROCKET_LAUNCHER.get());
        }

        if (showEmptyBar) {
            LoadedAmmo ammo = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            if (!ammo.hasAmmo()) {
                int barX = x + 2;
                int barY = y + 13;
                gui.fill(barX, barY, barX + BAR_WIDTH, barY + 2, BG_COLOR);
                return false;
            }
        }

        // 3. Empty bar for auto-reload cannons when no matching ammo is in inventory.
        // Small cannons always auto-reload; other cannons auto-reload when AUTO_RESUPPLY_ENABLED.
        if (stack.getItem() instanceof CannonItem) {
            boolean isAutoReloadCannon = stack.is(ModItems.SMALL_GUN.get())
                    || stack.is(ModItems.SINGLE_SMALL_GUN.get())
                    || ModCommonConfig.AUTO_RESUPPLY_ENABLED.get();
            if (isAutoReloadCannon && !hasMatchingAmmoInInventory(stack)) {
                int barX = x + 2;
                int barY = y + 13;
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
