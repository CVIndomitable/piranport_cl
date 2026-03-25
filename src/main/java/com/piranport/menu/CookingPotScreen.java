package com.piranport.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CookingPotScreen extends AbstractContainerScreen<CookingPotMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("piranport", "textures/gui/cooking_pot.png");

    public CookingPotScreen(CookingPotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 74;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        // Draw input slots (3x3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlotBg(guiGraphics, x + 7 + col * 18, y + 16 + row * 18);
            }
        }
        // Arrow
        int progress = menu.getProgress();
        int total = menu.getTotalTime();
        int arrowWidth = (total > 0) ? (int) (22.0 * progress / total) : 0;
        guiGraphics.fill(x + 98, y + 35, x + 98 + arrowWidth, y + 44, 0xFF55AA55);

        // Output slot
        drawSlotBg(guiGraphics, x + 123, y + 34);

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(guiGraphics, x + 8 + col * 18, y + 84 + row * 18);
            }
        }
        // Player hotbar
        for (int col = 0; col < 9; col++) {
            drawSlotBg(guiGraphics, x + 8 + col * 18, y + 142);
        }
    }

    private void drawSlotBg(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
