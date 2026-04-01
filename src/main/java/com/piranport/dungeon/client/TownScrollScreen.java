package com.piranport.dungeon.client;

import com.piranport.dungeon.network.TownScrollUsePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Confirmation screen for using the town scroll.
 */
public class TownScrollScreen extends Screen {

    public TownScrollScreen() {
        super(Component.translatable("gui.piranport.town_scroll.title"));
    }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2;
        int cy = height / 2;

        addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.town_scroll.confirm"),
                btn -> {
                    PacketDistributor.sendToServer(new TownScrollUsePayload());
                    onClose();
                }
        ).bounds(cx - 55, cy + 10, 50, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.town_scroll.cancel"),
                btn -> onClose()
        ).bounds(cx + 5, cy + 10, 50, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int cy = height / 2;

        gfx.fill(cx - 80, cy - 30, cx + 80, cy + 40, 0xCC1A1A1A);
        gfx.renderOutline(cx - 80, cy - 30, 160, 70, 0xFFFFD700);

        gfx.drawCenteredString(font,
                Component.translatable("gui.piranport.town_scroll.question"),
                cx, cy - 20, 0xFFFFFFFF);

        super.render(gfx, mouseX, mouseY, partialTick);
    }
}
