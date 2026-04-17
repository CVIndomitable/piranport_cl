package com.piranport.client;

import com.piranport.combat.TransformationManager;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SlotCooldowns;
import com.piranport.item.AircraftItem;
import com.piranport.item.CannonItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.client.IItemDecorator;

/**
 * Phase 25: draws stacked reload-progress bars directly on the ship-core item icon.
 *
 * Layout inside the 16×16 icon slot (x/y = top-left of the slot):
 *   y+2  slot 0  ── 1 px tall, x+2 to x+13 (12 px wide)
 *   y+4  slot 1
 *   y+6  slot 2
 *   …
 *
 * Colors by weapon type:
 *   Cannon        loading #E05030  ready #FF6A3D
 *   Torpedo       loading #3070C0  ready #50A0FF
 *   Aircraft      loading #30A050  ready #50D070
 *   Background (unfilled)  #3C3C3C
 */
public class ReloadBarDecorator implements IItemDecorator {

    // -- cannon (orange-red) --
    private static final int CANNON_LOADING = 0xFFE05030;
    private static final int CANNON_READY   = 0xFFFF6A3D;

    // -- torpedo (blue) --
    private static final int TORPEDO_LOADING = 0xFF3070C0;
    private static final int TORPEDO_READY   = 0xFF50A0FF;

    // -- aircraft (green) --
    private static final int AIRCRAFT_LOADING = 0xFF30A050;
    private static final int AIRCRAFT_READY   = 0xFF50D070;

    // -- bar background --
    private static final int BG = 0xFF3C3C3C;

    private static final int BAR_X_OFFSET = 2;   // from slot left edge
    private static final int BAR_Y_START  = 2;   // first bar top edge from slot top
    private static final int BAR_ROW_H    = 2;   // 1 px bar + 1 px gap
    private static final int BAR_MAX_W    = 12;  // 12 px wide (x+2 … x+13)

    @Override
    public boolean render(GuiGraphics gui, Font font, ItemStack stack, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return false;

        if (!(stack.getItem() instanceof ShipCoreItem sci)) return false;
        if (!TransformationManager.isTransformed(stack)) return false;

        ShipCoreItem.ShipType type = sci.getShipType();
        ItemContainerContents contents = stack.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(type.totalSlots(), ItemStack.EMPTY);
        contents.copyInto(items);

        long currentTick = mc.level != null ? mc.level.getGameTime() : 0L;
        SlotCooldowns cooldowns = stack.getOrDefault(ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);

        int rendered = 0;
        for (int i = 0; i < type.weaponSlots; i++) {
            ItemStack weapon = items.get(i);
            if (weapon.isEmpty()) continue;

            int barX = x + BAR_X_OFFSET;
            int barY = y + BAR_Y_START + rendered * BAR_ROW_H;

            // Guard: don't overflow the 16 px icon height (reserve bottom 2 px for vanilla bar)
            if (barY + 1 >= y + 14) break;

            // Background
            gui.fill(barX, barY, barX + BAR_MAX_W, barY + 1, BG);

            float cd = cooldowns.getFraction(i, currentTick);

            // Manual-reload launcher with no loaded ammo can't actually fire —
            // showing a moving cooldown bar would mislead the player.
            // Torpedo launcher also needs a reload item to auto-reload from inventory;
            // anti-ship/rocket missiles only reload at the Reload Facility block.
            boolean launcherCannotReload = false;
            if (weapon.getItem() instanceof TorpedoLauncherItem) {
                LoadedAmmo loaded = weapon.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
                if (!loaded.hasAmmo() && !TransformationManager.hasTorpedoReloadEquipped(player, stack)) {
                    launcherCannotReload = true;
                }
            } else if (weapon.getItem() instanceof MissileLauncherItem ml && ml.isManualReload()) {
                LoadedAmmo loaded = weapon.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
                if (!loaded.hasAmmo() && !TransformationManager.hasTorpedoReloadEquipped(player, stack)) {
                    launcherCannotReload = true;
                }
            }

            int loadingColor, readyColor;
            if (weapon.getItem() instanceof TorpedoLauncherItem) {
                loadingColor = TORPEDO_LOADING;
                readyColor   = TORPEDO_READY;
            } else if (weapon.getItem() instanceof AircraftItem) {
                loadingColor = AIRCRAFT_LOADING;
                readyColor   = AIRCRAFT_READY;
            } else {
                // cannons, missiles, and anything else
                loadingColor = CANNON_LOADING;
                readyColor   = CANNON_READY;
            }

            if (launcherCannotReload) {
                // empty bar: background already drawn above
            } else if (cd <= 0f) {
                // Ready: solid bright color — but only if ammo is actually available.
                // For cannons, verify matching ammo exists in the core's ammo slots;
                // otherwise leave the bar empty (background only) so the player can see
                // they can't fire yet.
                if (weapon.getItem() instanceof CannonItem && !hasCannonAmmoInPool(weapon, items, type)) {
                    // empty bar: background already drawn above
                } else {
                    gui.fill(barX, barY, barX + BAR_MAX_W, barY + 1, readyColor);
                }
            } else {
                // Loading: partial fill shows reload progress (1 - remaining fraction)
                int fillW = (int) (BAR_MAX_W * (1f - cd));
                if (fillW > 0) {
                    gui.fill(barX, barY, barX + fillW, barY + 1, loadingColor);
                }
            }

            rendered++;
        }

        // Return false so vanilla decorations (durability bar etc.) still render.
        return false;
    }

    private static boolean hasCannonAmmoInPool(ItemStack weapon,
                                               net.minecraft.core.NonNullList<ItemStack> items,
                                               ShipCoreItem.ShipType type) {
        int ammoEnd = type.weaponSlots + type.ammoSlots;
        for (int i = type.weaponSlots; i < ammoEnd; i++) {
            ItemStack ammo = items.get(i);
            if (!ammo.isEmpty() && ShipCoreItem.matchesCaliber(ammo, weapon)) {
                return true;
            }
        }
        return false;
    }
}
