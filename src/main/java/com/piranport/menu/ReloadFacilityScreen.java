package com.piranport.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ReloadFacilityScreen extends AbstractContainerScreen<ReloadFacilityMenu> {
    private static final ResourceLocation LAUNCHER_HINT = ResourceLocation.fromNamespaceAndPath("piranport", "textures/gui/launcher_hint.png");
    private static final ResourceLocation TORPEDO_HINT = ResourceLocation.fromNamespaceAndPath("piranport", "textures/gui/torpedo_hint.png");

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

        // 槽位背景必须与 ReloadFacilityMenu 里的 Slot 坐标完全一致（35/71/107/143）。
        // drawSlotBg 自己会向外扩 1 像素画边框；这里再减 1 会让物品比框洞偏右下 1 像素 ——
        // 输出槽紧挨进度条，偏位最明显，就是玩家看到的「输出格子错位」。
        // Input slot 1 (launcher) — left
        drawSlotBg(g, x + 35, y + 35);
        // Input slot 2 (ammo) — center
        drawSlotBg(g, x + 71, y + 35);
        // Input slot 3 (extra ammo)
        drawSlotBg(g, x + 107, y + 35);

        // Progress arrow between ammo and output
        int progress = menu.getProgress();
        int total = menu.getTotalTime();
        int arrowWidth = (total > 0) ? (int) (16.0 * progress / total) : 0;
        // 进度条夹在「备用弹药槽（107..123）」与「输出槽（143..159）」之间：
        // 起点 125、终点 141，既不压住弹药槽边框，也不伸进输出槽。
        // Arrow background (dark)
        g.fill(x + 125, y + 35, x + 141, y + 44, 0xFF4A4A4A);
        // Arrow fill (green)
        if (arrowWidth > 0) {
            g.fill(x + 125, y + 35, x + 125 + arrowWidth, y + 44, 0xFF55AA55);
        }

        // Output slot — right（Slot 坐标为 143,35）
        drawSlotBg(g, x + 143, y + 35);

        // "+" symbol between launcher and ammo slots
        g.drawString(font, "+", x + 56, y + 38, 0xFF404040, false);

        // Hint icons below slots (32x32 textures scaled to 16x16)
        g.blit(LAUNCHER_HINT, x + 35, y + 55, 0, 0, 16, 16, 16, 16);
        g.blit(TORPEDO_HINT, x + 71, y + 55, 0, 0, 16, 16, 16, 16);
        g.blit(TORPEDO_HINT, x + 107, y + 55, 0, 0, 16, 16, 16, 16);

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
