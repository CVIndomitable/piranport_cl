package com.piranport.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ReloadFacilityScreen extends AbstractContainerScreen<ReloadFacilityMenu> {
    public ReloadFacilityScreen(ReloadFacilityMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 74;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // Background
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        // Input slot 1 (launcher) — left
        drawSlotBg(g, x + 34, y + 34);
        // Input slot 2 (ammo) — center
        drawSlotBg(g, x + 70, y + 34);

        // Progress arrow between ammo and output
        int progress = menu.getProgress();
        int total = menu.getTotalTime();
        int arrowWidth = (total > 0) ? (int) (22.0 * progress / total) : 0;
        // Arrow background (dark)
        g.fill(x + 97, y + 35, x + 119, y + 44, 0xFF4A4A4A);
        // Arrow fill (green)
        if (arrowWidth > 0) {
            g.fill(x + 97, y + 35, x + 97 + arrowWidth, y + 44, 0xFF55AA55);
        }

        // Output slot — right
        drawSlotBg(g, x + 130, y + 34);

        // "+" symbol between launcher and ammo slots
        g.drawString(font, "+", x + 56, y + 38, 0xFF404040, false);

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(g, x + 8 + col * 18, y + 84 + row * 18);
            }
        }
        // Player hotbar
        for (int col = 0; col < 9; col++) {
            drawSlotBg(g, x + 8 + col * 18, y + 142);
        }
    }

    private void drawSlotBg(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
