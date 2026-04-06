package com.piranport.client;

import com.piranport.component.WeaponCooldown;
import com.piranport.item.DepthChargeLauncherItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

/**
 * Renders a durability-bar-style reload progress bar on weapon items (no-GUI mode).
 * Cannons: orange bar. Torpedo launchers: blue bar.
 */
public class WeaponReloadDecorator implements IItemDecorator {

    private static final int BAR_WIDTH = 13;

    // Colors
    private static final int CANNON_COLOR  = 0xFFE05030; // orange-red
    private static final int TORPEDO_COLOR = 0xFF3070C0; // blue
    private static final int DC_COLOR      = 0xFF40A040; // green
    private static final int MISSILE_COLOR = 0xFFD0D040; // yellow for anti-air/missile launchers
    private static final int BG_COLOR      = 0xFF000000; // black background

    @Override
    public boolean render(GuiGraphics gui, Font font, ItemStack stack, int x, int y) {
        WeaponCooldown cd = stack.get(ModDataComponents.WEAPON_COOLDOWN.get());
        if (cd == null) return false;

        Minecraft mc = Minecraft.getInstance();
        long currentTick = mc.level != null ? mc.level.getGameTime() : 0L;

        float fraction = cd.getFraction(currentTick);
        if (fraction <= 0f) return false; // ready — no bar

        int fillColor;
        if (stack.getItem() instanceof TorpedoLauncherItem) {
            fillColor = TORPEDO_COLOR;
        } else if (stack.getItem() instanceof DepthChargeLauncherItem) {
            fillColor = DC_COLOR;
        } else if (stack.getItem() instanceof MissileLauncherItem) {
            fillColor = MISSILE_COLOR;
        } else {
            fillColor = CANNON_COLOR;
        }

        // Progress: 0% at start of cooldown → 100% when ready
        int fillW = Math.round(BAR_WIDTH * (1f - fraction));

        int barX = x + 2;
        int barY = y + 13;

        // Background (2px tall, same as vanilla durability bar)
        gui.fill(barX, barY, barX + BAR_WIDTH, barY + 2, BG_COLOR);
        // Fill (1px tall on top row)
        if (fillW > 0) {
            gui.fill(barX, barY, barX + fillW, barY + 1, fillColor);
        }

        return false; // don't suppress vanilla decorations
    }
}
