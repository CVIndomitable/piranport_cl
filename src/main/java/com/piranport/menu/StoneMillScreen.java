package com.piranport.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class StoneMillScreen extends AbstractContainerScreen<StoneMillMenu> {

    public StoneMillScreen(StoneMillMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 74;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Main background
        gfx.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);

        // 3D border
        gfx.fill(x, y, x + imageWidth - 1, y + 1, 0xFFFFFFFF);
        gfx.fill(x, y, x + 1, y + imageHeight - 1, 0xFFFFFFFF);
        gfx.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF555555);
        gfx.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF555555);

        // Input slots: 2×2 at (7,19),(25,19),(7,37),(25,37)
        drawSlotBg(gfx, x + 7, y + 19);
        drawSlotBg(gfx, x + 25, y + 19);
        drawSlotBg(gfx, x + 7, y + 37);
        drawSlotBg(gfx, x + 25, y + 37);

        // Arrow →
        gfx.fill(x + 46, y + 28, x + 76, y + 30, 0xFF808080);
        gfx.fill(x + 70, y + 25, x + 76, y + 33, 0xFF808080);

        // Output slots: at (77,19),(77,37)
        drawSlotBg(gfx, x + 77, y + 19);
        drawSlotBg(gfx, x + 77, y + 37);

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(gfx, x + 7 + col * 18, y + 85 + row * 18);
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            drawSlotBg(gfx, x + 7 + col * 18, y + 143);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        gfx.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
    }

    private void drawSlotBg(GuiGraphics gfx, int x, int y) {
        com.piranport.client.GuiHelper.drawSlotBg(gfx, x, y);
    }
}
