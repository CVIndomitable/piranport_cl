package com.piranport.menu;

import com.piranport.config.ModClientConfig;
import com.piranport.item.ShipCoreItem;
import com.piranport.network.AutoLaunchTogglePayload;
import com.piranport.network.OpenFlightGroupPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class ShipCoreScreen extends AbstractContainerScreen<ShipCoreMenu> {

    private Button autoLaunchButton;

    public ShipCoreScreen(ShipCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 210;   // +22 vs previous 188 for ship config section
        this.inventoryLabelY = 116; // +22 vs previous 94
    }

    @Override
    protected void init() {
        super.init();
        // "编组" button — opens FlightGroup GUI (top-right area of screen)
        // Only shown when flightGroupEnabled = true in piranport-client.toml
        if (ModClientConfig.FLIGHT_GROUP_ENABLED.get()) {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("container.piranport.flight_groups"),
                    btn -> PacketDistributor.sendToServer(
                            new OpenFlightGroupPayload(this.menu.getCoreSlot()))
            ).bounds(leftPos + 118, topPos + 2, 50, 10).build());
        }

        // "战斗机自动升空" toggle button (ship config section)
        autoLaunchButton = Button.builder(
                getAutoLaunchMessage(),
                btn -> PacketDistributor.sendToServer(
                        new AutoLaunchTogglePayload(this.menu.getCoreSlot()))
        ).bounds(leftPos + 7, topPos + 105, 130, 10).build();
        this.addRenderableWidget(autoLaunchButton);
    }

    private Component getAutoLaunchMessage() {
        return Component.translatable(menu.isAutoLaunch()
                ? "container.piranport.auto_launch_on"
                : "container.piranport.auto_launch_off");
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        ShipCoreItem.ShipType type = menu.getShipType();
        boolean locked = menu.isTransformed();

        // Main background
        gfx.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);

        // 3D border
        gfx.fill(x, y, x + imageWidth - 1, y + 1, 0xFFFFFFFF);
        gfx.fill(x, y, x + 1, y + imageHeight - 1, 0xFFFFFFFF);
        gfx.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF555555);
        gfx.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF555555);

        // Weapon slot backgrounds
        for (int i = 0; i < type.weaponSlots; i++) {
            drawSlotBg(gfx, x + 7 + i * 18, y + 19);
        }

        // Ammo slot backgrounds
        for (int i = 0; i < type.ammoSlots; i++) {
            drawSlotBg(gfx, x + 7 + i * 18, y + 45);
        }

        // Enhancement slot backgrounds
        for (int i = 0; i < type.enhancementSlots; i++) {
            drawSlotBg(gfx, x + 7 + i * 18, y + 71);
        }

        // Thin separator before ship config section
        gfx.fill(x + 7, y + 95, x + imageWidth - 7, y + 96, 0xFF888888);

        // Player inventory slot backgrounds (shifted +22)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(gfx, x + 7 + col * 18, y + 127 + row * 18);
            }
        }

        // Hotbar slot backgrounds (shifted +22)
        for (int col = 0; col < 9; col++) {
            drawSlotBg(gfx, x + 7 + col * 18, y + 185);
        }

        // Load bar background (moved down to y+91)
        int barX = x + 8;
        int barY = y + 91;
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

        // Locked overlay for weapon and enhancement slots when transformed
        if (locked) {
            for (int i = 0; i < type.weaponSlots; i++) {
                gfx.fill(x + 8 + i * 18, y + 20, x + 8 + i * 18 + 16, y + 20 + 16, 0x88000000);
            }
            for (int i = 0; i < type.enhancementSlots; i++) {
                gfx.fill(x + 8 + i * 18, y + 72, x + 8 + i * 18 + 16, y + 72 + 16, 0x88000000);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // Title
        gfx.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        // Section labels
        gfx.drawString(this.font, Component.translatable("container.piranport.weapons"), 8, 10, 0x404040, false);
        gfx.drawString(this.font, Component.translatable("container.piranport.ammo"), 8, 36, 0x404040, false);
        gfx.drawString(this.font, Component.translatable("container.piranport.enhancement"), 8, 62, 0x404040, false);

        // Load display (to the right of bar)
        int currentLoad = menu.getCurrentLoad();
        int maxLoad = menu.getMaxLoad();
        String loadText = Component.translatable("container.piranport.load",
                currentLoad, maxLoad).getString();
        gfx.drawString(this.font, loadText, 112, 89, currentLoad > maxLoad ? 0xFF0000 : 0x404040, false);

        // Ship config section label
        gfx.drawString(this.font, Component.translatable("container.piranport.ship_config"), 8, 97, 0x404040, false);

        // Inventory label (shifted +22)
        gfx.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    private void drawSlotBg(GuiGraphics gfx, int x, int y) {
        com.piranport.client.GuiHelper.drawSlotBg(gfx, x, y);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // Keep button message in sync with server state
        if (autoLaunchButton != null) {
            autoLaunchButton.setMessage(getAutoLaunchMessage());
        }
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
    }
}
