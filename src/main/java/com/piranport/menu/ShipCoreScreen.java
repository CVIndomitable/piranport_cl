package com.piranport.menu;

import com.piranport.item.ShipCoreItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ShipCoreScreen extends AbstractContainerScreen<ShipCoreMenu> {

    public ShipCoreScreen(ShipCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Main background (Minecraft-style gray)
        gfx.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);

        // 3D border
        gfx.fill(x, y, x + imageWidth - 1, y + 1, 0xFFFFFFFF);
        gfx.fill(x, y, x + 1, y + imageHeight - 1, 0xFFFFFFFF);
        gfx.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF555555);
        gfx.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF555555);

        ShipCoreItem.ShipType type = menu.getShipType();

        // Weapon slot backgrounds
        for (int i = 0; i < type.weaponSlots; i++) {
            drawSlotBg(gfx, x + 7 + i * 18, y + 19);
        }

        // Ammo slot backgrounds
        for (int i = 0; i < type.ammoSlots; i++) {
            drawSlotBg(gfx, x + 7 + i * 18, y + 45);
        }

        // Player inventory slot backgrounds
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(gfx, x + 7 + col * 18, y + 83 + row * 18);
            }
        }

        // Hotbar slot backgrounds
        for (int col = 0; col < 9; col++) {
            drawSlotBg(gfx, x + 7 + col * 18, y + 141);
        }

        // Load bar background
        int barX = x + 8;
        int barY = y + 64;
        int barWidth = 100;
        int barHeight = 5;
        gfx.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xFF373737);
        gfx.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF8B8B8B);

        // Load bar fill
        int currentLoad = menu.getCurrentLoad();
        int maxLoad = menu.getMaxLoad();
        if (maxLoad > 0 && currentLoad > 0) {
            int filledWidth = Math.min((int) ((float) currentLoad / maxLoad * barWidth), barWidth);
            int barColor = currentLoad > maxLoad * 0.8f ? 0xFFFF4444 : 0xFF44CC44;
            gfx.fill(barX, barY, barX + filledWidth, barY + barHeight, barColor);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // Title
        gfx.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        // Section labels
        gfx.drawString(this.font, Component.translatable("container.piranport.weapons"), 8, 10, 0x404040, false);
        gfx.drawString(this.font, Component.translatable("container.piranport.ammo"), 8, 36, 0x404040, false);

        // Load display
        int currentLoad = menu.getCurrentLoad();
        int maxLoad = menu.getMaxLoad();
        String loadText = Component.translatable("container.piranport.load",
                currentLoad, maxLoad).getString();
        gfx.drawString(this.font, loadText, 8, 54, currentLoad > maxLoad ? 0xFF0000 : 0x404040, false);

        // Inventory label
        gfx.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    private void drawSlotBg(GuiGraphics gfx, int x, int y) {
        // Inset slot (dark top-left, light bottom-right)
        gfx.fill(x, y, x + 18, y + 1, 0xFF373737);
        gfx.fill(x, y, x + 1, y + 18, 0xFF373737);
        gfx.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        gfx.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        gfx.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
    }
}
