package com.piranport.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BlueprintChestScreen extends AbstractContainerScreen<BlueprintChestMenu> {
    public BlueprintChestScreen(BlueprintChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = 120;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        g.drawString(font, Component.translatable("gui.piranport.blueprint_chest.cache"),
                x + 8, y + 8, 0xFF404040, false);
        for (int col = 0; col < 9; col++) {
            drawSlotBg(g, x + 7 + col * 18, y + 19);
        }

        g.drawString(font, Component.translatable("gui.piranport.blueprint_chest.paper"),
                x + 70, y + 43, 0xFF404040, false);
        drawSlotBg(g, x + 79, y + 53);

        g.drawString(font, Component.translatable("gui.piranport.blueprint_chest.copy"),
                x + 8, y + 76, 0xFF404040, false);
        for (int col = 0; col < 9; col++) {
            drawSlotBg(g, x + 7 + col * 18, y + 87);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(g, x + 7 + col * 18, y + 131 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBg(g, x + 7 + col * 18, y + 189);
        }
    }

    private void drawSlotBg(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF373737);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFFE0E0E0);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
